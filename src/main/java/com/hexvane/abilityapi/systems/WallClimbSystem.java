package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.physics.systems.IVelocityModifyingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import com.hypixel.hytale.protocol.ChangeVelocityType;

/**
 * Allows players with wall_climb to climb any solid surface (spider-style).
 * Uses the game's movement pipeline: Velocity.addInstruction() so that PlayerVelocityInstructionSystem
 * sends ChangeVelocity to the client (same path as knockback and launch pads). Runs as IVelocityModifyingSystem.
 * Uses game-scale vertical velocities so movement is clearly visible (stick ~1.0 to counteract gravity, climb ~0.4 up).
 */
public class WallClimbSystem extends EntityTickingSystem<EntityStore> implements IVelocityModifyingSystem {
    /** Query only PlayerRef so registration never sees null. */
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

    /** Horizontal distance to probe in front of player (block units) */
    private static final double PROBE_DISTANCE = 0.6;
    /** Base vertical velocity per tick when against wall and pressing W – slower so it's not too fast. */
    private static final double BASE_CLIMB_VELOCITY_Y = 1.2;
    /** Extra climb when jump held (small). */
    private static final double CLIMB_SPEED_UP = 0.2;
    /** Climb down when crouch held */
    private static final double CLIMB_SPEED_DOWN = 0.5;

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
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

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        if (!PlayerAbilityStorage.hasAbility(playerRefComponent.getUuid(), world.getName(), "wall_climb")) {
            return;
        }

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        MovementStatesComponent movementStatesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;

        var movementStates = movementStatesComponent.getMovementStates();
        // Only climb when actively pressing forward into the wall (W), not just standing against it
        boolean pressingForward = movementStates.walking || movementStates.running || movementStates.sprinting;
        if (!pressingForward) return;

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) return;

        Vector3d position = transformComponent.getPosition();
        Vector3f rotation = transformComponent.getRotation();
        float yaw = rotation.getYaw();
        double forwardX = -Math.sin(yaw);
        double forwardZ = -Math.cos(yaw);
        double len = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (len < 1e-6) return;
        forwardX /= len;
        forwardZ /= len;

        if (!isSolidWallInFront(world, position, forwardX, forwardZ)) {
            return;
        }

        Velocity velocityComponent = store.getComponent(ref, Velocity.getComponentType());
        if (velocityComponent == null) return;

        // Allow climb when on ground too – walk into wall with W to start climbing
        boolean jumpHeld = movementStates.jumping;
        boolean crouchHeld = movementStates.crouching;

        double vy = BASE_CLIMB_VELOCITY_Y;
        if (jumpHeld) vy += CLIMB_SPEED_UP;
        else if (crouchHeld) vy -= CLIMB_SPEED_DOWN;

        Vector3d climbVelocity = new Vector3d(0, vy, 0);
        velocityComponent.addInstruction(climbVelocity, null, ChangeVelocityType.Add);
    }

    /**
     * Returns true if there is a solid block in front of the player.
     * Only reads from chunks already in memory so we never trigger chunk loading during the tick.
     */
    private static boolean isSolidWallInFront(World world, Vector3d position, double forwardX, double forwardZ) {
        double probeX = position.x + forwardX * PROBE_DISTANCE;
        double probeZ = position.z + forwardZ * PROBE_DISTANCE;
        int blockX = MathUtil.floor(probeX);
        int blockZ = MathUtil.floor(probeZ);
        int blockYFeet = MathUtil.floor(position.y);
        int blockYHead = MathUtil.floor(position.y + 1.6);
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockX, blockZ));
        if (chunk == null) return false;
        for (int by = blockYFeet; by <= blockYHead; by++) {
            if (by < 0 || by >= 320) continue;
            int blockId = chunk.getBlock(blockX, by, blockZ);
            BlockType blockType = blockId != 0 ? BlockType.getAssetMap().getAsset(blockId) : null;
            if (blockType != null && blockType.getMaterial() == BlockMaterial.Solid) {
                return true;
            }
        }
        return false;
    }
}
