package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.network.s2c.OpenTrainerDialoguePacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base for all breathing-style trainer NPCs.
 *
 * <p>Subclasses supply a {@link TrainerType} and optionally override
 * {@link #equipArmor()} for trainer-specific gear.</p>
 *
 * <h2>Prerequisite system</h2>
 * <p>Before a player can duel, they must carry the items defined in the
 * trainer's {@link TrainerType}. The items are <em>checked</em> but not
 * consumed — they act as proof of readiness.</p>
 *
 * <h2>Progression-aware dialogue</h2>
 * <ul>
 *   <li>{@link OpenTrainerDialoguePacket.DialogueState#STRANGER} — task not met</li>
 *   <li>{@link OpenTrainerDialoguePacket.DialogueState#PREREQ_MET} — items present, duel allowed</li>
 *   <li>{@link OpenTrainerDialoguePacket.DialogueState#STUDENT} — style already unlocked, can spar</li>
 *   <li>{@link OpenTrainerDialoguePacket.DialogueState#DUEL_COOLDOWN} — resting</li>
 * </ul>
 */
public abstract class BaseBreathingTrainerEntity extends PathfinderMob {

    // Constants
    protected static final float DUEL_HP              = 100.0f;
    protected static final float PEACEFUL_HP          = 200.0f;
    protected static final float DUEL_WIN_HP_THRESHOLD = 1.0f;
    protected static final float PLAYER_DUEL_MIN_HP   = 1.0f;
    protected static final int   DUEL_COOLDOWN_TICKS  = 20 * 60 * 3; // 3 minutes

    // State
    public enum TrainerMode { PEACEFUL, DUELING }

    @Getter
    protected final TrainerType trainerType;

    @Getter
    private TrainerMode mode           = TrainerMode.PEACEFUL;
    private UUID        duelPlayerId   = null;
    @Getter
    private int         duelCooldownTicks = 0;

    // Construction
    protected BaseBreathingTrainerEntity(EntityType<? extends BaseBreathingTrainerEntity> type,
                                         Level level, TrainerType trainerType) {
        super(type, level);
        this.trainerType = trainerType;
        this.setMaxUpStep(1.0f);
        this.setPersistenceRequired();
    }

    // Attributes
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,          PEACEFUL_HP)
                .add(Attributes.MOVEMENT_SPEED,      0.28)
                .add(Attributes.ATTACK_DAMAGE,       5.0)
                .add(Attributes.ARMOR,               6.0)
                .add(Attributes.FOLLOW_RANGE,        32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    // Goals
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TrainerDuelGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() { return mode == TrainerMode.DUELING && super.canUse(); }
        });
    }

    // Spawn setup
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType spawnType, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data, tag);
        equipArmor();
        return result;
    }

    /** Override to equip trainer-specific armor / weapons. Default: no armor. */
    protected void equipArmor() {}

    // Interaction
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.sidedSuccess(true);

        if (mode == TrainerMode.DUELING) {
            sp.displayClientMessage(
                    Component.literal("The spar is not over!")
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
            new OpenTrainerDialoguePacket(this.getUUID(), trainerType, state).toBytes(buf);
            NetworkManager.sendToPlayer(player, NichirinPacketRegistry.OPEN_TRAINER_DIALOGUE_ID, buf);
        } catch (Exception e) {
            // ignore
        }
    }

    private boolean playerHasPrerequisite(ServerPlayer player) {
        int have = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == trainerType.prerequisiteItem) have += stack.getCount();
        }
        return have >= trainerType.prerequisiteCount;
    }

    // Duel system
    /** Called from {@link com.xirc.nichirin.common.network.c2s.TrainerActionPacket}. */
    public void startDuel(ServerPlayer challenger) {
        if (mode == TrainerMode.DUELING) return;
        if (duelCooldownTicks > 0) {
            challenger.displayClientMessage(
                    Component.literal("I need time to recover. Come back later.")
                            .withStyle(s -> s.withColor(0xFFAA00)), false);
            return;
        }

        mode         = TrainerMode.DUELING;
        duelPlayerId = challenger.getUUID();

        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(DUEL_HP);
        setHealth(DUEL_HP);
        setTarget(challenger);

        challenger.displayClientMessage(
                Component.literal(trainerType.duelStartMsg)
                        .withStyle(s -> s.withColor(trainerType.styleColor)), false);
    }

    private void endDuel(boolean playerWon) {
        mode = TrainerMode.PEACEFUL;
        duelCooldownTicks = DUEL_COOLDOWN_TICKS;
        setTarget(null);

        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(PEACEFUL_HP);
        setHealth(PEACEFUL_HP);

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

    // Attack override — cap so player never dies during spar
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.@NotNull Entity target) {
        if (mode == TrainerMode.DUELING && target instanceof Player p
                && p.getUUID().equals(duelPlayerId)) {
            float atk = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (p.getHealth() - atk <= PLAYER_DUEL_MIN_HP) {
                p.setHealth(PLAYER_DUEL_MIN_HP);
                endDuel(false);
                p.displayClientMessage(
                        Component.literal(trainerType.duelLoseMsg)
                                .withStyle(s -> s.withColor(trainerType.styleColor)), false);
                return false;
            }
        }
        return super.doHurtTarget(target);
    }

    // Damage override — trainer never dies; duel ends at threshold
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (mode != TrainerMode.DUELING) return false;

        float safe = Math.min(amount, Math.max(0, getHealth() - 1.0f));
        boolean hit = safe > 0 && super.hurt(source, safe);

        if (hit && getHealth() <= DUEL_WIN_HP_THRESHOLD) {
            boolean won = source.getEntity() instanceof Player atk
                    && atk.getUUID().equals(duelPlayerId);
            endDuel(won);
        }

        return hit;
    }

    // Tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (duelCooldownTicks > 0) duelCooldownTicks--;

        if (mode == TrainerMode.DUELING && duelPlayerId != null) {
            Player duelist = level().getPlayerByUUID(duelPlayerId);
            if (duelist == null || !duelist.isAlive()) endDuel(false);
        }

        if ((tickCount % 200) == 0) ensureEquipment();
    }

    private void ensureEquipment() {
        if (getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty()) {
            equipArmor();
        }
    }

    // NBT
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TrainerMode", mode.name());
        tag.putInt("DuelCooldown", duelCooldownTicks);
        if (duelPlayerId != null) tag.putUUID("DuelPlayer", duelPlayerId);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try { mode = TrainerMode.valueOf(tag.getString("TrainerMode")); }
        catch (Exception e) { mode = TrainerMode.PEACEFUL; }
        duelCooldownTicks = tag.getInt("DuelCooldown");
        duelPlayerId = tag.hasUUID("DuelPlayer") ? tag.getUUID("DuelPlayer") : null;
        // Always reset to peaceful on load to avoid phantom duels after restarts
        if (mode == TrainerMode.DUELING) { mode = TrainerMode.PEACEFUL; duelPlayerId = null; }
    }

    // Misc
    @Override public boolean canBeLeashed(@NotNull Player p)                            { return false; }
    @Override protected boolean shouldDespawnInPeaceful()                      { return false; }
    @Override public boolean removeWhenFarAway(double dist)                    { return false; }

    // Inner goal
    private static class TrainerDuelGoal extends MeleeAttackGoal {
        private final BaseBreathingTrainerEntity trainer;

        TrainerDuelGoal(BaseBreathingTrainerEntity trainer) {
            super(trainer, 1.1, true);
            this.trainer = trainer;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse()          { return trainer.mode == TrainerMode.DUELING && super.canUse(); }
        @Override public boolean canContinueToUse() { return trainer.mode == TrainerMode.DUELING && super.canContinueToUse(); }

        @Override protected double getAttackReachSqr(net.minecraft.world.entity.@NotNull LivingEntity t) { return 9.0; }
        @Override protected int getAttackInterval() { return 25; }
    }
}
