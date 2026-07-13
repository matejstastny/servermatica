package matejstastny.servermatica.network;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public record InitEasyPlaceProtocolPacket(ImmutableSet<Property<?>> whitelistedProperties) implements CustomPayload {
    public static final CustomPayload.Id<InitEasyPlaceProtocolPacket> ID =
            new CustomPayload.Id<>(Identifier.of("servermatica", "init_easy_place"));
    public static final PacketCodec<RegistryByteBuf, InitEasyPlaceProtocolPacket> CODEC =
            PacketCodec.of((packet, buf) -> packet.write(buf), InitEasyPlaceProtocolPacket::read);

    public static InitEasyPlaceProtocolPacket read(RegistryByteBuf buf) {
        return new InitEasyPlaceProtocolPacket(readWhitelistedProperties(buf));
    }

    public void write(RegistryByteBuf buf) {
        writeWhitelistedProperties(buf, whitelistedProperties);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static ImmutableSet<Property<?>> readWhitelistedProperties(RegistryByteBuf buf) {
        int numProperties = buf.readVarInt();
        ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builderWithExpectedSize(numProperties);
        for (int i = 0; i < numProperties; i++) {
            Identifier blockId = buf.readIdentifier();
            Block block = Registries.BLOCK.get(blockId);
            String propertyName = buf.readString(256);
            Property<?> property = block.getStateManager().getProperty(propertyName);
            if (property != null) {
                properties.add(property);
            }
        }
        return properties.build();
    }

    private static void writeWhitelistedProperties(RegistryByteBuf buf, ImmutableSet<Property<?>> whitelistedProperties) {
        buf.writeVarInt(whitelistedProperties.size());
        Set<Property<?>> propertiesToWrite = new HashSet<>(whitelistedProperties);
        for (Block block : Registries.BLOCK) {
            for (Property<?> property : block.getStateManager().getProperties()) {
                if (propertiesToWrite.remove(property)) {
                    buf.writeIdentifier(Registries.BLOCK.getId(block));
                    buf.writeString(property.getName(), 256);
                    if (propertiesToWrite.isEmpty()) {
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("Found properties with no block containing them: " + propertiesToWrite);
    }
}
