package edn.stratodonut.trackwork;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import edn.stratodonut.trackwork.tracks.TrackPonderScenes;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class TrackPonders {

    public static final boolean REGISTER_DEBUG_SCENES = false;

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);


        HELPER.addStoryBoard(TrackBlocks.PHYS_TRACK, "tracks", TrackPonderScenes::trackTutorial);
        HELPER.forComponents(TrackBlocks.PHYS_TRACK, TrackBlocks.SUSPENSION_TRACK,
                        TrackBlocks.LARGE_PHYS_TRACK, TrackBlocks.LARGE_SUSPENSION_TRACK,
                        TrackBlocks.MED_PHYS_TRACK, TrackBlocks.MED_SUSPENSION_TRACK)
                .addStoryBoard("tracks", TrackPonderScenes::trackTutorial);

        HELPER.forComponents(TrackBlocks.SIMPLE_WHEEL, TrackBlocks.MED_SIMPLE_WHEEL)
                .addStoryBoard("wheels", TrackPonderScenes::wheelTutorial);
    }


}
