package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.thunder.*;
import com.xirc.nichirin.common.util.BreathingManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thunder Breathing moveset implementation
 * All 7 forms of Thunder Breathing
 */
public class ThunderBreathingMoveset extends AbstractMoveset {

    // Track cooldowns per player per move
    private static final Map<UUID, Map<Integer, Long>> playerCooldowns = new HashMap<>();

    // Track active attacks to prevent breath consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    public ThunderBreathingMoveset() {
        super("thunder_breathing", "Thunder Breathing", createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:thunder_idle")
                .withSpeedMultiplier(1.3f) // Thunder Breathing emphasizes speed

                // First Form: Thunderclap and Flash
                .withMove(new MoveBuilder("thunderclap_flash", "Thunderclap and Flash")
                        .withIcon("nichirin:textures/gui/moves/thunder_first_form.png")
                        .withAnimation("nichirin:thunderclap_flash", 10)
                        .withStats(8.0f, 15.0f, 30) // 30 tick cooldown (1.5 seconds)
                        .withBreathCost(20.0f)
                        .withAction(player -> {
                            ThunderClapFlashAttack attack = new ThunderClapFlashAttack();
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "thunderclap_flash");
                        })
                )

                // Second Form: Rice Spirit
                .withMove(new MoveBuilder("rice_spirit", "Rice Spirit")
                        .withIcon("nichirin:textures/gui/moves/thunder_second_form.png")
                        .withAnimation("nichirin:rice_spirit", 8)
                        .withStats(8.0f, 5.0f, 40)
                        .withBreathCost(25.0f)
                        .withAction(player -> {
                            RiceSpiritAttack attack = new RiceSpiritAttack();
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "rice_spirit");
                        })
                )

                // Third Form: Thunder Swarm
                .withMove(new MoveBuilder("thunder_swarm", "Thunder Swarm")
                        .withIcon("nichirin:textures/gui/moves/thunder_third_form.png")
                        .withAnimation("nichirin:thunder_swarm", 9)
                        .withStats(10.0f, 8.0f, 50)
                        .withBreathCost(30.0f)
                        .withAction(player -> {
                            ThunderSwarmAttack attack = new ThunderSwarmAttack();
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "thunder_swarm");
                        })
                )

                // Fourth Form: Distant Thunder
                .withMove(new MoveBuilder("distant_thunder", "Distant Thunder")
                        .withIcon("nichirin:textures/gui/moves/thunder_fourth_form.png")
                        .withAnimation("nichirin:distant_thunder", 7)
                        .withStats(8.0f, 20.0f, 80)
                        .withBreathCost(25.0f)
                        .withAction(player -> {
                            DistantThunderAttack attack = new DistantThunderAttack();
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "distant_thunder");
                        })
                )

                // Fifth Form: Heat Lightning
                .withMove(new MoveBuilder("heat_lightning", "Heat Lightning")
                        .withIcon("nichirin:textures/gui/moves/thunder_fifth_form.png")
                        .withAnimation("nichirin:heat_lightning", 9)
                        .withStats(14.0f, 12.0f, 60)
                        .withBreathCost(25.0f)
                        .withAction(player -> {
                            HeatLightningAttack attack = new HeatLightningAttack();
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "heat_lightning");
                        })
                )

                // Sixth Form: Rumble and Flash
                .withMove(new MoveBuilder("rumble_flash", "Rumble and Flash")
                        .withIcon("nichirin:textures/gui/moves/thunder_sixth_form.png")
                        .withAnimation("nichirin:rumble_flash", 8)
                        .withStats(16.0f, 25.0f, 70)
                        .withBreathCost(35.0f)
                        .withAction(player -> {
                            RumbleFlashAttack attack = new RumbleFlashAttack();
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "rumble_flash");
                        })
                )

                // Seventh Form: Honoikazuchi no Kami
                .withMove(new MoveBuilder("honoikazuchi_no_kami", "Honoikazuchi no Kami")
                        .withIcon("nichirin:textures/gui/moves/thunder_seventh_form.png")
                        .withAnimation("nichirin:honoikazuchi_no_kami", 15)
                        .withStats(50.0f, 30.0f, 300) // 15 second cooldown
                        .withBreathCost(50.0f)
                        .withAction(player -> {
                            HonoikazuchiNoKamiAttack attack = new HonoikazuchiNoKamiAttack();
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "honoikazuchi_no_kami");
                        })
                );
    }

    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        // Always Thunder Clap and Flash - just store the crouch state
        ThunderClapFlashAttack.setCrouchDash(player, isCrouching);

        // Execute Thunder Clap and Flash (index 0)
        performMove(player, 0);
        onMovePerformed(player, 0, isCrouching);

        return true;
    }

    @Override
    public void performMove(Player player, int moveIndex) {
        // Check cooldown before allowing move
        if (!canUseMove(player, moveIndex)) {
            // Show cooldown message
            MoveConfiguration config = getMove(moveIndex);
            if (config != null) {
                Map<Integer, Long> cooldowns = playerCooldowns.get(player.getUUID());
                if (cooldowns != null) {
                    Long cooldownEnd = cooldowns.get(moveIndex);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - player.level().getGameTime()) / 20;
                        player.displayClientMessage(
                                Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0xFFFF00)),
                                true
                        );
                    }
                }
            }
            return;
        }

        // Check breath BEFORE executing
        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            // Get the breath cost from the move configuration
            float breathCost = config.getBreathCost();

            if (!BreathingManager.hasBreath(player, breathCost)) {
                player.displayClientMessage(
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                return;
            }
        }

        // Mark that we're executing a move
        executingMove.put(player.getUUID(), true);

        // Execute the move
        super.performMove(player, moveIndex);

        // Check if move actually executed by seeing if breath was consumed
        boolean moveExecuted = !executingMove.getOrDefault(player.getUUID(), false);
        executingMove.remove(player.getUUID());

        if (moveExecuted && config != null) {
            // Set cooldown after successful execution
            setMoveCooldown(player, moveIndex);

            // Send cooldown display packet if on server and has cooldown
            // Skip cooldown display for Thunder Clap Flash (index 0)
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer
                    && config.getCooldown() > 0 && moveIndex != 0) {  // Added moveIndex != 0 check
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeUtf(config.getDisplayName());
                buf.writeInt(config.getCooldown());

                NetworkManager.sendToPlayer(serverPlayer, new ResourceLocation("nichirin", "cooldown_display"), buf);
            }
        }
    }

    /**
     * Check if a player can use a specific move (not on cooldown)
     */
    private boolean canUseMove(Player player, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldown() <= 0) {
            return true; // No cooldown
        }

        Map<Integer, Long> cooldowns = playerCooldowns.get(player.getUUID());
        if (cooldowns == null) {
            return true; // No cooldowns tracked yet
        }

        Long cooldownEnd = cooldowns.get(moveIndex);
        if (cooldownEnd == null) {
            return true; // Move never used
        }

        long currentTime = player.level().getGameTime();
        return currentTime >= cooldownEnd;
    }

    /**
     * Set a move on cooldown
     */
    private void setMoveCooldown(Player player, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldown() <= 0) {
            return; // No cooldown
        }

        long cooldownEnd = player.level().getGameTime() + config.getCooldown();
        playerCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
    }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return 0; // Always Thunder Clap and Flash
    }

    @Override
    public String getRightClickMoveName() {
        return "Thunder Clap and Flash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Thunder Clap and Flash"; // Same move, just with backwards turn
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Special handling for Thunder Clap and Flash when used while initially crouching
        if (moveIndex == 0 && isCrouching) {
            // The crouch state is already stored in ThunderClapFlashAttack via setCrouchDash
        }
    }

    /**
     * Called when a player logs out - clean up their data
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
        executingMove.remove(player.getUUID());
    }
}