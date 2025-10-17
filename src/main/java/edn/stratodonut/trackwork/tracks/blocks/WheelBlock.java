package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Lang;
import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import edn.stratodonut.trackwork.TrackworkConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class WheelBlock extends HorizontalKineticBlock implements IBE<WheelBlockEntity> {
    public static final Property<VisualVariant> VISUAL_VARIANT = EnumProperty.create("variant", VisualVariant.class);

    public enum VisualVariant implements StringRepresentable {
        DEFAULT,
        NO_SPRING,
        NO_LOWER_RIB;

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }
    }
    
    @Nonnull
    private final Supplier<BlockEntityType<WheelBlockEntity>> wbet;
    
    public WheelBlock(Properties properties) {
        super(properties);
        this.wbet = TrackBlockEntityTypes.SIMPLE_WHEEL;
    }

    public WheelBlock(Properties properties, Supplier<BlockEntityType<WheelBlockEntity>> wbet) {
        super(properties);
        this.wbet = wbet;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(VISUAL_VARIANT));
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn,
                                          BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(handIn);

        if (AllItems.WRENCH.isIn(heldItem)) {
            if (state.hasProperty(VISUAL_VARIANT)) {
                VisualVariant old = state.getValue(VISUAL_VARIANT);
                VisualVariant newVariant;
                switch (old) {
                    case DEFAULT -> newVariant = VisualVariant.NO_SPRING;
                    case NO_SPRING -> newVariant = VisualVariant.NO_LOWER_RIB;
                    case NO_LOWER_RIB -> newVariant = VisualVariant.DEFAULT;
                    default -> newVariant = VisualVariant.DEFAULT;
                }
                world.setBlockAndUpdate(pos, state.setValue(VISUAL_VARIANT, newVariant));
                return InteractionResult.SUCCESS;
            }
        };
        return super.use(state, world, pos, player, handIn, hit);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredHorizontalFacing(context);
        if ((context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown()) || preferred == null)
            return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
        return defaultBlockState().setValue(HORIZONTAL_FACING, preferred);
    }

    public static boolean isValid(Direction facing) {
        return !facing.getAxis().isVertical();
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        Direction wheelFacing = state.getValue(HORIZONTAL_FACING);
        
        // Always allow connection from the front
        if (face == wheelFacing) {
            return true;
        }
        
        // Check config setting for RPM passthrough
        if (!TrackworkConfigs.server().wheelRPMPassthrough.get()) {
            return false;
        }
        
        // Only allow pass-through (back connection) if there's a kinetic block connected to the front
        if (face == wheelFacing.getOpposite()) {
            BlockPos frontPos = pos.relative(wheelFacing);
            BlockState frontState = world.getBlockState(frontPos);
            if (frontState.getBlock() instanceof KineticBlock ke) {
                return ke.hasShaftTowards(world, frontPos, frontState, wheelFacing.getOpposite());
            }
        }
        
        return false;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<WheelBlockEntity> getBlockEntityClass() {
        return WheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<WheelBlockEntity> getBlockEntityType() {
        return wbet.get();
    }
}
