package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmRoundAirlockDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientboundHbmRoundAirlockDoorSoundPacket(BlockPos pos, boolean start) implements CustomPacketPayload {

    public static final Type<ClientboundHbmRoundAirlockDoorSoundPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "hbm_round_airlock_door_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundHbmRoundAirlockDoorSoundPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ClientboundHbmRoundAirlockDoorSoundPacket decode(@NotNull final RegistryFriendlyByteBuf buffer) {
            return new ClientboundHbmRoundAirlockDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
        }

        @Override
        public void encode(@NotNull final RegistryFriendlyByteBuf buffer, @NotNull final ClientboundHbmRoundAirlockDoorSoundPacket packet) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeBoolean(packet.start);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ClientboundHbmRoundAirlockDoorSoundPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> HbmRoundAirlockDoorSoundHooks.handleRoundAirlockDoorSound(packet.pos, packet.start));
    }
}
