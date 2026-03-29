package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.command.BreathingCommand;
import com.xirc.nichirin.common.command.DemonCommand;
import com.xirc.nichirin.common.command.NichirinCommand;
import dev.architectury.event.events.common.CommandRegistrationEvent;

/**
 * Registry for all Nichirin commands
 */
public interface NichirinCommandRegistry {

    static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            NichirinCommand.register(dispatcher);
            BreathingCommand.register(dispatcher);
            DemonCommand.register(dispatcher);
        });
    }
}