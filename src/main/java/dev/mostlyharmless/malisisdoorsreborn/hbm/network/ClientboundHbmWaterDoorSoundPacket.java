package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmWaterDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientboundHbmWaterDoorSoundPacket(BlockPos pos, byte action) implements CustomPacketPayload {

    public static final Type<ClientboundHbmWaterDoorSoundPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_water_door_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundHbmWaterDoorSoundPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ClientboundHbmWaterDoorSoundPacket decode(@NotNull final RegistryFriendlyByteBuf buffer) {
            return new ClientboundHbmWaterDoorSoundPacket(buffer.readBlockPos(), buffer.readByte());
        }

        @Override
        public void encode(@NotNull final RegistryFriendlyByteBuf buffer, @NotNull final ClientboundHbmWaterDoorSoundPacket packet) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeByte(packet.action);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ClientboundHbmWaterDoorSoundPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> HbmWaterDoorSoundHooks.handleWaterDoorSound(packet.pos, packet.action));
    }
}
