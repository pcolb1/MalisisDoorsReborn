package dev.mostlyharmless.malisisdoorsreborn.hbm.item;

import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.item.TooltipBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class HbmVehicleDoorBlockItem extends TooltipBlockItem {

    public static final String SKIN_TAG = "SkinIndex";
    public static final String REDSTONE_MODE_TAG = "RedstoneMode";
    private static final String SKIN_PREVIEW_CYCLE_TAG = "SkinPreviewCycle";
    public static final int SKIN_COUNT = 1;

    public HbmVehicleDoorBlockItem(final Supplier<? extends Block> block,
                                   final Properties properties,
                                   final String tooltipKey) {
        super(block, properties, tooltipKey);
    }

    public static int skinIndexFromStack(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? null : data.copyTag();
        if (tag == null || !tag.contains(SKIN_TAG)) return 0;
        return Math.floorMod(tag.getIntOr(SKIN_TAG, 0), SKIN_COUNT);
    }

    public static ItemStack stackWithSkinAndRedstone(@NotNull final Item item,
                                                     final int skinIndex,
                                                     @NotNull final HbmDoorRedstoneMode redstoneMode) {
        final ItemStack stack = new ItemStack(item);
        setSkinIndex(stack, skinIndex);
        setRedstoneMode(stack, redstoneMode);
        return stack;
    }


    public static ItemStack stackWithSkinPreviewCycle(@NotNull final Item item) {
        final ItemStack stack = new ItemStack(item);
        setSkinPreviewCycle(stack, true);
        return stack;
    }

    public static int skinIndexForRender(@NotNull final ItemStack stack) {
        if (hasSkinPreviewCycle(stack) && SKIN_COUNT > 1) {
            return (int) ((System.currentTimeMillis() / 1000L) % SKIN_COUNT);
        }
        return skinIndexFromStack(stack);
    }

    private static boolean hasSkinPreviewCycle(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? null : data.copyTag();
        return tag != null && tag.getBooleanOr(SKIN_PREVIEW_CYCLE_TAG, false);
    }

    @SuppressWarnings("SameParameterValue")
    private static void setSkinPreviewCycle(@NotNull final ItemStack stack, final boolean enabled) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        if (enabled) {
            tag.putBoolean(SKIN_PREVIEW_CYCLE_TAG, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return;
        }
        tag.remove(SKIN_PREVIEW_CYCLE_TAG);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static void clearSkinPreviewCycle(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        final CompoundTag tag = data.copyTag();
        if (!tag.contains(SKIN_PREVIEW_CYCLE_TAG)) return;
        tag.remove(SKIN_PREVIEW_CYCLE_TAG);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void setSkinIndex(@NotNull final ItemStack stack, final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, SKIN_COUNT);
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        if (normalisedSkin == 0) {
            tag.remove(SKIN_TAG);
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            return;
        }
        tag.putInt(SKIN_TAG, normalisedSkin);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @SuppressWarnings("unused")
    public static String skinName(final int skinIndex) {
        return "Default";
    }

    public static HbmDoorRedstoneMode redstoneModeFromStack(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? null : data.copyTag();
        if (tag == null || !tag.contains(REDSTONE_MODE_TAG)) return HbmDoorRedstoneMode.DEFAULT;
        return HbmDoorRedstoneMode.fromOrdinal(tag.getIntOr(REDSTONE_MODE_TAG, 0));
    }

    public static void setRedstoneMode(@NotNull final ItemStack stack,
                                       @NotNull final HbmDoorRedstoneMode redstoneMode) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        if (redstoneMode == HbmDoorRedstoneMode.DEFAULT) {
            tag.remove(REDSTONE_MODE_TAG);
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            return;
        }
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String redstoneModeName(@NotNull final HbmDoorRedstoneMode redstoneMode) {
        return redstoneMode.displayName();
    }


    @Override
    public void inventoryTick(@NotNull final ItemStack stack,
                              @NotNull final ServerLevel level,
                              @NotNull final Entity entity,
                              @Nullable final EquipmentSlot slot) {
        clearSkinPreviewCycle(stack);
        super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack,
                                @NotNull final Item.TooltipContext context,
                                @NotNull final TooltipDisplay tooltipDisplay,
                                @NotNull final Consumer<Component> consumer,
                                @NotNull final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, flag);
        consumer.accept(Component.literal("Skin: " + skinName(skinIndexFromStack(stack))).withStyle(ChatFormatting.WHITE));
        consumer.accept(Component.literal("Redstone: " + redstoneModeName(redstoneModeFromStack(stack))).withStyle(ChatFormatting.WHITE));
    }
}
