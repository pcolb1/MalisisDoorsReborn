package dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity;

import dev.mostlyharmless.malisisdoorsreborn.access.AccessControlledDoor;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmSlidingSealDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmSlidingSealDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HbmSlidingSealDoorBlockEntity extends BlockEntity implements AccessControlledDoor {

    private static final int OPENING_TIME = 20;

    public enum DoorPhase {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING
    }

    private DoorPhase phase = DoorPhase.CLOSED;
    private int ticks = 0;
    private int previousTicks = 0;
    private int skinIndex = 0;
    private HbmDoorRedstoneMode redstoneMode = HbmDoorRedstoneMode.DEFAULT;
    private DoorAccessLevel accessLevel = DoorAccessLevel.DEFAULT;

    public HbmSlidingSealDoorBlockEntity(final BlockPos pos, final BlockState state) {
        super(MdrBlockEntities.HBM_SLIDING_SEAL_DOOR.get(), pos, state);
        snapProgressToState(state);
    }

    @SuppressWarnings("unused")
    public static void tickClient(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmSlidingSealDoorBlockEntity be) {
        be.tickProgress(level, pos, state);
    }

    public static void tickServer(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmSlidingSealDoorBlockEntity be) {
        be.tickProgress(level, pos, state);
    }

    private void tickProgress(@NotNull final Level level,
                              @NotNull final BlockPos pos,
                              @NotNull final BlockState state) {
        previousTicks = ticks;
        if (phase == DoorPhase.OPENING) {
            ticks = Math.min(OPENING_TIME, ticks + 1);
            if (ticks == OPENING_TIME) {
                phase = DoorPhase.OPENED;
                if (!level.isClientSide() && state.getBlock() instanceof final HbmSlidingSealDoorBlock sealDoor) {
                    sealDoor.setWholeOpen(level, pos, state, true);
                    sealDoor.playStopSound(level, pos);
                }
            }
            markForSync(level, pos, state);
        } else if (phase == DoorPhase.CLOSING) {
            ticks = Math.max(0, ticks - 1);
            if (ticks == 0) {
                phase = DoorPhase.CLOSED;
                if (!level.isClientSide() && state.getBlock() instanceof final HbmSlidingSealDoorBlock sealDoor) {
                    sealDoor.setWholeOpen(level, pos, state, false);
                    sealDoor.playStopSound(level, pos);
                }
            }
            markForSync(level, pos, state);
        }
    }

    public void setOpen(final boolean open) {
        final Level level = getLevel();
        if (level == null) return;
        final BlockState state = getBlockState();
        if (!(state.getBlock() instanceof HbmSlidingSealDoorBlock)) return;

        if (open && phase == DoorPhase.CLOSED) {
            ticks = 0;
            previousTicks = 0;
            phase = DoorPhase.OPENING;
        } else if (!open && phase == DoorPhase.OPENED) {
            ticks = OPENING_TIME;
            previousTicks = OPENING_TIME;
            phase = DoorPhase.CLOSING;
        } else {
            return;
        }

        markForSync(level, worldPosition, state);
    }

    public boolean isMoving() {
        return phase == DoorPhase.OPENING || phase == DoorPhase.CLOSING;
    }

    public float getProgress(final float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, previousTicks, ticks) / (float) OPENING_TIME, 0.0F, 1.0F);
    }

    private void snapProgressToState(@NotNull final BlockState state) {
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        phase = open ? DoorPhase.OPENED : DoorPhase.CLOSED;
        ticks = open ? OPENING_TIME : 0;
        previousTicks = ticks;
    }

    private void markForSync(@NotNull final Level level,
                             @NotNull final BlockPos pos,
                             @NotNull final BlockState state) {
        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void loadShared(@NotNull final ValueInput input) {
        skinIndex = Math.floorMod(input.getIntOr("SkinIndex", 0), HbmSlidingSealDoorBlockItem.SKIN_COUNT);
        redstoneMode = HbmDoorRedstoneMode.fromOrdinal(input.getIntOr("RedstoneMode", 0));
        accessLevel = DoorAccessLevel.fromOrdinal(input.getIntOr("AccessLevel", 0));
    }

    public int getSkinIndex() {
        return skinIndex;
    }

    public void setSkinIndex(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, HbmSlidingSealDoorBlockItem.SKIN_COUNT);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public int cycleSkin() {
        skinIndex = (skinIndex + 1) % HbmSlidingSealDoorBlockItem.SKIN_COUNT;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return skinIndex;
    }

    public HbmDoorRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(@NotNull final HbmDoorRedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public HbmDoorRedstoneMode cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return redstoneMode;
    }

    @Override
    public @NotNull DoorAccessLevel getAccessLevel() {
        return accessLevel;
    }

    @Override
    public void setAccessLevel(@NotNull final DoorAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public @NotNull DoorAccessLevel cycleAccessLevel() {
        accessLevel = accessLevel.next();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return accessLevel;
    }

    @Override
    protected void saveAdditional(@NotNull final ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SkinIndex", skinIndex);
        output.putInt("RedstoneMode", redstoneMode.ordinal());
        output.putInt("AccessLevel", accessLevel.ordinal());
        output.putInt("Phase", phase.ordinal());
        output.putInt("Ticks", ticks);
    }

    @Override
    protected void loadAdditional(@NotNull final ValueInput input) {
        super.loadAdditional(input);
        loadShared(input);
        loadAnimation(input);
    }

    private void loadAnimation(@NotNull final ValueInput input) {
        final int phaseId = Mth.clamp(input.getIntOr("Phase", phaseFromState(getBlockState()).ordinal()), 0, DoorPhase.values().length - 1);
        phase = DoorPhase.values()[phaseId];
        ticks = Mth.clamp(input.getIntOr("Ticks", ticksForPhase(phase)), 0, OPENING_TIME);
        previousTicks = ticks;
    }

    private static DoorPhase phaseFromState(@NotNull final BlockState state) {
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        return open ? DoorPhase.OPENED : DoorPhase.CLOSED;
    }

    private static int ticksForPhase(@NotNull final DoorPhase phase) {
        return switch (phase) {
            case OPENING, CLOSED -> 0;
            case OPENED, CLOSING -> OPENING_TIME;
        };
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull final HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void handleUpdateTag(@NotNull final ValueInput input) {
        loadShared(input);
        loadAnimation(input);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
