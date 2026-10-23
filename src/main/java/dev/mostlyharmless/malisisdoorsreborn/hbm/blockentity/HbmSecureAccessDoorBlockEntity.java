package dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity;

import dev.mostlyharmless.malisisdoorsreborn.block.CustomSkinnedDoorHelper;
import dev.mostlyharmless.malisisdoorsreborn.common.DoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.access.AccessControlledDoor;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessHelper;
import dev.mostlyharmless.malisisdoorsreborn.access.DoorAccessLevel;
import dev.mostlyharmless.malisisdoorsreborn.block.AccessCardReaderCardLevelMode;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmSecureAccessDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrBlockEntities;
import dev.mostlyharmless.malisisdoorsreborn.network.MdrNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HbmSecureAccessDoorBlockEntity extends BlockEntity implements AccessControlledDoor {

    private static final int OPENING_TIME = 120;

    private float progressPrev = 0.0F;
    private float progress = 0.0F;
    private int skinIndex = 0;
    private DoorRedstoneMode redstoneMode = DoorRedstoneMode.DEFAULT;
    private DoorAccessLevel accessLevel = DoorAccessLevel.DEFAULT;
    private AccessCardReaderCardLevelMode cardLevelMode = AccessCardReaderCardLevelMode.defaultValue();
    private boolean skinReconciled = false;

    public HbmSecureAccessDoorBlockEntity(final BlockPos pos, final BlockState state) {
        super(MdrBlockEntities.HBM_SECURE_ACCESS_DOOR.get(), pos, state);
        snapProgressToState(state);
    }

    @SuppressWarnings("unused")
    public static void tickClient(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmSecureAccessDoorBlockEntity be) {
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        be.progress = Mth.approach(be.progress, open ? 1.0F : 0.0F, 1.0F / OPENING_TIME);
    }

    public static void tickServer(@NotNull final Level level,
                                  @NotNull final BlockPos pos,
                                  @NotNull final BlockState state,
                                  @NotNull final HbmSecureAccessDoorBlockEntity be) {
        be.reconcileSkinOnce(level, pos, state);
        be.progressPrev = be.progress;
        final boolean open = state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        final float target = open ? 1.0F : 0.0F;
        final float previous = be.progress;
        be.progress = Mth.approach(be.progress, target, 1.0F / OPENING_TIME);
        if (previous != target && be.progress == target) {
            if (level instanceof final ServerLevel serverLevel) {
                MdrNetwork.sendHbmSecureAccessDoorSound(serverLevel, pos, false);
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
        skinIndex = Math.floorMod(tag.getInt("SkinIndex"), 4);
        redstoneMode = DoorRedstoneMode.fromOrdinal(tag.getInt("RedstoneMode"));
        accessLevel = tag.contains("AccessLevel") ? DoorAccessLevel.fromOrdinal(tag.getInt("AccessLevel")) : DoorAccessLevel.DEFAULT;
        cardLevelMode = DoorAccessHelper.cardLevelModeFromTag(tag, accessLevel);
        skinReconciled = false;
    }


    public int getSkinIndex() {
        return skinIndex;
    }

    public void setSkinIndex(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, 4);
        skinReconciled = true;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setSkinIndexNoBlockUpdate(final int skinIndex) {
        this.skinIndex = Math.floorMod(skinIndex, 4);
        skinReconciled = true;
        setChanged();
    }

    public int cycleSkin() {
        skinIndex = (skinIndex + 1) % 4;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return skinIndex;
    }

    public DoorRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(@NotNull final DoorRedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public DoorRedstoneMode cycleRedstoneMode() {
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
    public @NotNull AccessCardReaderCardLevelMode getCardLevelMode() {
        return cardLevelMode;
    }

    @Override
    public void setCardLevelMode(@NotNull final AccessCardReaderCardLevelMode cardLevelMode) {
        if (this.cardLevelMode == cardLevelMode) return;
        this.cardLevelMode = cardLevelMode;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public @NotNull AccessCardReaderCardLevelMode cycleCardLevelMode() {
        final AccessCardReaderCardLevelMode next = cardLevelMode.next();
        setCardLevelMode(next);
        return next;
    }


    private void reconcileSkinOnce(@NotNull final Level level,
                                   @NotNull final BlockPos pos,
                                   @NotNull final BlockState state) {
        if (skinReconciled || level.isClientSide) return;
        if (!(state.getBlock() instanceof HbmSecureAccessDoorBlock)) {
            skinReconciled = true;
            return;
        }

        CustomSkinnedDoorHelper.reconcile(level, pos, state, this);
        skinReconciled = true;
    }


    @Override
    protected void saveAdditional(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SkinIndex", skinIndex);
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
        tag.putInt("AccessLevel", accessLevel.ordinal());
        tag.putString(DoorAccessHelper.CARD_LEVEL_MODE_TAG, cardLevelMode.serializedName());
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
