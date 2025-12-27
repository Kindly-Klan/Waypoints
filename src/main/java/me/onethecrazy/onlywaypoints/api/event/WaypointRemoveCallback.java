package me.onethecrazy.onlywaypoints.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Callback que se invoca cuando se elimina un waypoint.
 */
@FunctionalInterface
public interface WaypointRemoveCallback {
    /**
     * Se llama cuando se elimina un waypoint.
     *
     * @param waypoint El waypoint que se eliminó
     */
    void onWaypointRemoved(me.onethecrazy.onlywaypoints.api.Waypoint waypoint);

    /**
     * Evento que se dispara cuando se elimina un waypoint.
     */
    Event<WaypointRemoveCallback> EVENT = EventFactory.createArrayBacked(
            WaypointRemoveCallback.class,
            (listeners) -> (waypoint) -> {
                for (WaypointRemoveCallback listener : listeners) {
                    listener.onWaypointRemoved(waypoint);
                }
            }
    );
}
