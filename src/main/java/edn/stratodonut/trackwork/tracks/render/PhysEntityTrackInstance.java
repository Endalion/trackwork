package edn.stratodonut.trackwork.tracks.render;

import edn.stratodonut.trackwork.TrackworkPartialModels;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlock;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class PhysEntityTrackInstance extends ShaftInstance<PhysEntityTrackBlockEntity> implements DynamicInstance {

    private final ModelData gantryCogs;
    final Direction.Axis axis;
    final Direction.Axis rotationAxis;
    final float rotationMult;
    final BlockPos visualPos;
    private float lastAngle = Float.NaN;

    public PhysEntityTrackInstance(MaterialManager materialManager, PhysEntityTrackBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        gantryCogs = getTransformMaterial()
                .getModel(TrackworkPartialModels.COGS, blockState)
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

        animateCogs(getCogAngle());
    }

    @Override
    public void beginFrame() {
        float cogAngle = getCogAngle();

        if (Mth.equal(cogAngle, lastAngle)) return;

        animateCogs(cogAngle);
    }

    private float getCogAngle() {
        return SuspensionRenderer.getAngleForBE(blockEntity, visualPos, rotationAxis) * rotationMult;
    }

    private void animateCogs(float cogAngle) {
        gantryCogs.loadIdentity()
                .translate(getInstancePosition())
                .centre()
                .rotateY(axis == Direction.Axis.X ? 0 : 90)
                .rotateX(-cogAngle * rotationMult)
                .translate(0, 9 / 16f, 0)
                .unCentre();
    }

    @Override
    public void updateLight() {
        relight(pos, gantryCogs, rotatingModel);
    }

    @Override
    public void remove() {
        super.remove();
        gantryCogs.delete();
    }
}
