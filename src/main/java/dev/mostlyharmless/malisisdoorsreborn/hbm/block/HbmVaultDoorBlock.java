package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import dev.mostlyharmless.malisisdoorsreborn.access.AccessCardDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomDoorBreakTarget;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmVaultDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverMode;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmVaultDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlocks;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.material.Fluids;
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

public class HbmVaultDoorBlock extends Block implements EntityBlock, CustomDoorBreakTarget, CustomSkinnedDoorTarget, HbmScrewdriverDoorTarget, AccessCardDoorTarget {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty X_PART = IntegerProperty.create("x", 0, 4);
    public static final IntegerProperty Y_PART = IntegerProperty.create("y", 0, 4);
    public static final IntegerProperty Z_PART = IntegerProperty.create("z", 0, 1);
    public static final EnumProperty<HbmVaultDoorPart> PART = EnumProperty.create("part", HbmVaultDoorPart.class);
    public static final IntegerProperty SKIN = IntegerProperty.create("skin", 0, 6);

    private static final int ROOT_X_PART = 2;
    private static final int ROOT_Y_PART = 0;
    private static final int ROOT_Z_PART = 1;
    private static final int WIDTH = 5;
    private static final int HEIGHT = 5;
    private static final int MAIN_Z_PART = 1;
    private static final int EXTRA_Z_PART = 0;
    private static final List<DoorPartCoord> DOOR_PART_COORDS = buildDoorPartCoords();

    public HbmVaultDoorBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(X_PART, ROOT_X_PART)
                .setValue(Y_PART, ROOT_Y_PART)
                .setValue(Z_PART, ROOT_Z_PART)
                .setValue(PART, HbmVaultDoorPart.ROOT)
                .setValue(SKIN, 0));
    }

    @Override
    public boolean isCustomDoorBreakRoot(@NotNull final BlockState rootState, final BlockEntity blockEntity) {
        return rootState.getBlock() == this
                && rootState.hasProperty(PART)
                && rootState.getValue(PART) == HbmVaultDoorPart.ROOT
                && blockEntity instanceof HbmVaultDoorBlockEntity;
    }

    @Override
    public BlockPos customDoorBreakRootPos(@NotNull final BlockPos pos, @NotNull final BlockState state) {
        return rootPos(pos, state);
    }

    @Override
    public AABB customDoorBreakBounds(@NotNull final BlockGetter level,
                                      @NotNull final BlockPos rootPos,
                                      @NotNull final BlockState rootState) {
        return customDoorBreakBoundsForParts(level, rootPos, rootState, false);
    }

    @Override
    public AABB customDoorBreakBounds(@NotNull final BlockGetter level,
                                      @NotNull final BlockPos rootPos,
                                      @NotNull final BlockState rootState,
                                      @NotNull final BlockPos targetPos,
                                      @NotNull final BlockState targetState) {
        if (targetState.getBlock() == this && isRearBasePart(targetState)) {
            return customDoorBreakBoundsForParts(level, rootPos, rootState, true);
        }
        return customDoorBreakBounds(level, rootPos, rootState);
    }

    private AABB customDoorBreakBoundsForParts(@NotNull final BlockGetter level,
                                               @NotNull final BlockPos rootPos,
                                               @NotNull final BlockState rootState,
                                               final boolean rearRailOnly) {
        AABB bounds = null;
        final Direction facing = rootState.getValue(FACING);
        final Direction across = facing.getCounterClockWise();
        for (DoorPartCoord coord : doorPartCoords()) {
            if (rearRailOnly) {
                if (!(coord.z == EXTRA_Z_PART && coord.y == ROOT_Y_PART)) continue;
            } else if (coord.z != MAIN_Z_PART) {
                continue;
            }
            final BlockPos partPos = partPos(rootPos, facing, coord.x, coord.y, coord.z);
            final BlockState partState = level.getBlockState(partPos);
            if (partState.getBlock() != this) continue;

            double minX = partPos.getX() - rootPos.getX();
            final double minY = partPos.getY() - rootPos.getY();
            double minZ = partPos.getZ() - rootPos.getZ();
            double maxX = minX + 1.0D;
            double maxZ = minZ + 1.0D;
            if (rearRailOnly && isRearSelectionRightEdge(coord)) {
                if (across.getStepX() < 0) minX += 0.5D;
                else if (across.getStepX() > 0) maxX -= 0.5D;
                else if (across.getStepZ() < 0) minZ += 0.5D;
                else if (across.getStepZ() > 0) maxZ -= 0.5D;
            }
            final AABB partBounds = new AABB(minX, minY, minZ, maxX, minY + selectionHeight(coord), maxZ);
            bounds = bounds == null ? partBounds : bounds.minmax(partBounds);
        }
        return bounds == null ? new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D) : bounds;
    }

    @Override
    public BlockState customDoorBreakParticleState(@NotNull final Level level,
                                                   @NotNull final BlockPos pos,
                                                   @NotNull final BlockState state) {
        final BlockPos rootPos = rootPos(pos, state);
        return particleStateForSkinIndex(skinIndexAt(level, rootPos));
    }

    private BlockState particleStateForSkinIndex(final int skinIndex) {
        return switch (Math.floorMod(skinIndex, HbmVaultDoorBlockItem.SKIN_COUNT)) {
            case 1 -> MdrBlocks.HBM_VAULT_DOOR_PARTICLE_87.get().defaultBlockState();
            case 2 -> MdrBlocks.HBM_VAULT_DOOR_PARTICLE_106.get().defaultBlockState();
            case 3 -> MdrBlocks.HBM_VAULT_DOOR_PARTICLE_81.get().defaultBlockState();
            case 4 -> MdrBlocks.HBM_VAULT_DOOR_PARTICLE_111.get().defaultBlockState();
            case 5 -> MdrBlocks.HBM_VAULT_DOOR_PARTICLE_2.get().defaultBlockState();
            case 6 -> MdrBlocks.HBM_VAULT_DOOR_PARTICLE_99.get().defaultBlockState();
            default -> MdrBlocks.HBM_VAULT_DOOR_PARTICLE_101.get().defaultBlockState();
        };
    }

    private int skinIndexAt(@NotNull final BlockGetter level, @NotNull final BlockPos rootPos) {
        final BlockState rootState = level.getBlockState(rootPos);
        final int entitySkin = level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door ? door.getSkinIndex() : 0;
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
        return HbmVaultDoorBlockItem.SKIN_COUNT;
    }

    @Override
    public @NotNull Iterable<BlockPos> customDoorSkinPartPositions(@NotNull final BlockPos rootPos,
                                                                   @NotNull final BlockState rootState) {
        final List<BlockPos> positions = new ArrayList<>(WIDTH * HEIGHT + WIDTH);
        final Direction facing = rootState.getValue(FACING);
        for (DoorPartCoord coord : doorPartCoords()) {
            positions.add(partPos(rootPos, facing, coord.x, coord.y, coord.z));
        }
        return positions;
    }

    @Override
    public int customDoorEntitySkin(@NotNull final BlockEntity blockEntity) {
        return blockEntity instanceof final HbmVaultDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
    }

    @Override
    public void customDoorSetEntitySkin(@NotNull final BlockEntity blockEntity, final int skinIndex) {
        if (blockEntity instanceof final HbmVaultDoorBlockEntity door) {
            door.setSkinIndexNoBlockUpdate(skinIndex);
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, X_PART, Y_PART, Z_PART, PART, SKIN);
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
                .setValue(SKIN, HbmVaultDoorBlockItem.skinIndexFromStack(context.getItemInHand()));
    }

    @Override
    public void setPlacedBy(@NotNull final Level level,
                            @NotNull final BlockPos pos,
                            @NotNull final BlockState state,
                            @Nullable final LivingEntity placer,
                            @NotNull final ItemStack stack) {
        if (!level.isClientSide()) {
            final int skinIndex = HbmVaultDoorBlockItem.skinIndexFromStack(stack);
            final HbmDoorRedstoneMode redstoneMode = HbmVaultDoorBlockItem.redstoneModeFromStack(stack);
            final DoorAccessLevel accessLevel = HbmVaultDoorBlockItem.accessLevelFromStack(stack);
            placeDoorParts(level, pos, state.getValue(FACING), state.getValue(OPEN), state.getValue(POWERED), skinIndex);
            if (level.getBlockEntity(pos) instanceof final HbmVaultDoorBlockEntity door) {
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
        if (!(root.getBlock() instanceof final HbmVaultDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity accessDoor
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
        if (!(root.getBlock() instanceof final HbmVaultDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity accessDoor) {
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

        if (level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door) {
            final int skinIndex = door.cycleSkin();
            updateDoorSkin(level, rootPos, root.getValue(FACING), skinIndex);
            player.sendOverlayMessage(Component.translatable("tooltip.malisisdoorsreborn.label.skin", HbmVaultDoorBlockItem.skinName(skinIndex)));
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

        if (level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door) {
            final HbmDoorRedstoneMode mode = door.cycleRedstoneMode();
            player.sendOverlayMessage(Component.translatable("tooltip.malisisdoorsreborn.label.redstone", mode.displayName()));
            playScrewdriverClick(level, rootPos);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private static void playScrewdriverClick(@NotNull final Level level, @NotNull final BlockPos pos) {
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
        if (!(root.getBlock() instanceof final HbmVaultDoorBlock doorBlock)) return;

        final boolean powered = doorBlock.hasDoorSignal(level, rootPos, root.getValue(FACING));
        final boolean wasPowered = root.getValue(POWERED);
        if (powered == wasPowered) return;

        if (!(level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door) || door.isMoving()) return;
        if (door.getAccessLevel() != DoorAccessLevel.DEFAULT) return;

        doorBlock.setDoorPowered(level, rootPos, powered);
        doorBlock.applyRedstoneChange(level, rootPos, powered, door.getRedstoneMode());
    }

    @Override
    protected void affectNeighborsAfterRemoval(@NotNull final BlockState state,
                                               @NotNull final net.minecraft.server.level.ServerLevel level,
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
        final HbmDoorRedstoneMode redstoneMode = level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door ? door.getRedstoneMode() : HbmDoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door ? door.getAccessLevel() : DoorAccessLevel.DEFAULT;
        return HbmVaultDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_VAULT_DOOR.get(), skinIndexAt(level, rootPos), redstoneMode, accessLevel);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull final BlockState state, @NotNull final LootParams.Builder builder) {
        if (state.getValue(PART) != HbmVaultDoorPart.ROOT) return List.of();
        final BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        final int entitySkin = blockEntity instanceof final HbmVaultDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
        final int stateSkin = state.hasProperty(SKIN) ? state.getValue(SKIN) : 0;
        final int skinIndex = stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
        final HbmDoorRedstoneMode redstoneMode = blockEntity instanceof final HbmVaultDoorBlockEntity doorEntity ? doorEntity.getRedstoneMode() : HbmDoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = blockEntity instanceof final HbmVaultDoorBlockEntity doorEntity ? doorEntity.getAccessLevel() : DoorAccessLevel.DEFAULT;
        return List.of(HbmVaultDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_VAULT_DOOR.get(), skinIndex, redstoneMode, accessLevel));
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
        final Direction depth = state.getValue(FACING);
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;
        final int localZ = state.getValue(Z_PART) - ROOT_Z_PART;
        VoxelShape shape = Shapes.empty();

        for (DoorPartCoord coord : doorPartCoords()) {
            final int otherLocalX = coord.x - ROOT_X_PART;
            final int otherLocalZ = coord.z - ROOT_Z_PART;
            final int acrossOffset = otherLocalX - localX;
            final int depthOffset = otherLocalZ - localZ;
            final double minX = across.getStepX() * acrossOffset + depth.getStepX() * depthOffset;
            final double minZ = across.getStepZ() * acrossOffset + depth.getStepZ() * depthOffset;
            double x1 = Math.min(minX, minX + 1.0D);
            double z1 = Math.min(minZ, minZ + 1.0D);
            double x2 = Math.max(minX, minX + 1.0D);
            double z2 = Math.max(minZ, minZ + 1.0D);
            if (isRearSelectionRightEdge(coord)) {
                if (across.getStepX() < 0) x1 += 0.5D;
                else if (across.getStepX() > 0) x2 -= 0.5D;
                else if (across.getStepZ() < 0) z1 += 0.5D;
                else if (across.getStepZ() > 0) z2 -= 0.5D;
            }
            final double y1 = coord.y - localY;
            final double y2 = y1 + selectionHeight(coord);
            shape = Shapes.or(shape, Shapes.box(x1, y1, z1, x2, y2, z2));
        }

        return shape;
    }

    private static double selectionHeight(final DoorPartCoord coord) {
        return coord.z == EXTRA_Z_PART && coord.y == ROOT_Y_PART ? 0.75D : 1.0D;
    }

    private static boolean isRearSelectionRightEdge(final DoorPartCoord coord) {
        return coord.z == EXTRA_Z_PART && coord.y == ROOT_Y_PART && coord.x == WIDTH - 1;
    }

    private VoxelShape collisionShapeForPart(final BlockState state) {
        if (isRearBasePart(state)) return Shapes.block();
        if (!isMainFacePart(state)) return Shapes.empty();
        if (!state.getValue(OPEN)) return Shapes.block();
        return isOpenCollisionFramePart(state) ? Shapes.block() : Shapes.empty();
    }

    private static boolean isMainFacePart(final BlockState state) {
        return state.getValue(Z_PART) == MAIN_Z_PART;
    }

    private static boolean isRearBasePart(final BlockState state) {
        return state.getValue(Z_PART) == EXTRA_Z_PART && state.getValue(Y_PART) == ROOT_Y_PART;
    }

    private static boolean isOpenCollisionFramePart(final BlockState state) {
        final int x = state.getValue(X_PART);
        final int y = state.getValue(Y_PART);
        return x == 0 || x == WIDTH - 1 || y == 0 || y == HEIGHT - 1;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull final BlockPos pos, @NotNull final BlockState state) {
        return state.getValue(PART) == HbmVaultDoorPart.ROOT ? new HbmVaultDoorBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull final Level level,
                                                                            @NotNull final BlockState state,
                                                                            @NotNull final BlockEntityType<T> type) {
        if (type != MdrBlockEntities.HBM_VAULT_DOOR.get()
                || state.getValue(PART) != HbmVaultDoorPart.ROOT) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> HbmVaultDoorBlockEntity.tickClient(lvl, pos, st, (HbmVaultDoorBlockEntity) be);
        }
        return (lvl, pos, st, be) -> HbmVaultDoorBlockEntity.tickServer(lvl, pos, st, (HbmVaultDoorBlockEntity) be);
    }


    private boolean canPlaceDoor(final LevelAccessor level, final BlockPos rootPos, final Direction facing, final BlockPlaceContext context) {
        for (DoorPartCoord coord : doorPartCoords()) {
            final BlockPos partPos = partPos(rootPos, facing, coord.x, coord.y, coord.z);
            final BlockState existing = level.getBlockState(partPos);
            if (!existing.canBeReplaced(context)) return false;
        }
        return true;
    }

    private void placeDoorParts(final Level level,
                                final BlockPos rootPos,
                                final Direction facing,
                                final boolean open,
                                final boolean powered,
                                final int skinIndex) {
        final BlockState base = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(OPEN, open)
                .setValue(POWERED, powered)
                .setValue(SKIN, Math.floorMod(skinIndex, HbmVaultDoorBlockItem.SKIN_COUNT));
        for (DoorPartCoord coord : doorPartCoords()) {
            final HbmVaultDoorPart part = coord.x == ROOT_X_PART && coord.y == ROOT_Y_PART && coord.z == ROOT_Z_PART ? HbmVaultDoorPart.ROOT : HbmVaultDoorPart.PART;
            level.setBlock(partPos(rootPos, facing, coord.x, coord.y, coord.z), base
                    .setValue(X_PART, coord.x)
                    .setValue(Y_PART, coord.y)
                    .setValue(Z_PART, coord.z)
                    .setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    private void removeDoorParts(final Level level, final BlockPos rootPos, final Direction facing) {
        for (DoorPartCoord coord : doorPartCoords()) {
            final BlockPos partPos = partPos(rootPos, facing, coord.x, coord.y, coord.z);
            final BlockState state = level.getBlockState(partPos);
            if (state.getBlock() == this) {
                level.setBlock(partPos, Fluids.EMPTY.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            }
        }
    }

    private void updateDoorSkin(final Level level, final BlockPos rootPos, final Direction facing, final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, HbmVaultDoorBlockItem.SKIN_COUNT);
        for (DoorPartCoord coord : doorPartCoords()) {
            final BlockPos partPos = partPos(rootPos, facing, coord.x, coord.y, coord.z);
            BlockState state = level.getBlockState(partPos);
            if (state.getBlock() != this || !state.hasProperty(SKIN)) continue;
            level.setBlock(partPos, state.setValue(SKIN, normalisedSkin), Block.UPDATE_ALL);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canManualToggle(@NotNull final Level level, @NotNull final BlockPos rootPos) {
        if (level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door) {
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
        if (level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door) {
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
        for (DoorPartCoord coord : doorPartCoords()) {
            final BlockPos partPos = partPos(rootPos, facing, coord.x, coord.y, coord.z);
            BlockState state = level.getBlockState(partPos);
            if (state.getBlock() != this) continue;
            if (powered != null) state = state.setValue(POWERED, powered);
            if (open != null) state = state.setValue(OPEN, open);
            level.setBlock(partPos, state, Block.UPDATE_ALL);
        }
    }

    private static boolean isDoorMoving(final Level level, final BlockPos rootPos) {
        if (level.getBlockEntity(rootPos) instanceof final HbmVaultDoorBlockEntity door) {
            return door.isMoving();
        }
        return false;
    }

    private boolean hasDoorSignal(final Level level, final BlockPos rootPos, final Direction facing) {
        for (DoorPartCoord coord : doorPartCoords()) {
            if (level.hasNeighborSignal(partPos(rootPos, facing, coord.x, coord.y, coord.z))) return true;
        }
        return false;
    }

    public static BlockPos rootPos(final BlockPos pos, final BlockState state) {
        final Direction across = state.getValue(FACING).getCounterClockWise();
        final Direction depth = state.getValue(FACING);
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;
        final int localZ = state.getValue(Z_PART) - ROOT_Z_PART;
        return pos.relative(across, -localX).relative(depth, -localZ).below(localY);
    }

    public static BlockPos partPos(final BlockPos rootPos, final Direction facing, final int xPart, final int yPart, final int zPart) {
        final Direction across = facing.getCounterClockWise();
        final int localX = xPart - ROOT_X_PART;
        final int localZ = zPart - ROOT_Z_PART;
        return rootPos.relative(across, localX).relative(facing, localZ).above(yPart - ROOT_Y_PART);
    }

    private static List<DoorPartCoord> doorPartCoords() {
        return DOOR_PART_COORDS;
    }

    private static List<DoorPartCoord> buildDoorPartCoords() {
        final List<DoorPartCoord> coords = new ArrayList<>(WIDTH * HEIGHT + WIDTH);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                coords.add(new DoorPartCoord(x, y, MAIN_Z_PART));
            }
            coords.add(new DoorPartCoord(x, ROOT_Y_PART, EXTRA_Z_PART));
        }
        return List.copyOf(coords);
    }

    private record DoorPartCoord(int x, int y, int z) {}

    public enum HbmVaultDoorPart implements StringRepresentable {
        ROOT("root"),
        PART("part");

        private final String name;

        HbmVaultDoorPart(final String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
