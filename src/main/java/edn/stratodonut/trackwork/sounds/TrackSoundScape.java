package edn.stratodonut.trackwork.sounds;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.List;

public class TrackSoundScape {
	List<ContinuousSound> continuous;
	List<RepeatingSound> repeating;
	private int sound_volume_arg_max = 15;
	private final float pitch;
	private final AmbientGroup group;
	private Vec3 meanPos;
	private final PitchGroups.Group pitchGroup;

	public TrackSoundScape(float pitch, AmbientGroup group) {
		this.pitchGroup = PitchGroups.getGroupFromPitch(pitch);
		this.pitch = pitch;
		this.group = group;
		continuous = new ArrayList<>();
		repeating = new ArrayList<>();
	}

	public TrackSoundScape continuous(SoundEvent sound, float relativeVolume, float relativePitch) {
		return add(new ContinuousSound(sound, this, pitch * relativePitch, relativeVolume));
	}

	public TrackSoundScape repeating(SoundEvent sound, float relativeVolume, float relativePitch, int delay) {
		return add(new RepeatingSound(sound, this, pitch * relativePitch, relativeVolume, delay));
	}

	public TrackSoundScape withArgMax(int max) {
		this.sound_volume_arg_max = max;
		return this;
	}

	public TrackSoundScape add(ContinuousSound continuousSound) {
		continuous.add(continuousSound);
		return this;
	}

	public TrackSoundScape add(RepeatingSound repeatingSound) {
		repeating.add(repeatingSound);
		return this;
	}

	public void play() {
		continuous.forEach(Minecraft.getInstance()
			.getSoundManager()::play);
	}

	public void tick() {
		if (AnimationTickHolder.getTicks() % TrackSoundScapes.UPDATE_INTERVAL == 0)
			meanPos = null;
		repeating.forEach(RepeatingSound::tick);
	}

	public void remove() {
		continuous.forEach(ContinuousSound::remove);
	}

	public Vec3 getMeanPos() {
		return meanPos == null ? meanPos = determineMeanPos() : meanPos;
	}

	private Vec3 determineMeanPos() {
		meanPos = Vec3.ZERO;
		int amount = 0;
		Minecraft mc = Minecraft.getInstance();
		for (BlockPos blockPos : TrackSoundScapes.getAllLocations(group, pitchGroup)) {
			if (mc.level != null) {
				blockPos = BlockPos.containing(VSGameUtilsKt.toWorldCoordinates(mc.level, Vec3.atCenterOf(blockPos)));
			}
			meanPos = meanPos.add(VecHelper.getCenterOf(blockPos));
			amount++;
		}
		if (amount == 0)
			return meanPos;
		return meanPos.scale(1f / amount);
	}

	public float getVolume() {
		Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
		float distanceMultiplier = 0;
		if (renderViewEntity != null) {
			double distanceTo = renderViewEntity.position()
				.distanceTo(getMeanPos());
			distanceMultiplier = (float) Mth.lerp(distanceTo / TrackSoundScapes.getMaxAmbientSourceDistance(), 2, 0);
		}
		int soundCount = TrackSoundScapes.getSoundCount(group, pitchGroup);
		float max = AllConfigs.client().ambientVolumeCap.getF();
		return Mth.clamp(soundCount / (sound_volume_arg_max * 10f), 0.025f, max) * distanceMultiplier;
	}

}
