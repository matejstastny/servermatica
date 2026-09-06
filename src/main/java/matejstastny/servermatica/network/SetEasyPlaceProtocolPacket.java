package matejstastny.servermatica.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetEasyPlaceProtocolPacket(int protocol) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetEasyPlaceProtocolPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermatica", "set_easy_place_protocol"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetEasyPlaceProtocolPacket> CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeVarInt(packet.protocol()),
                    buf -> new SetEasyPlaceProtocolPacket(buf.readVarInt())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
