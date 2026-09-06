package matejstastny.servermatica.mixin.client;

import fi.dy.masa.litematica.util.EasyPlaceUtils;
import matejstastny.servermatica.client.EasyPlaceFix;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EasyPlaceUtils.class, remap = false)
public class WorldUtilsMixin_EasyPlaceFix {
    @Inject(method = "handleEasyPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", remap = true), require = 1)
    private static void preInteractBlock(CallbackInfoReturnable<InteractionResult> cir) {
        EasyPlaceFix.isPlacingWithEasyPlace = true;
    }

    @Inject(method = "handleEasyPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", shift = At.Shift.AFTER, remap = true), require = 1)
    private static void postInteractBlock(CallbackInfoReturnable<InteractionResult> cir) {
        EasyPlaceFix.isPlacingWithEasyPlace = false;
    }
}
