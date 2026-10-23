package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmSecureAccessDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundHbmSecureAccessDoorSoundPacket(BlockPos pos, boolean start) {

    public static void encode(final ClientboundHbmSecureAccessDoorSoundPacket packet,
                              final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.start);
    }

    public static ClientboundHbmSecureAccessDoorSoundPacket decode(final FriendlyByteBuf buffer) {
        return new ClientboundHbmSecureAccessDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(final ClientboundHbmSecureAccessDoorSoundPacket packet,
                              final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                HbmSecureAccessDoorSoundHooks.handleSecureAccessDoorSound(packet.pos, packet.start)));
        context.setPacketHandled(true);
    }
}
