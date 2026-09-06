package matejstastny.servermatica.network;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashSet;
import java.util.Set;

public record InitEasyPlaceProtocolPacket(ImmutableSet<Property<?>> whitelistedProperties) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<InitEasyPlaceProtocolPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermatica", "init_easy_place"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InitEasyPlaceProtocolPacket> CODEC =
            StreamCodec.of((buf, packet) -> packet.write(buf), InitEasyPlaceProtocolPacket::read);

    public static InitEasyPlaceProtocolPacket read(RegistryFriendlyByteBuf buf) {
        return new InitEasyPlaceProtocolPacket(readWhitelistedProperties(buf));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        writeWhitelistedProperties(buf, whitelistedProperties);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static ImmutableSet<Property<?>> readWhitelistedProperties(RegistryFriendlyByteBuf buf) {
        int numProperties = buf.readVarInt();
        ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builderWithExpectedSize(numProperties);
        for (int i = 0; i < numProperties; i++) {
            Identifier blockId = buf.readIdentifier();
            Block block = BuiltInRegistries.BLOCK.getValue(blockId);
            String propertyName = buf.readUtf(256);
            Property<?> property = block.getStateDefinition().getProperty(propertyName);
            if (property != null) {
                properties.add(property);
            }
        }
        return properties.build();
    }

    private static void writeWhitelistedProperties(RegistryFriendlyByteBuf buf, ImmutableSet<Property<?>> whitelistedProperties) {
        buf.writeVarInt(whitelistedProperties.size());
        Set<Property<?>> propertiesToWrite = new HashSet<>(whitelistedProperties);
        for (Block block : BuiltInRegistries.BLOCK) {
            for (Property<?> property : block.getStateDefinition().getProperties()) {
                if (propertiesToWrite.remove(property)) {
                    buf.writeIdentifier(BuiltInRegistries.BLOCK.getKey(block));
                    buf.writeUtf(property.getName(), 256);
                    if (propertiesToWrite.isEmpty()) {
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("Found properties with no block containing them: " + propertiesToWrite);
    }
}
