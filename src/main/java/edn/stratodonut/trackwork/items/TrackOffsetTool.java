package edn.stratodonut.trackwork.items;

import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class TrackOffsetTool extends Item {
    public TrackOffsetTool(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, @NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.mayBuild())
            return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof SuspensionTrackBlockEntity se) {
            Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, context.getClickedPos());
            if (ship == null) return InteractionResult.FAIL;
            se.setHorizontalOffset(VectorConversionsMCKt.toJOML(context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()))));

            return InteractionResult.SUCCESS;
        }

        return this.use(level, player, context.getHand()).getResult();
    }
}
