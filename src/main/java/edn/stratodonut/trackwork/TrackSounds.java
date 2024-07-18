package edn.stratodonut.trackwork;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TrackSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TrackworkMod.MOD_ID);

    public static final RegistryObject<SoundEvent> SUSPENSION_CREAK = registerSoundEvents("suspension_creak");
//    public static final RegistryObject<SoundEvent> TRACK_CREAK = registerSoundEvents("suspension_creak");
    public static final RegistryObject<SoundEvent> POWER_TOOL = registerSoundEvents("power_wrench");
    public static final RegistryObject<SoundEvent> SPRING_TOOL = registerSoundEvents("spring_tool");

    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TrackworkMod.MOD_ID, name)));
    }

    public static void register(IEventBus bus) { SOUND_EVENTS.register(bus); }
}
