package me.onethecrazy.onlywaypoints;

import me.onethecrazy.OnlyWaypoints;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.onethecrazy.onlywaypoints.commands.Commands;
import me.onethecrazy.onlywaypoints.util.FileUtil;
import me.onethecrazy.onlywaypoints.waypoints.ServerWaypointManager;
import me.onethecrazy.onlywaypoints.waypoints.WaypointManager;
import me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;


import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;


public class OnlyWaypointsClient implements ClientModInitializer {
	@Nullable
	private static OnlyWaypointClientOptions options;

	public static OnlyWaypointClientOptions options(){
		// Get options
		try{
			if(options == null)
				options = FileUtil.getSave();
		} catch (IOException e) {
            OnlyWaypoints.LOGGER.error("Error while getting save: {0}", e);
        }

		return options;
    }

	@Override
	public void onInitializeClient() {
		registerCommands();
		registerKeybinds();
		registerJoinEventHook();
		registerRenderHook();
		registerNetworking();

		try {
			FileUtil.createDefaultPath();
		} catch (IOException e) {
			OnlyWaypoints.LOGGER.error("Ran into error while creating default path: {0}", e);
		}
	}

	private void registerRenderHook(){
		// If we draw in one event (e.g.) AFTER_TRANSLUCENT we risk: - Clouds rendering on top of Beams
		//															 - Labels not rendering from very far away (likely due to clip plane)
		// so we just draw the labels and beams separately
		WorldRenderEvents.AFTER_TRANSLUCENT.register(WaypointManager::renderBeams);
		HudRenderCallback.EVENT.register(WaypointManager::renderLabels);
	}

	private void registerJoinEventHook(){
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			// Clear waypoints when joining - they will be synced from server
			WaypointManager.waypoints = new java.util.ArrayList<>();
		});
	}

	private void registerKeybinds(){
		var TOGGLE_VISIBILITY_HOTKEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.onlywaypoints.toggle_visibility",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				Text.translatable("category.onlywaypoints").getString()
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if(TOGGLE_VISIBILITY_HOTKEY.wasPressed()){
				WaypointManager.toggleGlobalVisibility();
			}
		});
	}

	private void registerCommands(){
		Commands.initializeCommands();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(Commands.WAYPOINT_COMMAND);
		});
	}

	private void registerNetworking(){
		ClientPlayNetworking.registerGlobalReceiver(ServerWaypointManager.WAYPOINTS_SYNC_PAYLOAD_ID, (payload, context) -> {
			String json = payload.json();
			Gson gson = new Gson();
			Type listType = new TypeToken<List<Waypoint>>(){}.getType();
			List<Waypoint> waypoints = gson.fromJson(json, listType);

			context.client().execute(() -> {
				WaypointManager.waypoints = waypoints;
			});
		});
	}
}