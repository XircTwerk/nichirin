package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.config.NichirinServerConfig;
import com.xirc.nichirin.common.effect.WisteriasGraceStatusEffect;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.util.WisteriaBlocks;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class WisteriaGraceHandler {

    private static final int CHECK_INTERVAL = 20;
    private static final int EFFECT_DURATION = 60;
    private static final int RADIUS = 6;
    private static final Set<UUID> reducingDamage = new HashSet<>();

    private WisteriaGraceHandler() {}

    public static void register() {
        TickEvent.SERVER_PRE.register(WisteriaGraceHandler::tick);
        EntityEvent.LIVING_HURT.register(WisteriaGraceHandler::reduceDemonDamage);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (DemonManager.isDemon(player)) {
                applyFromNearbyWisteria(player);
            }

            AABB nearby = player.getBoundingBox().inflate(96.0);
            for (DemonNPCEntity demon : player.serverLevel().getEntitiesOfClass(DemonNPCEntity.class, nearby, LivingEntity::isAlive)) {
                applyFromNearbyWisteria(demon);
            }
        }
    }

    private static EventResult reduceDemonDamage(LivingEntity target, DamageSource source, float amount) {
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return EventResult.pass();
        if (attacker.level().isClientSide) return EventResult.pass();
        if (!WisteriasGraceStatusEffect.affects(attacker)) return EventResult.pass();
        if (!attacker.hasEffect(NichirinEffectRegistry.wisteriasGrace())) return EventResult.pass();
        if (reducingDamage.contains(attacker.getUUID())) return EventResult.pass();

        reducingDamage.add(attacker.getUUID());
        target.hurt(source, amount * 0.5f);
        reducingDamage.remove(attacker.getUUID());
        return EventResult.interruptFalse();
    }

    private static void applyFromNearbyWisteria(LivingEntity entity) {
        // When wisteria isn't set to affect demons, don't apply the grace effect at all — otherwise
        // the slow/particles (and its residual damage) would still fire with the config off.
        if (!NichirinServerConfig.get().demon.wisteriaDamagesDemons) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (hasNearbyWisteriaLight(level, entity.blockPosition())) {
            entity.addEffect(new MobEffectInstance(NichirinEffectRegistry.wisteriasGrace(), EFFECT_DURATION, 0, true, true, true));
        }
    }

    private static boolean hasNearbyWisteriaLight(ServerLevel level, BlockPos center) {
        int radiusSqr = RADIUS * RADIUS;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (x * x + y * y + z * z > radiusSqr) continue;
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    var state = level.getBlockState(mutable);
                    if (WisteriaBlocks.isWisteriaLightSource(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
