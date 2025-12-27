package me.onethecrazy.onlywaypoints.api;

import me.onethecrazy.onlywaypoints.OnlyWaypointsServer;
import me.onethecrazy.onlywaypoints.waypoints.objects.Coordinates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * API pública para interactuar con el sistema de waypoints de OnlyWaypoints.
 *
 * Esta API permite a otros mods:
 * - Obtener información sobre waypoints existentes
 * - Crear nuevos waypoints
 * - Modificar la visibilidad de waypoints
 * - Eliminar waypoints
 *
 * Todos los métodos son thread-safe y pueden ser llamados desde cualquier hilo.
 */
public final class WaypointAPI {

    /**
     * Obtiene una lista inmutable de todos los waypoints existentes.
     *
     * @return Lista de waypoints, nunca null
     */
    @NotNull
    public static List<Waypoint> getWaypoints() {
        return OnlyWaypointsServer.getWaypointManager().getWaypoints().stream()
                .map(WaypointAPI::convertToPublicWaypoint)
                .toList();
    }

    /**
     * Obtiene un waypoint específico por su ID.
     *
     * @param id ID del waypoint
     * @return El waypoint si existe, null si no
     */
    @Nullable
    public static Waypoint getWaypoint(int id) {
        var internalWaypoint = OnlyWaypointsServer.getWaypointManager().getWaypointById(id);
        return internalWaypoint != null ? convertToPublicWaypoint(internalWaypoint) : null;
    }

    /**
     * Crea un nuevo waypoint con configuración completa.
     *
     * @param dimension Dimensión donde crear el waypoint
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @param name Nombre del waypoint (soporta códigos de color con §)
     * @param color Color del beam en formato RGB (0xRRGGBB)
     * @param visibleToPlayers Set de UUIDs de jugadores que pueden ver el waypoint.
     *                        Si está vacío, todos los jugadores lo verán.
     * @return El waypoint creado, o null si falló
     */
    @Nullable
    public static Waypoint createWaypoint(@NotNull RegistryKey<World> dimension,
                                         int x, int y, int z,
                                         @NotNull String name,
                                         int color,
                                         @Nullable Set<String> visibleToPlayers) {
        var internalWaypoint = new me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint(
                OnlyWaypointsServer.getWaypointManager().getNextId(),
                dimension,
                new Coordinates(x, y, z),
                name.replace("&", "§"), // Convertir códigos de color
                me.onethecrazy.onlywaypoints.waypoints.objects.WaypointType.USER,
                color,
                visibleToPlayers
        );

        OnlyWaypointsServer.getWaypointManager().addWaypoint(internalWaypoint);
        return convertToPublicWaypoint(internalWaypoint);
    }

    /**
     * Crea un waypoint básico visible para todos los jugadores.
     *
     * @param dimension Dimensión donde crear el waypoint
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @param name Nombre del waypoint
     * @return El waypoint creado, o null si falló
     */
    @Nullable
    public static Waypoint createWaypoint(@NotNull RegistryKey<World> dimension,
                                         int x, int y, int z,
                                         @NotNull String name) {
        return createWaypoint(dimension, x, y, z, name, 0x4B0082, Set.of()); // Color púrpura por defecto
    }

    /**
     * Crea un waypoint en la posición de un jugador.
     *
     * @param player Jugador cuya posición usar
     * @param name Nombre del waypoint
     * @param color Color del beam
     * @param visibleToPlayers Jugadores que pueden ver el waypoint
     * @return El waypoint creado, o null si falló
     */
    @Nullable
    public static Waypoint createWaypointAtPlayer(@NotNull ServerPlayerEntity player,
                                                 @NotNull String name,
                                                 int color,
                                                 @Nullable Set<String> visibleToPlayers) {
        return createWaypoint(
                player.getWorld().getRegistryKey(),
                player.getBlockX(),
                player.getBlockY(),
                player.getBlockZ(),
                name,
                color,
                visibleToPlayers
        );
    }

    /**
     * Elimina un waypoint por su ID.
     *
     * @param id ID del waypoint a eliminar
     * @return true si se eliminó exitosamente, false si no existía
     */
    public static boolean removeWaypoint(int id) {
        return OnlyWaypointsServer.getWaypointManager().removeWaypointById(id);
    }

    /**
     * Cambia la visibilidad de un waypoint para un jugador específico.
     *
     * @param waypointId ID del waypoint
     * @param playerUuid UUID del jugador
     * @param visible true para mostrar, false para ocultar
     * @return true si se cambió exitosamente, false si el waypoint no existe
     */
    public static boolean setWaypointVisibility(int waypointId, @NotNull String playerUuid, boolean visible) {
        OnlyWaypointsServer.getWaypointManager().setWaypointVisibility(waypointId, playerUuid, visible);
        return getWaypoint(waypointId) != null;
    }

    /**
     * Oculta un waypoint para un jugador específico.
     *
     * @param waypointId ID del waypoint
     * @param playerUuid UUID del jugador
     * @return true si se ocultó exitosamente
     */
    public static boolean hideWaypoint(int waypointId, @NotNull String playerUuid) {
        return setWaypointVisibility(waypointId, playerUuid, false);
    }

    /**
     * Muestra un waypoint para un jugador específico.
     *
     * @param waypointId ID del waypoint
     * @param playerUuid UUID del jugador
     * @return true si se mostró exitosamente
     */
    public static boolean showWaypoint(int waypointId, @NotNull String playerUuid) {
        return setWaypointVisibility(waypointId, playerUuid, true);
    }

    /**
     * Convierte un waypoint interno a uno público de la API.
     */
    private static Waypoint convertToPublicWaypoint(me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint internal) {
        return new Waypoint(
                internal.id,
                internal.dimension,
                internal.coordinates.x,
                internal.coordinates.y,
                internal.coordinates.z,
                internal.name,
                internal.color,
                internal.visibleToPlayers
        );
    }

    private WaypointAPI() {
        // Clase utilitaria
    }
}
