package com.xirc.nichirin.common.worldgen.structure;

import com.mojang.serialization.Codec;
import com.xirc.nichirin.common.worldgen.structure.pieces.UrokodakiHousePiece;
import com.xirc.nichirin.registry.NichirinStructureTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class UrokodakiHouseStructure extends Structure {
    public static final Codec<UrokodakiHouseStructure> CODEC =
            simpleCodec(UrokodakiHouseStructure::new);

    public UrokodakiHouseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        BlockPos centerPos = new BlockPos(chunkPos.getMinBlockX() + 8, 0, chunkPos.getMinBlockZ() + 8);

        // Get surface height
        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
                centerPos.getX(), centerPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());

        // Check if the area is relatively flat for a good placement
        if (isAreaFlat(context, centerPos.getX(), centerPos.getZ(), 5)) {
            BlockPos structurePos = new BlockPos(centerPos.getX(), surfaceY, centerPos.getZ());
            return Optional.of(new GenerationStub(structurePos, builder -> {
                generatePieces(builder, context, structurePos);
            }));
        }

        return Optional.empty();
    }

    private boolean isAreaFlat(GenerationContext context, int centerX, int centerZ, int radius) {
        int centerY = context.chunkGenerator().getFirstOccupiedHeight(
                centerX, centerZ, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());

        // Check surrounding area for flatness (within 2 blocks height difference)
        for (int x = centerX - radius; x <= centerX + radius; x += 2) {
            for (int z = centerZ - radius; z <= centerZ + radius; z += 2) {
                int y = context.chunkGenerator().getFirstOccupiedHeight(
                        x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                if (Math.abs(y - centerY) > 2) {
                    return false;
                }
            }
        }
        return true;
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos pos) {
        WorldgenRandom random = context.random();
        builder.addPiece(new UrokodakiHousePiece(pos, random.nextInt(4)));
    }

    @Override
    public StructureType<?> type() {
        return NichirinStructureTypeRegistry.UROKODAKI_HOUSE.get();
    }
}