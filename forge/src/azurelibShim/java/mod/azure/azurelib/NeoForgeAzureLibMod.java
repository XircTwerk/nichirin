package mod.azure.azurelib;

import mod.azure.azurelib.config.TestingConfig;
import mod.azure.azurelib.config.format.ConfigFormats;
import mod.azure.azurelib.config.io.ConfigIO;
import mod.azure.azurelib.network.Networking;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Forge 1.20.1 shim for AzureLib Neo's NeoForgeAzureLibMod.
 *
 * AzureLib Neo's original class has a constructor taking FMLJavaModLoadingContext,
 * which is NeoForge constructor injection — Forge 1.20.1 FMLModContainer only calls
 * getDeclaredConstructor() (no-arg), so it fails with NoSuchMethodException.
 *
 * This shim is compiled into an isolated output directory (not the main module),
 * then patched into the remapped AzureLib JAR by the patchAzureLibForForgeCompat
 * Gradle task. The @Mod annotation is required so FML's bytecode scan finds the
 * mod entrypoint in the JAR.
 */
@Mod(AzureLib.MOD_ID)
public final class NeoForgeAzureLibMod {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
        ForgeRegistries.BLOCKS,
        AzureLib.MOD_ID
    );

    public static final DeferredRegister<BlockEntityType<?>> TILE_TYPES = DeferredRegister.create(
        ForgeRegistries.BLOCK_ENTITY_TYPES,
        AzureLib.MOD_ID
    );

    public NeoForgeAzureLibMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        AzureLib.initialize();
        AzureLibMod.config = AzureLibMod.registerConfig(TestingConfig.class, ConfigFormats.json()).getConfigInstance();
        BLOCKS.register(modEventBus);
        TILE_TYPES.register(modEventBus);
        modEventBus.addListener(this::init);
    }

    private void init(FMLCommonSetupEvent event) {
        Networking.PacketRegistry.register();
        ConfigIO.FILE_WATCH_MANAGER.startService();
    }
}
