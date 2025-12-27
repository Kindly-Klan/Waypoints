package me.onethecrazy.onlywaypoints.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Callback que se invoca cuando cambia la visibilidad de un waypoint para un jugador.
 */
@FunctionalInterface
public interface WaypointVisibilityChangeCallback {
    /**
     * Se llama cuando cambia la visibilidad de un waypoint para un jugador.
     *
     * @param waypoint El waypoint cuya visibilidad cambió
     * @param playerUuid UUID del jugador
     * @param visible true si ahora es visible, false si es invisible
     */
    void onWaypointVisibilityChanged(me.onethecrazy.onlywaypoints.api.Waypoint waypoint, String playerUuid, boolean visible);

    /**
     * Evento que se dispara cuando cambia la visibilidad de un waypoint.
     */
    Event<WaypointVisibilityChangeCallback> EVENT = EventFactory.createArrayBacked(
            WaypointVisibilityChangeCallback.class,
            (listeners) -> (waypoint, playerUuid, visible) -> {
                for (WaypointVisibilityChangeCallback listener : listeners) {
                    listener.onWaypointVisibilityChanged(waypoint, playerUuid, visible);
                }
            }
    );
}
