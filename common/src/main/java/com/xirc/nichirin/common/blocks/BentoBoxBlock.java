package com.xirc.nichirin.common.blocks;

import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.registry.NichirinBlockEntityRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.animatable.GeoBlockEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.util.AzureLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BentoBoxBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(BentoBoxBlock.class);
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BentoBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Shift + right click = pick up the block
        if (player.isShiftKeyDown()) {
            return pickupBlock(level, pos, player);
        }

        // Regular right click = open the bento box
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BentoBoxBlockEntity bentoBoxEntity) {
            player.openMenu(bentoBoxEntity);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult pickupBlock(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BentoBoxBlockEntity bentoBoxEntity)) {
            return InteractionResult.FAIL;
        }

        // Create bento box item with the stored contents
        ItemStack bentoBoxItem = new ItemStack(NichirinItemRegistry.BENTO_BOX.get());

        // Transfer the contents from block entity to item
        bentoBoxEntity.saveToItem(bentoBoxItem);

        // Remove the block
        level.removeBlock(pos, false);

        // Give item to player or drop it
        if (!player.getInventory().add(bentoBoxItem)) {
            player.drop(bentoBoxItem, false);
        }

        LOGGER.info("Player {} picked up bento box block at {}", player.getName().getString(), pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide && stack.getItem() instanceof BentoBoxItem) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BentoBoxBlockEntity bentoBoxEntity) {
                bentoBoxEntity.loadFromItem(stack);
                LOGGER.info("Placed bento box block with contents at {}", pos);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BentoBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null; // No ticking needed for static bento box
    }

    // Block Entity Implementation
    public static class BentoBoxBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
        private static final int BENTO_SIZE = 9;
        private final SimpleContainer inventory;
        private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);

        public BentoBoxBlockEntity(BlockPos pos, BlockState blockState) {
            super(NichirinBlockEntityRegistry.BENTO_BOX_BLOCK_ENTITY.get(), pos, blockState);
            this.inventory = new SimpleContainer(BENTO_SIZE) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    BentoBoxBlockEntity.this.setChanged();
                }
            };
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Bento Box");
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
            return BentoBoxItem.createBlockMenu(syncId, playerInventory, inventory, this.worldPosition);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            BentoBoxItem.loadItemsFromNbt(tag, inventory);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            BentoBoxItem.saveItemsToNbt(tag, inventory);
        }

        public void loadFromItem(ItemStack stack) {
            if (stack.hasTag()) {
                BentoBoxItem.loadItemsFromNbt(stack.getOrCreateTag(), inventory);
                setChanged();
            }
        }

        public void saveToItem(ItemStack stack) {
            CompoundTag tag = stack.getOrCreateTag();
            BentoBoxItem.saveItemsToNbt(tag, inventory);
        }

        public Container getContainer() {
            return inventory;
        }

        // AzureLib methods (no animations)
        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            // No animation controllers needed
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }
}