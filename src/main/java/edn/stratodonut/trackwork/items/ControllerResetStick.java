package edn.stratodonut.trackwork.items;

import edn.stratodonut.trackwork.tracks.forces.PhysEntityTrackController;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class ControllerResetStick extends Item {
    public ControllerResetStick(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.mayBuild())
            return super.useOn(context);

        Level level = context.getLevel();
        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, context.getClickedPos());
        if (ship == null) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            PhysEntityTrackController controller = PhysEntityTrackController.getOrCreate((ServerShip) ship);
            controller.resetController();
            MutableComponent chatMessage = MutableComponent.create(ComponentContents.EMPTY);
            player.displayClientMessage(chatMessage.append("Fix! "), true);
        }

        return InteractionResult.SUCCESS;
    }
}
