package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmWaterDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundHbmWaterDoorSoundPacket(BlockPos pos, byte action) {

    public static void encode(final ClientboundHbmWaterDoorSoundPacket packet,
                              final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeByte(packet.action);
    }

    public static ClientboundHbmWaterDoorSoundPacket decode(final FriendlyByteBuf buffer) {
        return new ClientboundHbmWaterDoorSoundPacket(buffer.readBlockPos(), buffer.readByte());
    }

    public static void handle(final ClientboundHbmWaterDoorSoundPacket packet,
                              final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                HbmWaterDoorSoundHooks.handleWaterDoorSound(packet.pos, packet.action)));
        context.setPacketHandled(true);
    }
}
