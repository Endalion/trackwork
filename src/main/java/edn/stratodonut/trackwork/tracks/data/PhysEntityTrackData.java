package edn.stratodonut.trackwork.tracks.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3dc;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint;
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId;
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public class PhysEntityTrackData {
    public final Vector3dc trackPos;
    public final Vector3dc wheelAxis;
    public final long shiptraptionID;
    public final double springConstant;
    public final double damperConstant;
    public final VSAttachmentConstraint springConstraint;
    public final VSHingeOrientationConstraint axleConstraint;
    public final Integer springId;
    public final Integer axleId;
    public final double trackRPM;
    public float trackSU;
    public double previousSpringDist;

    // For Jackson
    private PhysEntityTrackData() {
        this.trackPos = null;
        this.wheelAxis = null;
        this.springConstant = 0;
        this.damperConstant = 0;
        this.springConstraint = null;
        this.shiptraptionID = -1L;
        this.axleConstraint = null;
        this.springId = null;
        this.axleId = null;
        this.trackRPM = 0;
        this.previousSpringDist = 0;
    }

    private PhysEntityTrackData(Vector3dc trackPos, Vector3dc wheelAxis, long shiptraptionID, double springConstant, double damperConstant, VSAttachmentConstraint springConstraint, VSHingeOrientationConstraint axleConstraint, int springId, int axleId, double trackRPM, double springDist) {
        this.trackPos = trackPos;
        this.wheelAxis = wheelAxis;
        this.springConstant = springConstant;
        this.damperConstant = damperConstant;
        this.springConstraint = springConstraint;
        this.shiptraptionID = shiptraptionID;
        this.axleConstraint = axleConstraint;
        this.springId = springId;
        this.axleId = axleId;
        this.trackRPM = trackRPM;
        this.previousSpringDist = springDist;
    }

    public final PhysEntityTrackData updateWith(UpdateData update) {
        return new PhysEntityTrackData(this.trackPos, this.wheelAxis, this.shiptraptionID, update.springConstant, update.damperConstant, this.springConstraint, axleConstraint, springId, axleId, update.trackRPM, this.previousSpringDist);
    }

    public static PhysEntityTrackData from(CreateData data) {
        return new PhysEntityTrackData(data.trackPos, data.wheelAxis, data.shiptraptionID, data.springConstant, data.damperConstant, (VSAttachmentConstraint) data.springConstraint.getVsConstraint(), (VSHingeOrientationConstraint) data.axleConstraint.getVsConstraint(), data.springConstraint.getConstraintId(), data.axleConstraint.getConstraintId(), data.trackRPM, 0);
    }

    public record UpdateData(double springConstant, double damperConstant, double trackRPM) {
    }

    public record CreateData(Vector3dc trackPos, Vector3dc wheelAxis, long shiptraptionID, double springConstant, double damperConstant, VSConstraintAndId springConstraint, VSConstraintAndId axleConstraint, double trackRPM) {
    }
}
