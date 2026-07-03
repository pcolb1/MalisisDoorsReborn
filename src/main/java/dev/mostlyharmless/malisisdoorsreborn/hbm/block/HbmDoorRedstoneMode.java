package dev.mostlyharmless.malisisdoorsreborn.hbm.block;

import org.jetbrains.annotations.NotNull;

public enum HbmDoorRedstoneMode {
    DEFAULT("Default"),
    REDSTONE_ONLY("Redstone-only"),
    ALWAYS_IGNORE("Always Ignore"),
    OPEN_WHEN_POWERED("Open When Powered"),
    CLOSE_WHEN_POWERED("Close When Powered");

    private final String displayName;

    HbmDoorRedstoneMode(final String displayName) {
        this.displayName = displayName;
    }

    public @NotNull String displayName() {
        return displayName;
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
