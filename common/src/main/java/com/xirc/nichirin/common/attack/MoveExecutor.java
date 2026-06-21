package com.xirc.nichirin.common.attack;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.common.attack.component.AbstractAttack;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.moves.AbstractKatanaAttack;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.ComboTracker;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified attack executor — handles both breathing and demon attacks for players and NPCs.
 */
@SuppressWarnings({"deprecation", "removal"})
public class MoveExecutor {

    private static final ConcurrentHashMap<UUID, List<Object>> activeAttacks = new ConcurrentHashMap<>();
    private static final ResourceLocation COOLDOWN_PACKET_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "cooldown_display");
    private static boolean hitboxDebuggingEnabled = false;
    private static final Set<Object> comboScaledAttacks = Collections.newSetFromMap(new WeakHashMap<>());

    public static void executeAttack(LivingEntity entity, Object attack, String movesetId, String moveId) {
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) return;

        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            handleAttack(entity, breathingAttack, movesetId, moveId);
        } else if (attack instanceof AbstractDemonAttack<?, ?> demonAttack) {
            handleAttack(entity, demonAttack, movesetId, moveId);
        } else {
            handleGenericAttack(entity, attack, movesetId, moveId);
        }
    }

    public static void executeAttack(Player player, Object attack, String movesetId, String moveId) {
        executeAttack((LivingEntity) player, attack, movesetId, moveId);
    }

    /**
     * Runs a move declared via {@code MoveBuilder.withAttack(...)}: configures the freshly built
     * attack with its own move config (mirroring the old hand-written lambdas exactly), then
     * dispatches it. Called by {@link AbstractMoveset}'s move-execution path.
     */
    public static void executeFactoryAttack(LivingEntity entity, Object attack, String movesetId, AbstractMoveset.MoveConfiguration config) {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.configure(config);
        } else if (attack instanceof AbstractDemonAttack<?, ?> demonAttack) {
            demonAttack.configure(config);
        }
        executeAttack(entity, attack, movesetId, config.getMoveId());
    }

    private static void handleAttack(LivingEntity entity, AbstractBreathingAttack<?, ?> attack, String movesetId, String moveId) {
        if (!isAttackConfigured(attack)) {
            configureAttackFromMoveset(entity, attack, movesetId, moveId);
        } else {
            applyPreConfiguredEffects(entity, attack, moveId);
        }
        executeConfiguredAttack(entity, attack, movesetId, moveId);
    }

    private static void handleAttack(LivingEntity entity, AbstractDemonAttack<?, ?> attack, String movesetId, String moveId) {
        if (!isDemonAttackConfigured(attack)) {
            configureAttackFromMoveset(entity, attack, movesetId, moveId);
        } else {
            applyPreConfiguredDemonEffects(entity, attack, moveId);
        }
        executeConfiguredAttack(entity, attack, movesetId, moveId);
    }

    private static void configureAttackFromMoveset(LivingEntity entity, Object attack, String movesetId, String moveId) {
        AbstractMoveset moveset = NichirinMovesetRegistry.getMoveset(movesetId);
        if (moveset == null) return;

        AbstractMoveset.MoveConfiguration config = findMoveConfig(moveset, moveId);
        if (config == null) return;

        int originalHitStun = config.getHitStunOrDefault(0);
        int modifiedHitStun = originalHitStun;
        if (entity instanceof Player player) {
            modifiedHitStun = ComboTracker.getModifiedHitStun(player, moveId, originalHitStun);
        }
        if (modifiedHitStun != originalHitStun) {
            config = createModifiedConfig(config, modifiedHitStun);
        }

        applyMoveStun(entity, config);

        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.configure(config);
        } else if (attack instanceof AbstractDemonAttack<?, ?> demonAttack) {
            demonAttack.configure(config);
        }
    }

    private static void applyPreConfiguredEffects(LivingEntity entity, AbstractBreathingAttack<?, ?> attack, String moveId) {
        if (entity instanceof Player player) {
            ComboTracker.getModifiedHitStun(player, moveId, attack.getHitStun());
        }
        applyPreConfiguredMoveStun(entity, attack);
    }

    private static void applyPreConfiguredDemonEffects(LivingEntity entity, AbstractDemonAttack<?, ?> attack, String moveId) {
        if (entity instanceof Player player) {
            ComboTracker.getModifiedHitStun(player, moveId, getHitStunFromAttack(attack));
        }
        applyPreConfiguredDemonMoveStun(entity, attack);
    }

    private static void executeConfiguredAttack(LivingEntity entity, Object attack, String movesetId, String moveId) {
        NichirinMovesetRegistry.MoveInfo moveInfo = NichirinMovesetRegistry.getMove(movesetId, moveId);
        AbstractMoveset moveset = NichirinMovesetRegistry.getMoveset(movesetId);
        AbstractMoveset.MoveConfiguration config = moveset != null ? findMoveConfig(moveset, moveId) : null;
        String displayName;
        if (moveInfo != null) {
            displayName = moveInfo.displayName;
        } else {
            displayName = config != null ? config.getDisplayName() : attack.getClass().getSimpleName();
        }
        int cooldown = getCooldownForAttack(attack);
        executeAttackInternal(entity, attack, displayName, cooldown, movesetId, config);
    }

    private static void handleGenericAttack(LivingEntity entity, Object attack, String movesetId, String moveId) {
        String displayName = attack.getClass().getSimpleName();
        AbstractMoveset moveset = NichirinMovesetRegistry.getMoveset(movesetId);
        AbstractMoveset.MoveConfiguration config = moveset != null ? findMoveConfig(moveset, moveId) : null;
        if (config != null) {
            displayName = config.getDisplayName();
        }
        int cooldown = getCooldownForAttack(attack);
        executeAttackInternal(entity, attack, displayName, cooldown, movesetId, config);
    }

    public static void executeAttackWithVisuals(LivingEntity entity, Object attack, String movesetId, String moveId) {
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) return;
        if (entity.level().isClientSide) AttackHitboxRenderer.clearAll();
        executeAttack(entity, attack, movesetId, moveId);
    }

    public static void executeAttackWithVisuals(Player player, Object attack, String movesetId, String moveId) {
        executeAttackWithVisuals((LivingEntity) player, attack, movesetId, moveId);
    }

    private static boolean isAttackConfigured(AbstractBreathingAttack<?, ?> attack) {
        try { return attack.getDuration() > 0; } catch (Exception e) { return false; }
    }

    private static boolean isDemonAttackConfigured(AbstractDemonAttack<?, ?> attack) {
        try { return attack.getDuration() > 0; } catch (Exception e) { return false; }
    }

    private static int getHitStunFromAttack(Object attack) {
        return getIntFromAttack(attack, "getHitStun");
    }

    private static void applyMoveStun(LivingEntity entity, AbstractMoveset.MoveConfiguration config) {
        applyStun(entity, config.getWindupOrDefault(0) + config.getDurationOrDefault(0));
    }

    private static void applyPreConfiguredMoveStun(LivingEntity entity, AbstractBreathingAttack<?, ?> attack) {
        applyStun(entity, getWindupFromAttack(attack));
    }

    private static void applyPreConfiguredDemonMoveStun(LivingEntity entity, AbstractDemonAttack<?, ?> attack) {
        applyStun(entity, getWindupFromAttack(attack));
    }

    private static void applyStun(LivingEntity entity, int ticks) {
        if (ticks > 0) {
            entity.addEffect(new MobEffectInstance(NichirinEffectRegistry.stunned(),
                    ticks, 0, false, false, false));
        }
    }

    private static int getWindupFromAttack(Object attack) {
        return getIntFromAttack(attack, "getWindup");
    }

    private static int getIntFromAttack(Object attack, String methodName) {
        try {
            return (int) attack.getClass().getMethod(methodName).invoke(attack);
        } catch (Exception e) { return 0; }
    }

    private static AbstractMoveset.MoveConfiguration createModifiedConfig(AbstractMoveset.MoveConfiguration originalConfig, int newHitStun) {
        return originalConfig;
    }

    private static AbstractMoveset.MoveConfiguration findMoveConfig(AbstractMoveset moveset, String moveId) {
        for (int i = 0; i < moveset.getMoveCount(); i++) {
            var config = moveset.getMove(i);
            if (config != null && config.getMoveId().equals(moveId)) return config;
        }
        var lc = moveset.getLeftClickMove();
        if (lc != null && moveId.equals(lc.getMoveId())) return lc;
        var rc = moveset.getRightClickMove();
        if (rc != null && moveId.equals(rc.getMoveId())) return rc;
        var crc = moveset.getCrouchRightClickMove();
        if (crc != null && moveId.equals(crc.getMoveId())) return crc;
        return null;
    }

    public static void executeAttackWithInfo(LivingEntity entity, Object attack, String displayName, int cooldown) {
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) return;
        executeAttackInternal(entity, attack, displayName, cooldown, null, null);
    }

    public static void executeAttackWithInfo(Player player, Object attack, String displayName, int cooldown) {
        executeAttackWithInfo((LivingEntity) player, attack, displayName, cooldown);
    }

    public static void executeAttackWithInfo(LivingEntity entity, Object attack, String movesetId, AbstractMoveset.MoveConfiguration config) {
        if (entity.hasEffect(NichirinEffectRegistry.stunned()) || config == null) return;
        executeAttackInternal(entity, attack, config.getDisplayName(), config.getCooldownOrDefault(0), movesetId, config);
    }

    public static void executeAttackWithInfo(Player player, Object attack, String movesetId, AbstractMoveset.MoveConfiguration config) {
        executeAttackWithInfo((LivingEntity) player, attack, movesetId, config);
    }

    private static void executeAttackInternal(LivingEntity entity, Object attack, String displayName, int cooldown,
                                              String movesetId, AbstractMoveset.MoveConfiguration config) {
        if (!isAttackActive(attack)) {
            applyComboAttackScaling(entity, attack, displayName);
            startAttack(entity, attack);

            if (isAttackActive(attack)) {
                trackAttack(entity, attack);
                if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer && cooldown > 0) {
                    if (movesetId != null && config != null) {
                        CooldownDisplayPacket.sendToClient(serverPlayer, movesetId, config);
                    } else {
                        sendCooldownToClient(serverPlayer, displayName, cooldown);
                    }
                }
            }
        }
    }

    private static void applyComboAttackScaling(LivingEntity entity, Object attack, String displayName) {
        if (!(entity instanceof Player player) || attack == null || comboScaledAttacks.contains(attack)) {
            return;
        }

        ComboTracker.registerAttackStart(player, displayName);
        float multiplier = ComboTracker.getAttackComboMultiplier(player);
        if (multiplier == 1.0f) {
            comboScaledAttacks.add(attack);
            return;
        }

        if (attack instanceof AbstractAttack abstractAttack) {
            abstractAttack.applyDamageAndStunMultiplier(multiplier);
            comboScaledAttacks.add(attack);
            return;
        }

        if (attack instanceof AbstractKatanaAttack katanaAttack) {
            katanaAttack.applyDamageAndStunMultiplier(multiplier);
            comboScaledAttacks.add(attack);
        }
    }

    private static boolean isAttackActive(Object attack) {
        if (attack instanceof AbstractAttack a) return a.isActive();
        try {
            return (boolean) attack.getClass().getMethod("isActive").invoke(attack);
        } catch (Exception e) { return false; }
    }

    private static void startAttack(LivingEntity entity, Object attack) {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            if (entity instanceof Player player) {
                try { breathingAttack.start(player, entity.level()); } catch (Exception e) { BreathOfNichirin.LOGGER.error("Failed to start breathing attack", e); }
            } else {
                try {
                    attack.getClass().getMethod("start", LivingEntity.class, Level.class).invoke(attack, entity, entity.level());
                } catch (Exception e) { BreathOfNichirin.LOGGER.error("Failed to start breathing attack", e); }
            }
            return;
        }

        // Demon attacks and others — try LivingEntity first, then Player overloads
        try {
            attack.getClass().getMethod("start", LivingEntity.class, Level.class).invoke(attack, entity, entity.level());
            return;
        } catch (NoSuchMethodException ignored) {}
        catch (Exception e) { BreathOfNichirin.LOGGER.error("Failed to start attack", e); return; }

        // AbstractKatanaAttack uses start(LivingEntity) without a Level parameter
        try {
            attack.getClass().getMethod("start", LivingEntity.class).invoke(attack, entity);
            return;
        } catch (NoSuchMethodException ignored) {}
        catch (Exception e) { BreathOfNichirin.LOGGER.error("Failed to start attack", e); return; }

        if (entity instanceof Player player) {
            try {
                attack.getClass().getMethod("start", Player.class, Level.class).invoke(attack, player, entity.level());
                return;
            } catch (NoSuchMethodException ignored) {}
            catch (Exception e) { BreathOfNichirin.LOGGER.error("Failed to start attack", e); return; }

            try {
                attack.getClass().getMethod("start", Player.class).invoke(attack, player);
            } catch (Exception e) { BreathOfNichirin.LOGGER.error("Failed to start attack", e); }
        }
    }

    private static int getCooldownForAttack(Object attack) {
        return getIntFromAttack(attack, "getCooldown");
    }

    private static void sendCooldownToClient(ServerPlayer player, String displayName, int cooldown) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(displayName);
            buf.writeInt(cooldown);
            NetworkManager.sendToPlayer(player, COOLDOWN_PACKET_ID, NetworkBufferUtils.server(buf, player));
        } catch (Exception ignored) {}
    }

    /** Send a cooldown display to the player's HUD without executing an attack. */
    public static void sendCooldownDisplay(Player player, String displayName, int cooldownTicks) {
        if (player instanceof ServerPlayer sp && cooldownTicks > 0) {
            sendCooldownToClient(sp, displayName, cooldownTicks);
        }
    }

    public static void sendCooldownDisplay(Player player, String movesetId, AbstractMoveset.MoveConfiguration config) {
        if (player instanceof ServerPlayer sp && config != null && config.getCooldownOrDefault(0) > 0) {
            CooldownDisplayPacket.sendToClient(sp, movesetId, config);
        }
    }

    public static void setHitboxDebugging(boolean enabled) { hitboxDebuggingEnabled = enabled; }
    public static boolean isHitboxDebuggingEnabled() { return hitboxDebuggingEnabled; }

    public static void tickAttacks(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        if (attacks != null) {
            attacks.removeIf(attack -> {
                try { return !tickAndCheckActive(entity, attack); }
                catch (Exception e) { return true; }
            });
            if (attacks.isEmpty()) activeAttacks.remove(entity.getUUID());
        }
    }

    public static void tickAttacks(Player player) { tickAttacks((LivingEntity) player); }

    private static boolean tickAndCheckActive(LivingEntity entity, Object attack) throws Exception {
        if (attack instanceof AbstractBreathingAttack<?, ?> breathingAttack) {
            breathingAttack.tick();
            return breathingAttack.isActive();
        }

        try {
            attack.getClass().getMethod("tick").invoke(attack);
        } catch (NoSuchMethodException e) {
            try {
                attack.getClass().getMethod("tick", LivingEntity.class).invoke(attack, entity);
            } catch (NoSuchMethodException e2) {
                if (entity instanceof Player player) {
                    attack.getClass().getMethod("tick", Player.class).invoke(attack, player);
                }
            }
        }
        return (boolean) attack.getClass().getMethod("isActive").invoke(attack);
    }

    private static void trackAttack(LivingEntity entity, Object attack) {
        if (attack instanceof AbstractBreathingAttack<?, ?> && entity instanceof Player) {
            return;
        }
        if (attack instanceof AbstractDemonAttack<?, ?>) {
            return;
        }
        activeAttacks.computeIfAbsent(entity.getUUID(), k -> new ArrayList<>()).add(attack);
    }

    public static void clearAttacks(LivingEntity entity) {
        var attacks = activeAttacks.remove(entity.getUUID());
        if (attacks != null) {
            for (Object attack : attacks) {
                stopAttackObject(attack);
            }
        }
    }

    public static void clearAttacks(Player player) { clearAttacks((LivingEntity) player); }

    public static boolean hasActiveAttacks(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        return attacks != null && !attacks.isEmpty();
    }

    public static boolean hasActiveAttacks(Player player) { return hasActiveAttacks((LivingEntity) player); }

    public static boolean hasActiveBreathingAttacks(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        if (attacks == null) return false;
        for (Object attack : attacks) {
            if (attack instanceof AbstractBreathingAttack<?, ?>) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasActiveBreathingAttacks(Player player) {
        return hasActiveBreathingAttacks((LivingEntity) player);
    }

    public static int getActiveAttackCount(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        return attacks != null ? attacks.size() : 0;
    }

    public static int getActiveAttackCount(Player player) { return getActiveAttackCount((LivingEntity) player); }

    public static List<Object> getActiveAttacks(LivingEntity entity) {
        var attacks = activeAttacks.get(entity.getUUID());
        return attacks != null ? new ArrayList<>(attacks) : new ArrayList<>();
    }

    public static List<Object> getActiveAttacks(Player player) { return getActiveAttacks((LivingEntity) player); }

    public static boolean stopAttack(LivingEntity entity, Object attack) {
        var attacks = activeAttacks.get(entity.getUUID());
        if (attacks != null && attacks.contains(attack)) {
            if (stopAttackObject(attack)) {
                attacks.remove(attack);
                if (attacks.isEmpty()) activeAttacks.remove(entity.getUUID());
                return true;
            }
            attacks.remove(attack);
            return false;
        }
        return false;
    }

    public static boolean stopAttack(Player player, Object attack) { return stopAttack((LivingEntity) player, attack); }

    private static boolean stopAttackObject(Object attack) {
        try {
            if (attack instanceof AbstractAttack a) {
                a.stop();
            } else {
                attack.getClass().getMethod("stop").invoke(attack);
            }
            return true;
        } catch (NoSuchMethodException ignored) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static int stopAttacksOfType(LivingEntity entity, Class<?> attackType) {
        var attacks = activeAttacks.get(entity.getUUID());
        if (attacks == null) return 0;
        List<Object> toStop = new ArrayList<>();
        for (Object attack : attacks) { if (attackType.isInstance(attack)) toStop.add(attack); }
        int stopped = 0;
        for (Object attack : toStop) { if (stopAttack(entity, attack)) stopped++; }
        return stopped;
    }

    public static int stopAttacksOfType(Player player, Class<?> attackType) {
        return stopAttacksOfType((LivingEntity) player, attackType);
    }

    public static void registerClientHandler() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, COOLDOWN_PACKET_ID, (buf, context) -> {
            String moveName = buf.readUtf();
            int cooldownTicks = buf.readInt();
            context.queue(() -> CooldownHUD.setCooldown(moveName, cooldownTicks));
        });
    }

    public static void tickAllAttacks(MinecraftServer server) {
        if (server == null) return;
        for (var player : server.getPlayerList().getPlayers()) tickAttacks(player);
        AbstractAttack.tickAllActiveAttacks(server);
    }
}
