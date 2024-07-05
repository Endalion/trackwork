package edn.stratodonut.trackwork;

import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import edn.stratodonut.trackwork.blocks.TrackAdjusterBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.PhysEntityTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.SuspensionTrackBlockEntity;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlockEntity;
import edn.stratodonut.trackwork.tracks.render.PhysEntityTrackRenderer;
import edn.stratodonut.trackwork.tracks.render.SimpleWheelRenderer;
import edn.stratodonut.trackwork.tracks.render.SuspensionRenderer;

import static edn.stratodonut.trackwork.TrackworkMod.REGISTRATE;

public class TrackBlockEntityTypes {
    // TODO: Instance shaders (I obviously don't know glsl :clueless:)

    public static final BlockEntityEntry<SuspensionTrackBlockEntity> LARGE_SUSPENSION_TRACK = REGISTRATE
            .blockEntity("large_suspension_track", SuspensionTrackBlockEntity::large)
//            .instance(() -> SuspensionInstance::new, false)
            .validBlocks(TrackBlocks.LARGE_SUSPENSION_TRACK)
            .renderer(() -> SuspensionRenderer::new)
            .register();
    public static final BlockEntityEntry<SuspensionTrackBlockEntity> MED_SUSPENSION_TRACK = REGISTRATE
            .blockEntity("med_suspension_track", SuspensionTrackBlockEntity::med)
//            .instance(() -> SuspensionInstance::new, false)
            .validBlocks(TrackBlocks.MED_SUSPENSION_TRACK)
            .renderer(() -> SuspensionRenderer::new)
            .register();
    public static final BlockEntityEntry<SuspensionTrackBlockEntity> SUSPENSION_TRACK = REGISTRATE
            .blockEntity("suspension_track", SuspensionTrackBlockEntity::new)
//            .instance(() -> SuspensionInstance::new, false)
            .validBlocks(TrackBlocks.SUSPENSION_TRACK)
            .renderer(() -> SuspensionRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysEntityTrackBlockEntity> LARGE_PHYS_TRACK = REGISTRATE
            .blockEntity("large_phys_track", PhysEntityTrackBlockEntity::large)
//            .instance(() -> PhysEntityTrackInstance::new, false)
            .validBlocks(TrackBlocks.LARGE_PHYS_TRACK)
            .renderer(() -> PhysEntityTrackRenderer::new)
            .register();
    public static final BlockEntityEntry<PhysEntityTrackBlockEntity> MED_PHYS_TRACK = REGISTRATE
            .blockEntity("med_phys_track", PhysEntityTrackBlockEntity::med)
//            .instance(() -> PhysEntityTrackInstance::new, false)
            .validBlocks(TrackBlocks.MED_PHYS_TRACK)
            .renderer(() -> PhysEntityTrackRenderer::new)
            .register();
    public static final BlockEntityEntry<PhysEntityTrackBlockEntity> PHYS_TRACK = REGISTRATE
            .blockEntity("phys_track", PhysEntityTrackBlockEntity::new)
//            .instance(() -> PhysEntityTrackInstance::new, false)
            .validBlocks(TrackBlocks.PHYS_TRACK)
            .renderer(() -> PhysEntityTrackRenderer::new)
            .register();

    public static final BlockEntityEntry<WheelBlockEntity> SIMPLE_WHEEL = REGISTRATE
            .blockEntity("simple_wheel", WheelBlockEntity::new)
//            .instance(() -> PhysEntityTrackInstance::new, false)
            .validBlocks(TrackBlocks.SIMPLE_WHEEL)
            .renderer(() -> SimpleWheelRenderer::new)
            .register();

    public static final BlockEntityEntry<TrackAdjusterBlockEntity> TRACK_LEVEL_CONTROLLER = REGISTRATE
            .blockEntity("track_level_controller", TrackAdjusterBlockEntity::new)
            .instance(() -> ShaftInstance::new)
            .validBlocks(TrackBlocks.TRACK_LEVEL_CONTROLLER)
            .renderer(() -> ShaftRenderer::new)
            .register();

    public static void register() {}
}
