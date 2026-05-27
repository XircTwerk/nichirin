package com.xirc.nichirin.common.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;

public class NichirinServerConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {
    private final Class<T> configClass;

    public NichirinServerConfigSerializer(Config definition, Class<T> configClass) {
        this.configClass = configClass;
    }

    @Override
    public void serialize(T config) throws SerializationException {
        if (config instanceof NichirinModConfig nichirinConfig) {
            NichirinServerConfig.save(nichirinConfig);
        }
    }

    @Override
    public T deserialize() throws SerializationException {
        NichirinModConfig config = NichirinServerConfig.loadConfig();
        return configClass.cast(config);
    }

    @Override
    public T createDefault() {
        return configClass.cast(new NichirinModConfig());
    }
}
