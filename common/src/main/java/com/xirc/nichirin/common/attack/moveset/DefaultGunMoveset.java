package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.gun.GunBashAttack;
import com.xirc.nichirin.common.attack.moves.gun.GunGrabAttack;
import com.xirc.nichirin.common.attack.moves.gun.GunShotAttack;
import com.xirc.nichirin.common.item.gun.GenyaDB;
import com.xirc.nichirin.common.system.GrabManager;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default moveset for the Genya double-barrel ({@link GenyaDB}).
 *
 * <p>Unlike katanas this weapon doesn't use the block-based input scheme: right-click fires
 * directly. The two wheel moves are Gun Bash (0) and Grab (1). Every attack is an
 * {@link com.xirc.nichirin.common.attack.moves.AbstractKatanaAttack} configured from its
 * {@link MoveConfiguration} and run through {@link MoveExecutor}, matching the other movesets.</p>
 *
 * <ul>
 *   <li>Left-click — fire 1 bullet</li>
 *   <li>Right-click — fire both bullets</li>
 *   <li>Crouch + Right-click — reload to {@link GenyaDB#MAX_AMMO}</li>
 *   <li>Wheel 0 — Gun Bash, Wheel 1 — Grab</li>
 * </ul>
 */
public class DefaultGunMoveset extends AbstractMoveset {

    public static final DefaultGunMoveset INSTANCE = new DefaultGunMoveset();

    private static final int RELOAD_TICKS = 30;
    private static final float PISTOL_WHIP_DAMAGE = 3.0f;

    private static final Map<UUID, Long> reloadUntil = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> moveCooldowns = new HashMap<>();

    private DefaultGunMoveset() {
        super("default_gun", "Genya DB", MovesetType.NEUTRAL, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withMove(new MoveBuilder("shoot", "Shoot")
                        .withTiming(0, 0, 1)
                        .withRecovery(4)
                        .withDamage(16.0f)
                        .withRange(30.0f)
                        .withKnockback(0.6f)
                        .asLeftClick())

                .withMove(new MoveBuilder("double_shot", "Double Shot")
                        .withTiming(0, 0, 1)
                        .withRecovery(15)
                        .withDamage(16.0f)
                        .withRange(30.0f)
                        .withKnockback(0.6f)
                        .asRightClick())

                .withMove(new MoveBuilder("gun_bash", "Gun Bash")
                        .withTiming(60, 1, 4)
                        .withRecovery(6)
                        .withDamage(6.0f)
                        .withRange(1.5f)
                        .withKnockback(0.4f)
                        .withHitboxSize(1.2f)
                        .withHitStun(40)
                        .withDescription("Swift dash forward into a heavy-stun pistol-whip.")
                        .withAction(entity -> {
                            GunBashAttack attack = GunBashAttack.createDefault();
                            attack.configure(INSTANCE.getMove(0));
                            MoveExecutor.executeAttack(entity, attack, "default_gun", "gun_bash");
                        }))

                .withMove(new MoveBuilder("grab", "Grab")
                        .withTiming(80, 1, 1)
                        .withRecovery(6)
                        .withRange(2.0f)
                        .withHitboxSize(1.5f)
                        .withHitStun(40)                        .withDescription("Grab the enemy in front of you; left/right-click to fire point-blank.")
                        .withAction(entity -> {
                            GunGrabAttack attack = GunGrabAttack.createDefault();
                            attack.configure(INSTANCE.getMove(1));
                            MoveExecutor.executeAttack(entity, attack, "default_gun", "grab");
                        }));
    }

    @Override
    public int getMoveCount() {
        return 2;
    }

    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (entity instanceof Player player) fire(player, 1);
        return true;
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity instanceof Player player) reloadGun(player);
        return true;
    }

    /** Fire {@code barrels} barrels: 1 for a tapped left-click, 2 for a held left-click. */
    public void fire(Player player, int barrels) {
        if (player.level().isClientSide) return;
        MoveConfiguration config = barrels >= 2 ? getRightClickConfiguration() : getLeftClickConfiguration();
        shoot(player, barrels, config);
    }

    /** Reload to full (right-click). */
    public void reloadGun(Player player) {
        if (player.level().isClientSide) return;
        reload(player);
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (entity.level().isClientSide) return;
        if (!canPerformMoves(entity)) return;
        if (onCooldown(entity, moveIndex)) return;

        super.performMove(entity, moveIndex);

        MoveConfiguration config = getMove(moveIndex);
        if (config != null && config.getCooldownOrDefault(0) > 0) {
            setCooldown(entity, moveIndex, config.getCooldownOrDefault(0));
        }
    }

    private void shoot(Player player, int barrels, MoveConfiguration config) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GenyaDB)) return;

        if (GrabManager.isGrabbing(player)) {
            grabbedShot(player, stack, barrels);
            return;
        }

        if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
        if (MoveExecutor.hasActiveAttacks(player)) return;
        if (player.level().getGameTime() < reloadUntil.getOrDefault(player.getUUID(), 0L)) return;

        int ammo = GenyaDB.getAmmo(stack);
        if (ammo <= 0) {
            emptyClick(player);
            return;
        }

        int toFire = Math.min(barrels, ammo);
        GunShotAttack attack = GunShotAttack.create(toFire);
        attack.configure(config);
        MoveExecutor.executeAttack(player, attack, "default_gun", config.getMoveId());

        GenyaDB.setAmmo(stack, ammo - toFire);
        String animName = toFire >= 2 ? "fire" : ammo == 2 ? "fireleft" : "fireright";
        NichirinPacketRegistry.sendGunAnimation(player, animName);
        triggerAnimation(player, "fire");
        showAmmo(player, ammo - toFire);
    }

    private void grabbedShot(Player player, ItemStack stack, int barrels) {
        LivingEntity target = GrabManager.getGrabbedTarget(player);
        if (target != null) {
            int ammo = GenyaDB.getAmmo(stack);
            if (ammo > 0) {
                int toFire = Math.min(barrels, ammo);
                float mult = GunShotAttack.damageMultiplier(player.getEyePosition().distanceTo(target.position()));
                NichirinArmorDamage.hurt(target, NichirinDamageSources.blade(player),
                        getLeftClickConfiguration().getDamageOrDefault(0f) * toFire * mult);
                target.invulnerableTime = 0;
                GenyaDB.setAmmo(stack, ammo - toFire);
                String animName = toFire >= 2 ? "fire" : ammo == 2 ? "fireleft" : "fireright";
                NichirinPacketRegistry.sendGunAnimation(player, animName);
                triggerAnimation(player, "fire");
                playShotSound(player, toFire);
                GunShotAttack.sendShootShake(player, toFire >= 2 ? 0.85f : 0.7f);
                impactFx(target);
                showAmmo(player, ammo - toFire);
            } else {
                NichirinArmorDamage.hurt(target, NichirinDamageSources.blade(player), PISTOL_WHIP_DAMAGE);
                target.invulnerableTime = 0;
                playSound(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                impactFx(target);
            }
        }
        GrabManager.releaseGrab(player, true);
    }

    private void reload(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GenyaDB)) return;
        long now = player.level().getGameTime();        if (now < reloadUntil.getOrDefault(player.getUUID(), 0L)) return;
        if (GenyaDB.getAmmo(stack) >= GenyaDB.MAX_AMMO) {
            player.displayClientMessage(Component.literal("Already loaded").withStyle(s -> s.withColor(0xAAAAAA)), true);
            return;
        }
        reloadUntil.put(player.getUUID(), now + RELOAD_TICKS);
        GenyaDB.setAmmo(stack, GenyaDB.MAX_AMMO);
        NichirinPacketRegistry.sendGunAnimation(player, "reload");
        triggerAnimation(player, "reload");
        playRandomized(player, NicirinSoundRegistry.GENYA_RELOAD.get(), 0.9f, 1.0f, 0.06f);
        player.displayClientMessage(Component.literal("Reloading...").withStyle(s -> s.withColor(0xFFD080)), true);
    }

    private void emptyClick(Player player) {
        playSound(player, SoundEvents.LEVER_CLICK, 0.6f, 1.4f);
        player.displayClientMessage(
                Component.literal("Empty! Crouch + Right-click to reload").withStyle(s -> s.withColor(0xFF5555)), true);
    }

    private boolean onCooldown(LivingEntity entity, int moveIndex) {
        Map<Integer, Long> cds = moveCooldowns.get(entity.getUUID());
        if (cds == null) return false;
        Long end = cds.get(moveIndex);
        return end != null && entity.level().getGameTime() < end;
    }

    private void setCooldown(LivingEntity entity, int moveIndex, int ticks) {
        moveCooldowns.computeIfAbsent(entity.getUUID(), k -> new HashMap<>())
                .put(moveIndex, entity.level().getGameTime() + ticks);
    }

    private void impactFx(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel sl)) return;
        Vec3 p = target.position().add(0, target.getBbHeight() * 0.5, 0);
        sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 8, 0.2, 0.2, 0.2, 0.05);
    }

    private void showAmmo(Player player, int ammo) {
        player.displayClientMessage(
                Component.literal("Ammo: " + ammo + "/" + GenyaDB.MAX_AMMO)
                        .withStyle(s -> s.withColor(ammo > 0 ? 0xFFFFFF : 0xFF5555)), true);
    }

    private void playShotSound(Player player, int barrels) {
        SoundEvent sound = barrels >= 2
                ? NicirinSoundRegistry.GENYA_DOUBLESHOT.get()
                : NicirinSoundRegistry.GENYA_SINGLESHOT.get();
        playRandomized(player, sound, 1.0f, 1.0f, 0.08f);
    }

    private void playRandomized(Player player, SoundEvent sound, float baseVolume, float basePitch, float pitchSpread) {
        RandomSource random = player.level().getRandom();
        float pitch = basePitch + (random.nextFloat() - 0.5f) * 2.0f * pitchSpread;
        float volume = baseVolume * (0.92f + random.nextFloat() * 0.08f);
        playSound(player, sound, volume, pitch);
    }

    private void playSound(Player player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void cleanupPlayer(Player player) {
        UUID id = player.getUUID();
        reloadUntil.remove(id);
        moveCooldowns.remove(id);
    }
}
