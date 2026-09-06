package matejstastny.servermatica.server;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;

import matejstastny.servermatica.network.InitEasyPlaceProtocolPacket;
import matejstastny.servermatica.network.SetEasyPlaceProtocolPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
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
        BlockStateProperties.INVERTED,
        BlockStateProperties.OPEN,
        BlockStateProperties.PERSISTENT,
        // EnumProperty: AXIS, HALF, CHEST_TYPE, MODE_COMPARATOR, DOOR_HINGE, SLAB_TYPE, STAIRS_SHAPE, ATTACH_FACE
        BlockStateProperties.AXIS,
        BlockStateProperties.HALF,
        BlockStateProperties.CHEST_TYPE,
        BlockStateProperties.MODE_COMPARATOR,
        BlockStateProperties.DOOR_HINGE,
        BlockStateProperties.SLAB_TYPE,
        BlockStateProperties.STAIRS_SHAPE,
        BlockStateProperties.ATTACH_FACE,
        // IntegerProperty: BITES, DELAY, NOTE, ROTATION_16 (Banner/Sign/Skull)
        BlockStateProperties.BITES,
        BlockStateProperties.DELAY,
        BlockStateProperties.NOTE,
        BlockStateProperties.ROTATION_16,
        // EnumProperty<FrontAndTop>: ORIENTATION (Crafter)
        BlockStateProperties.ORIENTATION
    );

    private EasyPlaceProtocolServer() {
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(InitEasyPlaceProtocolPacket.ID, InitEasyPlaceProtocolPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetEasyPlaceProtocolPacket.ID, SetEasyPlaceProtocolPacket.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, InitEasyPlaceProtocolPacket.ID)) {
                ServerPlayNetworking.send(handler.player, new InitEasyPlaceProtocolPacket(WHITELISTED_PROPERTIES));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(SetEasyPlaceProtocolPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                LOGGER.info("Player {} is using easy place protocol {}", context.player().getScoreboardName(), payload.protocol());
                ((NetworkHandlerExt) context.player().connection).servermatica_setEasyPlaceProtocol(payload.protocol());
            });
        });
    }

    public static int getEasyPlaceProtocol(ServerPlayer player) {
        return ((NetworkHandlerExt) player.connection).servermatica_getEasyPlaceProtocol();
    }

    public static <T extends Comparable<T>> BlockState applyEasyPlaceProtocolV3(BlockState state, BlockPlaceContext context) {
        int protocolValue = (int) (context.getClickLocation().x - (double) context.getClickedPos().getX()) - 2;

        if (protocolValue < 0) {
            return state;
        }

        @Nullable EnumProperty<Direction> property = getFirstDirectionProperty(state);

        // Direction property - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property != null && property != BlockStateProperties.VERTICAL_DIRECTION) {
            state = applyDirectionProperty(state, context, property, protocolValue);

            if (state == null) {
                return null;
            }

            protocolValue >>>= 3;
        }

        protocolValue >>>= 1;

        List<Property<?>> propList = new ArrayList<>(state.getBlock().getStateDefinition().getProperties());
        propList.sort(Comparator.comparing(Property::getName));

        try {
            for (Property<?> p : propList) {
                if (!isDirectionProperty(p) && WHITELISTED_PROPERTIES.contains(p)) {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getPossibleValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = Mth.log2(Mth.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;

                    if (valueIndex < list.size()) {
                        T value = list.get(valueIndex);

                        if (!state.getValue(prop).equals(value) && allowPropertyValueThroughProtocol(value)) {
                            state = state.setValue(prop, value);
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

    private static BlockState applyDirectionProperty(BlockState state, BlockPlaceContext context,
                                                     EnumProperty<Direction> property, int protocolValue) {
        Direction facingOrig = state.getValue(property);
        Direction facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0xF) >> 1;

        if (decodedFacingIndex == 6) {
            facing = facing.getOpposite();
        } else if (decodedFacingIndex <= 5) {
            facing = Direction.from3DDataValue(decodedFacingIndex);

            if (!property.getPossibleValues().contains(facing)) {
                facing = context.getHorizontalDirection().getOpposite();
            }
        }

        if (facing != facingOrig && property.getPossibleValues().contains(facing)) {
            if (state.getBlock() instanceof BedBlock) {
                BlockPos headPos = context.getClickedPos().relative(facing);

                if (!context.getLevel().getBlockState(headPos).canBeReplaced(context)) {
                    return null;
                }
            }

            state = state.setValue(property, facing);
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
        Collection<?> values = prop.getPossibleValues();
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
