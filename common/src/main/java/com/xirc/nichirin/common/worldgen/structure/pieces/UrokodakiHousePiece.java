package com.xirc.nichirin.common.worldgen.structure.pieces;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinStructurePieceTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.entity.MobSpawnType;

public class UrokodakiHousePiece extends StructurePiece {
    private static final ResourceLocation STRUCTURE_LOCATION =
            new ResourceLocation(BreathOfNichirin.MOD_ID, "urokodaki_house");

    private final Rotation rotation;
    private final BlockPos structurePos;
    private boolean trainerSpawned = false;

    public UrokodakiHousePiece(BlockPos pos, int rotationIndex) {
        super(NichirinStructurePieceTypeRegistry.UROKODAKI_HOUSE_PIECE.get(), 0,
                calculateBoundingBox(pos, Rotation.values()[rotationIndex % 4]));
        this.rotation = Rotation.values()[rotationIndex % 4];
        this.structurePos = pos;
    }

    public UrokodakiHousePiece(StructurePieceSerializationContext context, CompoundTag nbt) {
        super(NichirinStructurePieceTypeRegistry.UROKODAKI_HOUSE_PIECE.get(), nbt);
        this.rotation = Rotation.valueOf(nbt.getString("Rotation"));
        this.structurePos = new BlockPos(nbt.getInt("TPX"), nbt.getInt("TPY"), nbt.getInt("TPZ"));
        this.trainerSpawned = nbt.getBoolean("TrainerSpawned");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putString("Rotation", this.rotation.name());
        nbt.putInt("TPX", this.structurePos.getX());
        nbt.putInt("TPY", this.structurePos.getY());
        nbt.putInt("TPZ", this.structurePos.getZ());
        nbt.putBoolean("TrainerSpawned", this.trainerSpawned);
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureManager structureManager,
                            ChunkGenerator chunkGenerator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {

        StructureTemplateManager templateManager = world.getLevel().getServer().getStructureManager();
        StructureTemplate template = templateManager.getOrCreate(STRUCTURE_LOCATION);

        if (template == null) {
            return;
        }

        int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, structurePos.getX(), structurePos.getZ());
        BlockPos finalPos = new BlockPos(structurePos.getX(), surfaceY - 1, structurePos.getZ());

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(this.rotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);

        template.placeInWorld(world, finalPos, finalPos, settings, random, 18);

        if (!trainerSpawned && !world.isClientSide()) {
            // Spawn Urokodaki near the front of the house (center X, +2 Z offset)
            BlockPos spawnPos = new BlockPos(
                    structurePos.getX() + 5,
                    surfaceY,
                    structurePos.getZ() + 2
            );
            WaterBreathingTrainerEntity trainer = NichirinEntityRegistry.WATER_BREATHING_TRAINER.get().create(world.getLevel());
            if (trainer != null) {
                trainer.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);
                trainer.finalizeSpawn(world, world.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
                world.addFreshEntity(trainer);
                trainerSpawned = true;
            }
        }
    }

    private static BoundingBox calculateBoundingBox(BlockPos pos, Rotation rotation) {
        int width = 10;
        int height = 9;
        int depth = 13;

        return switch (rotation) {
            case NONE -> BoundingBox.fromCorners(pos, pos.offset(width, height, depth));
            case CLOCKWISE_90 -> BoundingBox.fromCorners(pos, pos.offset(-depth, height, width));
            case CLOCKWISE_180 -> BoundingBox.fromCorners(pos, pos.offset(-width, height, -depth));
            case COUNTERCLOCKWISE_90 -> BoundingBox.fromCorners(pos, pos.offset(depth, height, -width));
        };
    }
}
