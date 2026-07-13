package matejstastny.servermatica.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetEasyPlaceProtocolPacket(int protocol) implements CustomPayload {
    public static final CustomPayload.Id<SetEasyPlaceProtocolPacket> ID =
            new CustomPayload.Id<>(Identifier.of("servermatica", "set_easy_place_protocol"));
    public static final PacketCodec<RegistryByteBuf, SetEasyPlaceProtocolPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeVarInt(packet.protocol()),
                    buf -> new SetEasyPlaceProtocolPacket(buf.readVarInt())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
