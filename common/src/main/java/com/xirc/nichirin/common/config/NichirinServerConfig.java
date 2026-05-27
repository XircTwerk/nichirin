package com.xirc.nichirin.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xirc.nichirin.BreathOfNichirin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class NichirinServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Paths.get("config", "nichirin-server.json");

    private static NichirinModConfig config;

    private NichirinServerConfig() {
    }

    public static void load() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    config = GSON.fromJson(reader, NichirinModConfig.class);
                }
            }
            if (config == null) {
                config = new NichirinModConfig();
                save();
            }
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.warn("Failed to load {}, using defaults.", PATH, e);
            config = new NichirinModConfig();
        }
    }

    public static NichirinModConfig get() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void save() {
        if (config == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            BreathOfNichirin.LOGGER.warn("Failed to save {}.", PATH, e);
        }
    }
}
