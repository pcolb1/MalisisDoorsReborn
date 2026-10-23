package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmCargoDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundHbmCargoDoorSoundPacket(BlockPos pos, boolean start) {

    public static void encode(final ClientboundHbmCargoDoorSoundPacket packet,
                              final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.start);
    }

    public static ClientboundHbmCargoDoorSoundPacket decode(final FriendlyByteBuf buffer) {
        return new ClientboundHbmCargoDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(final ClientboundHbmCargoDoorSoundPacket packet,
                              final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                HbmCargoDoorSoundHooks.handleCargoDoorSound(packet.pos, packet.start)));
        context.setPacketHandled(true);
    }
}
