package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.system.PlayerSpatialSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Pulls item entities toward the closest player with the item_magnet ability within magnet range.
 * Lerps item position toward the player each tick (same idea as pickup fly-to-player), no physics.
 * Uses ItemComponent-only query; exclusions (Interactable, PreventPickup) applied in tick.
 * Runs after PlayerSpatialSystem. Must run on world thread (EntityTickingSystem).
 */
public class ItemMagnetSystem extends EntityTickingSystem<EntityStore> {

    /** Base magnet radius in blocks when ability value is 1.0. */
    private static final double BASE_MAGNET_RANGE = 8.0;
    /** Maximum ability value for computing max query radius. */
    private static final double MAX_ABILITY_VALUE = 5.0;
    /** Stop lerping when item is within this distance so it enters normal pickup range. */
    private static final double PICKUP_RANGE_THRESHOLD = 0.5;
    /** Lerp speed in blocks per second (items move this far toward player per second). */
    private static final double LERP_SPEED = 3.0;

    @Nonnull
    private final Query<EntityStore> query;
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public ItemMagnetSystem() {
        EntityModule entityModule = EntityModule.get();
        this.query = entityModule.getItemComponentType();
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, PlayerSpatialSystem.class, OrderPriority.CLOSEST)
        );
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();
        if (world == null) return;

        Ref<EntityStore> itemRef = archetypeChunk.getReferenceTo(index);
        if (itemRef == null || !itemRef.isValid()) return;

        if (store.getComponent(itemRef, Interactable.getComponentType()) != null) return;
        if (store.getComponent(itemRef, PreventPickup.getComponentType()) != null) return;

        TransformComponent itemTransform = store.getComponent(itemRef, TransformComponent.getComponentType());
        if (itemTransform == null) return;

        Vector3d itemPosition = itemTransform.getPosition();

        ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> playerSpatialResourceType =
            EntityModule.get().getPlayerSpatialResourceType();
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
            store.getResource(playerSpatialResourceType);
        if (playerSpatialResource == null) return;

        SpatialStructure<Ref<EntityStore>> spatialStructure = playerSpatialResource.getSpatialStructure();
        double maxMagnetRadius = BASE_MAGNET_RANGE * MAX_ABILITY_VALUE;
        List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
        spatialStructure.ordered(itemPosition, maxMagnetRadius, playerRefs);

        Ref<EntityStore> targetPlayerRef = null;

        for (Ref<EntityStore> playerRef : playerRefs) {
            if (!playerRef.isValid()) continue;
            if (store.getArchetype(playerRef).contains(DeathComponent.getComponentType())) continue;

            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent == null) continue;

            AbilityValue abilityValue = AbilityConditionService.getActiveAbilityValue(
                playerRef, store, world, playerRefComponent.getUuid(), "item_magnet");
            if (abilityValue == null || !abilityValue.isPresent() || !(abilityValue.getRaw() instanceof Number n)) continue;

            double value = n.doubleValue();
            if (value < 1.0) continue;

            double effectiveRadius = BASE_MAGNET_RANGE * value;
            TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
            if (playerTransform == null) continue;
            double distanceSq = playerTransform.getPosition().distanceSquaredTo(itemPosition);
            if (distanceSq > effectiveRadius * effectiveRadius) continue;

            targetPlayerRef = playerRef;
            break;
        }

        if (targetPlayerRef == null || !targetPlayerRef.isValid()) return;

        TransformComponent targetTransform = store.getComponent(targetPlayerRef, TransformComponent.getComponentType());
        if (targetTransform == null) return;

        Vector3d targetPosition = targetTransform.getPosition();
        Vector3d direction = targetPosition.clone().subtract(itemPosition);
        double distance = direction.length();
        if (distance < 1e-6) return;
        if (distance <= PICKUP_RANGE_THRESHOLD) return;

        direction.scale(1.0 / distance);

        double moveAmount = Math.min(LERP_SPEED * dt, distance - PICKUP_RANGE_THRESHOLD);
        itemPosition.add(
            direction.getX() * moveAmount,
            direction.getY() * moveAmount,
            direction.getZ() * moveAmount
        );
    }
}
