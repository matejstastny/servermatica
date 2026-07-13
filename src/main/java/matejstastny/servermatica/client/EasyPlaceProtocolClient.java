package matejstastny.servermatica.client;

import com.google.common.collect.ImmutableSet;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import matejstastny.servermatica.mixin.client.PlacementHandlerAccessor;
import matejstastny.servermatica.network.InitEasyPlaceProtocolPacket;
import matejstastny.servermatica.network.SetEasyPlaceProtocolPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.state.property.Property;

public final class EasyPlaceProtocolClient {
    private static final ImmutableSet<Property<?>> DEFAULT_WHITELISTED_PROPERTIES = PlacementHandler.WHITELISTED_PROPERTIES;
    public static boolean serverHasV3Protocol = false;

    private EasyPlaceProtocolClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(InitEasyPlaceProtocolPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                PlacementHandlerAccessor.setWhitelistedProperties(payload.whitelistedProperties());
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(SetEasyPlaceProtocolPacket.ID)) {
                ClientPlayNetworking.send(new SetEasyPlaceProtocolPacket(getEasyPlaceProtocol()));
                serverHasV3Protocol = true;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlacementHandlerAccessor.setWhitelistedProperties(DEFAULT_WHITELISTED_PROPERTIES);
            serverHasV3Protocol = false;
        });

        Configs.Generic.EASY_PLACE_PROTOCOL.setValueChangeCallback(config -> {
            if (ClientPlayNetworking.canSend(SetEasyPlaceProtocolPacket.ID)) {
                ClientPlayNetworking.send(new SetEasyPlaceProtocolPacket(getEasyPlaceProtocol()));
            }
        });
    }

    private static int getEasyPlaceProtocol() {
        return switch (PlacementHandler.getEffectiveProtocolVersion()) {
            case V3 -> 3;
            case V2 -> 2;
            case SLAB_ONLY -> 1;
            default -> 0;
        };
    }
}
