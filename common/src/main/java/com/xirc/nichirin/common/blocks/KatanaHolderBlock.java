package com.xirc.nichirin.common.blocks;

import com.xirc.nichirin.common.item.katana.Katana;
import com.xirc.nichirin.registry.NichirinBlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KatanaHolderBlock extends BaseEntityBlock {
    public static final MapCodec<KatanaHolderBlock> CODEC = simpleCodec(KatanaHolderBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ROTATED = BooleanProperty.create("rotated");

    private static final VoxelShape FLOOR_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final VoxelShape CEILING_SHAPE = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);

    private static final VoxelShape FLOOR_SHAPE_ROTATED = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final VoxelShape CEILING_SHAPE_ROTATED = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_SHAPE_ROTATED = Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE_ROTATED = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    private static final VoxelShape WEST_SHAPE_ROTATED = Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_SHAPE_ROTATED = Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);

    public KatanaHolderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(ROTATED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATED);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        boolean rotated = state.getValue(ROTATED);

        if (rotated) {
            return switch (state.getValue(FACING)) {
                case UP -> FLOOR_SHAPE_ROTATED;
                case DOWN -> CEILING_SHAPE_ROTATED;
                case NORTH -> NORTH_SHAPE_ROTATED;
                case SOUTH -> SOUTH_SHAPE_ROTATED;
                case WEST -> WEST_SHAPE_ROTATED;
                case EAST -> EAST_SHAPE_ROTATED;
            };
        } else {
            return switch (state.getValue(FACING)) {
                case UP -> FLOOR_SHAPE;
                case DOWN -> CEILING_SHAPE;
                case NORTH -> NORTH_SHAPE;
                case SOUTH -> SOUTH_SHAPE;
                case WEST -> WEST_SHAPE;
                case EAST -> EAST_SHAPE;
            };
        }
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(this);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        return this.defaultBlockState().setValue(FACING, clickedFace).setValue(ROTATED, false);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(facing.getOpposite());
        BlockState attachedBlock = level.getBlockState(attachedPos);

        return attachedBlock.isFaceSturdy(level, attachedPos, facing) &&
                !(attachedBlock.getBlock() instanceof KatanaHolderBlock);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("Crouch Right click the block to rotate by 90°").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getOpposite() && !this.canSurvive(state, level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, Level level,
                                                       @NotNull BlockPos pos, @NotNull Player player,
                                                       @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        interact(state, level, pos, player, hand);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof KatanaHolderBlockEntity holderEntity)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack handItem = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            level.setBlock(pos, state.setValue(ROTATED, !state.getValue(ROTATED)), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOD_STEP, SoundSource.BLOCKS, 0.6f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        if (!holderEntity.getStoredKatana().isEmpty()) {
            if (handItem.isEmpty()) {
                player.setItemInHand(hand, holderEntity.removeKatana());
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }

        if (handItem.getItem() instanceof Katana) {
            ItemStack katanaToPlace = handItem.copy();
            katanaToPlace.setCount(1);
            holderEntity.setKatana(katanaToPlace);
            handItem.shrink(1);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof KatanaHolderBlockEntity holderEntity) {
                ItemStack katana = holderEntity.getStoredKatana();
                if (!katana.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, katana);
                    level.addFreshEntity(itemEntity);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new KatanaHolderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return null;
    }

    public static class KatanaHolderBlockEntity extends BlockEntity {
        private static final String KATANA_TAG = "StoredKatana";

        @Getter
        private ItemStack storedKatana = ItemStack.EMPTY;

        public KatanaHolderBlockEntity(BlockPos pos, BlockState blockState) {
            super(NichirinBlockEntityRegistry.KATANA_HOLDER_BLOCK_ENTITY.get(), pos, blockState);
        }

        public void setKatana(ItemStack katana) {
            if (katana.isEmpty() || katana.getItem() instanceof Katana) {
                this.storedKatana = katana.copy();
                if (!katana.isEmpty()) {
                    this.storedKatana.setCount(1);
                }
                setChanged();
                if (level != null && !level.isClientSide) {
                    syncToClient();
                }
            }
        }

        public ItemStack removeKatana() {
            ItemStack result = storedKatana.copy();
            storedKatana = ItemStack.EMPTY;
            setChanged();
            if (level != null && !level.isClientSide) {
                syncToClient();
            }
            return result;
        }

        public boolean shouldRenderKatana() {
            return !storedKatana.isEmpty();
        }

        public boolean hasKatana() {
            return !storedKatana.isEmpty();
        }

        public Direction getFacing() {
            return getBlockState().getValue(FACING);
        }

        public boolean isRotated() {
            return getBlockState().getValue(ROTATED);
        }

        private void syncToClient() {
            if (level == null || level.isClientSide) return;
            setChanged();
            // Let vanilla push getUpdatePacket() to every player tracking this chunk — the old
            // hand-rolled 64-block broadcast missed players who track the chunk from farther away
            // (they'd only see the katana after re-triggering it).
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }

        @Override
        protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
            super.loadAdditional(tag, provider);
            if (tag.contains(KATANA_TAG)) {
                storedKatana = ItemStack.parseOptional(provider, tag.getCompound(KATANA_TAG));
            } else {
                // Explicit empty marker OR missing tag both mean no katana
                storedKatana = ItemStack.EMPTY;
            }
        }

        @Override
        protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
            super.saveAdditional(tag, provider);
            if (!storedKatana.isEmpty()) {
                tag.put(KATANA_TAG, storedKatana.save(provider, new CompoundTag()));
            }
        }

        @Override
        public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
            CompoundTag tag = super.getUpdateTag(provider);
            if (!storedKatana.isEmpty()) {
                tag.put(KATANA_TAG, storedKatana.save(provider, new CompoundTag()));
            } else {
                // Always write an explicit empty marker so the client clears its rendered katana
                tag.putBoolean("HasKatana", false);
            }
            return tag;
        }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}