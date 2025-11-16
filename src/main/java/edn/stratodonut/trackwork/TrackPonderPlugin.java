package edn.stratodonut.trackwork;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TrackPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return TrackworkMod.MOD_ID;
    }
    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        TrackPonders.register(helper);
    }

    public static void registerPlugin() {
        PonderIndex.addPlugin(new TrackPonderPlugin());
    }
}
