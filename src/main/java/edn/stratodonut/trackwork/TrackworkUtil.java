package edn.stratodonut.trackwork;

public class TrackworkUtil {
    public static double roundTowardZero(double val) {
        if (val < 0) {
            return Math.ceil(val);
        }
        return Math.floor(val);
    }
}
