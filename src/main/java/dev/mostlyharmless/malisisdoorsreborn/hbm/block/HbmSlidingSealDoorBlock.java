package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import dev.mostlyharmless.malisisdoorsreborn.block.CustomDoorBreakTarget;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.access.AccessCardDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmSlidingSealDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverItem;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverMode;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmSlidingSealDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlocks;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrItems;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class HbmSlidingSealDoorBlock extends Block implements EntityBlock, CustomDoorBreakTarget, HbmScrewdriverDoorTarget, AccessCardDoorTarget {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty Y_PART = IntegerProperty.create("y", 0, 1);
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final EnumProperty<HbmSlidingSealDoorPart> PART = EnumProperty.create("part", HbmSlidingSealDoorPart.class);

    private static final int ROOT_Y_PART = 0;
    private static final int HEIGHT = 2;

    public HbmSlidingSealDoorBlock() {
        super(Properties.of()
                .sound(SoundType.METAL)
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(HINGE, DoorHingeSide.RIGHT)
                .setValue(Y_PART, ROOT_Y_PART)
                .setValue(PART, HbmSlidingSealDoorPart.ROOT));
    }

    @Override
    public void initializeClient(@NotNull final Consumer<IClientBlockExtensions> consumer) {
        CustomDoorBreakHelper.initializeClient(consumer);
    }

    @Override
    public boolean isCustomDoorBreakRoot(@NotNull final BlockState rootState,
                                         final BlockEntity blockEntity) {
        return rootState.getBlock() == this
                && rootState.hasProperty(PART)
                && rootState.getValue(PART) == HbmSlidingSealDoorPart.ROOT
                && blockEntity instanceof HbmSlidingSealDoorBlockEntity;
    }

    @Override
    public BlockPos customDoorBreakRootPos(@NotNull final BlockPos pos,
                                           @NotNull final BlockState state) {
        return rootPos(pos, state);
    }

    @Override
    public AABB customDoorBreakBounds(@NotNull final BlockGetter level,
                                      @NotNull final BlockPos rootPos,
                                      @NotNull final BlockState rootState) {
        return new AABB(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D);
    }

    @Override
    public BlockState customDoorBreakParticleState(@NotNull final Level level,
                                                   @NotNull final BlockPos pos,
                                                   @NotNull final BlockState state) {
        return MdrBlocks.HBM_SLIDING_SEAL_DOOR_PARTICLE_DEFAULT.get().defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, HINGE, Y_PART, PART);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos rootPos = context.getClickedPos();
        final Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canPlaceDoor(level, rootPos, context)) return null;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HINGE, hingeForPlacement(context, facing));
    }

    @Override
    public void setPlacedBy(@NotNull final Level level,
                            @NotNull final BlockPos pos,
                            @NotNull final BlockState state,
                            @Nullable final LivingEntity placer,
                            @NotNull final ItemStack stack) {
        if (!level.isClientSide) {
            final int skinIndex = HbmSlidingSealDoorBlockItem.skinIndexFromStack(stack);
            final HbmDoorRedstoneMode redstoneMode = HbmSlidingSealDoorBlockItem.redstoneModeFromStack(stack);
            final DoorAccessLevel accessLevel = HbmSlidingSealDoorBlockItem.accessLevelFromStack(stack);
            placeDoorParts(level, pos, state.getValue(FACING), state.getValue(HINGE), state.getValue(OPEN), state.getValue(POWERED));
            if (level.getBlockEntity(pos) instanceof final HbmSlidingSealDoorBlockEntity door) {
                door.setSkinIndex(skinIndex);
                door.setRedstoneMode(redstoneMode);
                door.setAccessLevel(accessLevel);
            }
        }
    }

    @Override
    public @NotNull InteractionResult use(@NotNull final BlockState state,
                                          @NotNull final Level level,
                                          @NotNull final BlockPos pos,
                                          @NotNull final Player player,
                                          @NotNull final InteractionHand hand,
                                          @NotNull final BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (!(root.getBlock() instanceof final HbmSlidingSealDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos, root)) return InteractionResult.CONSUME;

        final ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && held.is(MdrItems.HBM_SCREWDRIVER.get())) {
            if (level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity accessDoor
                    && !DoorAccessHelper.canOpen(player, accessDoor.getAccessLevel())) {
                return InteractionResult.CONSUME;
            }
            return doorBlock.applyHbmScrewdriverMode(level, pos, state, player, HbmScrewdriverItem.modeFromStack(held));
        }

        if (level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity accessDoor) {
            final InteractionResult accessResult = DoorAccessHelper.handleAccessCardUse(level, rootPos, player, accessDoor, DoorAccessHelper.cardStackForAccessEdit(player, held));
            if (accessResult != InteractionResult.PASS) return accessResult;
            if (!DoorAccessHelper.canOpen(player, accessDoor.getAccessLevel())) {
                DoorAccessHelper.showAccessLevel(player, accessDoor.getAccessLevel());
                return InteractionResult.CONSUME;
            }
        }

        if (!doorBlock.canManualToggle(level, rootPos)) return InteractionResult.CONSUME;
        doorBlock.setDoorOpen(level, rootPos, !root.getValue(OPEN), player);
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull InteractionResult applyAccessCard(@NotNull final Level level,
                                                         @NotNull final BlockPos pos,
                                                         @NotNull final BlockState state,
                                                         @NotNull final Player player,
                                                         @NotNull final ItemStack cardStack) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (root.getBlock() != this) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos, root)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity accessDoor) {
            return DoorAccessHelper.handleAccessCardUse(level, rootPos, player, accessDoor, cardStack);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult applyHbmScrewdriverMode(@NotNull final Level level,
                                                              @NotNull final BlockPos pos,
                                                              @NotNull final BlockState state,
                                                              @NotNull final Player player,
                                                              @NotNull final HbmScrewdriverMode mode) {
        return switch (mode) {
            case CYCLE_DOOR_SKIN -> cycleSkinWithScrewdriver(level, pos, state, player);
            case CYCLE_REDSTONE_BEHAVIOUR -> cycleRedstoneModeWithScrewdriver(level, pos, state, player);
        };
    }

    private @NotNull InteractionResult cycleSkinWithScrewdriver(@NotNull final Level level,
                                                                @NotNull final BlockPos pos,
                                                                @NotNull final BlockState state,
                                                                @NotNull final Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (root.getBlock() != this) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos, root)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door) {
            final int skinIndex = door.cycleSkin();
            player.displayClientMessage(Component.translatable("tooltip.malisisdoorsreborn.label.skin", HbmSlidingSealDoorBlockItem.skinName(skinIndex)), true);
            playScrewdriverClick(level, rootPos);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private @NotNull InteractionResult cycleRedstoneModeWithScrewdriver(@NotNull final Level level,
                                                                        @NotNull final BlockPos pos,
                                                                        @NotNull final BlockState state,
                                                                        @NotNull final Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (root.getBlock() != this) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos, root)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door) {
            final HbmDoorRedstoneMode mode = door.cycleRedstoneMode();
            player.displayClientMessage(Component.translatable("tooltip.malisisdoorsreborn.label.redstone", mode.displayName()), true);
            playScrewdriverClick(level, rootPos);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private static void playScrewdriverClick(@NotNull final Level level,
                                             @NotNull final BlockPos pos) {
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.35F, 1.35F);
    }

    @Override
    public void neighborChanged(@NotNull final BlockState state,
                                @NotNull final Level level,
                                @NotNull final BlockPos pos,
                                @NotNull final Block neighbourBlock,
                                @NotNull final BlockPos neighbourPos,
                                final boolean isMoving) {
        if (level.isClientSide) return;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (!(root.getBlock() instanceof final HbmSlidingSealDoorBlock doorBlock)) return;
        if (isSameDoorPart(level, neighbourPos, rootPos)) return;

        final boolean powered = doorBlock.hasDoorSignal(level, rootPos);
        final boolean wasPowered = root.getValue(POWERED);
        if (powered == wasPowered) return;

        if (!(level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door) || door.isMoving(root.getValue(OPEN))) return;
        if (door.getAccessLevel() != DoorAccessLevel.DEFAULT) return;

        doorBlock.setDoorPowered(level, rootPos, powered);
        doorBlock.applyRedstoneChange(level, rootPos, powered, door.getRedstoneMode());
    }

    @Override
    public void onRemove(@NotNull final BlockState state,
                         @NotNull final Level level,
                         @NotNull final BlockPos pos,
                         @NotNull final BlockState newState,
                         final boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            removeDoorParts(level, rootPos(pos, state));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final Player player) {
        if (!level.isClientSide && !player.isCreative()) {
            final BlockPos rootPos = rootPos(pos, state);
            final BlockState root = level.getBlockState(rootPos);
            if (root.getBlock() == this && !pos.equals(rootPos)) {
                level.destroyBlock(rootPos, true, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull final BlockGetter level,
                                                @NotNull final BlockPos pos,
                                                @NotNull final BlockState state) {
        final BlockPos rootPos = rootPos(pos, state);
        final HbmDoorRedstoneMode redstoneMode = level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door ? door.getRedstoneMode() : HbmDoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door ? door.getAccessLevel() : DoorAccessLevel.DEFAULT;
        final int skinIndex = level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door ? door.getSkinIndex() : 0;
        return HbmSlidingSealDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_SLIDING_SEAL_DOOR.get(), skinIndex, redstoneMode, accessLevel);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull final BlockState state,
                                             @NotNull final LootParams.Builder builder) {
        if (state.getValue(PART) != HbmSlidingSealDoorPart.ROOT) return List.of();
        final BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        final HbmDoorRedstoneMode redstoneMode = blockEntity instanceof final HbmSlidingSealDoorBlockEntity doorEntity ? doorEntity.getRedstoneMode() : HbmDoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = blockEntity instanceof final HbmSlidingSealDoorBlockEntity doorEntity ? doorEntity.getAccessLevel() : DoorAccessLevel.DEFAULT;
        final int skinIndex = blockEntity instanceof final HbmSlidingSealDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
        return List.of(HbmSlidingSealDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_SLIDING_SEAL_DOOR.get(), skinIndex, redstoneMode, accessLevel));
    }

    @Override
    public boolean onDestroyedByPlayer(@NotNull final BlockState state,
                                       @NotNull final Level level,
                                       @NotNull final BlockPos pos,
                                       @NotNull final Player player,
                                       final boolean willHarvest,
                                       @NotNull final FluidState fluid) {
        if (player.isCreative()) {
            final BlockPos rootPos = rootPos(pos, state);
            if (pos.equals(rootPos)) removeDoorParts(level, rootPos);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull final BlockState state,
                                        @NotNull final BlockGetter level,
                                        @NotNull final BlockPos pos,
                                        @NotNull final CollisionContext context) {
        return closedShapeForPart(state);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull final BlockState state,
                                                 @NotNull final BlockGetter level,
                                                 @NotNull final BlockPos pos,
                                                 @NotNull final CollisionContext context) {
        if (state.getValue(OPEN)) return Shapes.empty();
        return closedShapeForPart(state);
    }

    private VoxelShape closedShapeForPart(final BlockState state) {
        return switch (state.getValue(FACING)) {
            case NORTH -> Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.25D);
            case SOUTH -> Shapes.box(0.0D, 0.0D, 0.75D, 1.0D, 1.0D, 1.0D);
            case WEST -> Shapes.box(0.0D, 0.0D, 0.0D, 0.25D, 1.0D, 1.0D);
            case EAST -> Shapes.box(0.75D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
            default -> Shapes.block();
        };
    }


    private static DoorHingeSide hingeForPlacement(final BlockPlaceContext context, final Direction facing) {
        final BlockPos clickedPos = context.getClickedPos();
        final double hitX = context.getClickLocation().x - clickedPos.getX();
        final double hitZ = context.getClickLocation().z - clickedPos.getZ();

        final Direction leftDirection = facing.getCounterClockWise();
        final Direction rightDirection = facing.getClockWise();
        final int leftObstruction = obstructionScore(context.getLevel(), clickedPos, leftDirection);
        final int rightObstruction = obstructionScore(context.getLevel(), clickedPos, rightDirection);
        if (leftObstruction != rightObstruction) {
            return leftObstruction < rightObstruction ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
        }

        final boolean clickedLeft = switch (facing) {
            case NORTH -> hitX < 0.5D;
            case SOUTH -> hitX > 0.5D;
            case WEST -> hitZ > 0.5D;
            case EAST -> hitZ < 0.5D;
            default -> false;
        };
        return clickedLeft ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
    }

    private static int obstructionScore(final LevelAccessor level, final BlockPos rootPos, final Direction side) {
        int score = 0;
        for (int y = 0; y < HEIGHT; y++) {
            if (!level.getBlockState(rootPos.above(y).relative(side)).canBeReplaced()) score++;
        }
        return score;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull final BlockPos pos, @NotNull final BlockState state) {
        return state.getValue(PART) == HbmSlidingSealDoorPart.ROOT ? new HbmSlidingSealDoorBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull final Level level,
                                                                             @NotNull final BlockState state,
                                                                             @NotNull final BlockEntityType<T> type) {
        if (type != MdrBlockEntities.HBM_SLIDING_SEAL_DOOR.get()
                || state.getValue(PART) != HbmSlidingSealDoorPart.ROOT) {
            return null;
        }
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> HbmSlidingSealDoorBlockEntity.tickClient(lvl, pos, st, (HbmSlidingSealDoorBlockEntity) be);
        }
        return (lvl, pos, st, be) -> HbmSlidingSealDoorBlockEntity.tickServer(lvl, pos, st, (HbmSlidingSealDoorBlockEntity) be);
    }

    private boolean isSameDoorPart(final Level level, final BlockPos neighbourPos, final BlockPos rootPos) {
        final BlockState neighbour = level.getBlockState(neighbourPos);
        if (neighbour.getBlock() != this) return false;
        return rootPos(neighbourPos, neighbour).equals(rootPos);
    }

    private boolean canPlaceDoor(final LevelAccessor level, final BlockPos rootPos, final BlockPlaceContext context) {
        for (int y = 0; y < HEIGHT; y++) {
            final BlockPos partPos = rootPos.above(y);
            final BlockState existing = level.getBlockState(partPos);
            if (!existing.canBeReplaced(context)) return false;
        }
        return true;
    }

    private void placeDoorParts(final Level level,
                                final BlockPos rootPos,
                                final Direction facing,
                                final DoorHingeSide hinge,
                                final boolean open,
                                final boolean powered) {
        final BlockState base = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HINGE, hinge)
                .setValue(OPEN, open)
                .setValue(POWERED, powered);
        for (int y = 0; y < HEIGHT; y++) {
            final HbmSlidingSealDoorPart part = y == ROOT_Y_PART ? HbmSlidingSealDoorPart.ROOT : HbmSlidingSealDoorPart.PART;
            level.setBlock(rootPos.above(y), base.setValue(Y_PART, y).setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    private void removeDoorParts(final Level level, final BlockPos rootPos) {
        for (int y = 0; y < HEIGHT; y++) {
            final BlockPos partPos = rootPos.above(y);
            final BlockState state = level.getBlockState(partPos);
            if (state.getBlock() == this) {
                level.setBlock(partPos, Fluids.EMPTY.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            }
        }
    }

    private boolean canManualToggle(@NotNull final Level level,
                                    @NotNull final BlockPos rootPos) {
        if (level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door) {
            if (door.getAccessLevel() != DoorAccessLevel.DEFAULT) return true;
            return door.getRedstoneMode() != HbmDoorRedstoneMode.REDSTONE_ONLY;
        }
        return true;
    }

    private void applyRedstoneChange(@NotNull final Level level,
                                     @NotNull final BlockPos rootPos,
                                     final boolean powered,
                                     @NotNull final HbmDoorRedstoneMode mode) {
        switch (mode) {
            case DEFAULT, REDSTONE_ONLY -> setDoorOpen(level, rootPos, powered, null);
            case ALWAYS_IGNORE -> {}
            case OPEN_WHEN_POWERED -> {
                if (powered) setDoorOpen(level, rootPos, true, null);
            }
            case CLOSE_WHEN_POWERED -> {
                if (powered) setDoorOpen(level, rootPos, false, null);
            }
        }
    }

    private void setDoorPowered(final Level level, final BlockPos rootPos, final boolean powered) {
        updateDoorParts(level, rootPos, powered, null);
    }

    private void setDoorOpen(final Level level, final BlockPos rootPos, final boolean open, @Nullable final Player player) {
        final BlockState root = level.getBlockState(rootPos);
        if (root.getValue(OPEN) == open) return;
        updateDoorParts(level, rootPos, null, open);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, rootPos);
        level.playSound(null, rootPos, MdrSounds.HBM_SLIDING_SEAL_DOOR_MOVE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
    }

    private void updateDoorParts(final Level level,
                                 final BlockPos rootPos,
                                 @Nullable final Boolean powered,
                                 @Nullable final Boolean open) {
        for (int y = 0; y < HEIGHT; y++) {
            final BlockPos partPos = rootPos.above(y);
            BlockState state = level.getBlockState(partPos);
            if (state.getBlock() != this) continue;
            if (powered != null) state = state.setValue(POWERED, powered);
            if (open != null) state = state.setValue(OPEN, open);
            level.setBlock(partPos, state, Block.UPDATE_ALL);
        }
    }

    public void playStopSound(@NotNull final Level level, @NotNull final BlockPos rootPos) {
        level.playSound(null, rootPos, MdrSounds.HBM_SLIDING_SEAL_DOOR_STOP.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
    }

    private static boolean isDoorMoving(final Level level, final BlockPos rootPos, final BlockState root) {
        if (level.getBlockEntity(rootPos) instanceof final HbmSlidingSealDoorBlockEntity door) {
            return door.isMoving(root.getValue(OPEN));
        }
        return false;
    }

    private boolean hasDoorSignal(final Level level, final BlockPos rootPos) {
        for (int y = 0; y < HEIGHT; y++) {
            if (level.hasNeighborSignal(rootPos.above(y))) return true;
        }
        return false;
    }

    public static BlockPos rootPos(final BlockPos pos, final BlockState state) {
        return pos.below(state.getValue(Y_PART) - ROOT_Y_PART);
    }

    public enum HbmSlidingSealDoorPart implements StringRepresentable {
        ROOT("root"),
        PART("part");

        private final String name;

        HbmSlidingSealDoorPart(final String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
