package edn.stratodonut.trackwork;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.utility.Components;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import static edn.stratodonut.trackwork.TrackworkMod.REGISTRATE;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TrackCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrackworkMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BASE_CREATIVE_TAB = REGISTER.register("base",
            () -> CreativeModeTab.builder()
                    .title(Components.translatable("itemGroup.trackwork"))
                    .icon(AllBlocks.BELT::asStack)
                    .displayItems((displayParams, output) -> {
                        for (RegistryEntry<Block> entry : REGISTRATE.getAll(Registries.BLOCK)) {
                            if (CreateRegistrate.isInCreativeTab(entry, AllCreativeModeTabs.BASE_CREATIVE_TAB))
                                output.accept(entry.get().asItem());
                        }

                        for (RegistryEntry<Item> entry : REGISTRATE.getAll(Registries.ITEM)) {
                            if (CreateRegistrate.isInCreativeTab(entry, AllCreativeModeTabs.BASE_CREATIVE_TAB))
                                output.accept(entry.get());
                        }
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
