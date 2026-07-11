package dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity;

import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorHelper;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.access.AccessControlledDoor;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmQeContainmentDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmQeContainmentDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.network.MdrNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HbmQeContainmentDoorBlockEntity extends BlockEntity implements AccessControlledDoor {

    private static final int OPENING_TIME = 160;

    private float progressPrev = 0.0F;
    private float progress = 0.0F;
    private int skinIndex = 0;
    private HbmDoorRedstoneMode redstoneMode = HbmDoorRedstoneMode.DEFAULT;
    private DoorAccessLevel accessLevel = DoorAccessLevel.DEFAULT;
    private boolean skinReconciled = false;

    public HbmQeContainmentDoorBlockEntity(final BlockPos pos, final BlockState state) {
        super(MdrBlockEntities.HBM_QE_CONTAINMENT_DOOR.get(), pos, state);
        snapProgressToState(state);
    }

    @SuppressWarnings("unused")
    public static void tickClient(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmQeContainmentDoorBlockEntity be) {
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        be.progress = Mth.approach(be.progress, open ? 1.0F : 0.0F, 1.0F / OPENING_TIME);
    }

    public static void tickServer(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmQeContainmentDoorBlockEntity be) {
        be.reconcileSkinOnce(level, pos, state);
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        final float target = open ? 1.0F : 0.0F;
        final float previous = be.progress;
        be.progress = Mth.approach(be.progress, target, 1.0F / OPENING_TIME);
        if (previous != target && be.progress == target) {
            if (level instanceof final ServerLevel serverLevel) {
                MdrNetwork.sendHbmQeContainmentDoorSound(serverLevel, pos, false);
            }
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
        skinIndex = Math.floorMod(tag.getInt("SkinIndex"), HbmQeContainmentDoorBlockItem.SKIN_COUNT);
        redstoneMode = tag.contains("RedstoneMode") ? HbmDoorRedstoneMode.fromOrdinal(tag.getInt("RedstoneMode")) : HbmDoorRedstoneMode.DEFAULT;
        accessLevel = tag.contains("AccessLevel") ? DoorAccessLevel.fromOrdinal(tag.getInt("AccessLevel")) : DoorAccessLevel.DEFAULT;
        skinReconciled = false;
    }


    public int getSkinIndex() {
        return skinIndex;
    }

    public void setSkinIndex(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, HbmQeContainmentDoorBlockItem.SKIN_COUNT);
        skinReconciled = true;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setSkinIndexNoBlockUpdate(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, HbmQeContainmentDoorBlockItem.SKIN_COUNT);
        skinReconciled = true;
        setChanged();
    }

    public int cycleSkin() {
        skinIndex = (skinIndex + 1) % HbmQeContainmentDoorBlockItem.SKIN_COUNT;
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


    private void reconcileSkinOnce(@NotNull final Level level,
                                   @NotNull final BlockPos pos,
                                   @NotNull final BlockState state) {
        if (skinReconciled || level.isClientSide) return;
        if (!(state.getBlock() instanceof HbmQeContainmentDoorBlock)) {
            skinReconciled = true;
            return;
        }

        CustomSkinnedDoorHelper.reconcile(level, pos, state, this);
        skinReconciled = true;
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
    protected void saveAdditional(@NotNull final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SkinIndex", skinIndex);
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
        tag.putInt("AccessLevel", accessLevel.ordinal());
    }

    @Override
    public void load(@NotNull final CompoundTag tag) {
        super.load(tag);
        loadShared(tag);
        snapProgressToState(getBlockState());
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket pkt) {
        final CompoundTag tag = pkt.getTag();
        if (tag != null) loadShared(tag);
    }

    @Override
    public @NotNull AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-4, 0, -4), worldPosition.offset(4, 5, 4));
    }
}
