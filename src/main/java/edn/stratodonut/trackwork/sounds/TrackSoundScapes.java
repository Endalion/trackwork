package edn.stratodonut.trackwork.sounds;

import com.simibubi.create.infrastructure.config.AllConfigs;
import edn.stratodonut.trackwork.TrackworkConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.*;

public class TrackSoundScapes {
    static final int UPDATE_INTERVAL = 5;
    private static final Map<AmbientGroup, Map<PitchGroups.Group, Set<BlockPos>>> counter = new IdentityHashMap<>();
    private static final Map<Pair<AmbientGroup, PitchGroups.Group>, TrackSoundScape> activeSounds = new HashMap<>();

    public static void play(AmbientGroup group, BlockPos pos, float pitch) {
        if (!AllConfigs.client().enableAmbientSounds.get())
            return;
        if (!outOfRange(pos))
            addSound(group, pos, pitch);
    }

    public static void tick() {
        activeSounds.values()
                .forEach(TrackSoundScape::tick);

        if (AnimationTickHolder.getTicks() % UPDATE_INTERVAL != 0)
            return;

        boolean disable = !AllConfigs.client().enableAmbientSounds.get();
        for (Iterator<Map.Entry<Pair<AmbientGroup, PitchGroups.Group>, TrackSoundScape>> iterator = activeSounds.entrySet()
                .iterator(); iterator.hasNext();) {

            Map.Entry<Pair<AmbientGroup, PitchGroups.Group>, TrackSoundScape> entry = iterator.next();
            Pair<AmbientGroup, PitchGroups.Group> key = entry.getKey();
            TrackSoundScape value = entry.getValue();

            if (disable || getSoundCount(key.getFirst(), key.getSecond()) == 0) {
                value.remove();
                iterator.remove();
            }
        }

        counter.values()
                .forEach(m -> m.values()
                        .forEach(Set::clear));
    }

    private static void addSound(AmbientGroup group, BlockPos pos, float pitch) {
        PitchGroups.Group groupFromPitch = PitchGroups.getGroupFromPitch(pitch);
        Set<BlockPos> set = counter.computeIfAbsent(group, ag -> new IdentityHashMap<>())
                .computeIfAbsent(groupFromPitch, pg -> new HashSet<>());
        set.add(pos);

        Pair<AmbientGroup, PitchGroups.Group> pair = Pair.of(group, groupFromPitch);
        activeSounds.computeIfAbsent(pair, $ -> {
            TrackSoundScape TrackSoundScape = group.instantiate(pitch);
            TrackSoundScape.play();
            return TrackSoundScape;
        });
    }

    public static void invalidateAll() {
        counter.clear();
        activeSounds.forEach(($, sound) -> sound.remove());
        activeSounds.clear();
    }

    protected static boolean outOfRange(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            pos = BlockPos.containing(VSGameUtilsKt.toWorldCoordinates(mc.level, Vec3.atCenterOf(pos)));
        }
        return !getCameraPos().closerThan(pos, getMaxAmbientSourceDistance());
    }

    protected static int getMaxAmbientSourceDistance() {
        return TrackworkConfigs.client().trackSoundDist.get();
    }

    protected static BlockPos getCameraPos() {
        Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
        if (renderViewEntity == null)
            return BlockPos.ZERO;
        BlockPos playerLocation = renderViewEntity.blockPosition();
        return playerLocation;
    }

    public static int getSoundCount(AmbientGroup group, PitchGroups.Group pitchGroup) {
        return getAllLocations(group, pitchGroup).size();
    }

    public static Set<BlockPos> getAllLocations(AmbientGroup group, PitchGroups.Group pitchGroup) {
        return counter.getOrDefault(group, Collections.emptyMap())
                .getOrDefault(pitchGroup, Collections.emptySet());
    }
}
