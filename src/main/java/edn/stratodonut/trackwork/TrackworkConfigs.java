package edn.stratodonut.trackwork;

import com.simibubi.create.foundation.config.ConfigBase;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class TrackworkConfigs {
    private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);
    private static TServer server;
    private static TClient client;
    public static class TServer extends ConfigBase {
        public final ConfigBase.ConfigBool enableStress = this.b(false, "enableTrackStress", "Enable track Kinetic Stress");
        public final ConfigBase.ConfigFloat stressMult = this.f(1/50f, 0.0f, "stressMultiplier", "Stress multiplier, units SU/(ton x RPM)");
        public final ConfigBase.ConfigInt maxRPM = this.i(256, 1, "maxTrackRPM", "Maximum Track RPM, 1 RPM ~ 0.104 m/s");
        public final ConfigBase.ConfigBool enableTrackThrow = this.b(false, "enableTrackThrow", "Enable entire tracks being thrown off by explosions");
        public final ConfigBase.ConfigInt wheelPairDist = this.i(7, 5, 15, "wheelPairDistance", "The max distance between wheels where steering, etc. will be paired");

        @Override
        public String getName() {
            return "server";
        }
    }

    public static class TClient extends ConfigBase {
        public final ConfigBase.ConfigInt trackRenderDist = this.i(256, "trackRenderDist", "Track render distance");

        @Override
        public String getName() {
            return "client";
        }
    }

    public static TClient client() { return TrackworkConfigs.client; }
    public static TServer server() {
        return TrackworkConfigs.server;
    }

    private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type side) {
        Pair<T, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void register(ModLoadingContext context) {
        server = register(TServer::new, ModConfig.Type.SERVER);
        client = register(TClient::new, ModConfig.Type.CLIENT);

        for (Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
            context.registerConfig(pair.getKey(), pair.getValue().specification);
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onLoad();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onReload();
    }
}
