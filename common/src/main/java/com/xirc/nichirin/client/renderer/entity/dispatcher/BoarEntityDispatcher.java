package com.xirc.nichirin.client.renderer.entity.dispatcher;

import com.xirc.nichirin.common.entity.animal.BoarEntity;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;

public class BoarEntityDispatcher {

    private static final AzCommand IDLE_COMMAND = AzCommand.create(
            "main_controller",
            "idle",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand WALKING_COMMAND = AzCommand.create(
            "main_controller",
            "walking",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand RUNNING_COMMAND = AzCommand.create(
            "main_controller",
            "running",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand RUNNING_FAST_COMMAND = AzCommand.create(
            "main_controller",
            "running_fast",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand JUMP_HIT_COMMAND = AzCommand.create(
            "main_controller",
            "jump_hit",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand POUNCE_COMMAND = AzCommand.create(
            "main_controller",
            "pounce",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand SIT_COMMAND = AzCommand.create(
            "main_controller",
            "sit",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand LAY_DOWN_COMMAND = AzCommand.create(
            "main_controller",
            "lay_down",
            AzPlayBehaviors.LOOP
    );

    private final BoarEntity boar;

    public BoarEntityDispatcher(BoarEntity boar) {
        this.boar = boar;
    }

    public void idle() {
        IDLE_COMMAND.sendForEntity(boar);
    }

    public void walking() {
        WALKING_COMMAND.sendForEntity(boar);
    }

    public void running() {
        RUNNING_COMMAND.sendForEntity(boar);
    }

    public void runningFast() {
        RUNNING_FAST_COMMAND.sendForEntity(boar);
    }

    public void jumpHit() {
        JUMP_HIT_COMMAND.sendForEntity(boar);
    }

    public void pounce() {
        POUNCE_COMMAND.sendForEntity(boar);
    }

    public void sit() {
        SIT_COMMAND.sendForEntity(boar);
    }

    public void layDown() {
        LAY_DOWN_COMMAND.sendForEntity(boar);
    }
}