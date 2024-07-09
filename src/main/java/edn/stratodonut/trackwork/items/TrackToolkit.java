package edn.stratodonut.trackwork.items;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import edn.stratodonut.trackwork.TrackSounds;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.TrackBaseBlock;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlock;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlockEntity;
import edn.stratodonut.trackwork.tracks.forces.PhysicsTrackController;
import edn.stratodonut.trackwork.tracks.forces.SimpleWheelController;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.function.Consumer;

public class TrackToolkit extends Item {
    public enum TOOL implements StringRepresentable {
        STIFFNESS,
        OFFSET;

        private static final TOOL[] vals = values();

        public static TOOL from(int i) {
            return vals[i];
        }

        public static int next(int i) {
            return (i + 1) % vals.length;
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }
    }

    public TrackToolkit(Properties properties) {
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

        CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.contains("Tool")) {
            TrackToolkit.TOOL type = TrackToolkit.TOOL.from(nbt.getInt("Tool"));

            switch (type) {
                case OFFSET -> {
                    BlockEntity be = level.getBlockEntity(pos);

                    AllSoundEvents.WRENCH_ROTATE.playOnServer(player.level(), pos, 1, player.getRandom().nextFloat() + .5f);
//                    player.playSound(;, 1.0f, 0.8f + 0.2f * player.getRandom().nextFloat());

                    if (be instanceof SuspensionTrackBlockEntity se) {
                        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, context.getClickedPos());
                        if (ship == null) return InteractionResult.FAIL;
                        se.setHorizontalOffset(VectorConversionsMCKt.toJOML(context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()))));

                        return InteractionResult.SUCCESS;
                    } else if (be instanceof WheelBlockEntity wbe) {
                        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, context.getClickedPos());
                        if (ship == null) return InteractionResult.FAIL;
                        wbe.setHorizontalOffset(VectorConversionsMCKt.toJOML(context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()))));

                        return InteractionResult.SUCCESS;
                    }
                }
                default -> {
                    Block hitBlock = level.getBlockState(pos).getBlock();

                    player.playSound(TrackSounds.SPRING_TOOL.get(), 1.0f, 0.8f + 0.4f * player.getRandom().nextFloat());

                    boolean isSneaking = player.isShiftKeyDown();
                    if (hitBlock instanceof TrackBaseBlock<?>) {
                        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, context.getClickedPos());
                        if (ship == null) return InteractionResult.FAIL;
                        if (!level.isClientSide) {
                            PhysicsTrackController controller = PhysicsTrackController.getOrCreate((ServerShip) ship);
                            float result = controller.setDamperCoefficient(isSneaking ? -1f : 1f);

                            MutableComponent chatMessage = Lang.text("Adjusted suspension stiffness to ")
                                    .add(Components.literal(String.format("%.2fx", result))).component();

                            player.createCommandSourceStack().sendChatMessage(new OutgoingChatMessage.Disguised(chatMessage), false, ChatType.bind(ChatType.CHAT, player));
                        }
                        return InteractionResult.SUCCESS;

                    } else if (hitBlock instanceof WheelBlock) {
                        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, context.getClickedPos());
                        if (ship == null) return InteractionResult.FAIL;
                        if (!level.isClientSide) {
                            SimpleWheelController controller = SimpleWheelController.getOrCreate((ServerShip) ship);
                            float result = controller.setDamperCoefficient(isSneaking ? -1f : 1f);

                            MutableComponent chatMessage = Lang.text("Adjusted suspension stiffness to ")
                                    .add(Components.literal(String.format("%.2fx", result))).component();

                            player.createCommandSourceStack().sendChatMessage(new OutgoingChatMessage.Disguised(chatMessage), false, ChatType.bind(ChatType.CHAT, player));
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        return this.use(level, player, context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) nextMode(stack);
            player.getCooldowns()
                    .addCooldown(this, 2);
        }

        return InteractionResultHolder.pass(stack);
    }

    private void nextMode(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();

        if (!nbt.contains("Tool")) {
            nbt.putInt("Tool", 0);
        } else {
            nbt.putInt("Tool", TOOL.next(nbt.getInt("Tool")));
        }
        stack.setTag(nbt);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new TrackToolkitRenderer()));
    }
}
