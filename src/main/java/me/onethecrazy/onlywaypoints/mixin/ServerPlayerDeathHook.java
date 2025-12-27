package me.onethecrazy.onlywaypoints.mixin;

import me.onethecrazy.onlywaypoints.OnlyWaypointsServer;
import me.onethecrazy.onlywaypoints.waypoints.objects.Coordinates;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerDeathHook extends LivingEntity {

    protected ServerPlayerDeathHook(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Coordinates coords = new Coordinates(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        RegistryKey<World> dimension = player.getWorld().getRegistryKey();

        OnlyWaypointsServer.getWaypointManager().updateLastDeathWaypoint(coords, dimension);
    }
}
