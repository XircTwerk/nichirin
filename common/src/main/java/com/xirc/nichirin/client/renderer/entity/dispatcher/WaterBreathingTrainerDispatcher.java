package com.xirc.nichirin.client.renderer.entity.dispatcher;

import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

public class WaterBreathingTrainerDispatcher {

    private static final AzCommand IDLE                = AzCommand.create("main_controller", "idle",               AzPlayBehaviors.LOOP);
    private static final AzCommand WALK                = AzCommand.create("main_controller", "walk",               AzPlayBehaviors.LOOP);
    private static final AzCommand SPRINT              = AzCommand.create("main_controller", "sprint",             AzPlayBehaviors.LOOP);
    private static final AzCommand DASH                = AzCommand.create("main_controller", "dash",               AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand BACKSTEP            = AzCommand.create("main_controller", "backstep",           AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand AIR_DODGE           = AzCommand.create("main_controller", "air_dodge",          AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand DOUBLE_JUMP         = AzCommand.create("main_controller", "double_jump",        AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand WATER_SURFACE_SLASH = AzCommand.create("main_controller", "water_surface_slash",AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand WATER_WHEEL         = AzCommand.create("main_controller", "water_wheel",        AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand FLOWING_DANCE       = AzCommand.create("main_controller", "flowing_dance",      AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand STRIKING_TIDE       = AzCommand.create("main_controller", "striking_tide",      AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand DROP_RIPPLE_THRUST  = AzCommand.create("main_controller", "drop_ripple_thrust", AzPlayBehaviors.PLAY_ONCE);
    private static final AzCommand CONSTANT_FLUX       = AzCommand.create("main_controller", "constant_flux",      AzPlayBehaviors.PLAY_ONCE);

    private final WaterBreathingTrainerEntity trainer;

    public WaterBreathingTrainerDispatcher(WaterBreathingTrainerEntity trainer) {
        this.trainer = trainer;
    }

    public void idle()               { IDLE.sendForEntity(trainer); }
    public void walk()               { WALK.sendForEntity(trainer); }
    public void sprint()             { SPRINT.sendForEntity(trainer); }
    public void dash()               { DASH.sendForEntity(trainer); }
    public void backstep()           { BACKSTEP.sendForEntity(trainer); }
    public void airDodge()           { AIR_DODGE.sendForEntity(trainer); }
    public void doubleJump()         { DOUBLE_JUMP.sendForEntity(trainer); }
    public void waterSurfaceSlash()  { WATER_SURFACE_SLASH.sendForEntity(trainer); }
    public void waterWheel()         { WATER_WHEEL.sendForEntity(trainer); }
    public void flowingDance()       { FLOWING_DANCE.sendForEntity(trainer); }
    public void strikingTide()       { STRIKING_TIDE.sendForEntity(trainer); }
    public void dropRippleThrust()   { DROP_RIPPLE_THRUST.sendForEntity(trainer); }
    public void constantFlux()       { CONSTANT_FLUX.sendForEntity(trainer); }

    public void playAnimation(String name) {
        switch (name) {
            case "water_surface_slash"  -> waterSurfaceSlash();
            case "water_wheel"          -> waterWheel();
            case "flowing_dance"        -> flowingDance();
            case "striking_tide"        -> strikingTide();
            case "drop_ripple_thrust"   -> dropRippleThrust();
            case "constant_flux"        -> constantFlux();
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