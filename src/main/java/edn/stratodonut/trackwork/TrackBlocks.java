package edn.stratodonut.trackwork;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveGenerator;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import edn.stratodonut.trackwork.blocks.TrackAdjusterBlock;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlock;
import edn.stratodonut.trackwork.tracks.blocks.variants.LargePhysEntityTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.variants.LargeSuspensionTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.variants.MedPhysEntityTrackBlock;
import edn.stratodonut.trackwork.tracks.blocks.variants.MedSuspensionTrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static edn.stratodonut.trackwork.TrackworkMod.REGISTRATE;

public class TrackBlocks {
    static {
        REGISTRATE.setCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB);
    }

    // TODO: More Freeform Rigid tracks (like a massive wall of blocks)
    // TODO: Suspension model?

    public static final BlockEntry<LargeSuspensionTrackBlock> LARGE_SUSPENSION_TRACK =
            REGISTRATE.block("large_suspension_track", LargeSuspensionTrackBlock::new)
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(12.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
                    .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) -> p.models()
                            .getExistingFile(p.modLoc("block/" + c.getName() + "/" + suffix))).generate(c, p))
                    .item()
                    .transform(customItemModel())
                    .register();
    public static final BlockEntry<MedSuspensionTrackBlock> MED_SUSPENSION_TRACK =
            REGISTRATE.block("med_suspension_track", MedSuspensionTrackBlock::new)
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(12.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
//                    .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) -> p.models()
//                            .getExistingFile(p.modLoc("block/" + c.getName() + "/" + suffix))).generate(c, p))
                    .item()
                    .transform(customItemModel())
                    .register();
    public static final BlockEntry<SuspensionTrackBlock> SUSPENSION_TRACK =
            REGISTRATE.block("suspension_track", SuspensionTrackBlock::new)
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(12.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
                    .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) -> p.models()
                            .getExistingFile(p.modLoc("block/" + c.getName() + "/" + suffix))).generate(c, p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<LargePhysEntityTrackBlock> LARGE_PHYS_TRACK =
            REGISTRATE.block("large_phys_track", LargePhysEntityTrackBlock::new)
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(14.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
                    .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) -> p.models()
                            .getExistingFile(p.modLoc("block/" + c.getName() + "/" + suffix))).generate(c, p))
                    .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
                    .item()
                    .transform(customItemModel())
                    .register();
    public static final BlockEntry<MedPhysEntityTrackBlock> MED_PHYS_TRACK =
            REGISTRATE.block("med_phys_track", MedPhysEntityTrackBlock::new)
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(14.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
//                    .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) -> p.models()
//                            .getExistingFile(p.modLoc("block/" + c.getName() + "/" + suffix))).generate(c, p))
                    .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
                    .item()
                    .transform(customItemModel())
                    .register();
    public static final BlockEntry<PhysEntityTrackBlock> PHYS_TRACK =
            REGISTRATE.block("phys_track", PhysEntityTrackBlock::new)
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(14.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
                    .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) -> p.models()
                            .getExistingFile(p.modLoc("block/" + c.getName() + "/" + suffix))).generate(c, p))
                    .onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<WheelBlock> SIMPLE_WHEEL =
            REGISTRATE.block("simple_wheel", WheelBlock::new)
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(7.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
                    .blockstate(BlockStateGen.horizontalBlockProvider(true))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<WheelBlock> MED_SIMPLE_WHEEL =
            REGISTRATE.block("med_simple_wheel", p -> new WheelBlock(p, TrackBlockEntityTypes.MED_SIMPLE_WHEEL))
                    .initialProperties(() -> Blocks.RAIL)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).noCollission().strength(7.0f).sound(SoundType.METAL))
                    .transform(pickaxeOnly())
                    .blockstate(BlockStateGen.horizontalBlockProvider(true))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<? extends RotatedPillarBlock> SIMPLE_WHEEL_PART = wheelpart("simple_wheel_part");
    public static final BlockEntry<? extends RotatedPillarBlock> MED_SIMPLE_WHEEL_PART = wheelpart("med_simple_wheel_part");

    public static final BlockEntry<TrackAdjusterBlock> TRACK_LEVEL_CONTROLLER =
            REGISTRATE.block("track_level_controller", TrackAdjusterBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
                    .transform(axeOrPickaxe())
                    .blockstate(BlockStateGen.axisBlockProvider(true))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static BlockEntry<? extends RotatedPillarBlock> wheelpart(String name) {
        return REGISTRATE.block(name, (properties) -> new RotatedPillarBlock(properties) {
                    @Override
                    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
                        return AllShapes.CRUSHING_WHEEL_COLLISION_SHAPE;
                    }
                })
                .initialProperties(() -> Blocks.WHITE_WOOL)
                .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL).strength(2.0f, 7.0f).sound(SoundType.WOOL))
                .item()
                .transform(customItemModel())
                .register();
    }
    
    public static void register() {}
}
