package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.UpperMoonPact;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Base for Upper Moon demons — the named, characterful bosses (Akaza, and future ones).
 *
 * <p>Shared behavior beyond a plain demon NPC:</p>
 * <ul>
 *   <li><b>Mercy / recruitment.</b> When not enraged, a killing blow on a human is converted into a
 *       spare: the player is left at half a heart, offered demon blood, and the demon goes neutral
 *       toward them (per-demon — see {@link UpperMoonPact}). Attacking the demon again means a fight
 *       to the death.</li>
 *   <li><b>Personality.</b> Named tag, spoken recruit / re-engage lines, and randomized combat barks
 *       (engage, overdrive, when you flee) plus idle lines to spared players. Subclasses override the
 *       line pools to give each demon a distinct voice.</li>
 * </ul>
 *
 * <p>The killing-blow interception itself lives in {@code UpperMoonMercyHandler} (a LIVING_HURT
 * listener); this class provides the behavior it calls into.</p>
 */
public abstract class UpperMoonDemonEntity extends DemonNPCEntity {

    private static final int BARK_GAP_TICKS = 100;   // min 5s between any two barks
    private static final int IDLE_BARK_GAP_TICKS = 400;
    private static final double IDLE_BARK_RANGE = 8.0;
    private static final double BARK_HEAR_RANGE = 28.0;

    // How long (ticks) between each kneel escalation: recruit → "Kneel to me." → "KNEEL!" → death.
    private static final int STANDOFF_STAGE_TICKS = 60;
    private static final double STANDOFF_MAX_RANGE = 16.0;
    private static final double STANDOFF_FACE_RANGE = 2.4; // he stands this close before demanding you kneel

    // Players currently held in a standoff. While listed they take no damage at all, so lingering
    // shockwaves / in-flight attacks can't kill them before they get to kneel or refuse. The
    // deliberate execution (executeStandoff) removes the player from this set first, then kills.
    private static final java.util.Set<UUID> STANDOFF_PROTECTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** True while {@code playerId} is pinned in a mercy standoff and must not take damage. */
    public static boolean isStandoffProtected(UUID playerId) {
        return STANDOFF_PROTECTED.contains(playerId);
    }

    private int barkCooldown = 0;
    private int idleBarkTimer = IDLE_BARK_GAP_TICKS;
    private boolean nameInitialized = false;
    private UUID lastEngageTarget = null;
    private UUID lastReengageTarget = null;

    // Active mercy standoff (a player pinned at half a heart while the demon demands they kneel).
    private UUID standoffTarget = null;
    private int standoffStage = 0;   // 0 = recruit said, 1 = "Kneel to me." said, 2 = "KNEEL!" said
    private int standoffTimer = 0;
    private int standoffPathCd = 0;  // throttles path recompute while running over (avoids walk-in-place)
    private boolean standoffAwaitingDrink = false; // knelt — now waiting for them to drink the vial

    protected UpperMoonDemonEntity(EntityType<? extends DemonNPCEntity> type, Level level) {
        super(type, level);
        setCustomNameVisible(true);
    }

    // ---- Subclass hooks: state + voice ----

    /** Whether the demon is in its enraged phase. No mercy is offered while enraged. */
    public abstract boolean isEnraged();

    /** Name shown on the always-visible tag (may change with state, e.g. an Overdrive suffix). */
    protected abstract String titleText();

    /** ARGB-less RGB color for the name tag. */
    protected abstract int nameColor();

    protected String recruitLine(String playerName) { return "Become a demon, " + playerName + "!"; }
    protected String reengageLine() { return "So you're back for my head, aren't you."; }
    protected String[] engageBarks() { return new String[0]; }
    protected String[] overdriveBarks() { return new String[0]; }
    protected String[] fleeBarks() { return new String[0]; }
    protected String[] idleBarks() { return new String[0]; }

    // ---- Neutrality ----

    /**
     * True if this demon should leave {@code player} alone: fellow demons (kin) and players it has
     * already spared. Retaliation via {@code HurtByTargetGoal} still applies if they attack it.
     */
    public boolean isNeutralTo(Player player) {
        if (player.isCreative() || player.isSpectator()) return true;
        if (MovesetHelper.hasDemonMoveset(player)) return true;
        return UpperMoonPact.isSpared(player, getDemonType());
    }

    // ---- Mercy / recruitment standoff ----

    protected String kneelDemand1() { return "Kneel to me."; }
    protected String kneelDemand2() { return "KNEEL!"; }
    protected String drinkPromptLine() { return "Good. Now drink, and become one of us."; }
    protected String spareFarewellLine(String playerName) { return "We will meet again... grow strong until then."; }
    protected String standoffKillLine() { return "Then you are nothing."; }

    public boolean isInStandoff() {
        return standoffTarget != null;
    }

    public boolean isInStandoffWith(Player player) {
        return player != null && player.getUUID().equals(standoffTarget);
    }

    /**
     * Begins the mercy standoff instead of killing: the player is pinned at half a heart. The demon
     * closes in to stand right in front, demands they kneel, and — once they kneel — hands them a
     * blood vial. Only <em>drinking</em> the vial in front of the demon officially spares them.
     * Refusing through two warnings, walking off, or attacking the demon is fatal.
     */
    public void beginStandoff(ServerPlayer player) {
        standoffTarget = player.getUUID();
        standoffStage = 0;
        standoffTimer = STANDOFF_STAGE_TICKS;
        standoffAwaitingDrink = false;
        STANDOFF_PROTECTED.add(player.getUUID());

        player.setHealth(1.0f);            // half a heart
        player.hurtMarked = true;
        setTarget(null);
        getNavigation().stop();
        // Stop any in-flight CQC attack so it can't keep swinging during the truce.
        com.xirc.nichirin.common.attack.MoveExecutor.clearAttacks(this);

        say(player, recruitLine(player.getName().getString()));
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.7f, 1.4f);
    }

    private void tickStandoff() {
        if (getServer() == null) return;
        ServerPlayer player = getServer().getPlayerList().getPlayer(standoffTarget);
        if (player == null || !player.isAlive() || player.isRemoved()
                || player.distanceToSqr(this) > STANDOFF_MAX_RANGE * STANDOFF_MAX_RANGE) {
            clearStandoff();
            return;
        }

        // Drank the blood in front of me → officially spared, and I take my leave. Checked before
        // anything else so it fires the instant they transform, wherever they happen to be standing.
        if (standoffAwaitingDrink && MovesetHelper.hasDemonMoveset(player)) {
            officiallySpare(player);
            return;
        }

        // Pin the player where they are — they can still crouch to kneel and drink.
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 250, false, false, false));

        // Stand RIGHT in front of them before demanding anything — run over if needed. Recompute the
        // path on a timer (recomputing every tick resets progress and leaves him jogging in place),
        // and let navigation steer him rather than hard-locking his facing while he moves.
        if (player.distanceToSqr(this) > STANDOFF_FACE_RANGE * STANDOFF_FACE_RANGE) {
            if (standoffPathCd <= 0 || getNavigation().isDone()) {
                getNavigation().moveTo(player, 1.35);
                standoffPathCd = 10;
            } else {
                standoffPathCd--;
            }
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            return;
        }
        getNavigation().stop();
        faceEntity(player);

        // Phase 2: knelt already — stand and wait patiently for them to drink (detected at the top).
        if (standoffAwaitingDrink) {
            return; // attacking still triggers death via the handler
        }

        // Phase 1: demand the kneel. Crouch = kneel → hand over the vial.
        if (player.isShiftKeyDown()) {
            onKneel(player);
            return;
        }
        // First "Kneel to me." fires the instant he's in your face; escalate on the timer after that.
        if (standoffStage == 0) {
            say(player, kneelDemand1());
            standoffStage = 1;
            standoffTimer = STANDOFF_STAGE_TICKS;
            return;
        }
        if (standoffTimer > 0) {
            standoffTimer--;
            return;
        }
        standoffTimer = STANDOFF_STAGE_TICKS;
        if (standoffStage == 1) {
            say(player, kneelDemand2());
            standoffStage = 2;
        } else {
            executeStandoff(player); // third strike — no kneel, no mercy
        }
    }

    /** They knelt: hand over the vial and wait for them to drink it. */
    private void onKneel(ServerPlayer player) {
        standoffAwaitingDrink = true;
        say(player, drinkPromptLine());

        ItemStack vial = new ItemStack(NichirinItemRegistry.DEMON_BLOOD_VIAL.get());
        ItemEntity drop = new ItemEntity(level(), player.getX(), player.getY() + 0.6, player.getZ(), vial);
        drop.setDeltaMovement(0, 0.15, 0);
        drop.setPickUpDelay(10);
        level().addFreshEntity(drop);
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.BOTTLE_FILL, SoundSource.HOSTILE, 0.8f, 0.7f);
    }

    /** They drank the blood in front of him — officially spared. He gives an ominous farewell and leaves. */
    private void officiallySpare(ServerPlayer player) {
        say(player, spareFarewellLine(player.getName().getString()));
        UpperMoonPact.mark(player, getDemonType());
        // Drop aggro/revenge so nothing lingers, then vanish — his work here is done.
        setTarget(null);
        setLastHurtByMob(null);
        setLastHurtByPlayer(null);
        clearStandoff();
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.7f, 1.3f); // ominous
        vanish();
    }

    /** The player refused (or attacked) during the standoff — kill them, then vanish. */
    public void executeStandoff(ServerPlayer player) {
        clearStandoff(); // clear first so the mercy handler can't re-open a standoff on this kill
        say(player, standoffKillLine());
        // genericKill has no attacking entity, so the mercy handler won't spare this blow.
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        despawnAfterKill();
    }

    /** After killing a player, the demon melts away rather than lingering in the world. */
    public void despawnAfterKill() {
        if (isRemoved()) return;
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.6f, 1.6f);
        vanish();
    }

    /** Poof of smoke, then remove the entity from the world. */
    private void vanish() {
        if (isRemoved()) return;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + getBbHeight() * 0.5, getZ(), 40, 0.4, 0.8, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.SMOKE,
                    getX(), getY() + getBbHeight() * 0.5, getZ(), 30, 0.5, 0.9, 0.5, 0.01);
        }
        discard();
    }

    private void clearStandoff() {
        if (standoffTarget != null) {
            STANDOFF_PROTECTED.remove(standoffTarget);
        }
        standoffTarget = null;
        standoffStage = 0;
        standoffTimer = 0;
        standoffPathCd = 0;
        standoffAwaitingDrink = false;
    }

    private void faceEntity(Entity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        setYRot(yaw);
        yRotO = yaw;
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    /** Called when a player this demon spared attacks it again — quips once per attacker. */
    public void onReengaged(ServerPlayer player) {
        if (player.getUUID().equals(lastReengageTarget)) return;
        lastReengageTarget = player.getUUID();
        say(player, reengageLine());
    }

    // ---- Personality / barks ----

    /** Fired by the combat AI when a fight starts. Barks the engage line (once per target). */
    public void onEngage(LivingEntity target) {
        if (target == null || target.getUUID().equals(lastEngageTarget)) return;
        lastEngageTarget = target.getUUID();
        bark(engageBarks());
    }

    /** Fired by the combat AI when the target is running away. */
    public void onTargetFleeing() {
        bark(fleeBarks());
    }

    protected void barkOverdrive() {
        bark(overdriveBarks());
    }

    /** Picks a random line from the pool and speaks it to nearby players (rate-limited). */
    protected void bark(String[] pool) {
        if (pool.length == 0 || barkCooldown > 0 || level().isClientSide) return;
        String line = pool[getRandom().nextInt(pool.length)];
        sayNearby(line);
        barkCooldown = BARK_GAP_TICKS;
    }

    // ---- Name tag ----

    protected void applyNameTag() {
        setCustomName(Component.literal(titleText()).withStyle(style -> style.withColor(nameColor())));
        setCustomNameVisible(true);
    }

    // ---- Messaging ----

    private Component formatLine(String line) {
        return Component.literal("[" + titleText() + "] ").withStyle(s -> s.withColor(nameColor()))
                .append(Component.literal(line).withStyle(s -> s.withColor(0xE0E0E0)));
    }

    protected void say(ServerPlayer to, String line) {
        to.displayClientMessage(formatLine(line), false);
    }

    protected void sayNearby(String line) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        Component formatted = formatLine(line);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= BARK_HEAR_RANGE * BARK_HEAR_RANGE) {
                player.displayClientMessage(formatted, false);
            }
        }
    }

    // ---- Tick: name init, bark cooldowns, idle chatter ----

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (!nameInitialized) {
            applyNameTag();
            nameInitialized = true;
        }
        if (barkCooldown > 0) barkCooldown--;

        if (isInStandoff()) {
            tickStandoff(); // standoff freezes the fight and suppresses idle chatter
            return;
        }
        tickIdleBark();
    }

    private void tickIdleBark() {
        if (getTarget() != null) {
            idleBarkTimer = IDLE_BARK_GAP_TICKS;
            return;
        }
        if (idleBarkTimer > 0) {
            idleBarkTimer--;
            return;
        }
        idleBarkTimer = IDLE_BARK_GAP_TICKS;
        // Only chatter idly when a player he has spared is loitering nearby.
        Player near = level().getNearestPlayer(this, IDLE_BARK_RANGE);
        if (near != null && UpperMoonPact.isSpared(near, getDemonType())) {
            bark(idleBarks());
        }
    }

    /**
     * While a mercy standoff is active this goal claims MOVE/LOOK/JUMP so the demon holds still and
     * doesn't wander off (blocks stroll/look goals). The escalation logic itself runs from
     * {@link #tickStandoff()}.
     */
    public static class StandoffGoal extends Goal {

        private final UpperMoonDemonEntity demon;

        public StandoffGoal(UpperMoonDemonEntity demon) {
            this.demon = demon;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return demon.isInStandoff();
        }

        @Override
        public boolean canContinueToUse() {
            return demon.isInStandoff();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        // Movement/facing during the standoff is driven by tickStandoff (so he can run over to you).
        // This goal exists only to hold the MOVE/LOOK/JUMP flags so stroll/look goals can't run.
        @Override
        public void tick() {
        }
    }
}
