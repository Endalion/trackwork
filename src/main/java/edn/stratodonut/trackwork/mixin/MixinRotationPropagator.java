package edn.stratodonut.trackwork.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class MixinRotationPropagator {
    @Inject(
            method = "getRotationSpeedModifier(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void mixinGetRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to, CallbackInfoReturnable<Float> cir) {
        final BlockState stateFrom = from.getBlockState();
        final BlockState stateTo = to.getBlockState();

        Block fromBlock = stateFrom.getBlock();
        Block toBlock = stateTo.getBlock();

        final BlockPos diff = to.getBlockPos()
                .subtract(from.getBlockPos());
        final Direction direction = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());

        if (fromBlock instanceof TrackBaseBlock && toBlock instanceof TrackBaseBlock) {
            boolean connected = TrackBaseBlock.areBlocksConnected(stateFrom, stateTo, direction) && clockworkdev2$areTracksConnected(from, to);
            cir.setReturnValue(connected ? 1F : 0);
        }
    }

    @Unique
    private static boolean clockworkdev2$areTracksConnected(KineticBlockEntity from, KineticBlockEntity to) {
        if ((from instanceof TrackBaseBlockEntity te1) && (to instanceof TrackBaseBlockEntity te2)) return !te1.isDetracked() && !te2.isDetracked();
        return false;
    }
}
