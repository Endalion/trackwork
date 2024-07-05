package edn.stratodonut.trackwork.tracks;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import edn.stratodonut.trackwork.tracks.blocks.WheelBlockEntity;
import net.minecraft.core.Direction;

public class TrackPonderScenes {
    public static void trackTutorial(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("tracks", "How to place tracks");
        scene.showBasePlate();

        scene.overlay.showText(60)
                .independent(20)
                .text("Disclaimer: Tracks only work on assembled VS2 ships!");
        scene.idle(60);

        scene.world.showSection(util.select.fromTo(0, 2, 1, 4, 2, 1), Direction.DOWN);
        scene.overlay.showText(60)
                .text("Just like chaindrive blocks, ")
                .attachKeyFrame();

        scene.idle(60);

        scene.world.hideSection(util.select.fromTo(0, 2, 1, 4, 2, 1), Direction.UP);

        scene.idle(20);

        scene.overlay.showText(80)
                .text("tracks are built in a row.");
        for (int i = 0; i < 5; i++) {
            scene.world.showSection(util.select.position(i, 2, 2), Direction.DOWN);
            scene.idle(7);
        }

        scene.idle(40);

        scene.world.showSection(util.select.fromTo(0, 2, 3, 4, 2, 4), Direction.UP);
        scene.overlay.showSelectionWithText(util.select.position(0, 2, 2), 100)
                .text("Tracks are powered via connected sprockets.");
    }

    public static void wheelTutorial(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wheels", "How to place wheels");
        scene.showBasePlate();

        scene.overlay.showText(60)
                .independent(20)
                .text("Disclaimer: Wheels only work on assembled VS2 ships!");

        scene.idle(50);
        scene.addKeyframe();
        scene.idle(10);

//        scene.world.showSection(util.select.fromTo(0, 2, 1, 4, 2, 1), Direction.DOWN);
        scene.overlay.showText(60)
                .text("Wheels are placed like, well, wheels");
        // SHOW AXLES
        scene.world.showSection(util.select.fromTo(4, 3, 1, 4, 3, 3), Direction.DOWN);
        // SHOW WHEELS
        scene.world.showSection(util.select.position(0, 3, 0), Direction.DOWN);
        scene.world.showSection(util.select.position(0, 3, 4), Direction.DOWN);
        scene.world.showSection(util.select.position(4, 3, 0), Direction.DOWN);
        scene.world.showSection(util.select.position(4, 3, 4), Direction.DOWN);

        scene.idle(60);

        // GIVE ROTATION?
        scene.overlay.showSelectionWithText(util.select.position(4, 3, 1), 60)
                .text("Drive the wheels via the rib side,");

        scene.idle(60);

        // HIGHLIGHT EMPTY SHAFT
        scene.overlay.showSelectionWithText(util.select.position(0, 3, 1), 80)
                .text("Or don't connect them, which lets the wheel spin freely.");

        scene.idle(80);
        scene.addKeyframe();
        scene.idle(20);

        // SHOW REDSTONE LINk
        scene.world.showSection(util.select.fromTo(0, 3, 1, 0, 3, 3), Direction.DOWN);
        scene.overlay.showSelectionWithText(util.select.position(0, 3, 1), 80)
                .text("Power a wheel with redstone to steer it,");

        scene.idle(40);

        // ACTIVATE LINK
        scene.world.toggleRedstonePower(util.select.position(0, 3, 1));

        scene.world.modifyBlockEntity(util.grid.at(0, 3, 0), WheelBlockEntity.class,
                wbe -> wbe.setSteeringValue(-1.0f));
        scene.world.modifyBlockEntity(util.grid.at(0, 3, 4), WheelBlockEntity.class,
                wbe -> wbe.setSteeringValue(-1.0f));

        scene.idle(40);

        // SHOW LINKAGE
        scene.overlay.showSelectionWithText(util.select.position(0, 3, 4), 80)
                .text("and the opposite side will automatically follow.");
    }
}
