package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.command.NichirinCommand;
import dev.architectury.event.events.common.CommandRegistrationEvent;

/**
 * Registry for all Nichirin commands. Everything funnels through {@code /nichirin}; the old
 * {@code /breathing} and {@code /demon} top-level commands were merged into subcommands there
 * to keep the command tree shallow and consistent.
 */
public interface NichirinCommandRegistry {

    static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            NichirinCommand.register(dispatcher);
        });
    }
}