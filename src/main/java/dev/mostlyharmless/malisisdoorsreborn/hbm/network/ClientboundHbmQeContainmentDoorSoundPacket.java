package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmQeContainmentDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundHbmQeContainmentDoorSoundPacket(BlockPos pos, boolean start) {

    public static void encode(final ClientboundHbmQeContainmentDoorSoundPacket packet,
                              final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.start);
    }

    public static ClientboundHbmQeContainmentDoorSoundPacket decode(final FriendlyByteBuf buffer) {
        return new ClientboundHbmQeContainmentDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(final ClientboundHbmQeContainmentDoorSoundPacket packet,
                              final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                HbmQeContainmentDoorSoundHooks.handleQeContainmentDoorSound(packet.pos, packet.start)));
        context.setPacketHandled(true);
    }
}
