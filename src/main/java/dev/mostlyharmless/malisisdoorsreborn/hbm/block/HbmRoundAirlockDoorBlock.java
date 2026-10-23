package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import dev.mostlyharmless.malisisdoorsreborn.common.DoorRedstoneMode;

import dev.mostlyharmless.malisisdoorsreborn.block.CustomDoorBreakTarget;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.block.AccessCardReaderCardLevelMode;
import dev.mostlyharmless.malisisdoorsreborn.access.AccessCardDoorTarget;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmRoundAirlockDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmRoundAirlockDoorBlockItem;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
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

public class HbmRoundAirlockDoorBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, CustomDoorBreakTarget, CustomSkinnedDoorTarget, HbmScrewdriverDoorTarget, AccessCardDoorTarget {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty X_PART = IntegerProperty.create("x", 0, 4);
    public static final IntegerProperty Y_PART = IntegerProperty.create("y", 0, 4);
    public static final EnumProperty<HbmRoundAirlockDoorPart> PART = EnumProperty.create("part", HbmRoundAirlockDoorPart.class);
    public static final IntegerProperty SKIN = IntegerProperty.create("skin", 0, 2);

    private static final int ROOT_X_PART = 2;
    private static final int ROOT_Y_PART = 0;
    private static final int MIN_X_PART = 1;
    private static final int WIDTH = 4;
    private static final int HEIGHT = 4;
    private static final int STATE_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public HbmRoundAirlockDoorBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false)
                .setValue(X_PART, ROOT_X_PART)
                .setValue(Y_PART, ROOT_Y_PART)
                .setValue(PART, HbmRoundAirlockDoorPart.ROOT)
                .setValue(SKIN, 0));
    }


    @Override
    public boolean isCustomDoorBreakRoot(@NotNull final BlockState rootState,
                                         final BlockEntity blockEntity) {
        return rootState.getBlock() == this
                && rootState.hasProperty(PART)
                && rootState.getValue(PART) == HbmRoundAirlockDoorPart.ROOT
                && blockEntity instanceof HbmRoundAirlockDoorBlockEntity;
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
        final Direction across = facing.getCounterClockWise();
        final int step = across.getAxis() == Direction.Axis.X ? across.getStepX() : across.getStepZ();
        final double minAcross = step > 0 ? -1.0D : -2.0D;
        final double maxAcross = step > 0 ? 3.0D : 2.0D;
        return facing.getAxis() == Direction.Axis.Z
                ? new AABB(minAcross, 0.0D, 5.0D / 16.0D, maxAcross, 4.0D, 11.0D / 16.0D)
                : new AABB(5.0D / 16.0D, 0.0D, minAcross, 11.0D / 16.0D, 4.0D, maxAcross);
    }

    @Override
    public BlockState customDoorBreakParticleState(@NotNull final Level level,
                                                   @NotNull final BlockPos pos,
                                                   @NotNull final BlockState state) {
        final BlockPos rootPos = rootPos(pos, state);
        return particleStateForSkinIndex(skinIndexAt(level, rootPos));
    }


    private static BlockState particleStateForSkinIndex(final int skinIndex) {
        return switch (Math.floorMod(skinIndex, HbmRoundAirlockDoorBlockItem.SKIN_COUNT)) {
            case 1 -> MdrBlocks.HBM_ROUND_AIRLOCK_DOOR_PARTICLE_CLEAN.get().defaultBlockState();
            case 2 -> MdrBlocks.HBM_ROUND_AIRLOCK_DOOR_PARTICLE_GREEN.get().defaultBlockState();
            default -> MdrBlocks.HBM_ROUND_AIRLOCK_DOOR_PARTICLE_DEFAULT.get().defaultBlockState();
        };
    }

    private int skinIndexAt(@NotNull final BlockGetter level, @NotNull final BlockPos rootPos) {
        final BlockState rootState = level.getBlockState(rootPos);
        final int entitySkin = level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door ? door.getSkinIndex() : 0;
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
        return HbmRoundAirlockDoorBlockItem.SKIN_COUNT;
    }

    @Override
    public @NotNull Iterable<BlockPos> customDoorSkinPartPositions(@NotNull final BlockPos rootPos,
                                                                   @NotNull final BlockState rootState) {
        final List<BlockPos> positions = new ArrayList<>(WIDTH * HEIGHT);
        final Direction facing = rootState.getValue(FACING);
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                positions.add(partPos(rootPos, facing, x, y));
            }
        }
        return positions;
    }

    @Override
    public int customDoorEntitySkin(@NotNull final BlockEntity blockEntity) {
        return blockEntity instanceof final HbmRoundAirlockDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
    }

    @Override
    public void customDoorSetEntitySkin(@NotNull final BlockEntity blockEntity, final int skinIndex) {
        if (blockEntity instanceof final HbmRoundAirlockDoorBlockEntity door) {
            door.setSkinIndexNoBlockUpdate(skinIndex);
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, WATERLOGGED, X_PART, Y_PART, PART, SKIN);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull final BlockState state) {
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final Direction outward;
        if (localX == MIN_X_PART - ROOT_X_PART) {
            outward = state.getValue(FACING).getClockWise();
        } else if (localX == MIN_X_PART + WIDTH - ROOT_X_PART - 1) {
            outward = state.getValue(FACING).getCounterClockWise();
        } else {
            return Shapes.empty();
        }

        return switch (outward) {
            case NORTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
            case SOUTH -> Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
            case WEST -> Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
            case EAST -> Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            default -> Shapes.empty();
        };
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
                .setValue(SKIN, HbmRoundAirlockDoorBlockItem.skinIndexFromStack(context.getItemInHand()));
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
            final int skinIndex = HbmRoundAirlockDoorBlockItem.skinIndexFromStack(stack);
            final DoorRedstoneMode redstoneMode = HbmRoundAirlockDoorBlockItem.redstoneModeFromStack(stack);
            final DoorAccessLevel accessLevel = HbmRoundAirlockDoorBlockItem.accessLevelFromStack(stack);
            final AccessCardReaderCardLevelMode cardLevelMode = DoorAccessHelper.cardLevelModeFromStack(stack);
            placeDoorParts(level, pos, state.getValue(FACING), state.getValue(OPEN), state.getValue(POWERED), skinIndex);
            if (level.getBlockEntity(pos) instanceof final HbmRoundAirlockDoorBlockEntity door) {
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
        if (!(root.getBlock() instanceof final HbmRoundAirlockDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity accessDoor
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
        if (!(root.getBlock() instanceof final HbmRoundAirlockDoorBlock doorBlock)) return InteractionResult.PASS;
        if (isDoorMoving(level, rootPos)) return InteractionResult.CONSUME;

        if (level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity accessDoor) {
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

        if (level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door) {
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

        if (level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door) {
            final int skinIndex = door.cycleSkin();
            updateDoorSkin(level, rootPos, root.getValue(FACING), skinIndex);
            player.displayClientMessage(Component.translatable("tooltip.malisisdoorsreborn.label.skin", HbmRoundAirlockDoorBlockItem.skinName(skinIndex)), true);
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

        if (level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door) {
            final DoorRedstoneMode mode = door.cycleRedstoneMode();
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
        if (!(root.getBlock() instanceof final HbmRoundAirlockDoorBlock doorBlock)) return;

        final boolean powered = doorBlock.hasDoorSignal(level, rootPos, root.getValue(FACING));
        final boolean wasPowered = root.getValue(POWERED);
        if (powered == wasPowered) return;

        if (!(level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door) || door.isMoving()) return;
        if (door.getAccessLevel() != DoorAccessLevel.DEFAULT) return;

        doorBlock.setDoorPowered(level, rootPos, powered);
        doorBlock.applyRedstoneChange(level, rootPos, powered, door.getRedstoneMode());
    }

    @Override
    protected void affectNeighborsAfterRemoval(@NotNull final BlockState state,
                                               @NotNull final ServerLevel level,
                                               @NotNull final BlockPos pos,
                                               final boolean movedByPiston) {
        if (isRetiredLegacyPart(state)) {
            super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
            return;
        }
        removeDoorParts(level, rootPos(pos, state), state.getValue(FACING));
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            if (isRetiredLegacyPart(state)) {
                return super.playerWillDestroy(level, pos, state, player);
            }
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
        final DoorRedstoneMode redstoneMode = level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door ? door.getRedstoneMode() : DoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door ? door.getAccessLevel() : DoorAccessLevel.DEFAULT;
        final AccessCardReaderCardLevelMode cardLevelMode = level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door ? door.getCardLevelMode() : AccessCardReaderCardLevelMode.defaultValue();
        return HbmRoundAirlockDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_ROUND_AIRLOCK_DOOR.get(), skinIndexAt(level, rootPos), redstoneMode, accessLevel, cardLevelMode);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull final BlockState state,
                                             @NotNull final LootParams.Builder builder) {
        if (state.getValue(PART) != HbmRoundAirlockDoorPart.ROOT) return List.of();
        final BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        final int entitySkin = blockEntity instanceof final HbmRoundAirlockDoorBlockEntity doorEntity ? doorEntity.getSkinIndex() : 0;
        final int stateSkin = state.hasProperty(SKIN) ? state.getValue(SKIN) : 0;
        final int skinIndex = stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
        final DoorRedstoneMode redstoneMode = blockEntity instanceof final HbmRoundAirlockDoorBlockEntity doorEntity ? doorEntity.getRedstoneMode() : DoorRedstoneMode.DEFAULT;
        final DoorAccessLevel accessLevel = blockEntity instanceof final HbmRoundAirlockDoorBlockEntity doorEntity ? doorEntity.getAccessLevel() : DoorAccessLevel.DEFAULT;
        final AccessCardReaderCardLevelMode cardLevelMode = blockEntity instanceof final HbmRoundAirlockDoorBlockEntity doorEntity ? doorEntity.getCardLevelMode() : AccessCardReaderCardLevelMode.defaultValue();
        return List.of(HbmRoundAirlockDoorBlockItem.stackWithSkinRedstoneAccess(MdrItems.HBM_ROUND_AIRLOCK_DOOR.get(), skinIndex, redstoneMode, accessLevel, cardLevelMode));
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

        final float progress = level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door
                ? door.getProgress(1.0F)
                : rootOpen ? 1.0F : 0.0F;
        return collisionShapeForPart(state, progress);
    }

    private VoxelShape fullDoorShape(final BlockState state) {
        final Direction across = state.getValue(FACING).getCounterClockWise();
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;
        VoxelShape shape = Shapes.empty();

        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
            final int otherLocalX = x - ROOT_X_PART;
            final int acrossOffset = otherLocalX - localX;
            final double[] bounds = acrossBounds(state, otherLocalX);

            final double baseX = across.getStepX() * acrossOffset;
            final double baseZ = across.getStepZ() * acrossOffset;
            final double minAcross = bounds[0] / 16.0D;
            final double maxAcross = bounds[1] / 16.0D;

            for (int y = 0; y < 4; y++) {
                final double y1 = y - localY;
                if (state.getValue(FACING).getAxis() == Direction.Axis.Z) {
                    shape = Shapes.or(shape, Shapes.box(
                            baseX + minAcross, y1, baseZ,
                            baseX + maxAcross, y1 + 1.0D, baseZ + 1.0D
                    ));
                } else {
                    shape = Shapes.or(shape, Shapes.box(
                            baseX, y1, baseZ + minAcross,
                            baseX + 1.0D, y1 + 1.0D, baseZ + maxAcross
                    ));
                }
            }
        }

        return shape;
    }

    private VoxelShape collisionShapeForPart(final BlockState state) {
        return collisionShapeForPart(state, state.getValue(OPEN) ? 1.0F : 0.0F);
    }

    private VoxelShape collisionShapeForPart(final BlockState state, final float progress) {
        final int localX = state.getValue(X_PART) - ROOT_X_PART;
        final int localY = state.getValue(Y_PART) - ROOT_Y_PART;
        if (localY < 0 || localY >= HEIGHT) return Shapes.empty();

        VoxelShape shape = Shapes.empty();
        if (localX == MIN_X_PART - ROOT_X_PART
                || localX == MIN_X_PART + WIDTH - ROOT_X_PART - 1) {
            final double[] bounds = acrossBounds(state, localX);
            shape = fullDepthShape(state, bounds[0], bounds[1], 0.0D, 16.0D);
        } else if (localY == HEIGHT - 1) {
            shape = fullDepthShape(state, 0.0D, 16.0D, 8.0D, 16.0D);
        }

        final double slide = Math.max(0.0D, Math.min(1.0D, progress)) * 1.5D;
        final double gapMin = 0.625D - slide;
        final double gapMax = 0.375D + slide;
        if (gapMin >= gapMax) return Shapes.or(shape, Shapes.block());

        final double partMin = localX - 0.5D;
        final double partMax = localX + 0.5D;
        final double leftMax = Math.min(partMax, gapMin);
        if (leftMax > partMin) {
            shape = Shapes.or(shape, acrossSegmentShape(state, partMin, partMin, leftMax));
        }
        final double rightMin = Math.max(partMin, gapMax);
        if (rightMin < partMax) {
            shape = Shapes.or(shape, acrossSegmentShape(state, partMin, rightMin, partMax));
        }
        return shape;
    }

    private static VoxelShape acrossSegmentShape(@NotNull final BlockState state,
                                                 final double partMin,
                                                 final double minAcross,
                                                 final double maxAcross) {
        final double min = Math.max(0.0D, Math.min(1.0D, minAcross - partMin));
        final double max = Math.max(0.0D, Math.min(1.0D, maxAcross - partMin));
        final Direction across = state.getValue(FACING).getCounterClockWise();
        return switch (across) {
            case EAST -> Shapes.box(min, 0.0D, 0.0D, max, 1.0D, 1.0D);
            case WEST -> Shapes.box(1.0D - max, 0.0D, 0.0D, 1.0D - min, 1.0D, 1.0D);
            case SOUTH -> Shapes.box(0.0D, 0.0D, min, 1.0D, 1.0D, max);
            case NORTH -> Shapes.box(0.0D, 0.0D, 1.0D - max, 1.0D, 1.0D, 1.0D - min);
            default -> Shapes.empty();
        };
    }

    @SuppressWarnings("unused")
    private static double[] acrossBounds(@NotNull final BlockState state,
                                         final int localX) {
        return new double[] {0.0D, 16.0D};
    }

    @SuppressWarnings("SameParameterValue")
    private static VoxelShape fullDepthShape(@NotNull final BlockState state,
                                             final double minAcross,
                                             final double maxAcross,
                                             final double minY,
                                             final double maxY) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z
                ? Block.box(minAcross, minY, 0.0D, maxAcross, maxY, 16.0D)
                : Block.box(0.0D, minY, minAcross, 16.0D, maxY, maxAcross);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull final BlockPos pos, @NotNull final BlockState state) {
        return state.getValue(PART) == HbmRoundAirlockDoorPart.ROOT ? new HbmRoundAirlockDoorBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull final Level level,
                                                                             @NotNull final BlockState state,
                                                                             @NotNull final BlockEntityType<T> type) {
        if (type != MdrBlockEntities.HBM_ROUND_AIRLOCK_DOOR.get()
                || state.getValue(PART) != HbmRoundAirlockDoorPart.ROOT) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> HbmRoundAirlockDoorBlockEntity.tickClient(lvl, pos, st, (HbmRoundAirlockDoorBlockEntity) be);
        }
        return (lvl, pos, st, be) -> HbmRoundAirlockDoorBlockEntity.tickServer(lvl, pos, st, (HbmRoundAirlockDoorBlockEntity) be);
    }


    private boolean canPlaceDoor(final LevelAccessor level, final BlockPos rootPos, final Direction facing, final BlockPlaceContext context) {
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
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

    private void placeDoorParts(final Level level, final BlockPos rootPos, final Direction facing, final boolean open, final boolean powered, final int skinIndex) {
        final BlockState base = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(OPEN, open)
                .setValue(POWERED, powered)
                .setValue(SKIN, Math.floorMod(skinIndex, HbmRoundAirlockDoorBlockItem.SKIN_COUNT));
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final HbmRoundAirlockDoorPart part = (x == ROOT_X_PART && y == ROOT_Y_PART) ? HbmRoundAirlockDoorPart.ROOT : HbmRoundAirlockDoorPart.PART;
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
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
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
        final int normalisedSkin = Math.floorMod(skinIndex, HbmRoundAirlockDoorBlockItem.SKIN_COUNT);
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                final BlockPos partPos = partPos(rootPos, facing, x, y);
                BlockState state = level.getBlockState(partPos);
                if (state.getBlock() != this || !state.hasProperty(SKIN)) continue;
                level.setBlock(partPos, state.setValue(SKIN, normalisedSkin), STATE_UPDATE_FLAGS);
            }
        }
    }

    public void reconcileLegacyFootprint(@NotNull final Level level,
                                         @NotNull final BlockPos rootPos,
                                         @NotNull final BlockState rootState) {
        if (level.isClientSide() || rootState.getBlock() != this || rootState.getValue(PART) != HbmRoundAirlockDoorPart.ROOT) return;

        final Direction facing = rootState.getValue(FACING);

        for (int y = 0; y <= 4; y++) {
            removeLegacyPart(level, rootPos, partPos(rootPos, facing, 0, y), 0, y);
        }
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
            removeLegacyPart(level, rootPos, partPos(rootPos, facing, x, 4), x, 4);
        }
    }

    private void removeLegacyPart(@NotNull final Level level,
                                  @NotNull final BlockPos rootPos,
                                  @NotNull final BlockPos legacyPos,
                                  final int expectedX,
                                  final int expectedY) {
        final BlockState legacyState = level.getBlockState(legacyPos);
        if (legacyState.getBlock() != this
                || !legacyState.hasProperty(X_PART)
                || !legacyState.hasProperty(Y_PART)
                || legacyState.getValue(X_PART) != expectedX
                || legacyState.getValue(Y_PART) != expectedY
                || !rootPos(legacyPos, legacyState).equals(rootPos)) {
            return;
        }
        level.setBlock(
                legacyPos,
                legacyState.getValue(WATERLOGGED)
                        ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                        : Fluids.EMPTY.defaultFluidState().createLegacyBlock(),
                Block.UPDATE_ALL
        );
    }

    private static boolean isRetiredLegacyPart(@NotNull final BlockState state) {
        return state.hasProperty(X_PART)
                && state.hasProperty(Y_PART)
                && (state.getValue(X_PART) < MIN_X_PART || state.getValue(Y_PART) >= HEIGHT);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canManualToggle(@NotNull final Level level,
                                    @NotNull final BlockPos rootPos) {
        if (level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door) {
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
        if (level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door) {
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
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
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
        return level.getBlockEntity(rootPos) instanceof final HbmRoundAirlockDoorBlockEntity door && door.isMoving();
    }

    private boolean hasDoorSignal(final Level level, final BlockPos rootPos, final Direction facing) {
        for (int x = MIN_X_PART; x < MIN_X_PART + WIDTH; x++) {
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

    public enum HbmRoundAirlockDoorPart implements StringRepresentable {
        ROOT("root"),
        PART("part");

        private final String name;

        HbmRoundAirlockDoorPart(final String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
