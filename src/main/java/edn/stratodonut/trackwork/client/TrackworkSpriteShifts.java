package edn.stratodonut.trackwork.client;

import edn.stratodonut.trackwork.TrackworkMod;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;

public class TrackworkSpriteShifts {
    public static final SpriteShiftEntry BELT = get("block/belt", "block/belt_scroll");

    private static SpriteShiftEntry get(String originalLocation, String targetLocation) {
        return SpriteShifter.get(TrackworkMod.getResource(originalLocation), TrackworkMod.getResource(targetLocation));
    }

    public static void init() {}
}
