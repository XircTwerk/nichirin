package com.xirc.nichirin.client.renderer.entity.dispatcher;

import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;

public class TempleDemonDispatcher {

    private static final AzCommand IDLE_COMMAND = AzCommand.create(
            "main_controller",
            "idle",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand WALK_COMMAND = AzCommand.create(
            "main_controller",
            "walk",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand GUT_PUNCH_COMMAND = AzCommand.create(
            "main_controller",
            "demon_gut_punch",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand SLASH_COMMAND = AzCommand.create(
            "main_controller",
            "demon_slash",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand SLASH_2_COMMAND = AzCommand.create(
            "main_controller",
            "demon_slash_2",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand KICK_COMMAND = AzCommand.create(
            "main_controller",
            "demon_kick",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand DASH_STRIKE_COMMAND = AzCommand.create(
            "main_controller",
            "demon_dash_strike",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand BITE_COMMAND = AzCommand.create(
            "main_controller",
            "demon_bite",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand HIGH_JUMP_COMMAND = AzCommand.create(
            "main_controller",
            "demon_high_jump",
            AzPlayBehaviors.PLAY_ONCE
    );

    private static final AzCommand STOMP_COMMAND = AzCommand.create(
            "main_controller",
            "demon_stomp",
            AzPlayBehaviors.PLAY_ONCE
    );

    private final TempleDemonEntity demon;

    public TempleDemonDispatcher(TempleDemonEntity demon) {
        this.demon = demon;
    }

    public void idle() {
        IDLE_COMMAND.sendForEntity(demon);
    }

    public void walk() {
        WALK_COMMAND.sendForEntity(demon);
    }

    public void gutPunch() {
        GUT_PUNCH_COMMAND.sendForEntity(demon);
    }

    public void slash() {
        SLASH_COMMAND.sendForEntity(demon);
    }

    public void slash2() {
        SLASH_2_COMMAND.sendForEntity(demon);
    }

    public void kick() {
        KICK_COMMAND.sendForEntity(demon);
    }

    public void dashStrike() {
        DASH_STRIKE_COMMAND.sendForEntity(demon);
    }

    public void bite() {
        BITE_COMMAND.sendForEntity(demon);
    }

    public void highJump() {
        HIGH_JUMP_COMMAND.sendForEntity(demon);
    }

    public void stomp() {
        STOMP_COMMAND.sendForEntity(demon);
    }

    public void playAnimation(String animName) {
        switch (animName) {
            case "demon_gut_punch", "demon_punch" -> gutPunch();
            case "demon_slash" -> slash();
            case "demon_slash_2" -> slash2();
            case "demon_kick" -> kick();
            case "demon_dash_strike" -> dashStrike();
            case "demon_bite" -> bite();
            case "demon_high_jump" -> highJump();
            case "demon_stomp" -> stomp();
            default -> idle();
        }
    }
}