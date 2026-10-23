package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmSecureAccessDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientboundHbmSecureAccessDoorSoundPacket(BlockPos pos, boolean start) implements CustomPacketPayload {

    public static final Type<ClientboundHbmSecureAccessDoorSoundPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_secure_access_door_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundHbmSecureAccessDoorSoundPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ClientboundHbmSecureAccessDoorSoundPacket decode(@NotNull final RegistryFriendlyByteBuf buffer) {
            return new ClientboundHbmSecureAccessDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
        }

        @Override
        public void encode(@NotNull final RegistryFriendlyByteBuf buffer, @NotNull final ClientboundHbmSecureAccessDoorSoundPacket packet) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeBoolean(packet.start);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ClientboundHbmSecureAccessDoorSoundPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> HbmSecureAccessDoorSoundHooks.handleSecureAccessDoorSound(packet.pos, packet.start));
    }
}
