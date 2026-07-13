package matejstastny.servermatica.server;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;

import matejstastny.servermatica.network.InitEasyPlaceProtocolPacket;
import matejstastny.servermatica.network.SetEasyPlaceProtocolPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.enums.Orientation;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class EasyPlaceProtocolServer {
    private static final Logger LOGGER = LogUtils.getLogger();

    // See PlacementHandler.WHITELISTED_PROPERTIES
    private static final ImmutableSet<Property<?>> WHITELISTED_PROPERTIES = ImmutableSet.of(
        // BooleanProperty: INVERTED, OPEN, PERSISTENT
        Properties.INVERTED,
        Properties.OPEN,
        Properties.PERSISTENT,
        // EnumProperty: AXIS, BLOCK_HALF, CHEST_TYPE, COMPARATOR_MODE, DOOR_HINGE, SLAB_TYPE, STAIR_SHAPE, BLOCK_FACE
        Properties.AXIS,
        Properties.BLOCK_HALF,
        Properties.CHEST_TYPE,
        Properties.COMPARATOR_MODE,
        Properties.DOOR_HINGE,
        Properties.SLAB_TYPE,
        Properties.STAIR_SHAPE,
        Properties.BLOCK_FACE,
        // IntProperty: BITES, DELAY, NOTE, ROTATION (Banner/Sign/Skull)
        Properties.BITES,
        Properties.DELAY,
        Properties.NOTE,
        Properties.ROTATION,
        // EnumProperty<Orientation>: ORIENTATION (Crafter)
        Properties.ORIENTATION
    );

    private EasyPlaceProtocolServer() {
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(InitEasyPlaceProtocolPacket.ID, InitEasyPlaceProtocolPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SetEasyPlaceProtocolPacket.ID, SetEasyPlaceProtocolPacket.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, InitEasyPlaceProtocolPacket.ID)) {
                ServerPlayNetworking.send(handler.player, new InitEasyPlaceProtocolPacket(WHITELISTED_PROPERTIES));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(SetEasyPlaceProtocolPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                LOGGER.info("Player {} is using easy place protocol {}", context.player().getNameForScoreboard(), payload.protocol());
                ((NetworkHandlerExt) context.player().networkHandler).servermatica_setEasyPlaceProtocol(payload.protocol());
            });
        });
    }

    public static int getEasyPlaceProtocol(ServerPlayerEntity player) {
        return ((NetworkHandlerExt) player.networkHandler).servermatica_getEasyPlaceProtocol();
    }

    public static <T extends Comparable<T>> BlockState applyEasyPlaceProtocolV3(BlockState state, ItemPlacementContext context) {
        int protocolValue = (int) (context.getHitPos().x - (double) context.getBlockPos().getX()) - 2;

        if (protocolValue < 0) {
            return state;
        }

        @Nullable EnumProperty<Direction> property = getFirstDirectionProperty(state);

        // Direction property - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property != null && property != Properties.VERTICAL_DIRECTION) {
            state = applyDirectionProperty(state, context, property, protocolValue);

            if (state == null) {
                return null;
            }

            protocolValue >>>= 3;
        }

        protocolValue >>>= 1;

        List<Property<?>> propList = new ArrayList<>(state.getBlock().getStateManager().getProperties());
        propList.sort(Comparator.comparing(Property::getName));

        try {
            for (Property<?> p : propList) {
                if (!isDirectionProperty(p) && WHITELISTED_PROPERTIES.contains(p)) {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.floorLog2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;

                    if (valueIndex < list.size()) {
                        T value = list.get(valueIndex);

                        if (!state.get(prop).equals(value) && allowPropertyValueThroughProtocol(value)) {
                            state = state.with(prop, value);
                        }

                        protocolValue >>>= requiredBits;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Exception trying to apply placement protocol value", e);
        }

        return state;
    }

    private static BlockState applyDirectionProperty(BlockState state, ItemPlacementContext context,
                                                     EnumProperty<Direction> property, int protocolValue) {
        Direction facingOrig = state.get(property);
        Direction facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0xF) >> 1;

        if (decodedFacingIndex == 6) {
            facing = facing.getOpposite();
        } else if (decodedFacingIndex <= 5) {
            facing = Direction.byIndex(decodedFacingIndex);

            if (!property.getValues().contains(facing)) {
                facing = context.getHorizontalPlayerFacing().getOpposite();
            }
        }

        if (facing != facingOrig && property.getValues().contains(facing)) {
            if (state.getBlock() instanceof BedBlock) {
                BlockPos headPos = context.getBlockPos().offset(facing);

                if (!context.getWorld().getBlockState(headPos).canReplace(context)) {
                    return null;
                }
            }

            state = state.with(property, facing);
        }

        return state;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static EnumProperty<Direction> getFirstDirectionProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (isDirectionProperty(prop)) {
                return (EnumProperty<Direction>) prop;
            }
        }
        return null;
    }

    private static boolean isDirectionProperty(Property<?> prop) {
        if (!(prop instanceof EnumProperty<?>)) {
            return false;
        }
        Collection<?> values = prop.getValues();
        return !values.isEmpty() && values.iterator().next() instanceof Direction;
    }

    private static boolean allowPropertyValueThroughProtocol(Comparable<?> value) {
        // don't allow duping slabs by forcing a double slab via the protocol
        return value != SlabType.DOUBLE;
    }

    public interface NetworkHandlerExt {
        int servermatica_getEasyPlaceProtocol();
        void servermatica_setEasyPlaceProtocol(int easyPlaceProtocol);
    }
}
