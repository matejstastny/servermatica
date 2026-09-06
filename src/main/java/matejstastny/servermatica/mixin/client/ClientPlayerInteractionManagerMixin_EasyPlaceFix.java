package matejstastny.servermatica.mixin.client;

import fi.dy.masa.litematica.config.Configs;
import matejstastny.servermatica.client.EasyPlaceFix;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin_EasyPlaceFix {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && !EasyPlaceFix.isPlacingWithEasyPlace) {
            if (EasyPlaceFix.handleEasyPlaceRestriction()) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }
}
