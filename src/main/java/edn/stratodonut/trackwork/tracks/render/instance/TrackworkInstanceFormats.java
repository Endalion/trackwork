package edn.stratodonut.trackwork.tracks.render.instance;

import com.jozufozu.flywheel.core.layout.BufferLayout;
import com.jozufozu.flywheel.core.layout.CommonItems;

public class TrackworkInstanceFormats {
    public static final BufferLayout TRACK = BufferLayout.builder()
            .addItems(CommonItems.NORMALIZED_BYTE, CommonItems.VEC3,
                    CommonItems.FLOAT, CommonItems.FLOAT, CommonItems.FLOAT,
                    CommonItems.FLOAT, CommonItems.FLOAT, CommonItems.FLOAT)
            .build();
}
