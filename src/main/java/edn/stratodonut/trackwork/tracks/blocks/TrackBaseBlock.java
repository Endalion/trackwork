package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainGearshiftBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import edn.stratodonut.trackwork.TrackworkConfigs;
import edn.stratodonut.trackwork.tracks.ITrackPointProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public abstract class TrackBaseBlock<BE extends TrackBaseBlockEntity> extends RotatedPillarKineticBlock implements ITransformableBlock, IBE<BE> {

    public static final Property<TrackPart> PART = EnumProperty.create("part", TrackPart.class);
    public static final BooleanProperty CONNECTED_ALONG_FIRST_COORDINATE =
            DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;

    public enum TrackPart implements StringRepresentable {
        START, MIDDLE, END, NONE;

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }
    }

    public TrackBaseBlock(Properties properties) {
        super(properties);
    }

    public static boolean isValidAxis(Direction.Axis axis) {
        return !axis.isVertical();
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (TrackworkConfigs.server().enableTrackThrow.get()) {
            this.withBlockEntityDo(level, pos, be -> be.throwTrack(false));
        }

        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(PART, CONNECTED_ALONG_FIRST_COORDINATE));
    }

    // TODO: Centralised renderer parameter code
    public static void updateTrackSystem(BlockPos pos) {
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis placedAxis = context.getNearestLookingDirection()
                .getAxis();
        Direction.Axis axis = context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown() ? placedAxis : getPreferredAxis(context);
        if (axis == null)
            axis = placedAxis;
        if (axis == Direction.Axis.Y) {
            axis = Direction.Axis.X;
        }

        BlockState state = defaultBlockState().setValue(AXIS, axis);
        for (Direction facing : Iterate.directions) {
            if (facing.getAxis() == axis)
                continue;
            BlockPos pos = context.getClickedPos();
            BlockPos offset = pos.relative(facing);
            state = updateShape(state, facing, context.getLevel()
                    .getBlockState(offset), context.getLevel(), pos, offset);
        }
        return state;
    }

    @Override
    public @NotNull BlockState updateShape(BlockState stateIn, Direction face, @NotNull BlockState neighbour, @NotNull LevelAccessor worldIn,
                                           @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        TrackPart part = stateIn.getValue(PART);
        Direction.Axis axis = stateIn.getValue(AXIS);
        boolean connectionAlongFirst = stateIn.getValue(CONNECTED_ALONG_FIRST_COORDINATE);
        Direction.Axis connectionAxis =
                connectionAlongFirst ? (axis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X) : (axis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z);

        Direction.Axis faceAxis = face.getAxis();
        boolean facingAlongFirst = axis == Direction.Axis.X ? faceAxis.isVertical() : faceAxis == Direction.Axis.X;
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;

        if (axis == faceAxis)
            return stateIn;

        if (!(neighbour.getBlock() instanceof TrackBaseBlock)) {
            if (facingAlongFirst != connectionAlongFirst || part == TrackPart.NONE)
                return stateIn;
            if (part == TrackPart.MIDDLE)
                return stateIn.setValue(PART, positive ? TrackPart.END : TrackPart.START);
            if ((part == TrackPart.START) == positive)
                return stateIn.setValue(PART, TrackPart.NONE);
            return stateIn;
        }

        TrackPart otherPart = neighbour.getValue(PART);
        Direction.Axis otherAxis = neighbour.getValue(AXIS);
        boolean otherConnection = neighbour.getValue(CONNECTED_ALONG_FIRST_COORDINATE);
        Direction.Axis otherConnectionAxis =
                otherConnection ? (otherAxis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X) : (otherAxis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z);

        if (neighbour.getValue(AXIS) == faceAxis)
            return stateIn;
        if (otherPart != TrackPart.NONE && otherConnectionAxis != faceAxis)
            return stateIn;

        if (part == TrackPart.NONE) {
            part = positive ? TrackPart.START : TrackPart.END;
            connectionAlongFirst = axis == Direction.Axis.X ? faceAxis.isVertical() : faceAxis == Direction.Axis.X;
        } else if (connectionAxis != faceAxis) {
            return stateIn;
        }

        if ((part == TrackPart.START) != positive)
            part = TrackPart.MIDDLE;

        return stateIn.setValue(PART, part)
                .setValue(CONNECTED_ALONG_FIRST_COORDINATE, connectionAlongFirst);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if (originalState.getValue(PART) == TrackPart.NONE)
            return super.getRotatedBlockState(originalState, targetedFace);
        return super.getRotatedBlockState(originalState,
                Direction.get(Direction.AxisDirection.POSITIVE, getConnectionAxis(originalState)));
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    public static boolean areBlocksConnected(BlockState state, BlockState other, Direction facing) {
        TrackPart part = state.getValue(PART);
        Direction.Axis connectionAxis = getConnectionAxis(state);
        Direction.Axis otherConnectionAxis = getConnectionAxis(other);

        if (otherConnectionAxis != connectionAxis)
            return false;
        if (facing.getAxis() != connectionAxis)
            return false;
        if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE && (part == TrackPart.MIDDLE || part == TrackPart.START))
            return true;
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE && (part == TrackPart.MIDDLE || part == TrackPart.END))
            return true;

        return false;
    }

    protected static Direction.Axis getConnectionAxis(BlockState state) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean connectionAlongFirst = state.getValue(CONNECTED_ALONG_FIRST_COORDINATE);
        return connectionAlongFirst ? (axis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X) : (axis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z);
    }

    public static float getRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to) {
        float fromMod = 1;
        float toMod = 1;
        if (from instanceof ITrackPointProvider)
            fromMod = ((ITrackPointProvider) from).getWheelRadius();
        if (to instanceof ITrackPointProvider)
            toMod = ((ITrackPointProvider) to).getWheelRadius();
        return fromMod / toMod;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return rotate(state, rot, Direction.Axis.Y);
    }

    protected BlockState rotate(BlockState pState, Rotation rot, Direction.Axis rotAxis) {
        Direction.Axis connectionAxis = getConnectionAxis(pState);
        Direction direction = Direction.fromAxisAndDirection(connectionAxis, Direction.AxisDirection.POSITIVE);
        Direction normal = Direction.fromAxisAndDirection(pState.getValue(AXIS), Direction.AxisDirection.POSITIVE);
        for (int i = 0; i < rot.ordinal(); i++) {
            direction = direction.getClockWise(rotAxis);
            normal = normal.getClockWise(rotAxis);
        }

        if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            pState = reversePart(pState);

        Direction.Axis newAxis = normal.getAxis();
        Direction.Axis newConnectingDirection = direction.getAxis();
        boolean alongFirst = newAxis == Direction.Axis.X && newConnectingDirection == Direction.Axis.Y
                || newAxis != Direction.Axis.X && newConnectingDirection == Direction.Axis.X;

        return pState.setValue(AXIS, newAxis)
                .setValue(CONNECTED_ALONG_FIRST_COORDINATE, alongFirst);
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState pState, Mirror pMirror) {
        Direction.Axis connectionAxis = getConnectionAxis(pState);
        if (pMirror.mirror(Direction.fromAxisAndDirection(connectionAxis, Direction.AxisDirection.POSITIVE))
                .getAxisDirection() == Direction.AxisDirection.POSITIVE)
            return pState;
        return reversePart(pState);
    }

    protected BlockState reversePart(BlockState pState) {
        TrackPart part = pState.getValue(PART);
        if (part == TrackPart.START)
            return pState.setValue(PART, TrackPart.END);
        if (part == TrackPart.END)
            return pState.setValue(PART, TrackPart.START);
        return pState;
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        return rotate(mirror(state, transform.mirror), transform.rotation, transform.rotationAxis);
    }
}

