package me.onethecrazy.onlywaypoints.waypoints.objects;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.*;

public class Waypoint {
    public int id; // ID numérico simple
    public RegistryKey<World> dimension;
    public Coordinates coordinates;
    public String name;
    public WaypointType type;
    public boolean shouldRender;
    public int color; // Color del beam en formato RGB hexadecimal (ej: 0xFF0000 para rojo)
    public Set<String> visibleToPlayers; // UUIDs de jugadores que pueden ver este waypoint
    public Map<String, Boolean> playerVisibility; // UUID -> visible (para persistencia de hide/show)

    public Waypoint(int id, RegistryKey<World> dimension, Coordinates coordinates, String name, int color, Set<String> visibleToPlayers){
        this.id = id;
        this.dimension = dimension;
        this.coordinates = coordinates;
        this.name = name;
        this.type = WaypointType.USER;
        this.shouldRender = true;
        this.color = color;
        this.visibleToPlayers = visibleToPlayers != null ? visibleToPlayers : new HashSet<>();
        this.playerVisibility = new HashMap<>();
    }

    public Waypoint(int id, RegistryKey<World> dimension, Coordinates coordinates, String name, WaypointType type, int color, Set<String> visibleToPlayers){
        this.id = id;
        this.dimension = dimension;
        this.coordinates = coordinates;
        this.name = name;
        this.type = type;
        this.shouldRender = true;
        this.color = color;
        this.visibleToPlayers = visibleToPlayers != null ? visibleToPlayers : new HashSet<>();
        this.playerVisibility = new HashMap<>();
    }

    // Constructor para deserialización JSON
    public Waypoint() {
        this.visibleToPlayers = new HashSet<>();
        this.playerVisibility = new HashMap<>();
    }

    public boolean isVisibleTo(String playerUuid) {
        // Si está en la lista de visibleToPlayers, verificar playerVisibility
        if (!visibleToPlayers.isEmpty() && !visibleToPlayers.contains(playerUuid)) {
            return false;
        }
        // Verificar si el jugador lo ha ocultado manualmente
        return playerVisibility.getOrDefault(playerUuid, true);
    }

    public void setVisibilityFor(String playerUuid, boolean visible) {
        playerVisibility.put(playerUuid, visible);
    }
}
