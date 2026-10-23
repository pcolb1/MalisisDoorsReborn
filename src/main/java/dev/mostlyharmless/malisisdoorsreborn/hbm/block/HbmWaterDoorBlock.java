package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import dev.mostlyharmless.malisisdoorsreborn.common.DoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomDoorBreakTarget;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.access.AccessCardDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.block.AccessCardReaderCardLevelMode;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmWaterDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmWaterDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmScrewdriverMode;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlocks;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.material.FluidState;
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

public class HbmWaterDoorBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, CustomDoorBreakTarget, CustomSkinnedDoorTarget, HbmScrewdriverDoorTarget, AccessCardDoorTarget {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final IntegerProperty X_PART = IntegerProperty.create("x", 0, 2);
    public static final IntegerProperty Y_PART = IntegerProperty.create("y", 0, 2);
    public static final EnumProperty<HbmWaterDoorPart> PART = EnumProperty.create("part", HbmWaterDoorPart.class);
    public static final IntegerProperty SKIN = IntegerProperty.create("skin", 0, 1);

    private static final int ROOT_X_PART = 1;
    private static final int ROOT_Y_PART = 0;
    private static final int WIDTH = 3;
    private static final int HEIGHT = 3;

    private static final int STATE_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public HbmWaterDoorBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(X_PART, ROOT_X_PART)
                .setValue(Y_PART, ROOT_Y_PART)
                .setValue(PART, HbmWaterDoorPart.ROOT)
                .setValue(SKIN, 0));
    }


    @Override
    public boolean isCustomDoorBreakRoot(@NotNull final BlockState rootState,
                                         final BlockEntity blockEntity) {
        return rootState.getBlock() == this
                && rootState.hasProperty(PART)
                && rootState.getValue(PART) == HbmWaterDoorPart.ROOT
                && blockEntity instanceof HbmWaterDoorBlockEntity;
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
        final Direction facing = rootState.getValue(FACING);
        final AABB bounds = HbmBreakingOverlayHelper.fullDoorBounds(
                level, rootPos, facing, this, WIDTH, HEIGHT, HbmWaterDoorBlock::partPos
        );
        return HbmBreakingOverlayHelper.directionalSlab(
                bounds, facing, 4.0D / 16.0D
        );
    }

    @Override
    public BlockState customDoorBreakParticleState(@NotNull final Level level,
                                                   @NotNull final BlockPos pos,
                                                   @NotNull final BlockState state) {
        final BlockPos rootPos = rootPos(pos, state);
        final int skinIndex = Math.floorMod(
                skinIndexAt(level, rootPos),
                HbmWaterDoorBlockItem.SKIN_COUNT
        );

        return skinIndex == 1
                ? MdrBlocks.HBM_WATER_DOOR_PARTICLE_CLEAN.get().defaultBlockState()
                : MdrBlocks.HBM_WATER_DOOR_PARTICLE_DEFAULT.get().defaultBlockState();
    }

    private int skinIndexAt(@NotNull final BlockGetter level, @NotNull final BlockPos rootPos) {
        final BlockState rootState = level.getBlockState(rootPos);
        final int entitySkin = level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door ? door.getSkinIndex() : 0;
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
        return HbmWaterDoorBlockItem.SKIN_COUNT;
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
        return blockEntity instanceof final HbmWaterDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
    }

    @Override
    public void customDoorSetEntitySkin(@NotNull final BlockEntity blockEntity, final int skinIndex) {
        if (blockEntity instanceof final HbmWaterDoorBlockEntity door) {
            door.setSkinIndexNoBlockUpdate(skinIndex);
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, WATERLOGGED, HINGE, X_PART, Y_PART, PART, SKIN);
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
                .setValue(WATERLOGGED, level.getFluidState(rootPos).getType() == Fluids.WATER)
                .setValue(HINGE, hingeForPlacement(context, rootPos, facing))
                .setValue(SKIN, HbmWaterDoorBlockItem.skinIndexFromStack(context.getItemInHand()));
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull final BlockState state,
                                              @NotNull final LevelReader level,
                                              @NotNull final ScheduledTickAccess scheduledTickAccess,
                                              @NotNull final BlockPos pos,
                                              @NotNull final Direction direction,
                                              @NotNull final BlockPos neighbourPos,
                                              @NotNull final BlockState neighbourState,
                                              @NotNull final RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected @NotNull FluidState getFluidState(@NotNull final BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public void setPlacedBy(@NotNull final Level level,
                            @NotNull final BlockPos pos,
                            @NotNull final BlockState state,
                            @Nullable final LivingEntity placer,
                            @NotNull final ItemStack stack) {
        if (!level.isClientSide()) {
            final int skinIndex = HbmWaterDoorBlockItem.skinIndexFromStack(stack);
            final DoorRedstoneMode redstoneMode = HbmWaterDoorBlockItem.redstoneModeFromStack(stack);
            final DoorAccessLevel accessLevel = HbmWaterDoorBlockItem.accessLevelFromStack(stack);
            final AccessCardReaderCardLevelMode cardLevelMode = DoorAccessHelper.cardLevelModeFromStack(stack);
            placeDoorParts(level, pos, state.getValue(FACING), state.getValue(HINGE), state.getValue(OPEN), state.getValue(POWERED), skinIndex);
            if (level.getBlockEntity(pos) instanceof final HbmWaterDoorBlockEntity door) {
                door.setSkinIndex(skinIndex);
                door.setRedstoneMode(redstoneMode);
                door.setAccessLevel(accessLevel);
                door.setCardLevelMode(cardLevelMode);
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
        if (!(root.getBlock() instanceof final HbmWaterDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity accessDoor
                && !DoorAccessHelper.canOpen(player, accessDoor)) {
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
        if (!(root.getBlock() instanceof final HbmWaterDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity accessDoor) {
            final InteractionResult accessResult = DoorAccessHelper.handleAccessCardUse(level, rootPos, player, accessDoor, cardStack);
            if (accessResult != InteractionResult.PASS) return accessResult;
            if (!DoorAccessHelper.canOpen(player, accessDoor)) {
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
            case CYCLE_PULSE_DURATION -> InteractionResult.PASS;
            case CYCLE_CARD_LEVELS -> cycleCardLevelModeWithScrewdriver(level, pos, state, player);
        };
    }

    private @NotNull InteractionResult cycleCardLevelModeWithScrewdriver(@NotNull final Level level,
                                                                         @NotNull final BlockPos pos,
                                                                         @NotNull final BlockState state,
                                                                         @NotNull final Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        final BlockPos rootPos = rootPos(pos, state);
        final BlockState root = level.getBlockState(rootPos);
        if (root.getBlock() != this) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door) {
            DoorAccessHelper.cycleCardLevelMode(level, rootPos, player, door);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
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

        if (level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door) {
            final int skinIndex = door.cycleSkin();
            updateDoorSkin(level, rootPos, root.getValue(FACING), skinIndex);
            player.sendOverlayMessage(Component.translatable("tooltip.malisisdoorsreborn.label.skin", HbmWaterDoorBlockItem.skinName(skinIndex)));
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

        if (level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door) {
            final DoorRedstoneMode mode = door.cycleRedstoneMode();
            player.sendOverlayMessage(Component.translatable("tooltip.malisisdoorsreborn.label.redstone", mode.displayName()));
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
        if (!(root.getBlock() instanceof final HbmWaterDoorBlock doorBlock)) return;
        final boolean powered = doorBlock.hasDoorSignal(level, rootPos, root.getValue(FACING));
        final boolean wasPowered = root.getValue(POWERED);
        if (powered == wasPowered) return;

        if (!(level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door) || door.isMoving()) return;
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
        final DoorRedstoneMode redstoneMode = level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door ? door.getRedstoneMode() : DoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door ? door.getAccessLevel() : DoorAccessLevel.DEFAULT;
        final AccessCardReaderCardLevelMode cardLevelMode = level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door ? door.getCardLevelMode() : AccessCardReaderCardLevelMode.defaultValue();
        return HbmWaterDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_WATER_DOOR.get(), skinIndexAt(level, rootPos), redstoneMode, accessLevel, cardLevelMode);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull final BlockState state,
                                             @NotNull final LootParams.Builder builder) {
        if (state.getValue(PART) != HbmWaterDoorPart.ROOT) return List.of();
        final BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        final int entitySkin = blockEntity instanceof final HbmWaterDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
        final int stateSkin = state.hasProperty(SKIN) ? state.getValue(SKIN) : 0;
        final int skinIndex = stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
        final DoorRedstoneMode redstoneMode = blockEntity instanceof final HbmWaterDoorBlockEntity doorEntity ? doorEntity.getRedstoneMode() : DoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = blockEntity instanceof final HbmWaterDoorBlockEntity doorEntity ? doorEntity.getAccessLevel() : DoorAccessLevel.DEFAULT;
        final AccessCardReaderCardLevelMode cardLevelMode = blockEntity instanceof final HbmWaterDoorBlockEntity doorEntity ? doorEntity.getCardLevelMode() : AccessCardReaderCardLevelMode.defaultValue();
        return List.of(HbmWaterDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_WATER_DOOR.get(), skinIndex, redstoneMode, accessLevel, cardLevelMode));
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
        final BlockPos rootPos = rootPos(pos, state);
        final Boolean rootOpen = HbmCollisionHelper.rootOpen(level, rootPos, this);
        if (rootOpen == null) return collisionShapeForPart(state);

        final float progress = level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door
                ? door.getProgress(1.0F)
                : rootOpen ? 1.0F : 0.0F;
        return collisionShapeForPart(state, progress, rootOpen);
    }

    private VoxelShape fullDoorShape(final BlockState state) {
        final Direction facing = state.getValue(FACING);
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;
        VoxelShape shape = Shapes.empty();

        for (int x = 0; x < WIDTH; x++) {
            final int otherLocalX = x - ROOT_X_PART;
            final int acrossOffset = otherLocalX - localX;

            for (int y = 0; y < HEIGHT; y++) {
                final double y1 = y - localY;
                shape = Shapes.or(shape, doorSlabShape(facing, acrossOffset, y1, y1 + 1.0D));
            }
        }

        return shape;
    }

    private VoxelShape collisionShapeForPart(final BlockState state) {
        return collisionShapeForPart(state, state.getValue(OPEN) ? 1.0F : 0.0F, state.getValue(OPEN));
    }

    private VoxelShape collisionShapeForPart(final BlockState state,
                                             final float progress,
                                             final boolean opening) {
        final Direction facing = state.getValue(FACING);
        final int xPart = state.getValue(X_PART);
        final int yPart = state.getValue(Y_PART);
        final double doorProgress = waterDoorProgress(Math.max(0.0D, Math.min(1.0D, progress)), opening);
        if (doorProgress <= 0.0D) return halfDepthShape(facing, 0, 0.0D, 1.0D);

        VoxelShape shape = Shapes.empty();
        if (xPart == 0 || xPart == WIDTH - 1) {
            shape = Shapes.or(shape, sideFrameShape(facing, xPart));
        }
        if (yPart == HEIGHT - 1) {
            shape = Shapes.or(shape, halfDepthShape(facing, 0, 0.25D, 1.0D));
        }

        final double angle = Math.toRadians(120.0D * doorProgress);
        final int localX = xPart - ROOT_X_PART;
        final double partMin = localX - 0.5D;
        final double partMax = localX + 0.5D;
        final boolean rightHinge = state.getValue(HINGE) == DoorHingeSide.RIGHT;
        final double pivotAcross = rightHinge ? 1.1875D : -1.1875D;
        final double signedAngle = rightHinge ? angle : -angle;
        final double sin = Math.sin(signedAngle);
        final double cos = Math.cos(signedAngle);
        final double pivotDepth = 0.25D;
        final int segments = 12;

        for (int i = 0; i < segments; i++) {
            final double a0 = -1.25D + 2.5D * i / segments;
            final double a1 = -1.25D + 2.5D * (i + 1) / segments;
            double minAcross = Double.POSITIVE_INFINITY;
            double maxAcross = Double.NEGATIVE_INFINITY;
            double minDepth = Double.POSITIVE_INFINITY;
            double maxDepth = Double.NEGATIVE_INFINITY;

            for (double a : new double[] {a0, a1}) {
                for (double d : new double[] {0.0D, 0.5D}) {
                    final double da = a - pivotAcross;
                    final double dd = d - pivotDepth;
                    final double rotatedAcross = pivotAcross + da * cos - dd * sin;
                    final double rotatedDepth = pivotDepth + da * sin + dd * cos;
                    minAcross = Math.min(minAcross, rotatedAcross);
                    maxAcross = Math.max(maxAcross, rotatedAcross);
                    minDepth = Math.min(minDepth, rotatedDepth);
                    maxDepth = Math.max(maxDepth, rotatedDepth);
                }
            }

            final double clippedMinAcross = Math.max(partMin, minAcross);
            final double clippedMaxAcross = Math.min(partMax, maxAcross);
            if (clippedMinAcross < clippedMaxAcross) {
                shape = Shapes.or(shape, orientedPanelShape(
                        state, localX, clippedMinAcross, clippedMaxAcross, minDepth, maxDepth
                ));
            }
        }
        return shape;
    }

    private static double waterDoorProgress(final double progress, final boolean opening) {
        if (opening) {
            return sinFull(Math.max(0.0D, Math.min(1.0D, (progress - 0.5D) / 0.5D)));
        }
        final double elapsed = 1.0D - progress;
        return 1.0D - sinFull(Math.max(0.0D, Math.min(1.0D, elapsed / 0.5D)));
    }

    private static double sinFull(final double value) {
        return 0.5D - 0.5D * Math.cos(Math.PI * value);
    }

    private static VoxelShape orientedPanelShape(@NotNull final BlockState state,
                                                  final int localX,
                                                  final double minAcross,
                                                  final double maxAcross,
                                                  final double minDepth,
                                                  final double maxDepth) {
        final Direction facing = state.getValue(FACING);
        final Direction across = facing.getCounterClockWise();
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (double a : new double[] {minAcross, maxAcross}) {
            for (double d : new double[] {minDepth, maxDepth}) {
                final double x = 0.5D + across.getStepX() * (a - localX) + facing.getStepX() * d;
                final double z = 0.5D + across.getStepZ() * (a - localX) + facing.getStepZ() * d;
                minX = Math.min(minX, x);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxZ = Math.max(maxZ, z);
            }
        }
        return Shapes.box(minX, 0.0D, minZ, maxX, 1.0D, maxZ);
    }

    private static VoxelShape sideFrameShape(final Direction facing, final int xPart) {
        final boolean minSide = xPart == WIDTH - 1;
        return switch (facing) {
            case NORTH -> minSide
                    ? Shapes.box(0.0D, 0.0D, 0.0D, 0.5D, 1.0D, 0.5D)
                    : Shapes.box(0.5D, 0.0D, 0.0D, 1.0D, 1.0D, 0.5D);
            case SOUTH -> minSide
                    ? Shapes.box(0.5D, 0.0D, 0.5D, 1.0D, 1.0D, 1.0D)
                    : Shapes.box(0.0D, 0.0D, 0.5D, 0.5D, 1.0D, 1.0D);
            case WEST -> minSide
                    ? Shapes.box(0.0D, 0.0D, 0.5D, 0.5D, 1.0D, 1.0D)
                    : Shapes.box(0.0D, 0.0D, 0.0D, 0.5D, 1.0D, 0.5D);
            case EAST -> minSide
                    ? Shapes.box(0.5D, 0.0D, 0.0D, 1.0D, 1.0D, 0.5D)
                    : Shapes.box(0.5D, 0.0D, 0.5D, 1.0D, 1.0D, 1.0D);
            default -> Shapes.block();
        };
    }

    private static VoxelShape doorSlabShape(final Direction facing, final int acrossOffset, final double y1, final double y2) {
        final Direction across = facing.getCounterClockWise();
        final double baseX = across.getStepX() * acrossOffset;
        final double baseZ = across.getStepZ() * acrossOffset;
        final double near = 0.0D;
        final double far = 4.0D / 16.0D;

        return switch (facing) {
            case NORTH -> Shapes.box(baseX, y1, baseZ + near, baseX + 1.0D, y2, baseZ + far);
            case SOUTH -> Shapes.box(baseX, y1, baseZ + 1.0D - far, baseX + 1.0D, y2, baseZ + 1.0D - near);
            case WEST -> Shapes.box(baseX + near, y1, baseZ, baseX + far, y2, baseZ + 1.0D);
            case EAST -> Shapes.box(baseX + 1.0D - far, y1, baseZ, baseX + 1.0D - near, y2, baseZ + 1.0D);
            default -> Shapes.block();
        };
    }

    @SuppressWarnings("SameParameterValue")
    private static VoxelShape halfDepthShape(final Direction facing, final int acrossOffset, final double y1, final double y2) {
        final Direction across = facing.getCounterClockWise();
        final double baseX = across.getStepX() * acrossOffset;
        final double baseZ = across.getStepZ() * acrossOffset;

        return switch (facing) {
            case NORTH -> Shapes.box(baseX, y1, baseZ, baseX + 1.0D, y2, baseZ + 0.5D);
            case SOUTH -> Shapes.box(baseX, y1, baseZ + 0.5D, baseX + 1.0D, y2, baseZ + 1.0D);
            case WEST -> Shapes.box(baseX, y1, baseZ, baseX + 0.5D, y2, baseZ + 1.0D);
            case EAST -> Shapes.box(baseX + 0.5D, y1, baseZ, baseX + 1.0D, y2, baseZ + 1.0D);
            default -> Shapes.block();
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull final BlockPos pos, @NotNull final BlockState state) {
        return state.getValue(PART) == HbmWaterDoorPart.ROOT ? new HbmWaterDoorBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull final Level level,
                                                                             @NotNull final BlockState state,
                                                                             @NotNull final BlockEntityType<T> type) {
        if (type != MdrBlockEntities.HBM_WATER_DOOR.get()
                || state.getValue(PART) != HbmWaterDoorPart.ROOT) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> HbmWaterDoorBlockEntity.tickClient(lvl, pos, st, (HbmWaterDoorBlockEntity) be);
        }
        return (lvl, pos, st, be) -> HbmWaterDoorBlockEntity.tickServer(lvl, pos, st, (HbmWaterDoorBlockEntity) be);
    }



    private static DoorHingeSide hingeForPlacement(final BlockPlaceContext context,
                                                    final BlockPos rootPos,
                                                    final Direction facing) {
        final Direction right = facing.getCounterClockWise();
        final double clickX = context.getClickLocation().x - (rootPos.getX() + 0.5D);
        final double clickZ = context.getClickLocation().z - (rootPos.getZ() + 0.5D);
        final double rightOffset = (clickX * right.getStepX()) + (clickZ * right.getStepZ());
        return rightOffset > 0.0D ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    private boolean canPlaceDoor(final LevelAccessor level, final BlockPos rootPos, final Direction facing, final BlockPlaceContext context) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                final BlockState existing = level.getBlockState(partPos);
                final FluidState fluid = level.getFluidState(partPos);
                if (!existing.canBeReplaced(context)
                        || (fluid.getType() != Fluids.EMPTY && fluid.getType() != Fluids.WATER)) return false;
            }
        }
        return true;
    }

    private void placeDoorParts(final Level level,
                                final BlockPos rootPos,
                                final Direction facing,
                                final DoorHingeSide hinge,
                                final boolean open,
                                final boolean powered,
                                final int skinIndex) {
        final BlockState base = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HINGE, hinge)
                .setValue(OPEN, open)
                .setValue(POWERED, powered)
                .setValue(SKIN, Math.floorMod(skinIndex, HbmWaterDoorBlockItem.SKIN_COUNT));
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final HbmWaterDoorPart part = (x == ROOT_X_PART && y == ROOT_Y_PART) ? HbmWaterDoorPart.ROOT : HbmWaterDoorPart.PART;
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                final boolean waterlogged = level.getFluidState(partPos).getType() == Fluids.WATER;
                level.setBlock(partPos, base
                        .setValue(WATERLOGGED, waterlogged)
                        .setValue(X_PART, x)
                        .setValue(Y_PART, y)
                        .setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    private void removeDoorParts(final Level level, final BlockPos rootPos, final Direction facing) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                final BlockState state = level.getBlockState(partPos);
                if (state.getBlock() == this) {
                    level.setBlock(
                            partPos,
                            state.getValue(WATERLOGGED)
                                    ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                                    : Fluids.EMPTY.defaultFluidState().createLegacyBlock(),
                            Block.UPDATE_ALL
                    );
                }
            }
        }
    }

    private void updateDoorSkin(final Level level, final BlockPos rootPos, final Direction facing, final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, HbmWaterDoorBlockItem.SKIN_COUNT);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                BlockState state = level.getBlockState(partPos);
                if (state.getBlock() != this || !state.hasProperty(SKIN)) continue;
                level.setBlock(partPos, state.setValue(SKIN, normalisedSkin), Block.UPDATE_ALL);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canManualToggle(@NotNull final Level level,
                                    @NotNull final BlockPos rootPos) {
        if (level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door) {
            if (door.getAccessLevel() != DoorAccessLevel.DEFAULT) return true;
            return door.getRedstoneMode() != DoorRedstoneMode.REDSTONE_ONLY;
        }
        return true;
    }

    private void applyRedstoneChange(@NotNull final Level level,
                                     @NotNull final BlockPos rootPos,
                                     final boolean powered,
                                     @NotNull final DoorRedstoneMode mode) {
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
        if (level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door) {
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
        return level.getBlockEntity(rootPos) instanceof final HbmWaterDoorBlockEntity door && door.isMoving();
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

    public enum HbmWaterDoorPart implements StringRepresentable {
        ROOT("root"),
        PART("part");

        private final String name;

        HbmWaterDoorPart(final String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
