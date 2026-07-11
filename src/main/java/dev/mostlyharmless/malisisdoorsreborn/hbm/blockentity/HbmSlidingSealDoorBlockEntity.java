package dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity;

import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.access.AccessControlledDoor;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmSlidingSealDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmSlidingSealDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HbmSlidingSealDoorBlockEntity extends BlockEntity implements AccessControlledDoor {

    private static final int OPENING_TIME = 20;

    private float progressPrev = 0.0F;
    private float progress = 0.0F;
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
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        be.progress = Mth.approach(be.progress, open ? 1.0F : 0.0F, 1.0F / OPENING_TIME);
    }

    public static void tickServer(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmSlidingSealDoorBlockEntity be) {
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        final float target = open ? 1.0F : 0.0F;
        final float previous = be.progress;
        be.progress = Mth.approach(be.progress, target, 1.0F / OPENING_TIME);
        if (previous != target && be.progress == target && state.getBlock() instanceof final HbmSlidingSealDoorBlock doorBlock) {
            doorBlock.playStopSound(level, pos);
        }
    }

    public boolean isMoving(final boolean open) {
        return progress != (open ? 1.0F : 0.0F);
    }

    public float getProgress(final float partialTick) {
        return Mth.lerp(partialTick, progressPrev, progress);
    }

    private void snapProgressToState(@NotNull final BlockState state) {
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        progress = open ? 1.0F : 0.0F;
        progressPrev = progress;
    }

    private void loadShared(@NotNull final CompoundTag tag) {
        skinIndex = Math.floorMod(tag.getInt("SkinIndex"), HbmSlidingSealDoorBlockItem.SKIN_COUNT);
        redstoneMode = tag.contains("RedstoneMode") ? HbmDoorRedstoneMode.fromOrdinal(tag.getInt("RedstoneMode")) : HbmDoorRedstoneMode.DEFAULT;
        accessLevel = tag.contains("AccessLevel") ? DoorAccessLevel.fromOrdinal(tag.getInt("AccessLevel")) : DoorAccessLevel.DEFAULT;
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

    public void setRedstoneMode(final HbmDoorRedstoneMode redstoneMode) {
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
    protected void saveAdditional(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SkinIndex", skinIndex);
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
        tag.putInt("AccessLevel", accessLevel.ordinal());
    }

    @Override
    protected void loadAdditional(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadShared(tag);
        snapProgressToState(getBlockState());
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull final CompoundTag tag,
                                @NotNull final HolderLookup.Provider registries) {
        loadShared(tag);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@NotNull final Connection net,
                             @NotNull final ClientboundBlockEntityDataPacket pkt,
                             @NotNull final HolderLookup.Provider registries) {
        loadShared(pkt.getTag());
    }
}
