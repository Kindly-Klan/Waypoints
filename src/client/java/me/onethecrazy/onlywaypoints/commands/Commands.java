package me.onethecrazy.onlywaypoints.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.onethecrazy.onlywaypoints.waypoints.WaypointManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;


public class Commands {
    public static LiteralArgumentBuilder<FabricClientCommandSource> WAYPOINT_COMMAND;

    public static void initializeCommands(){
        WAYPOINT_COMMAND = ClientCommandManager.literal("onlywaypoints")
                .then(ClientCommandManager.literal("toggle").executes(context -> waypointsCommandHandler(context, WaypointCommandType.TOGGLE)));
    }

    private static int waypointsCommandHandler(CommandContext<FabricClientCommandSource> ctx, WaypointCommandType cmdType){
        switch(cmdType){
            case TOGGLE -> {
                WaypointManager.toggleGlobalVisibility();
                return 1;
            }
        }

        return 0;
    }

    private enum WaypointCommandType {
        TOGGLE
    }
}
