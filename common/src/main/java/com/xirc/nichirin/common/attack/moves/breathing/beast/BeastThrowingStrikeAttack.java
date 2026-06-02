package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.entity.projectile.ThrownKatanaEntity;
import com.xirc.nichirin.common.item.katana.BeastKatana;
import com.xirc.nichirin.common.item.katana.IndividualBeastKatana;
import com.xirc.nichirin.common.item.katana.IndividualSoundKatana;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.item.katana.SoundKatana;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Eleventh Fang: Sudden Throwing Strike. Throws held katanas as piercing projectiles.
public class BeastThrowingStrikeAttack extends BeastBreathingAttackBase {

    private boolean thrown = false;

    @Override
    protected void onStart() {
        thrown = false;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide || thrown) return;

        throwKatanas();
        thrown = true;
    }

    private void throwKatanas() {
        ItemStack mainhand = user.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offhand = user.getItemInHand(InteractionHand.OFF_HAND);

        boolean hasMain = isKatana(mainhand);
        boolean hasOff = isKatana(offhand);

        Vec3 look = user.getLookAngle();
        Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 origin = user.getEyePosition();
        ItemStack fallback = new ItemStack(NichirinItemRegistry.KATANA.get());

        if (hasMain && hasOff) {
            spawnKatana(origin.add(perp.scale(-0.3)), look, damage, mainhand);
            spawnKatana(origin.add(perp.scale(0.3)), look, damage, offhand);
        } else if (hasMain) {
            spawnKatana(origin, look, damage, mainhand);
        } else if (hasOff) {
            spawnKatana(origin, look, damage, offhand);
        } else {
            spawnKatana(origin.add(perp.scale(-0.3)), look, damage, fallback);
            spawnKatana(origin.add(perp.scale(0.3)), look, damage, fallback);
        }

        createThrowEffect();
    }

    private static boolean isKatana(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof SimpleKatana
                || stack.getItem() instanceof BeastKatana
                || stack.getItem() instanceof IndividualBeastKatana
                || stack.getItem() instanceof SoundKatana
                || stack.getItem() instanceof IndividualSoundKatana);
    }

    private void spawnKatana(Vec3 spawnPos, Vec3 direction, float dmg, ItemStack item) {
        if (!(world instanceof ServerLevel sl)) return;

        ThrownKatanaEntity katana = new ThrownKatanaEntity(
                NichirinEntityRegistry.THROWN_KATANA.get(), sl, user, dmg, hitStun);
        katana.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        katana.setThrownItem(item);

        double speed = 2.5;
        katana.setDeltaMovement(
                direction.x * speed,
                direction.y * speed,
                direction.z * speed
        );

        sl.addFreshEntity(katana);
    }

    private void createThrowEffect() {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 pos = user.getEyePosition();
        sl.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 15, 0.3, 0.3, 0.3, 0.3);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0);
    }

    @Override
    protected void onStop() {
        thrown = false;
    }
}