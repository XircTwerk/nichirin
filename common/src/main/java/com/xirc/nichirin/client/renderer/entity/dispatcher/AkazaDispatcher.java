package com.xirc.nichirin.client.renderer.entity.dispatcher;

import com.xirc.nichirin.common.entity.npc.AkazaEntity;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

/**
 * Maps Akaza's server-driven animation names onto AzureLib controller commands. Attack names match
 * the keys in {@code animations/akaza.animation.json} (copied/remapped from the CQC and Destructive
 * Death player animations). Donut reuses the Annihilation Type clip.
 */
public class AkazaDispatcher {

    private static final AzCommand IDLE = AzCommand.create("main_controller", "idle", AzPlayBehaviors.LOOP);
    private static final AzCommand WALK = AzCommand.create("main_controller", "walk", AzPlayBehaviors.LOOP);

    private static final AzCommand SNAP_PUNCH = AzCommand.create("main_controller", "snap_punch", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand CROWN_SPLITTER = AzCommand.create("main_controller", "crown_splitter", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand ANNIHILATION = AzCommand.create("main_controller", "annihilation_type", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand FLYING_PLANET = AzCommand.create("main_controller", "flying_planet_thousand_wheels", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand EIGHT_LAYERED = AzCommand.create("main_controller", "eight_layered_demon_core", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand TEN_THOUSAND = AzCommand.create("main_controller", "ten_thousand_leaves_flashing_willow", AzPlayBehaviors.PLAY_ONCE);

    private final AkazaEntity akaza;

    public AkazaDispatcher(AkazaEntity akaza) {
        this.akaza = akaza;
    }

    public void idle() { IDLE.sendForEntity(akaza); }
    public void walk() { WALK.sendForEntity(akaza); }

    public void playAnimation(String animName) {
        switch (animName) {
            case "snap_punch" -> SNAP_PUNCH.sendForEntity(akaza);
            case "crown_splitter" -> CROWN_SPLITTER.sendForEntity(akaza);
            case "annihilation_type", "donut" -> ANNIHILATION.sendForEntity(akaza);
            case "flying_planet_thousand_wheels" -> FLYING_PLANET.sendForEntity(akaza);
            case "eight_layered_demon_core" -> EIGHT_LAYERED.sendForEntity(akaza);
            case "ten_thousand_leaves_flashing_willow" -> TEN_THOUSAND.sendForEntity(akaza);
            default -> IDLE.sendForEntity(akaza);
        }
    }
}
