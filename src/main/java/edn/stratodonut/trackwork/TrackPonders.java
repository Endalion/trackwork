package edn.stratodonut.trackwork;

import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import edn.stratodonut.trackwork.tracks.TrackPonderScenes;

import javax.sound.midi.Track;

public class TrackPonders {
    static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(TrackworkMod.MOD_ID);

    public static final boolean REGISTER_DEBUG_SCENES = false;

    public static void register() {
        HELPER.forComponents(TrackBlocks.PHYS_TRACK, TrackBlocks.SUSPENSION_TRACK,
                        TrackBlocks.LARGE_PHYS_TRACK, TrackBlocks.LARGE_SUSPENSION_TRACK,
                        TrackBlocks.MED_PHYS_TRACK, TrackBlocks.MED_SUSPENSION_TRACK)
                .addStoryBoard("tracks", TrackPonderScenes::trackTutorial);

        HELPER.forComponents(TrackBlocks.SIMPLE_WHEEL, TrackBlocks.MED_SIMPLE_WHEEL, TrackBlocks.LARGE_SIMPLE_WHEEL)
                .addStoryBoard("wheels", TrackPonderScenes::wheelTutorial);
    }
}
