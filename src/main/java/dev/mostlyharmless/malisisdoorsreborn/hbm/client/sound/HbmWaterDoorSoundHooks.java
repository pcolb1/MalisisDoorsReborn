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

public final class HbmWaterDoorSoundHooks {

    private static final Map<BlockPos, HbmWaterDoorMovingSound> MOVE_SOUNDS = new HashMap<>();

    private HbmWaterDoorSoundHooks() {
    }

    public static void handleWaterDoorSound(@NotNull final BlockPos pos, final byte action) {
        switch (action) {
            case 0 -> start(pos, true);
            case 1 -> start(pos, false);
            case 2 -> finish(pos, MdrSounds.HBM_WATER_DOOR_STOP.get());
            case 3 -> finish(pos, MdrSounds.HBM_WATER_DOOR_LEVER.get());
            case 4 -> playOneShot(pos, MdrSounds.HBM_WATER_DOOR_STOP.get());
            default -> { }
        }
    }

    private static void start(@NotNull final BlockPos pos, final boolean opening) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        final BlockPos immutablePos = pos.immutable();
        if (opening) playOneShot(minecraft, immutablePos, MdrSounds.HBM_WATER_DOOR_LEVER.get());
        startLoop(minecraft, MOVE_SOUNDS, immutablePos, MdrSounds.HBM_WATER_DOOR_MOVE.get(), 2.0F);
    }

    @SuppressWarnings("SameParameterValue")
    private static void startLoop(@NotNull final Minecraft minecraft,
                                  @NotNull final Map<BlockPos, HbmWaterDoorMovingSound> sounds,
                                  @NotNull final BlockPos pos,
                                  @NotNull final SoundEvent soundEvent,
                                  final float volume) {
        final HbmWaterDoorMovingSound existing = sounds.get(pos);
        if (existing != null && !existing.isStopped()) return;

        cleanupStoppedSounds(sounds);

        final HbmWaterDoorMovingSound sound = new HbmWaterDoorMovingSound(pos, soundEvent, volume);
        sounds.put(pos, sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void finish(@NotNull final BlockPos pos, @NotNull final SoundEvent soundEvent) {
        stopLoop(MOVE_SOUNDS, pos);
        playOneShot(pos, soundEvent);
    }

    private static void playOneShot(@NotNull final BlockPos pos,
                                    @NotNull final SoundEvent soundEvent) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        playOneShot(minecraft, pos, soundEvent);
    }

    private static void playOneShot(@NotNull final Minecraft minecraft,
                                    @NotNull final BlockPos pos,
                                    @NotNull final SoundEvent soundEvent) {
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                soundEvent.location(),
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
    private static void stopLoop(@NotNull final Map<BlockPos, HbmWaterDoorMovingSound> sounds,
                                 @NotNull final BlockPos pos) {
        final HbmWaterDoorMovingSound sound = sounds.remove(pos);
        if (sound != null) sound.stopNow();
    }

    @SuppressWarnings("SameParameterValue")
    private static void remove(@NotNull final Map<BlockPos, HbmWaterDoorMovingSound> sounds,
                               @NotNull final BlockPos pos,
                               @NotNull final HbmWaterDoorMovingSound sound) {
        if (sounds.get(pos) == sound) sounds.remove(pos);
    }

    private static void cleanupStoppedSounds(@NotNull final Map<BlockPos, HbmWaterDoorMovingSound> sounds) {
        sounds.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }

    private static final class HbmWaterDoorMovingSound extends AbstractTickableSoundInstance {

        private static final int MAX_LIFETIME_TICKS = 220;

        private final BlockPos pos;
        private int lifetimeTicks = 0;

        private HbmWaterDoorMovingSound(@NotNull final BlockPos pos,
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
            if (lifetimeTicks > MAX_LIFETIME_TICKS) {
                stopNow();
                HbmWaterDoorSoundHooks.remove(MOVE_SOUNDS, pos, this);
            }
        }

        private void stopNow() {
            volume = 0.0F;
            stop();
        }
    }
}
