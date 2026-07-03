package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmFireDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientboundHbmFireDoorSoundPacket(BlockPos pos, boolean start) implements CustomPacketPayload {

    public static final Type<ClientboundHbmFireDoorSoundPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_fire_door_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundHbmFireDoorSoundPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ClientboundHbmFireDoorSoundPacket decode(@NotNull final RegistryFriendlyByteBuf buffer) {
            return new ClientboundHbmFireDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
        }

        @Override
        public void encode(@NotNull final RegistryFriendlyByteBuf buffer, @NotNull final ClientboundHbmFireDoorSoundPacket packet) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeBoolean(packet.start);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ClientboundHbmFireDoorSoundPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> HbmFireDoorSoundHooks.handleFireDoorSound(packet.pos, packet.start));
    }
}
