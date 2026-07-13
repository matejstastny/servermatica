package matejstastny.servermatica.mixin.client;

import fi.dy.masa.litematica.util.EasyPlaceUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = EasyPlaceUtils.class, remap = false)
public interface WorldUtilsAccessor {
    @Invoker("placementRestrictionInEffect")
    static boolean invokePlacementRestrictionInEffect() {
        throw new UnsupportedOperationException();
    }
}
