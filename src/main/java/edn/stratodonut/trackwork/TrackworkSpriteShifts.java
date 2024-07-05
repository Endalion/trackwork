package edn.stratodonut.trackwork;

import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.block.render.SpriteShifter;

public class TrackworkSpriteShifts {
    public static final SpriteShiftEntry BELT = get("block/belt", "block/belt_scroll");

    private static SpriteShiftEntry get(String originalLocation, String targetLocation) {
        return SpriteShifter.get(TrackworkMod.getResource(originalLocation), TrackworkMod.getResource(targetLocation));
    }

    public static void init() {}
}
