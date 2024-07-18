package edn.stratodonut.trackwork;

import com.simibubi.create.foundation.damageTypes.DamageTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public class TrackDamageTypes {
    public static final ResourceKey<DamageType> RUN_OVER = key("run_over");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, TrackworkMod.getResource(name));
    }

    public static void bootstrap(BootstapContext<DamageType> ctx) {
        new DamageTypeBuilder(RUN_OVER).register(ctx);
    }
}
