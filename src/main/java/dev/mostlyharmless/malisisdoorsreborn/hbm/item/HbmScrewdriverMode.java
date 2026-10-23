package dev.mostlyharmless.malisisdoorsreborn.hbm.item;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public enum HbmScrewdriverMode {
    CYCLE_DOOR_SKIN("Cycle Door Skin"),
    CYCLE_REDSTONE_BEHAVIOUR("Cycle Redstone Behaviour"),
    CYCLE_PULSE_DURATION("Cycle Pulse Duration"),
    CYCLE_CARD_LEVELS("Cycle Card Levels");

    private final String displayName;

    HbmScrewdriverMode(final String displayName) {
        this.displayName = displayName;
    }

    public @NotNull Component actionbarComponent() {
        return Component.literal("Sonic Screwdriver: " + displayName);
    }

    public @NotNull HbmScrewdriverMode next() {
        final HbmScrewdriverMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public @NotNull HbmScrewdriverMode previous() {
        final HbmScrewdriverMode[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }
}
