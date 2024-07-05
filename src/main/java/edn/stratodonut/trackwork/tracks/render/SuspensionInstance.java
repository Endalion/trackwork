package edn.stratodonut.trackwork.tracks.render;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.core.materials.FlatLit;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import edn.stratodonut.trackwork.TrackworkPartialModels;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class SuspensionInstance extends SingleRotatingInstance<SuspensionTrackBlockEntity> implements DynamicInstance {
    private final ModelData wheels;
    final Direction.Axis axis;
    final Direction.Axis rotationAxis;
    final float rotationMult;
    final BlockPos visualPos;

    private float lastAngle = Float.NaN;

    public SuspensionInstance(MaterialManager materialManager, SuspensionTrackBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        wheels = getTransformMaterial()
                .getModel(TrackworkPartialModels.SUSPENSION_WHEEL, blockState)
                .createInstance();

        axis = blockState.getValue(SuspensionTrackBlock.AXIS);
//        alongFirst = blockState.getValue(SuspensionTrackBlock.AXIS_ALONG_FIRST_COORDINATE);
        rotationAxis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);

        if (axis == Direction.Axis.X)
            rotationMult = -1;
        else {
            rotationMult = 1;
        }

        visualPos = blockEntity.getBlockPos();

        animateWheels(getCogAngle());
    }

    @Override
    public void beginFrame() {
        float wheelAngle = getCogAngle();

        if (Mth.equal(wheelAngle, lastAngle)) return;

        animateWheels(wheelAngle);
    }

    private float getCogAngle() {
        return SuspensionRenderer.getAngleForBE(blockEntity, visualPos, rotationAxis) * rotationMult;
    }

    private void animateWheels(float wheelAngle) {
        wheels.loadIdentity()
                .translate(getInstancePosition())
                .centre()
                .rotateY(axis == Direction.Axis.X ? 0 : 90)
                .translate(0, -blockEntity.getWheelTravel(), 0)
                .rotateX(-wheelAngle * rotationMult)
                .translate(0, 9 / 16f, 0)
                .unCentre();
    }

    @Override
    public void updateLight() {
        relight(pos, wheels, rotatingModel);
    }

    @Override
    public void remove() {
        rotatingModel.delete();
        wheels.delete();
    }
}
