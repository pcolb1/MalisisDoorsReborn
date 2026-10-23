package dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound;

import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmSlidingBlastDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.registry.MdrSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class HbmSlidingBlastDoorSoundHooks {

    private static final Map<BlockPos, HbmSlidingBlastDoorMovingSound> MOVE_SOUNDS = new HashMap<>();

    private HbmSlidingBlastDoorSoundHooks() {
    }

    public static void handleSlidingBlastDoorSound(@NotNull final BlockPos pos, final boolean start) {
        if (start) {
            start(pos);
        } else {
            stopWithEndpoint(pos);
        }
    }

    private static void start(@NotNull final BlockPos pos) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        final BlockPos immutablePos = pos.immutable();
        final HbmSlidingBlastDoorMovingSound existing = MOVE_SOUNDS.remove(immutablePos);
        if (existing != null) existing.stopNow();

        cleanupStoppedSounds();

        final HbmSlidingBlastDoorMovingSound sound = new HbmSlidingBlastDoorMovingSound(
                immutablePos,
                MdrSounds.HBM_SLIDING_BLAST_DOOR_MOVE.get(),
                2.0F
        );
        MOVE_SOUNDS.put(immutablePos, sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void stopWithEndpoint(@NotNull final BlockPos pos) {
        final BlockPos immutablePos = pos.immutable();
        final HbmSlidingBlastDoorMovingSound sound = MOVE_SOUNDS.remove(immutablePos);
        if (sound != null) sound.stopNow();

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.isLoaded(immutablePos)) return;

        final BlockState state = minecraft.level.getBlockState(immutablePos);
        if (!(state.getBlock() instanceof HbmSlidingBlastDoorBlock)) return;

        minecraft.getSoundManager().play(new SimpleSoundInstance(
                (state.getValue(HbmSlidingBlastDoorBlock.OPEN)
                        ? MdrSounds.HBM_SLIDING_BLAST_DOOR_OPENED.get()
                        : MdrSounds.HBM_SLIDING_BLAST_DOOR_SHUT.get()).getLocation(),
                SoundSource.BLOCKS,
                2.0F,
                1.0F,
                RandomSource.create(),
                false,
                0,
                net.minecraft.client.resources.sounds.SoundInstance.Attenuation.LINEAR,
                immutablePos.getX() + 0.5D,
                immutablePos.getY() + 0.5D,
                immutablePos.getZ() + 0.5D,
                false
        ));
    }

    private static void remove(@NotNull final BlockPos pos,
                               @NotNull final HbmSlidingBlastDoorMovingSound sound) {
        if (MOVE_SOUNDS.get(pos) == sound) MOVE_SOUNDS.remove(pos);
    }

    private static void cleanupStoppedSounds() {
        MOVE_SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }

    private static final class HbmSlidingBlastDoorMovingSound extends AbstractTickableSoundInstance {

        private static final int MAX_LIFETIME_TICKS = 40;

        private final BlockPos pos;
        private int lifetimeTicks = 0;

        private HbmSlidingBlastDoorMovingSound(@NotNull final BlockPos pos,
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
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null
                    || !minecraft.level.isLoaded(pos)
                    || !(minecraft.level.getBlockState(pos).getBlock() instanceof HbmSlidingBlastDoorBlock)) {
                stopNow();
                HbmSlidingBlastDoorSoundHooks.remove(pos, this);
                return;
            }

            lifetimeTicks++;
            if (lifetimeTicks > MAX_LIFETIME_TICKS) {
                stopNow();
                HbmSlidingBlastDoorSoundHooks.remove(pos, this);
            }
        }

        private void stopNow() {
            volume = 0.0F;
            stop();
        }
    }
}
