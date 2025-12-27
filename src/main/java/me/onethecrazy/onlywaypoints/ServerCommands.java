package me.onethecrazy.onlywaypoints;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.onethecrazy.onlywaypoints.waypoints.objects.Coordinates;
import me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class ServerCommands {

    // Sugerencias para IDs de waypoints
    private static final SuggestionProvider<ServerCommandSource> WAYPOINT_ID_SUGGESTIONS = (context, builder) -> {
        List<Waypoint> waypoints = OnlyWaypointsServer.getWaypointManager().getWaypoints();
        return CommandSource.suggestMatching(
            waypoints.stream().map(wp -> String.valueOf(wp.id)),
            builder
        );
    };

    // Colores predefinidos de Minecraft
    private static final Map<String, Integer> COLORS = new HashMap<>() {{
        put("rojo", 0xFF0000);
        put("red", 0xFF0000);
        put("azul", 0x0000FF);
        put("blue", 0x0000FF);
        put("verde", 0x00FF00);
        put("green", 0x00FF00);
        put("amarillo", 0xFFFF00);
        put("yellow", 0xFFFF00);
        put("naranja", 0xFFA500);
        put("orange", 0xFFA500);
        put("morado", 0x800080);
        put("purple", 0x800080);
        put("rosa", 0xFFC0CB);
        put("pink", 0xFFC0CB);
        put("blanco", 0xFFFFFF);
        put("white", 0xFFFFFF);
        put("negro", 0x000000);
        put("black", 0x000000);
        put("cyan", 0x00FFFF);
        put("magenta", 0xFF00FF);
        put("gris", 0x808080);
        put("gray", 0x808080);
        put("aqua", 0x00FFFF);
    }};

    private static final SuggestionProvider<ServerCommandSource> COLOR_SUGGESTIONS = (context, builder) -> 
        CommandSource.suggestMatching(COLORS.keySet(), builder);

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("waypoint")
                    .requires(source -> source.hasPermissionLevel(2)) // Require OP level 2
                    
                    // /waypoint create <pos> <nombre> <color> [selector]
                    .then(CommandManager.literal("create")
                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            // Sin color ni selector (usar default)
                            return createWaypoint(context, null, null);
                        })))
                        
                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                        .then(CommandManager.argument("name", StringArgumentType.string())
                        .then(CommandManager.argument("color", StringArgumentType.string())
                            .suggests(COLOR_SUGGESTIONS)
                        .executes(context -> {
                            // Con color, sin selector
                            String color = StringArgumentType.getString(context, "color");
                            return createWaypoint(context, color, null);
                        })
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                        .executes(context -> {
                            // Con color y selector
                            String color = StringArgumentType.getString(context, "color");
                            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
                            return createWaypoint(context, color, players);
                        }))))))
                    
                    // /waypoint remove <id>
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(0))
                            .suggests(WAYPOINT_ID_SUGGESTIONS)
                        .executes(context -> {
                            int id = IntegerArgumentType.getInteger(context, "id");
                            return removeWaypoint(context.getSource(), id);
                        })))
                    
                    // /waypoint list
                    .then(CommandManager.literal("list")
                        .executes(context -> listWaypoints(context.getSource())))
                    
                    // /waypoint show <id> [selector]
                    .then(CommandManager.literal("show")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(0))
                            .suggests(WAYPOINT_ID_SUGGESTIONS)
                        .executes(context -> {
                            // Sin selector, mostrar al ejecutor
                            int id = IntegerArgumentType.getInteger(context, "id");
                            return setWaypointVisibility(context, id, true, null);
                        })
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                        .executes(context -> {
                            // Con selector
                            int id = IntegerArgumentType.getInteger(context, "id");
                            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
                            return setWaypointVisibility(context, id, true, players);
                        }))))
                    
                    // /waypoint hide <id> [selector]
                    .then(CommandManager.literal("hide")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(0))
                            .suggests(WAYPOINT_ID_SUGGESTIONS)
                        .executes(context -> {
                            // Sin selector, ocultar al ejecutor
                            int id = IntegerArgumentType.getInteger(context, "id");
                            return setWaypointVisibility(context, id, false, null);
                        })
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                        .executes(context -> {
                            // Con selector
                            int id = IntegerArgumentType.getInteger(context, "id");
                            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
                            return setWaypointVisibility(context, id, false, players);
                        }))))
            );
        });
    }

    private static int createWaypoint(CommandContext<ServerCommandSource> context, String colorStr, Collection<ServerPlayerEntity> targetPlayers) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        if (player == null) {
            source.sendError(Text.literal("Este comando solo puede ser ejecutado por un jugador"));
            return 0;
        }

        try {
            Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
            String name = StringArgumentType.getString(context, "name");
            
            // Procesar códigos de color de Minecraft en el nombre (§)
            name = name.replace("&", "§");
            
            // Parse color
            int color = 0x4B0082; // Color default (púrpura)
            if (colorStr != null) {
                if (COLORS.containsKey(colorStr.toLowerCase())) {
                    color = COLORS.get(colorStr.toLowerCase());
                } else {
                    // Intentar parsear como hex
                    try {
                        if (colorStr.startsWith("#")) {
                            colorStr = colorStr.substring(1);
                        }
                        color = Integer.parseInt(colorStr, 16);
                    } catch (NumberFormatException e) {
                        source.sendError(Text.literal("Color inválido. Usa un nombre de color o formato hexadecimal (ej: #FF0000 o FF0000)"));
                        return 0;
                    }
                }
            }

            RegistryKey<World> dimension = player.getWorld().getRegistryKey();
            Coordinates coords = new Coordinates((int) pos.x, (int) pos.y, (int) pos.z);

            // Determinar jugadores que pueden ver este waypoint
            Set<String> visibleToPlayers = new HashSet<>();
            if (targetPlayers != null && !targetPlayers.isEmpty()) {
                visibleToPlayers = targetPlayers.stream()
                    .map(p -> p.getUuidAsString())
                    .collect(Collectors.toSet());
            }

            int id = OnlyWaypointsServer.getWaypointManager().getNextId();
            Waypoint waypoint = new Waypoint(id, dimension, coords, name, color, visibleToPlayers);

            OnlyWaypointsServer.getWaypointManager().addWaypoint(waypoint);

            // Variables finales para el lambda
            final int finalId = id;
            final String finalName = name;
            final int finalX = (int)pos.x;
            final int finalY = (int)pos.y;
            final int finalZ = (int)pos.z;
            final String colorHex = String.format("#%06X", color);
            
            source.sendFeedback(() -> Text.literal("§aWaypoint §f[ID: " + finalId + "] §a'" + finalName + "§a' creado en (" + 
                finalX + ", " + finalY + ", " + finalZ + ") con color " + colorHex), true);

            // Sync to all players
            OnlyWaypointsServer.getWaypointManager().syncWaypointsToClients();

            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error al crear waypoint: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeWaypoint(ServerCommandSource source, int id) {
        Waypoint waypoint = OnlyWaypointsServer.getWaypointManager().getWaypointById(id);

        if (waypoint == null) {
            source.sendError(Text.literal("No se encontró un waypoint con ID: " + id));
            return 0;
        }

        OnlyWaypointsServer.getWaypointManager().removeWaypoint(waypoint);

        final int finalId = id;
        final String finalName = waypoint.name;
        source.sendFeedback(() -> Text.literal("§aWaypoint §f[ID: " + finalId + "] §a'" + finalName + "§a' eliminado"), true);

        // Sync to all players
        OnlyWaypointsServer.getWaypointManager().syncWaypointsToClients();

        return 1;
    }

    private static int listWaypoints(ServerCommandSource source) {
        var waypoints = OnlyWaypointsServer.getWaypointManager().getWaypoints();

        if (waypoints.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§eNo hay waypoints creados"), false);
            return 1;
        }

        source.sendFeedback(() -> Text.literal("§6=== Waypoints ==="), false);
        for (Waypoint wp : waypoints) {
            Waypoint finalWp = wp;
            String colorHex = String.format("#%06X", finalWp.color);
            source.sendFeedback(() -> Text.literal(
                "§f[§b" + finalWp.id + "§f] " + finalWp.name + " §7(" +
                finalWp.coordinates.x + ", " + finalWp.coordinates.y + ", " + finalWp.coordinates.z + 
                ") §7Color: " + colorHex), false);
        }

        return 1;
    }

    private static int setWaypointVisibility(CommandContext<ServerCommandSource> context, int id, boolean visible, Collection<ServerPlayerEntity> targetPlayers) {
        ServerCommandSource source = context.getSource();
        Waypoint waypoint = OnlyWaypointsServer.getWaypointManager().getWaypointById(id);

        if (waypoint == null) {
            source.sendError(Text.literal("No se encontró un waypoint con ID: " + id));
            return 0;
        }

        Collection<ServerPlayerEntity> players;
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Si no hay selector, aplicar al ejecutor
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("Este comando requiere un selector de jugadores cuando se ejecuta desde consola"));
                return 0;
            }
            players = Collections.singletonList(player);
        } else {
            players = targetPlayers;
        }

        for (ServerPlayerEntity player : players) {
            OnlyWaypointsServer.getWaypointManager().setWaypointVisibility(id, player.getUuidAsString(), visible);
        }

        final String action = visible ? "mostrado" : "ocultado";
        final String playerNames = players.stream()
            .map(ServerPlayerEntity::getName)
            .map(Text::getString)
            .collect(Collectors.joining(", "));
        final int finalId = id;

        source.sendFeedback(() -> Text.literal("§aWaypoint §f[ID: " + finalId + "] " + action + " para: " + playerNames), true);

        // Sync to affected players
        OnlyWaypointsServer.getWaypointManager().syncWaypointsToClients();

        return 1;
    }
}
