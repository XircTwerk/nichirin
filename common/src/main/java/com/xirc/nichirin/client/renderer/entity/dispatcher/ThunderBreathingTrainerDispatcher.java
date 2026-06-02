package com.xirc.nichirin.client.renderer.entity.dispatcher;

import com.xirc.nichirin.common.entity.npc.ThunderBreathingTrainerEntity;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

public class ThunderBreathingTrainerDispatcher {

    private static final AzCommand IDLE                   = AzCommand.create("main_controller", "idle",                  AzPlayBehaviors.LOOP);
    private static final AzCommand WALK                   = AzCommand.create("main_controller", "walk",                  AzPlayBehaviors.LOOP);
    private static final AzCommand SPRINT                 = AzCommand.create("main_controller", "sprint",                AzPlayBehaviors.LOOP);
    private static final AzCommand DASH                   = AzCommand.create("main_controller", "dash",                  AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand BACKSTEP               = AzCommand.create("main_controller", "backstep",              AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand AIR_DODGE              = AzCommand.create("main_controller", "air_dodge",             AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand DOUBLE_JUMP            = AzCommand.create("main_controller", "double_jump",           AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand THUNDERCLAP_FLASH      = AzCommand.create("main_controller", "thunderclap_flash",     AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand RICE_SPIRIT            = AzCommand.create("main_controller", "rice_spirit",           AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand THUNDER_SWARM          = AzCommand.create("main_controller", "thunder_swarm",         AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand DISTANT_THUNDER        = AzCommand.create("main_controller", "distant_thunder",       AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand HEAT_LIGHTNING         = AzCommand.create("main_controller", "heat_lightning",        AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand RUMBLE_FLASH           = AzCommand.create("main_controller", "rumble_flash",          AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand HONOIKAZUCHI_NO_KAMI   = AzCommand.create("main_controller", "honoikazuchi_no_kami",  AzPlayBehaviors.PLAY_ONCE);

    private final ThunderBreathingTrainerEntity trainer;

    public ThunderBreathingTrainerDispatcher(ThunderBreathingTrainerEntity trainer) {
        this.trainer = trainer;
    }

    public void idle()                  { IDLE.sendForEntity(trainer); }
    public void walk()                  { WALK.sendForEntity(trainer); }
    public void sprint()                { SPRINT.sendForEntity(trainer); }
    public void dash()                  { DASH.sendForEntity(trainer); }
    public void backstep()              { BACKSTEP.sendForEntity(trainer); }
    public void airDodge()              { AIR_DODGE.sendForEntity(trainer); }
    public void doubleJump()            { DOUBLE_JUMP.sendForEntity(trainer); }
    public void thunderclapFlash()      { THUNDERCLAP_FLASH.sendForEntity(trainer); }
    public void riceSpirit()            { RICE_SPIRIT.sendForEntity(trainer); }
    public void thunderSwarm()          { THUNDER_SWARM.sendForEntity(trainer); }
    public void distantThunder()        { DISTANT_THUNDER.sendForEntity(trainer); }
    public void heatLightning()         { HEAT_LIGHTNING.sendForEntity(trainer); }
    public void rumbleFlash()           { RUMBLE_FLASH.sendForEntity(trainer); }
    public void honoikazuchiNoKami()    { HONOIKAZUCHI_NO_KAMI.sendForEntity(trainer); }

    public void playAnimation(String name) {
        switch (name) {
            case "thunderclap_flash"    -> thunderclapFlash();
            case "rice_spirit"          -> riceSpirit();
            case "thunder_swarm"        -> thunderSwarm();
            case "distant_thunder"      -> distantThunder();
            case "heat_lightning"       -> heatLightning();
            case "rumble_flash"         -> rumbleFlash();
            case "honoikazuchi_no_kami" -> honoikazuchiNoKami();
            case "dash"                 -> dash();
            case "backstep"             -> backstep();
            case "air_dodge"            -> airDodge();
            case "double_jump"          -> doubleJump();
            case "sprint"               -> sprint();
            case "walk"                 -> walk();
            default                     -> idle();
        }
    }
}