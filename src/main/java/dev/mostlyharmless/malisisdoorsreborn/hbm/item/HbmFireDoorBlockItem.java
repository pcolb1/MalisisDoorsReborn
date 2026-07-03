package dev.mostlyharmless.malisisdoorsreborn.hbm.item;

import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmDoorRedstoneMode;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.render.item.HbmFireDoorItemRenderer;
import dev.mostlyharmless.malisisdoorsreborn.item.TooltipBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HbmFireDoorBlockItem extends TooltipBlockItem {

    public static final String SKIN_TAG = "SkinIndex";
    public static final String REDSTONE_MODE_TAG = "RedstoneMode";
    private static final String SKIN_PREVIEW_CYCLE_TAG = "SkinPreviewCycle";
    public static final int SKIN_COUNT = 5;

    private static final String[] SKIN_NAMES = {
            "Default",
            "Black",
            "Orange",
            "Yellow",
            "Trefoil"
    };

    public HbmFireDoorBlockItem(final Supplier<? extends Block> block,
                                final Properties properties,
                                final String tooltipKey) {
        super(block, properties, tooltipKey);
    }

    public static int skinIndexFromStack(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? null : data.copyTag();
        if (tag == null || !tag.contains(SKIN_TAG)) return 0;
        return Math.floorMod(tag.getInt(SKIN_TAG), SKIN_COUNT);
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
        if (SKIN_COUNT > 1) setSkinPreviewCycle(stack, true);
        return stack;
    }

    public static int skinIndexForRender(@NotNull final ItemStack stack) {
        if (hasSkinPreviewCycle(stack) && SKIN_COUNT > 1) {
            return (int) ((Util.getMillis() / 1000L) % SKIN_COUNT);
        }
        return skinIndexFromStack(stack);
    }

    private static boolean hasSkinPreviewCycle(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? null : data.copyTag();
        return tag != null && tag.getBoolean(SKIN_PREVIEW_CYCLE_TAG);
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

    public static String skinName(final int skinIndex) {
        return SKIN_NAMES[Math.floorMod(skinIndex, SKIN_NAMES.length)];
    }

    public static HbmDoorRedstoneMode redstoneModeFromStack(@NotNull final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = data == null ? null : data.copyTag();
        if (tag == null || !tag.contains(REDSTONE_MODE_TAG)) return HbmDoorRedstoneMode.DEFAULT;
        return HbmDoorRedstoneMode.fromOrdinal(tag.getInt(REDSTONE_MODE_TAG));
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
                              @NotNull final Level level,
                              @NotNull final Entity entity,
                              final int slotId,
                              final boolean isSelected) {
        clearSkinPreviewCycle(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack,
                                @NotNull final Item.TooltipContext context,
                                @NotNull final List<Component> tooltip,
                                @NotNull final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Skin: " + skinName(skinIndexFromStack(stack))).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("Redstone: " + redstoneModeName(redstoneModeFromStack(stack))).withStyle(ChatFormatting.WHITE));
    }

    @SuppressWarnings("removal")
    @Override
    public void initializeClient(@NotNull final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HbmFireDoorItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    final Minecraft minecraft = Minecraft.getInstance();
                    renderer = new HbmFireDoorItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels()
                    );
                }
                return renderer;
            }
        });
    }
}
