package matejstastny.servermatica.client;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.MessageOutputType;
import matejstastny.servermatica.mixin.client.WorldUtilsAccessor;

public final class EasyPlaceFix {
    public static boolean isPlacingWithEasyPlace = false;

    private EasyPlaceFix() {
    }

    public static boolean handleEasyPlaceRestriction() {
        boolean cancel = WorldUtilsAccessor.invokePlacementRestrictionInEffect();

        if (cancel) {
            MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();

            if (type == MessageOutputType.MESSAGE) {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.easy_place_fail");
            } else if (type == MessageOutputType.ACTIONBAR) {
                InfoUtils.printActionbarMessage("litematica.message.easy_place_fail");
            }
        }

        return cancel;
    }
}
