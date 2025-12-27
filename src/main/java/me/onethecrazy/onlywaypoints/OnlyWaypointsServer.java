package me.onethecrazy.onlywaypoints;

import me.onethecrazy.onlywaypoints.waypoints.ServerWaypointManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class OnlyWaypointsServer {
    private static ServerWaypointManager waypointManager;

    public static void initialize() {
        waypointManager = new ServerWaypointManager();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            waypointManager.loadWaypoints(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            waypointManager.saveWaypoints();
        });
    }

    public static ServerWaypointManager getWaypointManager() {
        return waypointManager;
    }
}
