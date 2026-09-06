package matejstastny.servermatica.mixin.server;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import matejstastny.servermatica.server.EasyPlaceProtocolServer;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 900) // lower priority than carpet's and litematica's mixins so that the redirect is only applied if neither have it
public class ServerPlayNetworkHandlerMixin_EasyPlaceProtocol implements EasyPlaceProtocolServer.NetworkHandlerExt {
    @Unique
    private int servermatica_easyPlaceProtocol;

    @Override
    public int servermatica_getEasyPlaceProtocol() {
        return servermatica_easyPlaceProtocol;
    }

    @Override
    public void servermatica_setEasyPlaceProtocol(int easyPlaceProtocol) {
        servermatica_easyPlaceProtocol = easyPlaceProtocol;
    }

    @Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"), require = 0)
    private Vec3 removeHitPosCheck(Vec3 hitVec, Vec3 blockCenter) {
        return Vec3.ZERO;
    }
}
