package edn.stratodonut.trackwork;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

// I'm blindly copying create because I have no clue why Mojang rewrote everything
public class TrackDatagen {
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        if (event.includeServer()) {
            GeneratedEntriesProvider generatedEntriesProvider = new GeneratedEntriesProvider(output, lookupProvider);
            generator.addProvider(true, generatedEntriesProvider);
        }
    }

    public static class GeneratedEntriesProvider extends DatapackBuiltinEntriesProvider {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.DAMAGE_TYPE, TrackDamageTypes::bootstrap);

        public GeneratedEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, BUILDER, Set.of(TrackworkMod.MOD_ID));
        }

        @Override
        public String getName() {
            return "Trackwork's Generated Registry Entries";
        }
    }
}
