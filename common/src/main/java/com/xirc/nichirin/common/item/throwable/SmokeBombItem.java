package com.xirc.nichirin.common.item.throwable;

import com.xirc.nichirin.common.entity.projectile.SmokeBombEntity;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class SmokeBombItem extends Item implements ProjectileItem {

    /** Time between the throw animation starting and the grenade actually leaving the hand. */
    private static final int THROW_DELAY_TICKS = 12; // 0.6s * 20 ticks/s

    public SmokeBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        SmokeBombEntity smokeBomb = new SmokeBombEntity(level, position.x(), position.y(), position.z());
        smokeBomb.setItem(stack);
        return smokeBomb;
    }

    @Override
    public @NonNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Snapshot a single copy now, before the stack gets shrunk below, so the thrown
            // entity still renders the right icon once the delayed release fires later.
            ItemStack renderStack = itemStack.copyWithCount(1);

            String animationName = hand == InteractionHand.OFF_HAND ? "smokebomb_throw_left" : "smokebomb_throw";
            NichirinPacketRegistry.broadcastPlayerAnimation(
                    serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), animationName) //animation applying
            );

            SmokeBombThrowScheduler.schedule(serverPlayer, renderStack, THROW_DELAY_TICKS);
        }

        player.getCooldowns().addCooldown(this, 100);

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    /** Actually spawns and throws the grenade - called by SmokeBombThrowScheduler once THROW_DELAY_TICKS has elapsed. */
    static void releaseGrenade(ServerLevel level, ServerPlayer player, ItemStack renderStack) {
        SmokeBombEntity grenade = getGrenade(level, player, renderStack);
        level.addFreshEntity(grenade);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 1F, 0.4F);
    }

    private static @NonNull SmokeBombEntity getGrenade(Level level, Player player, ItemStack itemStack) {
        float yawRad = (float) Math.toRadians(player.getYRot());
        double lookX = -Math.sin(yawRad);
        double lookZ = Math.cos(yawRad);

        double offset = 0.4;
        double spawnX = player.getX() + (lookX * offset);
        double spawnY = player.getEyeY() - 0.1;
        double spawnZ = player.getZ() + (lookZ * offset);

        SmokeBombEntity grenade = new SmokeBombEntity(level, spawnX, spawnY, spawnZ);
        grenade.setItem(itemStack);
        grenade.setOwner(player);

        grenade.shoot(0.0, -1.0, 0.0, 1.5F, 1.0F);

        return grenade;
    }
}