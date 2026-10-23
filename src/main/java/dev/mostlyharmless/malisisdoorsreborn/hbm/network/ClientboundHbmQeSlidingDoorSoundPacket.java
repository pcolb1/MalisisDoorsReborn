package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmQeSlidingDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundHbmQeSlidingDoorSoundPacket(BlockPos pos, boolean start) {

    public static void encode(final ClientboundHbmQeSlidingDoorSoundPacket packet,
                              final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.start);
    }

    public static ClientboundHbmQeSlidingDoorSoundPacket decode(final FriendlyByteBuf buffer) {
        return new ClientboundHbmQeSlidingDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(final ClientboundHbmQeSlidingDoorSoundPacket packet,
                              final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                HbmQeSlidingDoorSoundHooks.handleQeSlidingDoorSound(packet.pos, packet.start)));
        context.setPacketHandled(true);
    }
}
