package com.xirc.nichirin.common.attack.moves.breathing.water;

import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

// Eleventh Form: Dead Calm. Establishes a persistent field that auto-slashes all enemies who enter or remain in range.
public class DeadCalmAttack extends WaterBreathingAttackBase {

    private boolean fieldActive = false;
    private Vec3 fieldCenter;
    private Vec3 frozenPos;
    private final Set<LivingEntity> entitiesInField = new HashSet<>();
    private final Set<LivingEntity> recentlyTriggered = new HashSet<>();
    private int calmTicks = 0;

    @Override
    protected void onStart() {
        fieldActive = false;
        fieldCenter = null;
        entitiesInField.clear();
        recentlyTriggered.clear();
        calmTicks = 0;
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.5f);


        if (!world.isClientSide) {
            sendShaderPacketToNearbyPlayers();
        }
    }

    private void sendShaderPacketToNearbyPlayers() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(userPos) < 10000) {
                NichirinPacketRegistry.sendToPlayer(
                        new TriggerShaderPacket(
                                "com.xirc.nichirin.client.shader.DeadCalmShaderEffect",
                                true
                        ),
                        player
                );
            }
        }
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!fieldActive && tickCount == windup + 1) {
            establishCalmField();
            fieldActive = true;
        }

        if (fieldActive && tickCount > windup && tickCount < windup + duration) {
            if (hasMovedFromCalm()) {
                stop();
                return;
            }
            calmTicks++;
            maintainCalmField();
        }
    }


    private void establishCalmField() {
        frozenPos = user.position();

        Vec3 lookDir = user.getLookAngle();
        fieldCenter = user.position();
        playWaterVfx(VfxIds.DEAD_CALM, fieldCenter, lookDir, 1.0f);


        world.playSound(null, fieldCenter.x, fieldCenter.y, fieldCenter.z,
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    private void maintainCalmField() {
        if (fieldCenter == null || frozenPos == null) return;

        // Refresh before the previous pulse finishes fading so the field remains visible for
        // the full channel. Movement cancellation stops scheduling these immediately.
        if (calmTicks % 32 == 0) {
            playWaterVfx(VfxIds.DEAD_CALM, fieldCenter, user.getLookAngle(), 1.0f);
        }

        // Anchor the user in place — teleport overrides client-side prediction
        if (user instanceof ServerPlayer sp) {
            sp.teleportTo(frozenPos.x, frozenPos.y, frozenPos.z);
        } else {
            user.absMoveTo(frozenPos.x, frozenPos.y, frozenPos.z, user.getYRot(), user.getXRot());
        }
        user.setDeltaMovement(Vec3.ZERO);

        updateEntitiesInField();
        triggerAutoSlashes();

        if (calmTicks % 40 == 0) {
            world.playSound(null, fieldCenter.x, fieldCenter.y, fieldCenter.z,
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.4f, 0.7f);
        }
    }

    private boolean hasMovedFromCalm() {
        if (frozenPos == null) return false;
        Vec3 movement = user.position().subtract(frozenPos);
        return movement.horizontalDistanceSqr() > 0.0025
                || Math.abs(movement.y) > 0.05;
    }

    private void updateEntitiesInField() {
        AABB fieldArea = new AABB(
                fieldCenter.x - range, fieldCenter.y - 2, fieldCenter.z - range,
                fieldCenter.x + range, fieldCenter.y + 4, fieldCenter.z + range
        );

        List<LivingEntity> currentEntities = world.getEntitiesOfClass(LivingEntity.class, fieldArea,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator());

        Set<LivingEntity> newEntrants = new HashSet<>(currentEntities);
        newEntrants.removeAll(entitiesInField);

        for (LivingEntity newEntrant : newEntrants) {
            if (!recentlyTriggered.contains(newEntrant)) {
                triggerSlashSequence(newEntrant);
                recentlyTriggered.add(newEntrant);
            }
        }

        entitiesInField.clear();
        entitiesInField.addAll(currentEntities);

        // Re-arm slashes every 5 ticks. Tuned when breathing attacks were accidentally double-ticked
        // (pre MoveExecutor dedup) — halved from 10 to keep the original real-time slash rate.
        if (calmTicks % 5 == 0) {
            recentlyTriggered.clear();
        }
    }

    private void triggerAutoSlashes() {
        if (calmTicks % 2 == 0) {
            for (LivingEntity entity : entitiesInField) {
                if (!recentlyTriggered.contains(entity)) {
                    triggerSlashSequence(entity);
                    recentlyTriggered.add(entity);
                }
            }
        }
    }

    private void triggerSlashSequence(LivingEntity target) {
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

        for (int slash = 0; slash < 3; slash++) {
            scheduleSlash(target, targetPos, slash * 4);
        }

        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    private void scheduleSlash(LivingEntity target, Vec3 targetPos, int delay) {
        hitTargetNoImmunity(target);


        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.6f, 1.5f + delay * 0.1f);
    }


    @Override
    public boolean isPersistentArea() {
        return true;
    }

    @Override
    protected void onStop() {
        world.playSound(null, fieldCenter != null ? fieldCenter.x : user.getX(),
                fieldCenter != null ? fieldCenter.y : user.getY(),
                fieldCenter != null ? fieldCenter.z : user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f);

        entitiesInField.clear();
        recentlyTriggered.clear();
        fieldActive = false;
        fieldCenter = null;
        frozenPos = null;
        calmTicks = 0;
    }
}
