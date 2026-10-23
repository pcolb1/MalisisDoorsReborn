package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmQeSlidingDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientboundHbmQeSlidingDoorSoundPacket(BlockPos pos, boolean start) implements CustomPacketPayload {

    public static final Type<ClientboundHbmQeSlidingDoorSoundPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_qe_sliding_door_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundHbmQeSlidingDoorSoundPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ClientboundHbmQeSlidingDoorSoundPacket decode(@NotNull final RegistryFriendlyByteBuf buffer) {
            return new ClientboundHbmQeSlidingDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
        }

        @Override
        public void encode(@NotNull final RegistryFriendlyByteBuf buffer, @NotNull final ClientboundHbmQeSlidingDoorSoundPacket packet) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeBoolean(packet.start);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ClientboundHbmQeSlidingDoorSoundPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> HbmQeSlidingDoorSoundHooks.handleQeSlidingDoorSound(packet.pos, packet.start));
    }
}
