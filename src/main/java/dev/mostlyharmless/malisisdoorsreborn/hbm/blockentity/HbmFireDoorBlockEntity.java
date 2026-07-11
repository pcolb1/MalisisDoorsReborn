package dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity;

import dev.mostlyharmless.malisisdoorsreborn.access.AccessControlledDoor;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorHelper;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmFireDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.network.MdrNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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

public class HbmFireDoorBlockEntity extends BlockEntity implements AccessControlledDoor {

    private static final int OPENING_TIME = 160;

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
    private boolean skinReconciled = false;

    public HbmFireDoorBlockEntity(final BlockPos pos, final BlockState state) {
        super(MdrBlockEntities.HBM_FIRE_DOOR.get(), pos, state);
        snapProgressToState(state);
    }

    @SuppressWarnings("unused")
    public static void tickClient(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmFireDoorBlockEntity be) {
        be.tickProgress(level, pos, state);
    }

    public static void tickServer(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmFireDoorBlockEntity be) {
        be.reconcileSkinOnce(level, pos, state);
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
                if (!level.isClientSide() && state.getBlock() instanceof final HbmFireDoorBlock fireDoor) {
                    fireDoor.setWholeOpen(level, pos, state, true);
                    if (level instanceof final ServerLevel serverLevel) {
                        MdrNetwork.sendHbmFireDoorSound(serverLevel, pos, false);
                    }
                }
            }
            markForSync(level, pos, state);
        } else if (phase == DoorPhase.CLOSING) {
            ticks = Math.max(0, ticks - 1);
            if (ticks == 0) {
                phase = DoorPhase.CLOSED;
                if (!level.isClientSide() && state.getBlock() instanceof final HbmFireDoorBlock fireDoor) {
                    fireDoor.setWholeOpen(level, pos, state, false);
                    if (level instanceof final ServerLevel serverLevel) {
                        MdrNetwork.sendHbmFireDoorSound(serverLevel, pos, false);
                    }
                }
            }
            markForSync(level, pos, state);
        }
    }

    public void setOpen(final boolean open) {
        final Level level = getLevel();
        if (level == null) return;
        final BlockState state = getBlockState();
        if (!(state.getBlock() instanceof HbmFireDoorBlock)) return;

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

        if (!level.isClientSide() && level instanceof final ServerLevel serverLevel) {
            MdrNetwork.sendHbmFireDoorSound(serverLevel, worldPosition, true);
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
        skinIndex = Math.floorMod(input.getIntOr("SkinIndex", 0), 5);
        redstoneMode = HbmDoorRedstoneMode.fromOrdinal(input.getIntOr("RedstoneMode", 0));
        accessLevel = DoorAccessLevel.fromOrdinal(input.getIntOr("AccessLevel", 0));
        skinReconciled = false;
    }


    public int getSkinIndex() {
        return skinIndex;
    }

    public void setSkinIndex(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, 5);
        skinReconciled = true;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setSkinIndexNoBlockUpdate(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, 5);
        skinReconciled = true;
        setChanged();
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

    public int cycleSkin() {
        skinIndex = (skinIndex + 1) % 5;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return skinIndex;
    }


    private void reconcileSkinOnce(@NotNull final Level level,
                                   @NotNull final BlockPos pos,
                                   @NotNull final BlockState state) {
        if (skinReconciled || level.isClientSide()) return;
        if (!(state.getBlock() instanceof HbmFireDoorBlock)) {
            skinReconciled = true;
            return;
        }

        CustomSkinnedDoorHelper.reconcile(level, pos, state, this);
        skinReconciled = true;
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