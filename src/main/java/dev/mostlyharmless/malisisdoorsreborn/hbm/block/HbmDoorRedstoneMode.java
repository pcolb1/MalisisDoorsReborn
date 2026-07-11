package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public enum HbmDoorRedstoneMode {
    DEFAULT("tooltip.malisisdoorsreborn.redstone.default"),
    REDSTONE_ONLY("tooltip.malisisdoorsreborn.redstone.redstone_only"),
    ALWAYS_IGNORE("tooltip.malisisdoorsreborn.redstone.always_ignore"),
    OPEN_WHEN_POWERED("tooltip.malisisdoorsreborn.redstone.open_when_powered"),
    CLOSE_WHEN_POWERED("tooltip.malisisdoorsreborn.redstone.close_when_powered");

    private final String translationKey;

    HbmDoorRedstoneMode(final String translationKey) {
        this.translationKey = translationKey;
    }

    public @NotNull Component displayName() {
        return Component.translatable(translationKey);
    }

    public @NotNull HbmDoorRedstoneMode next() {
        final HbmDoorRedstoneMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static @NotNull HbmDoorRedstoneMode fromOrdinal(final int ordinal) {
        final HbmDoorRedstoneMode[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
