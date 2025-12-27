package me.onethecrazy.onlywaypoints.waypoints;

import me.onethecrazy.onlywaypoints.OnlyWaypointsClient;
import me.onethecrazy.onlywaypoints.util.BeamRenderer;
import me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WaypointManager {
    public static List<Waypoint> waypoints = new ArrayList<>();
    // To prevent Beacon Beam not spinning when /tick freeze is used
    private static double unfrozenTime = 0;
    // Toggle rendering of all waypoints
    public static boolean shouldRenderGlobally = true;

    public static void renderBeams(WorldRenderContext ctx){
        MinecraftClient client = MinecraftClient.getInstance();
        // Sanity Check, should never be null
        if (client.world == null || client.player == null) return;

        var playerDimension = client.world.getRegistryKey();
        String playerUuid = client.player.getUuidAsString();

        float delta = ctx.tickCounter().getTickDelta(false);
        unfrozenTime += delta;

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        for (Waypoint wp : waypoints) {
            if(!wp.shouldRender || !Objects.equals(playerDimension.getValue().getPath(), wp.dimension.getValue().getPath()) || !Objects.equals(playerDimension.getValue().getNamespace(), wp.dimension.getValue().getNamespace()) || !shouldRenderGlobally)
                continue;

            // Verificar si el jugador puede ver este waypoint
            if (!wp.isVisibleTo(playerUuid))
                continue;

            renderBeam(wp, ctx, delta, consumers);
        }
    }

    public static void renderLabels(DrawContext context, RenderTickCounter tick){
        MinecraftClient client = MinecraftClient.getInstance();
        // Sanity Check, should never be null
        if (client.world == null || client.player == null) return;

        var playerDimension = client.world.getRegistryKey();
        String playerUuid = client.player.getUuidAsString();


        for (Waypoint wp : waypoints) {
            if(!wp.shouldRender || !Objects.equals(playerDimension.getValue().getPath(), wp.dimension.getValue().getPath()) || !Objects.equals(playerDimension.getValue().getNamespace(), wp.dimension.getValue().getNamespace()) || !shouldRenderGlobally)
                continue;

            // Verificar si el jugador puede ver este waypoint
            if (!wp.isVisibleTo(playerUuid))
                continue;

            renderLabel(wp, context, tick);
        }
    }

    private static void renderLabel(Waypoint wp, DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        GameRenderer renderer = client.gameRenderer;
        Camera camera = renderer.getCamera();
        Vec3d camPos = camera.getPos();
        Window window = client.getWindow();

        // Construct projection Matrix
        // Obtener FOV base de las opciones
        double fovDeg = client.options.getFov().getValue();
        Matrix4f projMatrix = renderer.getBasicProjectionMatrix(fovDeg);

        // Construct View matrix
        float pitch = camera.getPitch();
        float yaw   = camera.getYaw();
        Matrix4f viewMatrix = new Matrix4f()
                .identity()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .translate(
                        (float) -camPos.x,
                        (float) -camPos.y,
                        (float) -camPos.z
                );

        // World Pos of the label
        Vec3d worldPos = new Vec3d(
                wp.coordinates.x + 0.5,
                wp.coordinates.y + 1,
                wp.coordinates.z + 0.5
        );

        // Project to clip space
        Vector4f clip = new Vector4f(
                (float) worldPos.x,
                (float) worldPos.y - 2f * ((float)worldPos.y - (float)camPos.y), // Wierd fix for label y being added the diff between label y and player y (*2)
                (float) worldPos.z,
                1f
        );

        viewMatrix.transform(clip);
        projMatrix.transform(clip);

        // Cull behind camera
        if (clip.w() >= 0f) return;

        // Evil Projection Fuckery
        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        int sw = window.getScaledWidth();
        int sh = window.getScaledHeight();
        int sx = (int) ((ndcX * 0.5f + 0.5f) * sw);
        int sy = (int) ((1f - (ndcY * 0.5f + 0.5f)) * sh);

        // Distance gate
        if (worldPos.distanceTo(camPos) > OnlyWaypointsClient.options().dontRenderAfterDistance)
            return;

        // Draw Background + Text
        TextRenderer tr = client.textRenderer;
        Text txt = Text.of(wp.name + " [" + (int) worldPos.distanceTo(camPos) + "m]");
        int color = 0xFFFFFFFF;

        float textWidth = tr.getWidth(txt);
        float textHeight = tr.fontHeight;
        float xOffset = textWidth / 2f;
        int margin = 2;

        ctx.fill(
                sx - margin - (int) xOffset,
                sy - margin,
                sx + (int) xOffset + margin,
                sy + (int) textHeight + margin,
                ColorHelper.Argb.getArgb(128, 0, 0, 0)
        );

        ctx.drawText(
                tr,
                txt,
                sx - (int) xOffset,
                sy,
                color,
                true
        );
    }

    private static void renderBeam(Waypoint wp, WorldRenderContext ctx, float delta, VertexConsumerProvider consumers){
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        MatrixStack ms = ctx.matrixStack();

        // Get Translations
        double bx = wp.coordinates.x - cam.x;
        double by = wp.coordinates.y - cam.y;
        double bz = wp.coordinates.z - cam.z;

        ms.push();


        // Get Alpha
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        double distance = Math.sqrt(Math.pow(player.getX() - wp.coordinates.x, 2) + Math.pow(player.getZ() - wp.coordinates.z, 2));
        float alpha = Math.max((float)(1 - Math.pow(0.5, Math.max(0, distance - 5))), 0.1f);

        if(distance > OnlyWaypointsClient.options().dontRenderAfterDistance)
            return;

        ms.translate(bx, by, bz);

        BeamRenderer.renderBeamWithOpacity(
                ms,
                consumers,
                BeaconBlockEntityRenderer.BEAM_TEXTURE,
                delta,
                1.0F,
                (long)unfrozenTime,
                0,
                BeaconBlockEntityRenderer.MAX_BEAM_HEIGHT,
                wp.color, // Usar el color del waypoint
                alpha,
                0.15F,
                0.175F
        );

        ms.pop();
    }

    public static void toggleGlobalVisibility(){
        shouldRenderGlobally = !shouldRenderGlobally;
    }
}
