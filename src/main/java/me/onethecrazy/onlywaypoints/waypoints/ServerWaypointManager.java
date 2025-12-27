package me.onethecrazy.onlywaypoints.waypoints;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.onethecrazy.OnlyWaypoints;
import me.onethecrazy.onlywaypoints.waypoints.objects.Coordinates;
import me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint;
import me.onethecrazy.onlywaypoints.waypoints.objects.WaypointType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ServerWaypointManager {
    public static final Identifier WAYPOINTS_SYNC_ID = Identifier.of("onlywaypoints", "sync_waypoints");
    public static final CustomPayload.Id<WaypointsSyncPayload> WAYPOINTS_SYNC_PAYLOAD_ID = new CustomPayload.Id<>(WAYPOINTS_SYNC_ID);

    public record WaypointsSyncPayload(String json) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, WaypointsSyncPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.json),
            buf -> new WaypointsSyncPayload(buf.readString())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return WAYPOINTS_SYNC_PAYLOAD_ID;
        }
    }

    private List<Waypoint> waypoints = new ArrayList<>();
    private MinecraftServer server;
    private Path savePath;
    private int nextId = 0;

    public void loadWaypoints(MinecraftServer server) {
        this.server = server;
        this.savePath = server.getRunDirectory().resolve("waypoints.json");

        try {
            if (Files.exists(savePath)) {
                String json = Files.readString(savePath);
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Waypoint>>(){}.getType();
                List<Waypoint> loadedWaypoints = gson.fromJson(json, listType);
                if (loadedWaypoints != null) {
                    this.waypoints = loadedWaypoints;
                    // Calcular el próximo ID basado en los waypoints cargados
                    nextId = waypoints.stream()
                            .mapToInt(wp -> wp.id)
                            .max()
                            .orElse(-1) + 1;
                }
            }
        } catch (Exception e) {
            OnlyWaypoints.LOGGER.error("Error loading waypoints: {}", e.getMessage());
            this.waypoints = new ArrayList<>();
        }

        OnlyWaypoints.LOGGER.info("Loaded {} waypoints", waypoints.size());
    }

    public void saveWaypoints() {
        if (savePath == null) return;

        try {
            Gson gson = new Gson();
            String json = gson.toJson(waypoints);
            Files.writeString(savePath, json);
            OnlyWaypoints.LOGGER.info("Saved {} waypoints", waypoints.size());
        } catch (Exception e) {
            OnlyWaypoints.LOGGER.error("Error saving waypoints: {}", e.getMessage());
        }
    }

    public int getNextId() {
        return nextId++;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        saveWaypoints();
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
        saveWaypoints();
    }

    public Waypoint getWaypointById(int id) {
        return waypoints.stream()
                .filter(wp -> wp.id == id)
                .findFirst()
                .orElse(null);
    }

    public boolean removeWaypointById(int id) {
        return waypoints.removeIf(wp -> wp.id == id);
    }

    public List<Waypoint> getWaypoints() {
        return new ArrayList<>(waypoints);
    }

    public void syncWaypointsToClients() {
        if (server == null) return;

        Gson gson = new Gson();
        String json = gson.toJson(waypoints);
        WaypointsSyncPayload payload = new WaypointsSyncPayload(json);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public void updateLastDeathWaypoint(Coordinates coords, RegistryKey<World> dimension) {
        // Find death waypoints and update numbering
        List<Waypoint> deathWaypoints = waypoints.stream()
                .filter(w -> w.type == WaypointType.DEATH)
                .toList();

        String lastDeathString = "Last Death";

        deathWaypoints.forEach(wp -> {
            String name = wp.name;
            if (name.contains("Last Death")) {
                if (name.contains("(")) {
                    // Extract number and increment
                    int start = name.indexOf("(") + 1;
                    int end = name.indexOf(")");
                    if (start > 0 && end > start) {
                        try {
                            String numStr = name.substring(start, end);
                            int num = Integer.parseInt(numStr) + 1;
                            wp.name = "Last Death (" + num + ")";
                        } catch (NumberFormatException e) {
                            wp.name = "Last Death (1)";
                        }
                    }
                } else {
                    wp.name = "Last Death (1)";
                }
            }
        });

        // Add new last death waypoint con color rojo para death
        addWaypoint(new Waypoint(getNextId(), dimension, coords, lastDeathString, WaypointType.DEATH, 0xFF0000, new java.util.HashSet<>()));

        syncWaypointsToClients();
    }

    public void setWaypointVisibility(int waypointId, String playerUuid, boolean visible) {
        Waypoint wp = getWaypointById(waypointId);
        if (wp != null) {
            wp.setVisibilityFor(playerUuid, visible);
            saveWaypoints();
        }
    }
}
