package matejstastny.servermatica.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import matejstastny.servermatica.server.EasyPlaceProtocolServer;

@Mixin(value = BlockItem.class, priority = 900) // lower priority than litematica's mixin to inject before it
public abstract class BlockItemMixin_EasyPlaceProtocol {
    @Shadow public abstract Block getBlock();

    @Shadow protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void applyEasyPlaceProtocolV3(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (ctx.getPlayer() instanceof ServerPlayer serverPlayer && EasyPlaceProtocolServer.getEasyPlaceProtocol(serverPlayer) == 3) {
            BlockState stateOrig = getBlock().getStateForPlacement(ctx);
            if (stateOrig != null) {
                BlockState newState = EasyPlaceProtocolServer.applyEasyPlaceProtocolV3(stateOrig, ctx);
                if (canPlace(ctx, newState)) {
                    cir.setReturnValue(newState);
                }
            }
        }
    }
}
