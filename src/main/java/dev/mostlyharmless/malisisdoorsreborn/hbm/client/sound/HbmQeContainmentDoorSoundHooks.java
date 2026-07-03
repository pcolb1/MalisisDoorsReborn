package dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound;

import dev.mostlyharmless.malisisdoorsreborn.registry.MdrSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class HbmQeContainmentDoorSoundHooks {

    private static final Map<BlockPos, HbmQeContainmentDoorMovingSound> MOVE_SOUNDS = new HashMap<>();

    private HbmQeContainmentDoorSoundHooks() {
    }

    public static void handleQeContainmentDoorSound(@NotNull final BlockPos pos, final boolean start) {
        if (start) {
            start(pos);
        } else {
            stop(pos);
        }
    }

    private static void start(@NotNull final BlockPos pos) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        final BlockPos immutablePos = pos.immutable();
        startLoop(minecraft, MOVE_SOUNDS, immutablePos, MdrSounds.HBM_FIRE_DOOR_MOVE.get(), 2.0F);
    }

    @SuppressWarnings("SameParameterValue")
    private static void startLoop(@NotNull final Minecraft minecraft,
                                  @NotNull final Map<BlockPos, HbmQeContainmentDoorMovingSound> sounds,
                                  @NotNull final BlockPos pos,
                                  @NotNull final SoundEvent soundEvent,
                                  final float volume) {
        final HbmQeContainmentDoorMovingSound existing = sounds.get(pos);
        if (existing != null && !existing.isStopping()) return;

        cleanupStoppedSounds(sounds);

        final HbmQeContainmentDoorMovingSound sound = new HbmQeContainmentDoorMovingSound(pos, soundEvent, volume);
        sounds.put(pos, sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void stop(@NotNull final BlockPos pos) {
        fadeOut(MOVE_SOUNDS, pos);

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        minecraft.getSoundManager().play(new SimpleSoundInstance(
                MdrSounds.HBM_FIRE_DOOR_STOP.get().getLocation(),
                SoundSource.BLOCKS,
                2.0F,
                1.0F,
                RandomSource.create(),
                false,
                0,
                net.minecraft.client.resources.sounds.SoundInstance.Attenuation.LINEAR,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                false
        ));
    }

    @SuppressWarnings("SameParameterValue")
    private static void fadeOut(@NotNull final Map<BlockPos, HbmQeContainmentDoorMovingSound> sounds,
                                @NotNull final BlockPos pos) {
        final HbmQeContainmentDoorMovingSound sound = sounds.get(pos);
        if (sound != null) sound.fadeOut();
    }

    @SuppressWarnings("SameParameterValue")
    private static void remove(@NotNull final Map<BlockPos, HbmQeContainmentDoorMovingSound> sounds,
                               @NotNull final BlockPos pos,
                               @NotNull final HbmQeContainmentDoorMovingSound sound) {
        if (sounds.get(pos) == sound) sounds.remove(pos);
    }

    private static void cleanupStoppedSounds(@NotNull final Map<BlockPos, HbmQeContainmentDoorMovingSound> sounds) {
        sounds.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }

    private static final class HbmQeContainmentDoorMovingSound extends AbstractTickableSoundInstance {

        private static final int FADE_TICKS = 5;
        private static final int MAX_LIFETIME_TICKS = 220;

        private final BlockPos pos;
        private int fadeTicks = -1;
        private int lifetimeTicks = 0;

        private HbmQeContainmentDoorMovingSound(@NotNull final BlockPos pos,
                                       @NotNull final SoundEvent soundEvent,
                                       final float volume) {
            super(soundEvent, SoundSource.BLOCKS, RandomSource.create());
            this.pos = pos;
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
            this.volume = volume;
            this.pitch = 1.0F;
            this.looping = true;
            this.delay = 0;
        }

        @Override
        public void tick() {
            lifetimeTicks++;
            if (fadeTicks < 0 && lifetimeTicks > MAX_LIFETIME_TICKS) fadeOut();
            if (fadeTicks < 0) return;

            fadeTicks--;
            if (fadeTicks <= 0) {
                volume = 0.0F;
                stop();
                HbmQeContainmentDoorSoundHooks.remove(MOVE_SOUNDS, pos, this);
                return;
            }

            volume = Math.max(0.0F, Math.min(2.0F, 2.0F * fadeTicks / (float) FADE_TICKS));
        }

        private void fadeOut() {
            if (fadeTicks < 0) fadeTicks = FADE_TICKS;
        }

        private boolean isStopping() {
            return fadeTicks >= 0;
        }
    }
}
