package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import dev.mostlyharmless.malisisdoorsreborn.access.AccessCardDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomDoorBreakTarget;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmVehicleDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmVehicleDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverMode;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrItems;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HbmVehicleDoorBlock extends Block implements EntityBlock, CustomDoorBreakTarget, CustomSkinnedDoorTarget, HbmScrewdriverDoorTarget, AccessCardDoorTarget {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty X_PART = IntegerProperty.create("x", 0, 6);
    public static final IntegerProperty Y_PART = IntegerProperty.create("y", 0, 5);
    public static final EnumProperty<HbmVehicleDoorPart> PART = EnumProperty.create("part", HbmVehicleDoorPart.class);
    public static final IntegerProperty SKIN = IntegerProperty.create("skin", 0, 4);

    private static final int ROOT_X_PART = 3;
    private static final int ROOT_Y_PART = 0;
    private static final int WIDTH = 7;
    private static final int HEIGHT = 6;
    private static final int STATE_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public HbmVehicleDoorBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(X_PART, ROOT_X_PART)
                .setValue(Y_PART, ROOT_Y_PART)
                .setValue(PART, HbmVehicleDoorPart.ROOT)
                .setValue(SKIN, 0));
    }


    @Override
    public boolean isCustomDoorBreakRoot(@NotNull final BlockState rootState,
                                         final BlockEntity blockEntity) {
        return rootState.getBlock() == this
                && rootState.hasProperty(PART)
                && rootState.getValue(PART) == HbmVehicleDoorPart.ROOT
                && blockEntity instanceof HbmVehicleDoorBlockEntity;
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
        AABB bounds = null;
        final Direction facing = rootState.getValue(FACING);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                final BlockState partState = level.getBlockState(partPos);
                if (partState.getBlock() != this) continue;
                final double minX = partPos.getX() - rootPos.getX();
                final double minY = partPos.getY() - rootPos.getY();
                final double minZ = partPos.getZ() - rootPos.getZ();
                final AABB partBounds = new AABB(minX, minY, minZ, minX + 1.0D, minY + 1.0D, minZ + 1.0D);
                bounds = bounds == null ? partBounds : bounds.minmax(partBounds);
            }
        }
        return bounds == null ? new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D) : bounds;
    }

    @Override
    public BlockState customDoorBreakParticleState(@NotNull final Level level,
                                                   @NotNull final BlockPos pos,
                                                   @NotNull final BlockState state) {
        return MdrBlocks.HBM_VEHICLE_DOOR_PARTICLE_DEFAULT.get().defaultBlockState();
    }

    private int skinIndexAt(@NotNull final BlockGetter level, @NotNull final BlockPos rootPos) {
        final BlockState rootState = level.getBlockState(rootPos);
        final int entitySkin = level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door ? door.getSkinIndex() : 0;
        if (rootState.getBlock() != this || !rootState.hasProperty(SKIN)) return entitySkin;
        final int stateSkin = rootState.getValue(SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }


    @Override
    public @NotNull IntegerProperty customDoorSkinProperty() {
        return SKIN;
    }

    @Override
    public int customDoorSkinCount() {
        return HbmVehicleDoorBlockItem.SKIN_COUNT;
    }

    @Override
    public @NotNull Iterable<BlockPos> customDoorSkinPartPositions(@NotNull final BlockPos rootPos,
                                                                   @NotNull final BlockState rootState) {
        final List<BlockPos> positions = new ArrayList<>(WIDTH * HEIGHT);
        final Direction facing = rootState.getValue(FACING);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                positions.add(partPos(rootPos, facing, x, y));
            }
        }
        return positions;
    }

    @Override
    public int customDoorEntitySkin(@NotNull final BlockEntity blockEntity) {
        return blockEntity instanceof final HbmVehicleDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
    }

    @Override
    public void customDoorSetEntitySkin(@NotNull final BlockEntity blockEntity, final int skinIndex) {
        if (blockEntity instanceof final HbmVehicleDoorBlockEntity door) {
            door.setSkinIndexNoBlockUpdate(skinIndex);
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, X_PART, Y_PART, PART, SKIN);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos rootPos = context.getClickedPos();
        final Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canPlaceDoor(level, rootPos, facing, context)) return null;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SKIN, HbmVehicleDoorBlockItem.skinIndexFromStack(context.getItemInHand()));
    }

    @Override
    public void setPlacedBy(@NotNull final Level level,
                            @NotNull final BlockPos pos,
                            @NotNull final BlockState state,
                            @Nullable final LivingEntity placer,
                            @NotNull final ItemStack stack) {
        if (!level.isClientSide()) {
            final int skinIndex = HbmVehicleDoorBlockItem.skinIndexFromStack(stack);
            final HbmDoorRedstoneMode redstoneMode = HbmVehicleDoorBlockItem.redstoneModeFromStack(stack);
            final DoorAccessLevel accessLevel = HbmVehicleDoorBlockItem.accessLevelFromStack(stack);
            placeDoorParts(level, pos, state.getValue(FACING), state.getValue(OPEN), state.getValue(POWERED), skinIndex);
            if (level.getBlockEntity(pos) instanceof final HbmVehicleDoorBlockEntity door) {
                door.setSkinIndex(skinIndex);
                door.setRedstoneMode(redstoneMode);
                door.setAccessLevel(accessLevel);
            }
        }
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull final BlockState state,
                                                     @NotNull final Level level,
                                                     @NotNull final BlockPos pos,
                                                     @NotNull final Player player,
                                                     @NotNull final BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (!(root.getBlock() instanceof final HbmVehicleDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity accessDoor
                && !DoorAccessHelper.canOpen(player, accessDoor.getAccessLevel())) {
            DoorAccessHelper.showAccessLevel(player, accessDoor.getAccessLevel());
            return InteractionResult.CONSUME;
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (!(root.getBlock() instanceof final HbmVehicleDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity accessDoor) {
            final InteractionResult accessResult = DoorAccessHelper.handleAccessCardUse(level, rootPos, player, accessDoor, cardStack);
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (root.getBlock() != this) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door) {
            final int skinIndex = door.cycleSkin();
            updateDoorSkin(level, rootPos, root.getValue(FACING), skinIndex);
            player.displayClientMessage(Component.translatable("tooltip.malisisdoorsreborn.label.skin", HbmVehicleDoorBlockItem.skinName(skinIndex)), true);
            playScrewdriverClick(level, rootPos);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private @NotNull InteractionResult cycleRedstoneModeWithScrewdriver(@NotNull final Level level,
                                                                        @NotNull final BlockPos pos,
                                                                        @NotNull final BlockState state,
                                                                        @NotNull final Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (root.getBlock() != this) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door) {
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
                                @Nullable final Orientation orientation,
                                final boolean isMoving) {
        if (level.isClientSide()) return;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (!(root.getBlock() instanceof final HbmVehicleDoorBlock doorBlock)) return;

        final boolean powered = doorBlock.hasDoorSignal(level, rootPos, root.getValue(FACING));
        final boolean wasPowered = root.getValue(POWERED);
        if (powered == wasPowered) return;

        if (!(level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door) || door.isMoving()) return;
        if (door.getAccessLevel() != DoorAccessLevel.DEFAULT) return;

        doorBlock.setDoorPowered(level, rootPos, powered);
        doorBlock.applyRedstoneChange(level, rootPos, powered, door.getRedstoneMode());
    }

    @Override
    protected void affectNeighborsAfterRemoval(@NotNull final BlockState state,
                                               @NotNull final ServerLevel level,
                                               @NotNull final BlockPos pos,
                                               final boolean movedByPiston) {
        removeDoorParts(level, rootPos(pos, state), state.getValue(FACING));
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull final Level level,
                                                 @NotNull final BlockPos pos,
                                                 @NotNull final BlockState state,
                                                 @NotNull final Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            final BlockPos rootPos = rootPos(pos, state);
            final BlockState root = level.getBlockState(rootPos);
            if (root.getBlock() == this && !pos.equals(rootPos)) {
                level.destroyBlock(rootPos, true, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull final LevelReader level,
                                                @NotNull final BlockPos pos,
                                                @NotNull final BlockState state,
                                                final boolean includeData,
                                                @NotNull final Player player) {
        final BlockPos rootPos = rootPos(pos, state);
        final HbmDoorRedstoneMode redstoneMode = level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door ? door.getRedstoneMode() : HbmDoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door ? door.getAccessLevel() : DoorAccessLevel.DEFAULT;
        return HbmVehicleDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_VEHICLE_DOOR.get(), skinIndexAt(level, rootPos), redstoneMode, accessLevel);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull final BlockState state,
                                             @NotNull final LootParams.Builder builder) {
        if (state.getValue(PART) != HbmVehicleDoorPart.ROOT) return List.of();
        final BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        final int entitySkin = blockEntity instanceof final HbmVehicleDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
        final int stateSkin = state.hasProperty(SKIN) ? state.getValue(SKIN) : 0;
        final int skinIndex = stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
        final HbmDoorRedstoneMode redstoneMode = blockEntity instanceof final HbmVehicleDoorBlockEntity doorEntity ? doorEntity.getRedstoneMode() : HbmDoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = blockEntity instanceof final HbmVehicleDoorBlockEntity doorEntity ? doorEntity.getAccessLevel() : DoorAccessLevel.DEFAULT;
        return List.of(HbmVehicleDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_VEHICLE_DOOR.get(), skinIndex, redstoneMode, accessLevel));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull final BlockState state,
                                        @NotNull final BlockGetter level,
                                        @NotNull final BlockPos pos,
                                        @NotNull final CollisionContext context) {
        return fullDoorShape(state);
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(@NotNull final BlockState state,
                                                   @NotNull final BlockGetter level,
                                                   @NotNull final BlockPos pos) {
        return fullDoorShape(state);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull final BlockState state,
                                                 @NotNull final BlockGetter level,
                                                 @NotNull final BlockPos pos,
                                                 @NotNull final CollisionContext context) {
        return collisionShapeForPart(state);
    }

    private VoxelShape fullDoorShape(final BlockState state) {
        final Direction across = state.getValue(FACING).getCounterClockWise();
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;
        VoxelShape shape = Shapes.empty();

        for (int x = 0; x < WIDTH; x++) {
            final int otherLocalX = x - ROOT_X_PART;
            final int acrossOffset = otherLocalX - localX;
            final double minX = across.getStepX() * acrossOffset;
            final double minZ = across.getStepZ() * acrossOffset;
            final double x1 = Math.min(minX, minX + 1.0D);
            final double z1 = Math.min(minZ, minZ + 1.0D);
            final double x2 = Math.max(minX, minX + 1.0D);
            final double z2 = Math.max(minZ, minZ + 1.0D);

            for (int y = 0; y < HEIGHT; y++) {
                final double y1 = y - localY;
                shape = Shapes.or(shape, Shapes.box(x1, y1, z1, x2, y1 + 1.0D, z2));
            }
        }

        return shape;
    }

    private VoxelShape collisionShapeForPart(final BlockState state) {
        if (!state.getValue(OPEN)) return Shapes.block();

        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;

        if (localX == 3 || localX == -3) return Shapes.block();
        if (localY == HEIGHT - 1) return Shapes.block();
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull final BlockPos pos, @NotNull final BlockState state) {
        return state.getValue(PART) == HbmVehicleDoorPart.ROOT ? new HbmVehicleDoorBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull final Level level,
                                                                             @NotNull final BlockState state,
                                                                             @NotNull final BlockEntityType<T> type) {
        if (type != MdrBlockEntities.HBM_VEHICLE_DOOR.get()
                || state.getValue(PART) != HbmVehicleDoorPart.ROOT) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> HbmVehicleDoorBlockEntity.tickClient(lvl, pos, st, (HbmVehicleDoorBlockEntity) be);
        }
        return (lvl, pos, st, be) -> HbmVehicleDoorBlockEntity.tickServer(lvl, pos, st, (HbmVehicleDoorBlockEntity) be);
    }


    private boolean canPlaceDoor(final LevelAccessor level, final BlockPos rootPos, final Direction facing, final BlockPlaceContext context) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                final BlockState existing = level.getBlockState(partPos);
                if (!partPos.equals(rootPos) && !existing.canBeReplaced(context)) return false;
                if (partPos.equals(rootPos) && !existing.canBeReplaced(context)) return false;
            }
        }
        return true;
    }

    private void placeDoorParts(final Level level, final BlockPos rootPos, final Direction facing, final boolean open, final boolean powered, final int skinIndex) {
        final BlockState base = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(OPEN, open)
                .setValue(POWERED, powered)
                .setValue(SKIN, Math.floorMod(skinIndex, HbmVehicleDoorBlockItem.SKIN_COUNT));
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final HbmVehicleDoorPart part = (x == ROOT_X_PART && y == ROOT_Y_PART) ? HbmVehicleDoorPart.ROOT : HbmVehicleDoorPart.PART;
                level.setBlock(partPos(rootPos, facing, x, y), base.setValue(X_PART, x).setValue(Y_PART, y).setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    private void removeDoorParts(final Level level, final BlockPos rootPos, final Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                final BlockState state = level.getBlockState(partPos);
                if (state.getBlock() == this) {
                    level.setBlock(partPos, Fluids.EMPTY.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private void updateDoorSkin(final Level level, final BlockPos rootPos, final Direction facing, final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, HbmVehicleDoorBlockItem.SKIN_COUNT);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                BlockState state = level.getBlockState(partPos);
                if (state.getBlock() != this || !state.hasProperty(SKIN)) continue;
                level.setBlock(partPos, state.setValue(SKIN, normalisedSkin), STATE_UPDATE_FLAGS);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canManualToggle(@NotNull final Level level,
                                    @NotNull final BlockPos rootPos) {
        if (level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door) {
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
        updateDoorParts(level, rootPos, level.getBlockState(rootPos).getValue(FACING), powered, null);
    }



    private void setDoorOpen(final Level level, final BlockPos rootPos, final boolean open, @Nullable final Player player) {
        final BlockState root = level.getBlockState(rootPos);
        if (root.getValue(OPEN) == open) return;
        if (level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door) {
            door.setOpen(open);
            level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, rootPos);
        }
    }

    public void setWholeOpen(final Level level, final BlockPos rootPos, final BlockState rootState, final boolean open) {
        if (rootState.getValue(OPEN) == open) return;
        updateDoorParts(level, rootPos, rootState.getValue(FACING), null, open);
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, rootPos);
    }

    private void updateDoorParts(final Level level,
                                 final BlockPos rootPos,
                                 final Direction facing,
                                 @Nullable final Boolean powered,
                                 @Nullable final Boolean open) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                BlockState state = level.getBlockState(partPos);
                if (state.getBlock() != this) continue;
                if (powered != null) state = state.setValue(POWERED, powered);
                if (open != null) state = state.setValue(OPEN, open);
                level.setBlock(partPos, state, STATE_UPDATE_FLAGS);
            }
        }
    }

    private static boolean isDoorMoving(final Level level, final BlockPos rootPos) {
        return level.getBlockEntity(rootPos) instanceof final HbmVehicleDoorBlockEntity door && door.isMoving();
    }

    private boolean hasDoorSignal(final Level level, final BlockPos rootPos, final Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (level.hasNeighborSignal(partPos(rootPos, facing, x, y))) return true;
            }
        }
        return false;
    }

    public static BlockPos rootPos(final BlockPos pos, final BlockState state) {
        final Direction across = state.getValue(FACING).getCounterClockWise();
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;
        return pos.relative(across, -localX).below(localY);
    }

    public static BlockPos partPos(final BlockPos rootPos, final Direction facing, final int xPart, final int yPart) {
        final Direction across = facing.getCounterClockWise();
        final int localX = xPart - ROOT_X_PART;
        return rootPos.relative(across, localX).above(yPart - ROOT_Y_PART);
    }

    public enum HbmVehicleDoorPart implements StringRepresentable {
        ROOT("root"),
        PART("part");

        private final String name;

        HbmVehicleDoorPart(final String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
