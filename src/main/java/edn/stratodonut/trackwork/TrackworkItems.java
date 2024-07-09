package edn.stratodonut.trackwork;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import edn.stratodonut.trackwork.items.ControllerResetStick;
import edn.stratodonut.trackwork.items.TrackToolkit;

import static edn.stratodonut.trackwork.TrackworkMod.REGISTRATE;

public class TrackworkItems {
    static {
        REGISTRATE.setCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB);
    }

    public static final ItemEntry<TrackToolkit> TRACK_TOOL_KIT =
            REGISTRATE.item("track_tool_kit", TrackToolkit::new)
                    .properties(p -> p.stacksTo(1))
                    .model(AssetLookup.itemModelWithPartials())
                    .register();

    public static final ItemEntry<ControllerResetStick> CONTROL_RESET_STICK =
            REGISTRATE.item("dev_reset_stick", ControllerResetStick::new)
                    .properties(p -> p.stacksTo(1))
                    .register();

    public static void register() {}
}
