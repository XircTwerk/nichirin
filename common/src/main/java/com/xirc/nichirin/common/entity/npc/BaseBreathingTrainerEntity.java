package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.item.katana.Katana;
import com.xirc.nichirin.common.network.s2c.OpenTrainerDialoguePacket;
import com.xirc.nichirin.common.system.NPCResourceManager;
import com.xirc.nichirin.common.system.movement.EntityMovement;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import com.xirc.nichirin.common.entity.ai.SmartTrainerAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public abstract class BaseBreathingTrainerEntity extends PathfinderMob implements MovesetCapableNPC {

    private static final EntityDataAccessor<String>  CURRENT_ANIMATION =
            SynchedEntityData.defineId(BaseBreathingTrainerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   ANIMATION_SPEED   =
            SynchedEntityData.defineId(BaseBreathingTrainerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ANIMATION_RESET   =
            SynchedEntityData.defineId(BaseBreathingTrainerEntity.class, EntityDataSerializers.BOOLEAN);

    /** Trainer health outside combat (also the base MAX_HEALTH attribute) — config-tunable. */
    protected static float peacefulHp() { return (float) NichirinModConfig.get().combat.npcPeacefulHealth; }
    /** Duel boss HP — config-tunable. */
    protected float duelHp() { return NichirinModConfig.get().combat.npcDuelHealth; }
    /**
     * Health floor that ends a non-lethal spar (trainer can't be killed during a spar).
     * Breathing trainers end the spar at 20% of the duel HP — the trainer "yields" rather than dying.
     */
    protected float duelWinHpThreshold() { return duelHp() * 0.20f; }
    /**
     * Minimum health a trainer leaves a defeated opponent at (mercy). For breathing trainer
     * spars this is 10% of the player's max — the trainer holds back to that floor.
     */
    protected float playerDuelMinHp(Player p) { return p.getMaxHealth() * 0.10f; }
    /** Ticks before a trainer can be challenged again after a duel — config-tunable (seconds × 20). */
    protected int duelCooldownDuration() { return NichirinModConfig.get().combat.npcDuelCooldownSeconds * 20; }

    public enum TrainerMode { PEACEFUL, DUELING, SELF_DEFENSE }

    public enum DuelDifficulty { EASY, MEDIUM, HARD }

    @Getter protected final TrainerType trainerType;
    @Getter private TrainerMode mode           = TrainerMode.PEACEFUL;
    private UUID        duelPlayerId   = null;
    @Getter private int duelCooldownTicks = 0;

    @Getter private DuelDifficulty duelDifficulty = DuelDifficulty.EASY;
    @Getter private boolean hasBeatenTrainer = false;
    private ServerBossEvent bossBar = null;

    // Provoked combat — tracks who hit us while peaceful so we can warn then fight
    private UUID provokedWarnedBy = null;
    // Grace period (ticks) before a self-defense fight reverts to peaceful after the target is lost.
    // Prevents the "Are you sure?" warning from re-arming every time the target is briefly out of range.
    /** Self-defense grace duration (ticks) — config-tunable. */
    protected static int selfDefenseGraceDuration() { return NichirinModConfig.get().combat.npcSelfDefenseGraceTicks; }
    private int selfDefenseGraceTicks = 0;

    protected AbstractMoveset moveset;
    protected float maxBreathGauge         = 100.0f;
    protected float maxStamina             = 100.0f;
    protected float breathRegenMultiplier  = 2.0f;
    protected float staminaRegenMultiplier = 1.5f;

    protected final Set<Integer> blacklistedMoves = new HashSet<>();
    private static final Map<UUID, Map<Integer, Long>> trainerCooldowns = new HashMap<>();

    /** Tracks which players are currently in a formal duel so other trainers can block interaction. */
    private static final Map<UUID, UUID> playersInDuel = new HashMap<>(); // playerUUID → trainerUUID

    protected BaseBreathingTrainerEntity(EntityType<? extends BaseBreathingTrainerEntity> type,
                                          Level level, TrainerType trainerType) {
        super(type, level);
        this.trainerType = trainerType;
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURRENT_ANIMATION, "");
        builder.define(ANIMATION_SPEED, 1.0f);
        builder.define(ANIMATION_RESET, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,           peacefulHp())
                .add(Attributes.MOVEMENT_SPEED,       0.28)
                .add(Attributes.ATTACK_DAMAGE,        5.0)
                .add(Attributes.ARMOR,                6.0)
                .add(Attributes.FOLLOW_RANGE,         32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.STEP_HEIGHT,          1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SmartTrainerAttackGoal(this, 1.2, true));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE) && super.canUse();
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, TempleDemonEntity.class, true) {
            @Override
            public boolean canUse() {
                if (mode == TrainerMode.DUELING) return false;
                return super.canUse();
            }
            @Override
            public void start() {
                super.start();
                if (mode == TrainerMode.PEACEFUL && getTarget() instanceof TempleDemonEntity) {
                    mode = TrainerMode.SELF_DEFENSE;
                    duelDifficulty = DuelDifficulty.HARD;
                }
            }
        });
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType spawnType, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data);
        equipArmor();
        return result;
    }

    protected void equipArmor() {}


    public void setAnimation(String animationID, float speed) {
        entityData.set(ANIMATION_RESET, entityData.get(CURRENT_ANIMATION).equals(animationID));
        entityData.set(CURRENT_ANIMATION, animationID);
        entityData.set(ANIMATION_SPEED, speed);
    }

    public void stopAnimation() { setAnimation("", 1.0f); }

    public String getCurrentAnimation()  { return entityData.get(CURRENT_ANIMATION); }
    public float  getAnimationSpeed()    { return entityData.get(ANIMATION_SPEED); }
    public boolean wasAnimationReset()   { return entityData.get(ANIMATION_RESET); }


    @Override public AbstractMoveset getMoveset()                { return moveset; }
    @Override public void setMoveset(AbstractMoveset m)          { moveset = m; }
    @Override public UUID getEntityUUID()                        { return getUUID(); }
    @Override public Level getEntityLevel()                      { return level(); }
    @Override public LivingEntity asLivingEntity()               { return this; }

    @Override
    public boolean canUseMove(int moveIndex) {
        if (isMoveBlacklisted(moveIndex) || moveset == null) return false;
        AbstractMoveset.MoveConfiguration cfg = moveset.getMove(moveIndex);
        if (cfg == null) return false;
        Map<Integer, Long> cooldowns = trainerCooldowns.get(getUUID());
        if (cooldowns != null) {
            Long end = cooldowns.get(moveIndex);
            if (end != null && level().getGameTime() < end) return false;
        }
        if (cfg.hasBreathCost() && getBreathGauge() < cfg.getBreathCostOrDefault(0f)) return false;
        return true;
    }

    @Override
    public void performMovesetMove(int moveIndex) {
        if (!canUseMove(moveIndex)) return;
        AbstractMoveset.MoveConfiguration cfg = moveset.getMove(moveIndex);
        if (cfg == null) return;
        if (cfg.hasBreathCost()) NPCResourceManager.consumeBreath(this, cfg.getBreathCostOrDefault(0f));
        int cd = cfg.getCooldownOrDefault(0);
        if (cd > 0) {
            // Scale cooldown by difficulty — Hard attacks much more frequently
            int scaledCd = Math.max(1, (int)(cd * getDifficultyCooldownScale()));
            trainerCooldowns.computeIfAbsent(getUUID(), k -> new HashMap<>())
                    .put(moveIndex, level().getGameTime() + scaledCd);
        }
        moveset.performMove(this, moveIndex);
    }

    protected float getDifficultyCooldownScale() {
        return 1.0f;
    }

    @Override
    public void triggerMovesetAnimation(String animationName) { setAnimation(animationName, 1.0f); }

    public boolean canUseRightClickMove(boolean crouching) {
        if (moveset == null) return false;
        AbstractMoveset.MoveConfiguration cfg = crouching
                ? moveset.getCrouchRightClickConfiguration()
                : moveset.getRightClickConfiguration();
        if (cfg == null) return false;
        int index = crouching ? -2 : -1;
        Map<Integer, Long> cooldowns = trainerCooldowns.get(getUUID());
        if (cooldowns != null) {
            Long end = cooldowns.get(index);
            if (end != null && level().getGameTime() < end) return false;
        }
        if (cfg.hasBreathCost() && getBreathGauge() < cfg.getBreathCostOrDefault(0f)) return false;
        return true;
    }

    public void performRightClickMove(boolean crouching) {
        if (!canUseRightClickMove(crouching)) return;
        AbstractMoveset.MoveConfiguration cfg = crouching
                ? moveset.getCrouchRightClickConfiguration()
                : moveset.getRightClickConfiguration();
        if (cfg == null) return;
        if (cfg.hasBreathCost()) NPCResourceManager.consumeBreath(this, cfg.getBreathCostOrDefault(0f));
        int index = crouching ? -2 : -1;
        int cd = cfg.getCooldownOrDefault(0);
        if (cd <= 0) cd = cfg.getWindupOrDefault(5) + cfg.getDurationOrDefault(10) + cfg.getRecoveryOrDefault(10);
        int scaledCd = Math.max(1, (int)(cd * getDifficultyCooldownScale()));
        trainerCooldowns.computeIfAbsent(getUUID(), k -> new HashMap<>())
                .put(index, level().getGameTime() + scaledCd);
        moveset.handleRightClick(this, crouching);
    }

    @Override public int   getBloodPoints()                    { return 0; }
    @Override public void  setBloodPoints(int v)               {}
    @Override public int   getMaxBloodPoints()                 { return 0; }
    @Override public boolean canRegenBlood()                   { return false; }
    @Override public float getBloodRegenMultiplier()           { return 0f; }

    @Override public float getBreathGauge()                    { return NPCResourceManager.getBreathGauge(getUUID(), maxBreathGauge); }
    @Override public void  setBreathGauge(float v)             { NPCResourceManager.setBreathGauge(getUUID(), v, maxBreathGauge); }
    @Override public float getMaxBreathGauge()                 { return maxBreathGauge; }
    @Override public float getBreathRegenMultiplier()          { return breathRegenMultiplier; }

    @Override public float getStamina()                        { return NPCResourceManager.getStamina(getUUID(), maxStamina); }
    @Override public void  setStamina(float v)                 { NPCResourceManager.setStamina(getUUID(), v, maxStamina); }
    @Override public float getMaxStamina()                     { return maxStamina; }
    @Override public float getStaminaRegenMultiplier()         { return staminaRegenMultiplier; }

    @Override public boolean canDoubleJump()                   { return !onGround() && NPCResourceManager.canDoubleJump(getUUID()); }
    @Override public void    markDoubleJumped()                { NPCResourceManager.markDoubleJumped(getUUID()); }
    @Override public void    resetDoubleJump()                 { NPCResourceManager.resetDoubleJump(getUUID()); }
    // isSprinting() / setSprinting() inherited from LivingEntity

    @Override public float getAggression()                     { return 0.6f; }
    @Override public float getDamageMultiplier()               { return 1.0f; }
    @Override public float getAttackSpeedMultiplier()          { return 1.0f; }
    @Override public float getMoveSpeedMultiplier()            { return 1.0f; }
    @Override public Set<Integer> getBlacklistedMoves()        { return blacklistedMoves; }

    @Override
    public void tickMovesetSystems() {
        if (!level().isClientSide) NPCResourceManager.tickNPC(this);
    }


    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.sidedSuccess(true);

        // Don't open dialogue if the player is holding a katana (they're likely mid-attack)
        if (player.getMainHandItem().getItem() instanceof Katana) {
            return InteractionResult.PASS;
        }

        if (mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE) {
            sp.displayClientMessage(
                    Component.literal("The spar is not over!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return InteractionResult.CONSUME;
        }

        // Block interaction if this player is sparring with a different trainer
        UUID activeDuelTrainer = playersInDuel.get(sp.getUUID());
        if (activeDuelTrainer != null && !activeDuelTrainer.equals(getUUID())) {
            sp.displayClientMessage(
                    Component.literal("Finish your current spar first.")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return InteractionResult.CONSUME;
        }

        sendDialoguePacket(sp);
        return InteractionResult.SUCCESS;
    }

    private void sendDialoguePacket(ServerPlayer player) {
        OpenTrainerDialoguePacket.DialogueState state;
        if (duelCooldownTicks > 0) {
            state = OpenTrainerDialoguePacket.DialogueState.DUEL_COOLDOWN;
        } else if (ProgressionHelper.isMovesetUnlocked(player, trainerType.movesetId)) {
            state = OpenTrainerDialoguePacket.DialogueState.STUDENT;
        } else if (playerHasPrerequisite(player)) {
            state = OpenTrainerDialoguePacket.DialogueState.PREREQ_MET;
        } else {
            state = OpenTrainerDialoguePacket.DialogueState.STRANGER;
        }

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new OpenTrainerDialoguePacket(this.getUUID(), trainerType, state, hasBeatenTrainer).toBytes(buf);
            NetworkManager.sendToPlayer(player, NichirinPacketRegistry.OPEN_TRAINER_DIALOGUE_ID, NetworkBufferUtils.server(buf, player));
        } catch (Exception ignored) {}
    }

    private boolean playerHasPrerequisite(ServerPlayer player) {
        int have = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == trainerType.prerequisiteItem) have += stack.getCount();
        }
        return have >= trainerType.prerequisiteCount;
    }


    /** Backward-compat bridge — defaults to EASY difficulty. */
    public void startDuel(ServerPlayer challenger) {
        startDuel(challenger, DuelDifficulty.EASY);
    }

    public void startDuel(ServerPlayer challenger, DuelDifficulty difficulty) {
        if (mode == TrainerMode.DUELING) return;
        if (duelCooldownTicks > 0) {
            challenger.displayClientMessage(
                    Component.literal("I need time to recover. Come back later.")
                            .withStyle(s -> s.withColor(0xFFAA00)), false);
            return;
        }

        mode           = TrainerMode.DUELING;
        duelPlayerId   = challenger.getUUID();
        duelDifficulty = difficulty;
        playersInDuel.put(challenger.getUUID(), getUUID());

        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(duelHp());
        setHealth(duelHp());
        setTarget(challenger);

        // Create boss bar
        bossBar = new ServerBossEvent(
                Component.literal(trainerType.npcName),
                getBossBarColor(),
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.addPlayer(challenger);
        bossBar.setProgress(1.0f);

        challenger.displayClientMessage(
                Component.literal(trainerType.duelStartMsg)
                        .withStyle(s -> s.withColor(trainerType.styleColor)), false);
    }

    protected BossEvent.BossBarColor getBossBarColor() {
        return BossEvent.BossBarColor.WHITE;
    }

    protected void endDuel(boolean playerWon) {
        mode = TrainerMode.PEACEFUL;
        duelCooldownTicks = duelCooldownDuration();
        provokedWarnedBy = null;
        if (duelPlayerId != null) playersInDuel.remove(duelPlayerId);
        setTarget(null);

        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(peacefulHp());
        setHealth(peacefulHp());

        if (playerWon) hasBeatenTrainer = true;

        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }

        if (playerWon && duelPlayerId != null) {
            Player winner = level().getPlayerByUUID(duelPlayerId);
            if (winner instanceof ServerPlayer sp) {
                ProgressionHelper.unlockMoveset(sp, trainerType.movesetId);
                sp.displayClientMessage(
                        Component.literal(trainerType.duelWinMsg)
                                .withStyle(s -> s.withColor(trainerType.styleColor)), false);
            }
        }

        duelPlayerId = null;
    }

    public float getDifficultyDamageMultiplier() {
        return switch (duelDifficulty) {
            case EASY   -> 1f / 3f;
            case MEDIUM -> 2f / 3f;
            case HARD   -> 1f;
        };
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (mode == TrainerMode.DUELING && target instanceof Player p
                && p.getUUID().equals(duelPlayerId)) {
            // If trainer is already near-dead the player is about to win — don't claim trainer wins
            if (getHealth() <= duelWinHpThreshold() + 1.0f) return false;
            float atk = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            float playerFloor = playerDuelMinHp(p);
            if (p.getHealth() - atk <= playerFloor) {
                p.setHealth(playerFloor);
                endDuel(false);
                p.displayClientMessage(
                        Component.literal(trainerType.duelLoseMsg)
                                .withStyle(s -> s.withColor(trainerType.styleColor)), false);
                return false;
            }
        }

        // Provoked combat: the trainer fights for real but spares the player — leaves them at
        // half a heart and stands down rather than killing them.
        if (mode == TrainerMode.SELF_DEFENSE && target instanceof Player p) {
            float atk = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            float playerFloor = playerDuelMinHp(p);
            if (p.getHealth() - atk <= playerFloor) {
                p.setHealth(playerFloor);
                standDownFromSelfDefense(p);
                return false;
            }
        }

        return super.doHurtTarget(target);
    }


    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if ((mode == TrainerMode.DUELING || mode == TrainerMode.SELF_DEFENSE)
                && source.getEntity() instanceof LivingEntity attacker
                && KatanaBlock.handleIncomingDamage(this, attacker, amount)) {
            return false;
        }

        if (mode == TrainerMode.DUELING) {
            // Clamp so trainer can't be killed — duel ends at duelWinHpThreshold()
            float safe = Math.min(amount, Math.max(0, getHealth() - duelWinHpThreshold()));
            // Zero out vanilla i-frames so rapid hits all register during a duel.
            this.invulnerableTime = 0;
            boolean hit = safe > 0 && super.hurt(source, safe);
            // End duel whenever health is at threshold, even if this specific hit was
            // blocked by hurtTime immunity (safe=0 or super.hurt returned false).
            if (getHealth() <= duelWinHpThreshold() && mode == TrainerMode.DUELING) {
                boolean won = source.getEntity() instanceof Player atk
                        && atk.getUUID().equals(duelPlayerId);
                endDuel(won);
            }
            return hit;
        }

        if (mode == TrainerMode.SELF_DEFENSE) {
            // No restrictions — real fight, trainer can die. Some breathing attacks grant their
            // user temporary invulnerability, but that must never make an aggressive trainer
            // immune to damage (or leave it permanently immune if the attack is interrupted).
            setInvulnerable(false);
            return super.hurt(source, amount);
        }

        // PEACEFUL — first hit warns and immediately enters self-defense.
        // Creative-mode players are running tests / sandbox; skip the dialogue and the
        // self-defense escalation entirely so they can poke at trainers without spam.
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof Player attacker && !attacker.isCreative()) {
            if (provokedWarnedBy == null || !provokedWarnedBy.equals(attacker.getUUID())) {
                provokedWarnedBy = attacker.getUUID();
                if (attacker instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(
                            Component.literal(trainerType.npcName + ": Are you sure you want to do this?")
                                    .withStyle(s -> s.withColor(0xFFAA00)));
                }
            }
            enterSelfDefense(attacker);
        }
        return result;
    }

    protected void enterSelfDefense(Player aggressor) {
        mode = TrainerMode.SELF_DEFENSE;
        duelDifficulty = DuelDifficulty.HARD;
        setInvulnerable(false);
        setTarget(aggressor);
        selfDefenseGraceTicks = selfDefenseGraceDuration();
        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(duelHp());
    }

    /**
     * Mercy stand-down: the trainer spares a defeated aggressor (leaves them at half a heart)
     * and returns to peaceful instead of killing them. The trainer itself can still be killed
     * in self-defense (handled in {@link #hurt}).
     */
    private void standDownFromSelfDefense(Player spared) {
        mode = TrainerMode.PEACEFUL;
        provokedWarnedBy = null;
        selfDefenseGraceTicks = 0;
        setTarget(null);
        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(peacefulHp());
        setHealth(peacefulHp());
        spared.displayClientMessage(
                Component.literal(trainerType.npcName + ": Enough. You've learned your lesson.")
                        .withStyle(s -> s.withColor(0xFFAA00)), false);
    }

    /**
     * Whether the trainer is in a formal duel waiting for the player's first blow before fighting.
     * Subclasses that implement the "wait for first strike" duel intro override this.
     */
    public boolean isWaitingForFirstBlow() {
        return false;
    }

    public void raiseGuard() {
        KatanaBlock.startBlocking(this);
    }

    public void dropGuard() {
        KatanaBlock.stopBlocking(this);
    }

    public boolean isGuardUp() {
        return KatanaBlock.isBlocking(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            entityData.set(ANIMATION_RESET, false);
            ensureMoveset();
            tickMovesetSystems();
            MoveExecutor.tickAttacks(this);
        }

        if (level().isClientSide) return;

        if (duelCooldownTicks > 0) duelCooldownTicks--;
        KatanaBlock.tick(this);

        if (mode == TrainerMode.DUELING && duelPlayerId != null) {
            Player duelist = level().getPlayerByUUID(duelPlayerId);
            if (duelist == null || !duelist.isAlive()) endDuel(false);
        }

        // Self-defense ends only after the target has been gone for a grace period, so a
        // momentary loss doesn't drop to peaceful and re-arm the "Are you sure?" warning.
        if (mode == TrainerMode.SELF_DEFENSE) {
            // SELF_DEFENSE is a real fight. Clear immunity reapplied by shared player attacks
            // every tick while preserving the trainer's visible, time-limited guard mechanics.
            setInvulnerable(false);
            LivingEntity selfDefenseTarget = getTarget();
            if (selfDefenseTarget == null || !selfDefenseTarget.isAlive()) {
                if (--selfDefenseGraceTicks <= 0) {
                    mode = TrainerMode.PEACEFUL;
                    provokedWarnedBy = null;
                    setTarget(null);
                }
            } else {
                selfDefenseGraceTicks = selfDefenseGraceDuration();
            }
        }

        if (bossBar != null && mode == TrainerMode.DUELING) {
            // Normalize to the usable HP band [threshold, duelHp()] so the bar reads exactly 0
            // when the duel is about to end (health is clamped and never reaches true zero).
            float usable = duelHp() - duelWinHpThreshold();
            float progress = (getHealth() - duelWinHpThreshold()) / usable;
            bossBar.setProgress(Math.max(0f, Math.min(1f, progress)));
        }

        if (onGround()) resetDoubleJump();

        if ((tickCount % 200) == 0) ensureEquipment();
    }

    private void ensureEquipment() {
        if (getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            equipArmor();
        }
    }

    protected void ensureMoveset() {
        if (moveset == null) {
            moveset = NichirinMovesetRegistry.getMoveset(trainerType.movesetId);
        }
    }


    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            trainerCooldowns.remove(getUUID());
            NPCResourceManager.cleanupNPC(getUUID());
            AbstractMoveset.cleanupEntity(this);
            if (bossBar != null) {
                bossBar.removeAllPlayers();
                bossBar = null;
            }
        }
        super.remove(reason);
    }


    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TrainerMode", mode.name());
        tag.putInt("DuelCooldown", duelCooldownTicks);
        if (duelPlayerId != null) tag.putUUID("DuelPlayer", duelPlayerId);
        tag.putFloat("breath_gauge", getBreathGauge());
        tag.putFloat("stamina", getStamina());
        if (moveset != null) tag.putString("moveset_id", moveset.getMovesetId());
        tag.putBoolean("HasBeatenTrainer", hasBeatenTrainer);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try { mode = TrainerMode.valueOf(tag.getString("TrainerMode")); }
        catch (Exception e) { mode = TrainerMode.PEACEFUL; }
        duelCooldownTicks = tag.getInt("DuelCooldown");
        duelPlayerId = tag.hasUUID("DuelPlayer") ? tag.getUUID("DuelPlayer") : null;
        if (mode == TrainerMode.DUELING) { mode = TrainerMode.PEACEFUL; duelPlayerId = null; }
        if (tag.contains("breath_gauge")) setBreathGauge(tag.getFloat("breath_gauge"));
        if (tag.contains("stamina"))      setStamina(tag.getFloat("stamina"));
        hasBeatenTrainer = tag.getBoolean("HasBeatenTrainer");
    }


    @Override public boolean canBeLeashed()                     { return false; }
    @Override protected boolean shouldDespawnInPeaceful()       { return false; }
    @Override public boolean removeWhenFarAway(double dist)     { return false; }

    /**
     * Generic combat AI for all breathing trainers that don't have a custom AttackGoal.
     * Uses the full moveset: left-click basic attacks, wheel moves, right-click moves,
     * dashing, backsteps, and double jumps.
     */
    private static class TrainerDuelGoal extends MeleeAttackGoal {
        private static final int BACKSTEP_RELEASE_DELAY = 10;

        private final BaseBreathingTrainerEntity trainer;

        private int globalCooldown     = 0;
        private int leftClickCooldown  = 0;
        private int backstepCooldown   = 0;
        private int doubleJumpCooldown = 0;
        private int thinkPauseTicks    = 0;
        private int guardHeldTicks     = 0;

        private int pendingMoveIndex   = -1;
        private int pendingMoveDelay   = 0;

        private int    stuckCheckTimer = 0;
        private double lastDistSq      = 0;
        private int    timesStuck      = 0;

        TrainerDuelGoal(BaseBreathingTrainerEntity trainer) {
            super(trainer, 1.2, true);
            this.trainer = trainer;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            return (trainer.mode == TrainerMode.DUELING || trainer.mode == TrainerMode.SELF_DEFENSE) && super.canUse();
        }

        @Override public boolean canContinueToUse() {
            return (trainer.mode == TrainerMode.DUELING || trainer.mode == TrainerMode.SELF_DEFENSE)
                    && super.canContinueToUse();
        }

        @Override protected int getAttackInterval() { return 10; }
        @Override protected void checkAndPerformAttack(@NotNull LivingEntity target) { }

        @Override
        public void tick() {
            super.tick();

            if (globalCooldown     > 0) globalCooldown--;
            if (leftClickCooldown  > 0) leftClickCooldown--;
            if (backstepCooldown   > 0) backstepCooldown--;
            if (doubleJumpCooldown > 0) doubleJumpCooldown--;
            if (pendingMoveDelay   > 0) pendingMoveDelay--;
            if (thinkPauseTicks    > 0) thinkPauseTicks--;

            LivingEntity target = trainer.getTarget();
            if (target == null || !target.isAlive()) return;

            trainer.getLookControl().setLookAt(target, 360f, 360f);

            double distSq = trainer.distanceToSqr(target);

            // Reactive blocking — detect target attacking and raise guard
            // Block check runs independently of globalCooldown so trainer can defend while on cooldown
            if (!trainer.isGuardUp() && distSq < 6.0 * 6.0) {
                boolean targetAttacking = target.swinging || target.attackAnim > 0
                        || target.hurtTime > 0 && target.hurtTime < 5
                        || MoveExecutor.hasActiveAttacks(target);
                // Also proactively block sometimes when close and on cooldown
                float ai = aiNorm();
                if (!targetAttacking && globalCooldown > 0 && distSq < 3.5 * 3.5) {
                    targetAttacking = trainer.getRandom().nextFloat() < 0.08f * ai;
                }
                if (targetAttacking) {
                    if (trainer.getRandom().nextFloat() < 0.1f + 0.5f * ai) {
                        trainer.raiseGuard();
                        guardHeldTicks = 0;
                        return;
                    }
                }
            }
            // Drop guard after a while to resume offense
            if (trainer.isGuardUp()) {
                guardHeldTicks++;
                int maxGuard = 20 + trainer.getRandom().nextInt(25);
                if (guardHeldTicks > maxGuard) {
                    trainer.dropGuard();
                    guardHeldTicks = 0;
                }
                return; // Don't attack while guarding
            }

            stuckCheckTimer++;
            if (stuckCheckTimer >= 40) {
                stuckCheckTimer = 0;
                if (Math.abs(distSq - lastDistSq) < 1.0) {
                    if (++timesStuck > 2) {
                        trainer.getNavigation().stop();
                        trainer.getNavigation().moveTo(target, 1.5);
                        timesStuck = 0;
                    }
                } else {
                    timesStuck = 0;
                }
                lastDistSq = distSq;
            }

            if (pendingMoveIndex >= 0 && pendingMoveDelay == 0) {
                int idx = pendingMoveIndex;
                pendingMoveIndex = -1;
                if (trainer.moveset != null && trainer.canUseMove(idx)) {
                    snapToFaceTarget();
                    trainer.performMovesetMove(idx);
                    globalCooldown = cooldownAfterMove(idx);
                }
                return;
            }

            if (thinkPauseTicks > 0 || pendingMoveIndex >= 0) return;

            if (trainer.moveset == null) return;

            if (globalCooldown > 0) {
                double approachSpeed = 1.5;
                if (distSq > 3.5 * 3.5) trainer.getNavigation().moveTo(target, approachSpeed);
                return;
            }

            if (!trainer.onGround() && doubleJumpCooldown == 0) {
                EntityMovement.applyAirDodge(
                        trainer, target.position().subtract(trainer.position()).normalize());
                doubleJumpCooldown = 40;
            }

            if (trainer.onGround() && target.getY() > trainer.getY() + 2.0 && trainer.canDoubleJump()) {
                trainer.markDoubleJumped();
                EntityMovement.applyDoubleJump(
                        trainer, target.position().subtract(trainer.position()));
            }

            // Low AI levels hesitate before attacking
            float ai = aiNorm();
            if (ai < 1.0f && trainer.getRandom().nextFloat() > ai) {
                thinkPauseTicks = 8 + trainer.getRandom().nextInt(20);
                return;
            }

            decideAction(target, distSq);
        }

        private void decideAction(LivingEntity target, double distSq) {
            int moveCount = trainer.moveset.getMoveCount();

            if (distSq < 3.5 * 3.5) {
                // Very close — attack immediately with whatever is ready
                if (leftClickCooldown == 0) {
                    fireLeftClick();
                } else if (tryRandomMove(moveCount)) {
                    // used a move
                } else if (trainer.canUseRightClickMove(false)) {
                    useRightClick(false);
                } else {
                    // Nothing ready — left click spam
                    if (leftClickCooldown <= 3) {
                        fireLeftClick();
                    } else {
                        applyDash(target);
                    }
                }

            } else if (distSq < 7.0 * 7.0) {
                // Close-mid — close gap and attack
                if (tryRandomMove(moveCount)) {
                    // used a move
                } else if (trainer.canUseRightClickMove(false)) {
                    useRightClick(false);
                } else {
                    applyDash(target);
                }

            } else if (distSq < 15.0 * 15.0) {
                // Mid — gap close aggressively
                if (trainer.canUseRightClickMove(false)) {
                    useRightClick(false);
                } else if (tryRandomMove(moveCount)) {
                    // used a move
                } else {
                    applyDash(target);
                }

            } else {
                // Long range — close the gap
                if (trainer.canUseRightClickMove(false)) {
                    useRightClick(false);
                } else {
                    applyDash(target);
                }
            }
        }

        private boolean tryRandomMove(int moveCount) {
            if (moveCount == 0) return false;
            int start = trainer.getRandom().nextInt(moveCount);
            for (int i = 0; i < moveCount; i++) {
                int idx = (start + i) % moveCount;
                if (trainer.canUseMove(idx)) {
                    useMove(idx);
                    return true;
                }
            }
            return false;
        }

        private static float aiNorm() {
            return NichirinModConfig.get().combat.npcAiLevel / 25f;
        }

        private void fireLeftClick() {
            if (trainer.moveset == null) return;
            snapToFaceTarget();
            trainer.moveset.handleLeftClick(trainer);
            leftClickCooldown = 4;
            globalCooldown = 2;
        }

        private void useMove(int idx) {
            if (!trainer.canUseMove(idx)) return;
            snapToFaceTarget();
            trainer.performMovesetMove(idx);
            globalCooldown = cooldownAfterMove(idx);
        }

        private void useRightClick(boolean crouching) {
            if (!trainer.canUseRightClickMove(crouching)) return;
            snapToFaceTarget();
            trainer.performRightClickMove(crouching);
            globalCooldown = 3;
        }

        private void snapToFaceTarget() {
            LivingEntity target = trainer.getTarget();
            if (target == null) return;
            double dx = target.getX() - trainer.getX();
            double dz = target.getZ() - trainer.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            trainer.setYRot(yaw);
            trainer.yRotO = yaw;
            trainer.setYBodyRot(yaw);
            trainer.setYHeadRot(yaw);
        }

        private void queueAfterBackstep(int idx) {
            EntityMovement.applyBackstep(trainer);
            backstepCooldown = 50;
            pendingMoveIndex = idx;
            pendingMoveDelay = BACKSTEP_RELEASE_DELAY;
        }

        private void applyDash(LivingEntity target) {
            Vec3 toTarget = target.position().subtract(trainer.position()).normalize();
            EntityMovement.applyDash(trainer, toTarget);
            globalCooldown = 3;
        }

        private int cooldownAfterMove(int idx) {
            AbstractMoveset.MoveConfiguration cfg =
                    trainer.moveset != null ? trainer.moveset.getMove(idx) : null;
            if (cfg == null) return 10;

            return cfg.getWindupOrDefault(8) + cfg.getDurationOrDefault(15);
        }
    }
}
