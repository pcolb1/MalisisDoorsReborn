package dev.mostlyharmless.malisisdoorsreborn.hbm.network;

import dev.mostlyharmless.malisisdoorsreborn.hbm.client.sound.HbmRoundAirlockDoorSoundHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundHbmRoundAirlockDoorSoundPacket(BlockPos pos, boolean start) {

    public static void encode(final ClientboundHbmRoundAirlockDoorSoundPacket packet,
                              final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.start);
    }

    public static ClientboundHbmRoundAirlockDoorSoundPacket decode(final FriendlyByteBuf buffer) {
        return new ClientboundHbmRoundAirlockDoorSoundPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(final ClientboundHbmRoundAirlockDoorSoundPacket packet,
                              final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                HbmRoundAirlockDoorSoundHooks.handleRoundAirlockDoorSound(packet.pos, packet.start)));
        context.setPacketHandled(true);
    }
}
