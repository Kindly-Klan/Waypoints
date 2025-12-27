package me.onethecrazy;


import me.onethecrazy.onlywaypoints.OnlyWaypointsServer;
import me.onethecrazy.onlywaypoints.ServerCommands;
import me.onethecrazy.onlywaypoints.waypoints.ServerWaypointManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlyWaypoints implements ModInitializer {
	public static final String MOD_ID = "onlywaypoints";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize()
	{
		// Register the custom payload
		PayloadTypeRegistry.playS2C().register(
			ServerWaypointManager.WAYPOINTS_SYNC_PAYLOAD_ID,
			ServerWaypointManager.WaypointsSyncPayload.CODEC
		);

		ServerCommands.register();
		OnlyWaypointsServer.initialize();
	}
}