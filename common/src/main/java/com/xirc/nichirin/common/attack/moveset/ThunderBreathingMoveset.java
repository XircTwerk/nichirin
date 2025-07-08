package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.thunder.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Thunder Breathing moveset implementation
 * All 7 forms of Thunder Breathing
 */
public class ThunderBreathingMoveset extends AbstractMoveset {

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
                        .withStats(15.0f, 15.0f, 1)
                        .withAction(player -> MoveExecutor.executeThunderAttack(player, new ThunderClapFlashAttack()))
                )

                // Second Form: Rice Spirit
                .withMove(new MoveBuilder("rice_spirit", "Rice Spirit")
                        .withIcon("nichirin:textures/gui/moves/thunder_second_form.png")
                        .withAnimation("nichirin:rice_spirit", 8)
                        .withStats(8.0f, 5.0f, 40)
                        .withAction(player -> MoveExecutor.executeThunderAttack(player, new RiceSpiritAttack()))
                )

                // Third Form: Thunder Swarm
                .withMove(new MoveBuilder("thunder_swarm", "Thunder Swarm")
                        .withIcon("nichirin:textures/gui/moves/thunder_third_form.png")
                        .withAnimation("nichirin:thunder_swarm", 9)
                        .withStats(10.0f, 8.0f, 50)
                        .withAction(player -> MoveExecutor.executeThunderAttack(player, new ThunderSwarmAttack()))
                )

                // Fourth Form: Distant Thunder
                .withMove(new MoveBuilder("distant_thunder", "Distant Thunder")
                        .withIcon("nichirin:textures/gui/moves/thunder_fourth_form.png")
                        .withAnimation("nichirin:distant_thunder", 7)
                        .withStats(12.0f, 20.0f, 80)
                        .withAction(player -> MoveExecutor.executeThunderAttack(player, new DistantThunderAttack()))
                )

                // Fifth Form: Heat Lightning
                .withMove(new MoveBuilder("heat_lightning", "Heat Lightning")
                        .withIcon("nichirin:textures/gui/moves/thunder_fifth_form.png")
                        .withAnimation("nichirin:heat_lightning", 9)
                        .withStats(18.0f, 12.0f, 60)
                        .withAction(player -> MoveExecutor.executeThunderAttack(player, new HeatLightningAttack()))
                )

                // Sixth Form: Rumble and Flash
                .withMove(new MoveBuilder("rumble_flash", "Rumble and Flash")
                        .withIcon("nichirin:textures/gui/moves/thunder_sixth_form.png")
                        .withAnimation("nichirin:rumble_flash", 8)
                        .withStats(20.0f, 25.0f, 70)
                        .withAction(player -> MoveExecutor.executeThunderAttack(player, new RumbleFlashAttack()))
                )

                // Seventh Form: Honoikazuchi no Kami (Ultimate)
                .withMove(new MoveBuilder("honoikazuchi_no_kami", "Honoikazuchi no Kami")
                        .withIcon("nichirin:textures/gui/moves/thunder_seventh_form.png")
                        .withAnimation("nichirin:honoikazuchi_no_kami", 15)
                        .withStats(50.0f, 30.0f, 300)
                        .withAction(player -> MoveExecutor.executeThunderAttack(player, new HonoikazuchiNoKamiAttack()))
                );

    }
    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        // Perform Thunderclap and Flash (index 0)
        performMove(player, 0);

        // If crouching, we'll handle the turn in the onMovePerformed callback
        return true; // We handled it
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // If this was Thunderclap and Flash (index 0) while crouching, turn around
        if (moveIndex == 0 && isCrouching) {
            // Store this in the move itself or use a delayed task system
            // For now, we'll add it to the ThunderClapFlashAttack
        }
    }
    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return 0; // Always Thunderclap and Flash for right-click
    }
}