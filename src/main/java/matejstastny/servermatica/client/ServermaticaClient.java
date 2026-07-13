package matejstastny.servermatica.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ServermaticaClient implements ClientModInitializer {
    public static final boolean HAS_NETWORKING = FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1");

    @Override
    public void onInitializeClient() {
        if (HAS_NETWORKING) {
            EasyPlaceProtocolClient.init();
        }
    }
}
