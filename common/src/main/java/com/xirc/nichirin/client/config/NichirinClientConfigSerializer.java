package com.xirc.nichirin.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xirc.nichirin.BreathOfNichirin;
import me.shedaniel.cloth.clothconfig.shadowed.com.moandjiezana.toml.Toml;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NichirinClientConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Paths.get("config", "nichirin_client.toml");
    private static final Path LEGACY_JSON_PATH = Paths.get("config", "nichirin_client.json");

    private final Class<T> configClass;

    public NichirinClientConfigSerializer(Config definition, Class<T> configClass) {
        this.configClass = configClass;
    }

    @Override
    public void serialize(T config) throws SerializationException {
        if (config instanceof NichirinClientConfig clientConfig) {
            save(clientConfig);
        }
    }

    @Override
    public T deserialize() throws SerializationException {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    NichirinClientConfig config = new Toml().read(reader).to(NichirinClientConfig.class);
                    return configClass.cast(config != null ? config : new NichirinClientConfig());
                }
            }
            if (Files.exists(LEGACY_JSON_PATH)) {
                try (Reader reader = Files.newBufferedReader(LEGACY_JSON_PATH)) {
                    NichirinClientConfig config = GSON.fromJson(reader, NichirinClientConfig.class);
                    if (config == null) {
                        config = new NichirinClientConfig();
                    }
                    save(config);
                    Files.deleteIfExists(LEGACY_JSON_PATH);
                    BreathOfNichirin.LOGGER.info("Migrated legacy config {} to {}.", LEGACY_JSON_PATH, PATH);
                    return configClass.cast(config);
                }
            } else {
                NichirinClientConfig defaults = new NichirinClientConfig();
                save(defaults);
                return configClass.cast(defaults);
            }
        } catch (IOException e) {
            BreathOfNichirin.LOGGER.warn("Failed to load {}, using defaults.", PATH, e);
            return createDefault();
        }
    }

    @Override
    public T createDefault() {
        return configClass.cast(new NichirinClientConfig());
    }

    private static void save(NichirinClientConfig cfg) {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                writer.write(toCommentedToml(cfg));
            }
        } catch (IOException e) {
            BreathOfNichirin.LOGGER.warn("Failed to save {}.", PATH, e);
        }
    }

    private static String toCommentedToml(NichirinClientConfig cfg) {
        NichirinClientConfig defaults = new NichirinClientConfig();
        StringBuilder out = new StringBuilder(512);
        out.append("# Breath of Nichirin client config\n");
        out.append("# Change the value before each inline comment. The comment shows the default.\n\n");
        out.append("[visual]\n");
        appendValue(out, "enableBreathingAuraParticles", cfg.visual.enableBreathingAuraParticles, defaults.visual.enableBreathingAuraParticles, true);
        appendValue(out, "enableHitParticles", cfg.visual.enableHitParticles, defaults.visual.enableHitParticles, true);
        appendValue(out, "enableParrySparks", cfg.visual.enableParrySparks, defaults.visual.enableParrySparks, false);
        return out.toString();
    }

    private static void appendValue(StringBuilder out, String key, boolean value, boolean defaultValue, boolean comma) {
        out.append(key).append(" = ").append(value).append(" # Default: ").append(defaultValue).append('\n');
    }
}