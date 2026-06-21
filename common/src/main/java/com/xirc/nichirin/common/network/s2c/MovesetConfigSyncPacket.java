package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;


/**
 * Packet to sync captured moveset configurations from server to client
 * This ensures the GUI can display right-click move stats immediately
 */
public class MovesetConfigSyncPacket {

    private final String movesetId;
    private final MoveConfigData rightClickConfig;
    private final MoveConfigData crouchRightClickConfig;

    public MovesetConfigSyncPacket(String movesetId, AbstractMoveset.MoveConfiguration rightClick, AbstractMoveset.MoveConfiguration crouchRightClick) {
        this.movesetId = movesetId;
        this.rightClickConfig = rightClick != null ? new MoveConfigData(rightClick) : null;
        this.crouchRightClickConfig = crouchRightClick != null ? new MoveConfigData(crouchRightClick) : null;
    }

    public MovesetConfigSyncPacket(FriendlyByteBuf buf) {
        this.movesetId = buf.readUtf();
        this.rightClickConfig = buf.readBoolean() ? new MoveConfigData(buf) : null;
        this.crouchRightClickConfig = buf.readBoolean() ? new MoveConfigData(buf) : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(movesetId);

        buf.writeBoolean(rightClickConfig != null);
        if (rightClickConfig != null) {
            rightClickConfig.toBytes(buf);
        }

        buf.writeBoolean(crouchRightClickConfig != null);
        if (crouchRightClickConfig != null) {
            crouchRightClickConfig.toBytes(buf);
        }
    }

    public void handleClient() {
        // Apply on client side
        AbstractMoveset moveset = NichirinMovesetRegistry.getMoveset(movesetId);
        if (moveset != null) {
            if (rightClickConfig != null) {
                moveset.captureRightClickConfig(rightClickConfig.toMoveConfig(), false);
            }
            if (crouchRightClickConfig != null) {
                moveset.captureRightClickConfig(crouchRightClickConfig.toMoveConfig(), true);
            }
        }
    }

    /**
     * Serializable move configuration data
     */
    public static class MoveConfigData {
        private final String moveId;
        private final String displayName;
        private final String description;
        private final ResourceLocation animationId;
        private final int animationPriority;

        private final Float damage;
        private final Float range;
        private final Float knockback;
        private final Integer hitStun;
        private final Float hitboxSize;

        private final Integer cooldown;
        private final Integer windup;
        private final Integer duration;
        private final Integer activeFrames;
        private final Integer recovery;

        private final Float breathCost;
        private final Float staminaCost;

        private final Float teleportDistance;
        private final Float dashSpeed;
        private final Integer teleportWindup;

        public MoveConfigData(AbstractMoveset.MoveConfiguration config) {
            this.moveId = config.getMoveId();
            this.displayName = config.getDisplayName();
            this.description = config.getDescription();
            this.animationId = config.getAnimationId();
            this.animationPriority = config.getAnimationPriority();

            this.damage = config.getDamage();
            this.range = config.getRange();
            this.knockback = config.getKnockback();
            this.hitStun = config.getHitStun();
            this.hitboxSize = config.getHitboxSize();

            this.cooldown = config.getCooldown();
            this.windup = config.getWindup();
            this.duration = config.getDuration();
            this.activeFrames = config.getActiveFrames();
            this.recovery = config.getRecovery();

            this.breathCost = config.getBreathCost();
            this.staminaCost = config.getStaminaCost();

            this.teleportDistance = config.getTeleportDistance();
            this.dashSpeed = config.getDashSpeed();
            this.teleportWindup = config.getTeleportWindup();
        }

        public MoveConfigData(FriendlyByteBuf buf) {
            this.moveId = buf.readUtf();
            this.displayName = buf.readUtf();
            this.description = buf.readBoolean() ? buf.readUtf() : null;
            this.animationId = buf.readBoolean() ? buf.readResourceLocation() : null;
            this.animationPriority = buf.readInt();

            this.damage = buf.readBoolean() ? buf.readFloat() : null;
            this.range = buf.readBoolean() ? buf.readFloat() : null;
            this.knockback = buf.readBoolean() ? buf.readFloat() : null;
            this.hitStun = buf.readBoolean() ? buf.readInt() : null;
            this.hitboxSize = buf.readBoolean() ? buf.readFloat() : null;

            this.cooldown = buf.readBoolean() ? buf.readInt() : null;
            this.windup = buf.readBoolean() ? buf.readInt() : null;
            this.duration = buf.readBoolean() ? buf.readInt() : null;
            this.activeFrames = buf.readBoolean() ? buf.readInt() : null;
            this.recovery = buf.readBoolean() ? buf.readInt() : null;

            this.breathCost = buf.readBoolean() ? buf.readFloat() : null;
            this.staminaCost = buf.readBoolean() ? buf.readFloat() : null;

            this.teleportDistance = buf.readBoolean() ? buf.readFloat() : null;
            this.dashSpeed = buf.readBoolean() ? buf.readFloat() : null;
            this.teleportWindup = buf.readBoolean() ? buf.readInt() : null;
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(moveId);
            buf.writeUtf(displayName);

            buf.writeBoolean(description != null);
            if (description != null) buf.writeUtf(description);

            buf.writeBoolean(animationId != null);
            if (animationId != null) buf.writeResourceLocation(animationId);

            buf.writeInt(animationPriority);

            buf.writeBoolean(damage != null);
            if (damage != null) buf.writeFloat(damage);

            buf.writeBoolean(range != null);
            if (range != null) buf.writeFloat(range);

            buf.writeBoolean(knockback != null);
            if (knockback != null) buf.writeFloat(knockback);

            buf.writeBoolean(hitStun != null);
            if (hitStun != null) buf.writeInt(hitStun);

            buf.writeBoolean(hitboxSize != null);
            if (hitboxSize != null) buf.writeFloat(hitboxSize);

            buf.writeBoolean(cooldown != null);
            if (cooldown != null) buf.writeInt(cooldown);

            buf.writeBoolean(windup != null);
            if (windup != null) buf.writeInt(windup);

            buf.writeBoolean(duration != null);
            if (duration != null) buf.writeInt(duration);

            buf.writeBoolean(activeFrames != null);
            if (activeFrames != null) buf.writeInt(activeFrames);

            buf.writeBoolean(recovery != null);
            if (recovery != null) buf.writeInt(recovery);

            buf.writeBoolean(breathCost != null);
            if (breathCost != null) buf.writeFloat(breathCost);

            buf.writeBoolean(staminaCost != null);
            if (staminaCost != null) buf.writeFloat(staminaCost);

            buf.writeBoolean(teleportDistance != null);
            if (teleportDistance != null) buf.writeFloat(teleportDistance);

            buf.writeBoolean(dashSpeed != null);
            if (dashSpeed != null) buf.writeFloat(dashSpeed);

            buf.writeBoolean(teleportWindup != null);
            if (teleportWindup != null) buf.writeInt(teleportWindup);
        }

        public AbstractMoveset.MoveConfiguration toMoveConfig() {
            return new AbstractMoveset.MoveBuilder(moveId, displayName)
                    .withDescription(description)
                    .withAnimation(animationId != null ? animationId.toString() : null, animationPriority)
                    .withDamage(damage != null ? damage : 0)
                    .withRange(range != null ? range : 0)
                    .withKnockback(knockback != null ? knockback : 0)
                    .withHitStun(hitStun != null ? hitStun : 0)
                    .withHitboxSize(hitboxSize != null ? hitboxSize : 0)
                    .withCooldown(cooldown != null ? cooldown : 0)
                    .withWindup(windup != null ? windup : 0)
                    .withDuration(duration != null ? duration : 0)
                    .withActiveFrames(activeFrames != null ? activeFrames : 0)
                    .withRecovery(recovery != null ? recovery : 0)
                    .withBreathCost(breathCost != null ? breathCost : 0)
                    .withStaminaCost(staminaCost != null ? staminaCost : 0)
                    .withTeleportDistance(teleportDistance != null ? teleportDistance : 0)
                    .withDashSpeed(dashSpeed != null ? dashSpeed : 0)
                    .withTeleportWindup(teleportWindup != null ? teleportWindup : 0)
                    .build();
        }
    }
}