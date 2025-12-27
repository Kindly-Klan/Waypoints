package me.onethecrazy.onlywaypoints.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Callback que se invoca cuando se crea un nuevo waypoint.
 */
@FunctionalInterface
public interface WaypointCreateCallback {
    /**
     * Se llama cuando se crea un waypoint.
     *
     * @param waypoint El waypoint que se creó
     */
    void onWaypointCreated(me.onethecrazy.onlywaypoints.api.Waypoint waypoint);

    /**
     * Evento que se dispara cuando se crea un waypoint.
     */
    Event<WaypointCreateCallback> EVENT = EventFactory.createArrayBacked(
            WaypointCreateCallback.class,
            (listeners) -> (waypoint) -> {
                for (WaypointCreateCallback listener : listeners) {
                    listener.onWaypointCreated(waypoint);
                }
            }
    );
}
