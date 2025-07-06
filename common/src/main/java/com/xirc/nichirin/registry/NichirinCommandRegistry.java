package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.command.BreathingCommand;
import dev.architectury.event.events.common.CommandRegistrationEvent;

/**
 * Registry for all Nichirin commands
 */
public class NichirinCommandRegistry {

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            BreathingCommand.register(dispatcher);
        });
    }
}