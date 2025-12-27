package me.onethecrazy.onlywaypoints.api.event;

/**
 * Clase que agrupa todos los eventos relacionados con waypoints.
 * Los mods pueden registrar callbacks para estos eventos para reaccionar
 * a cambios en los waypoints del servidor.
 *
 * Ejemplo de uso:
 * <pre>{@code
 * WaypointEvents.CREATE.register((waypoint) -> {
 *     System.out.println("Se creó waypoint: " + waypoint.getName());
 * });
 * }</pre>
 */
public final class WaypointEvents {
    /**
     * Evento que se dispara cuando se crea un nuevo waypoint.
     */
    public static final net.fabricmc.fabric.api.event.Event<WaypointCreateCallback> CREATE = WaypointCreateCallback.EVENT;

    /**
     * Evento que se dispara cuando se elimina un waypoint.
     */
    public static final net.fabricmc.fabric.api.event.Event<WaypointRemoveCallback> REMOVE = WaypointRemoveCallback.EVENT;

    /**
     * Evento que se dispara cuando cambia la visibilidad de un waypoint para un jugador.
     */
    public static final net.fabricmc.fabric.api.event.Event<WaypointVisibilityChangeCallback> VISIBILITY_CHANGE = WaypointVisibilityChangeCallback.EVENT;

    private WaypointEvents() {
        // Clase utilitaria
    }
}
