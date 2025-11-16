package edn.stratodonut.trackwork;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import edn.stratodonut.trackwork.client.TrackworkPartialModels;
import edn.stratodonut.trackwork.client.TrackworkSpriteShifts;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import static net.createmod.catnip.lang.FontHelper.Palette.STANDARD_CREATE;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("trackwork")
public class TrackworkMod
{
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String MOD_ID = "trackwork";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    static {
        REGISTRATE.setTooltipModifierFactory(item ->
                new ItemDescription.Modifier(item, STANDARD_CREATE));
    }

    public TrackworkMod() { onCtor(); }

    public void onCtor() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        IEventBus modEventBus = FMLJavaModLoadingContext.get()
                .getModEventBus();

        REGISTRATE.registerEventListeners(modEventBus);

        TrackworkConfigs.register(modLoadingContext);
        TrackSounds.register(modEventBus);
        TrackCreativeTabs.register(modEventBus);
        TrackworkItems.register();
        TrackBlocks.register();
        TrackBlockEntityTypes.register();
        TrackEntityTypes.register();
        TrackPackets.registerPackets();
        modEventBus.addListener(EventPriority.LOWEST, TrackDatagen::gatherData);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> TrackPonderPlugin::registerPlugin);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TrackworkPartialModels::init);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> TrackworkSpriteShifts::init);
    }

    public static void warn(String format, Object arg) {
        LOGGER.warn(format, arg);
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(format, args);
    }

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
