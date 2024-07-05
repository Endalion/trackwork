package edn.stratodonut.trackwork.tracks.blocks;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.Lang;
import edn.stratodonut.trackwork.TrackBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class SuspensionTrackBlock extends TrackBaseBlock<SuspensionTrackBlockEntity> {
    public static DamageSource damageSourceTrack = new DamageSource("trackwork.track");

    public static final Property<TrackVariant> WHEEL_VARIANT = EnumProperty.create("variant", TrackVariant.class);

    public enum TrackVariant implements StringRepresentable {
        WHEEL, WHEEL_ROLLER, ROLLER, BLANK;

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }
    }

    public SuspensionTrackBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, TrackPart.NONE).setValue(WHEEL_VARIANT, TrackVariant.WHEEL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(WHEEL_VARIANT));
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn,
                                          BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(handIn);

        if (AllItems.WRENCH.isIn(heldItem)) {
            if (state.hasProperty(WHEEL_VARIANT)) {
                TrackVariant old = state.getValue(WHEEL_VARIANT);
                switch (old) {
                    case WHEEL -> world.setBlockAndUpdate(pos, state.setValue(WHEEL_VARIANT, TrackVariant.BLANK));
                    default -> world.setBlockAndUpdate(pos, state.setValue(WHEEL_VARIANT, TrackVariant.WHEEL));
                }
                ;
                return InteractionResult.SUCCESS;
            }
        };
        return super.use(state, world, pos, player, handIn, hit);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public Class<SuspensionTrackBlockEntity> getBlockEntityClass() {
        return SuspensionTrackBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SuspensionTrackBlockEntity> getBlockEntityType() {
        return TrackBlockEntityTypes.SUSPENSION_TRACK.get();
    }
}

