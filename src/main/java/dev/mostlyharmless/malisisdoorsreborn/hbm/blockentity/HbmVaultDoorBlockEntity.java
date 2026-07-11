package dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity;

import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorHelper;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.access.AccessControlledDoor;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmVaultDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.item.HbmVaultDoorBlockItem;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HbmVaultDoorBlockEntity extends BlockEntity implements AccessControlledDoor {

    private static final int OPENING_TIME = 120;

    private float progressPrev = 0.0F;
    private float progress = 0.0F;
    private int skinIndex = 0;
    private HbmDoorRedstoneMode redstoneMode = HbmDoorRedstoneMode.DEFAULT;
    private DoorAccessLevel accessLevel = DoorAccessLevel.DEFAULT;
    private boolean skinReconciled = false;

    public HbmVaultDoorBlockEntity(final BlockPos pos, final BlockState state) {
        super(MdrBlockEntities.HBM_VAULT_DOOR.get(), pos, state);
        snapProgressToState(state);
    }

    @SuppressWarnings("unused")
    public static void tickClient(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmVaultDoorBlockEntity be) {
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        be.progress = Mth.approach(be.progress, open ? 1.0F : 0.0F, 1.0F / OPENING_TIME);
    }

    public static void tickServer(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmVaultDoorBlockEntity be) {
        be.reconcileSkinOnce(level, pos, state);
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        final float target = open ? 1.0F : 0.0F;
        final float previous = be.progress;
        be.progress = Mth.approach(be.progress, target, 1.0F / OPENING_TIME);
        if (previous != target) {
            be.playScheduledSounds(level, pos, open, previous, be.progress);
        }
    }


    private void playScheduledSounds(@NotNull final Level level,
                                     @NotNull final BlockPos pos,
                                     final boolean opening,
                                     final float previousProgress,
                                     final float currentProgress) {
        final int previousOpenTick = openTick(previousProgress);
        final int currentOpenTick = openTick(currentProgress);
        for (int tick = 45; tick <= 115; tick += 10) {
            final boolean crossed = opening
                    ? crossesUp(previousOpenTick, currentOpenTick, tick)
                    : crossesDown(previousOpenTick, currentOpenTick, tick);
            if (crossed) {
                level.playSound(null, pos, MdrSounds.HBM_VAULT_DOOR_STOP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        if (!opening && crossesDown(previousOpenTick, currentOpenTick, 30)) {
            level.playSound(null, pos, MdrSounds.HBM_VAULT_DOOR_MOVE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static boolean crossesUp(final int previousTick, final int currentTick, final int targetTick) {
        return previousTick < targetTick && currentTick >= targetTick;
    }

    private static boolean crossesDown(final int previousTick, final int currentTick, final int targetTick) {
        return previousTick > targetTick && currentTick <= targetTick;
    }

    private static int openTick(final float progress) {
        return Mth.clamp(Math.round(progress * OPENING_TIME), 0, OPENING_TIME);
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
        skinIndex = Math.floorMod(tag.getInt("SkinIndex"), HbmVaultDoorBlockItem.SKIN_COUNT);
        redstoneMode = tag.contains("RedstoneMode") ? HbmDoorRedstoneMode.fromOrdinal(tag.getInt("RedstoneMode")) : HbmDoorRedstoneMode.DEFAULT;
        accessLevel = tag.contains("AccessLevel") ? DoorAccessLevel.fromOrdinal(tag.getInt("AccessLevel")) : DoorAccessLevel.DEFAULT;
        skinReconciled = false;
    }


    public int getSkinIndex() {
        return skinIndex;
    }

    public void setSkinIndex(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, HbmVaultDoorBlockItem.SKIN_COUNT);
        skinReconciled = true;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setSkinIndexNoBlockUpdate(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, HbmVaultDoorBlockItem.SKIN_COUNT);
        skinReconciled = true;
        setChanged();
    }

    public int cycleSkin() {
        skinIndex = (skinIndex + 1) % HbmVaultDoorBlockItem.SKIN_COUNT;
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
        if (!(state.getBlock() instanceof HbmVaultDoorBlock)) {
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
