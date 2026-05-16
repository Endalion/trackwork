package edn.stratodonut.trackwork.wheel;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.properties.ShipInertiaData;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.internal.physics.PhysicsEntityData;
import org.valkyrienskies.core.internal.physics.PhysicsEntityServer;
import org.valkyrienskies.core.internal.physics.VSSphereCollisionShapeData;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.DimensionIdProvider;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.valkyrienskies.mod.common.ValkyrienSkiesMod.getVsCore;

public class WheelEntity {
    private static volatile Method newShipTeleportDataMethod;
    private static volatile Method teleportPhysicsEntityMethod;

    public static @Nullable PhysicsEntityServer getInLevel(ServerLevel level, long id) {
        if (!aliveInLevel(level, id)) {
            return null;
        }
        return VSGameUtilsKt.getShipObjectWorld(level)
                .retrieveLoadedPhysicsEntities().get(id);
    }

    public static boolean aliveInLevel(ServerLevel level, long id) {
        return VSGameUtilsKt.getShipObjectWorld(level)
                .retrieveLoadedPhysicsEntities().containsKey(id);
    }

    public static void createInLevel(ServerLevel level, PhysicsEntityData data) {
        VSGameUtilsKt.getShipObjectWorld(level).createPhysicsEntity(data, ((DimensionIdProvider) level).getDimensionId());
    }

    public static void removeInLevel(ServerLevel level, long id) {
        if (aliveInLevel(level, id)) {
            VSGameUtilsKt.getShipObjectWorld(level).deletePhysicsEntity(id);
        }
    }

    public static boolean moveTo(ServerLevel level, long id, Vector3dc pos) {
        if (!aliveInLevel(level, id)) {
            return false;
        }
        PhysicsEntityServer serverData = VSGameUtilsKt.getShipObjectWorld(level)
                .retrieveLoadedPhysicsEntities().get(id);

        Object vsCore = getVsCore();
        Object shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        try {
            Method create = newShipTeleportDataMethod;
            if (create == null) {
                create = vsCore.getClass().getMethod(
                        "newShipTeleportData",
                        Vector3dc.class, Quaterniondc.class, Vector3dc.class, Vector3dc.class,
                        String.class, Double.class, Vector3dc.class
                );
                newShipTeleportDataMethod = create;
            }
            Object teleportData = create.invoke(vsCore,
                    pos,
                    new Quaterniond(),
                    new Vector3d(),
                    new Vector3d(),
                    null,
                    null,
                    null
            );

            Method teleport = teleportPhysicsEntityMethod;
            if (teleport == null) {
                teleport = findTeleportPhysicsEntity(shipWorld.getClass());
                teleportPhysicsEntityMethod = teleport;
            }
            teleport.invoke(shipWorld, serverData, teleportData);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Trackwork: incompatible Valkyrien Skies version, could not teleport wheel physics entity", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException("Trackwork: failed to teleport wheel physics entity", cause);
        }
        return true;
    }

    private static Method findTeleportPhysicsEntity(Class<?> shipWorldClass) throws NoSuchMethodException {
        for (Method m : shipWorldClass.getMethods()) {
            if (!"teleportPhysicsEntity".equals(m.getName())) continue;
            if (m.getParameterCount() != 2) continue;
            if (m.getParameterTypes()[0].isAssignableFrom(PhysicsEntityServer.class)) {
                return m;
            }
        }
        throw new NoSuchMethodException("teleportPhysicsEntity(PhysicsEntityServer, ShipTeleportData) not found on " + shipWorldClass.getName());
    }

    public static final class DataBuilder {
        private DataBuilder() {
        }

        @NotNull
        public static PhysicsEntityData createBasicData(long shipId, @NotNull ShipTransform transform, double radius, double mass) {
            double inertia = 0.4 * mass * radius * radius;
            ShipInertiaData inertiaData = getVsCore().newShipInertiaData(new Vector3d(), mass * radius, new Matrix3d().scale(inertia));
            VSSphereCollisionShapeData collisionShapeData = new VSSphereCollisionShapeData(radius);
            // TODO: Current crashes physics, reimplement when safe
//            VSWheelCollisionShapeData collisionShapeData = new VSWheelCollisionShapeData(radius, 0.45, (int)(11 * radius));
            return new PhysicsEntityData(
                    shipId,
                    transform,
                    inertiaData,
                    new Vector3d(),
                    new Vector3d(),
                    collisionShapeData,
                    -1,
                    0.8,
                    0.6,
                    0.6,
                    false

            );
        }
    }
}
