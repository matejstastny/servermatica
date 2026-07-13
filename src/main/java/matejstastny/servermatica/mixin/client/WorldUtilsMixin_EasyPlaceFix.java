package matejstastny.servermatica.mixin.client;

import fi.dy.masa.litematica.util.EasyPlaceUtils;
import matejstastny.servermatica.client.EasyPlaceFix;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EasyPlaceUtils.class, remap = false)
public class WorldUtilsMixin_EasyPlaceFix {
    @Inject(method = "handleEasyPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", remap = true), require = 1)
    private static void preInteractBlock(CallbackInfoReturnable<ActionResult> cir) {
        EasyPlaceFix.isPlacingWithEasyPlace = true;
    }

    @Inject(method = "handleEasyPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", shift = At.Shift.AFTER, remap = true), require = 1)
    private static void postInteractBlock(CallbackInfoReturnable<ActionResult> cir) {
        EasyPlaceFix.isPlacingWithEasyPlace = false;
    }
}
