package matejstastny.servermatica.server;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ServermaticaServer implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1")) {
            EasyPlaceProtocolServer.init();
        }
    }
}
