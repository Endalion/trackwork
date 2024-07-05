package edn.stratodonut.trackwork;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.infrastructure.item.BaseCreativeModeTab;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class TrackworkCreativeTabs {
    public static final CreativeModeTab TRACKWORK_CREATIVE_TAB = new CreativeModeTab(TrackworkMod.MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return AllBlocks.BELT.asStack();
        }
    };

    public static void init() {

    }
}
