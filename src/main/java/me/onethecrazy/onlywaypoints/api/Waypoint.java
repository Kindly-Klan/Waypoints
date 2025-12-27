package me.onethecrazy.onlywaypoints.api;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Representa un waypoint público para uso de otros mods.
 * Esta clase es inmutable y thread-safe.
 */
public final class Waypoint {
    private final int id;
    private final RegistryKey<World> dimension;
    private final int x, y, z;
    private final String name;
    private final int color;
    private final Set<String> visibleToPlayers;

    // Constructor público para uso de la API
    public Waypoint(int id, RegistryKey<World> dimension, int x, int y, int z, String name, int color, Set<String> visibleToPlayers) {
        this.id = id;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name;
        this.color = color;
        this.visibleToPlayers = visibleToPlayers != null ? Set.copyOf(visibleToPlayers) : Set.of();
    }

    /**
     * @return El ID único del waypoint
     */
    public int getId() {
        return id;
    }

    /**
     * @return La dimensión donde se encuentra el waypoint
     */
    @NotNull
    public RegistryKey<World> getDimension() {
        return dimension;
    }

    /**
     * @return Coordenada X del waypoint
     */
    public int getX() {
        return x;
    }

    /**
     * @return Coordenada Y del waypoint
     */
    public int getY() {
        return y;
    }

    /**
     * @return Coordenada Z del waypoint
     */
    public int getZ() {
        return z;
    }

    /**
     * @return El nombre del waypoint (puede contener códigos de color de Minecraft)
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * @return El color del beam en formato RGB (0xRRGGBB)
     */
    public int getColor() {
        return color;
    }

    /**
     * @return Un conjunto inmutable con los UUIDs de los jugadores que pueden ver este waypoint.
     * Si está vacío, todos los jugadores pueden verlo.
     */
    @NotNull
    public Set<String> getVisibleToPlayers() {
        return visibleToPlayers;
    }

    /**
     * Verifica si este waypoint es visible para un jugador específico.
     *
     * @param playerUuid UUID del jugador
     * @return true si el jugador puede ver este waypoint
     */
    public boolean isVisibleTo(@Nullable String playerUuid) {
        return visibleToPlayers.isEmpty() || (playerUuid != null && visibleToPlayers.contains(playerUuid));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Waypoint other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Waypoint{id=%d, name='%s', pos=(%d,%d,%d), color=#%06X}",
                id, name, x, y, z, color);
    }
}
