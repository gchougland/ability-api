package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.config.MiningFortuneConfig;
import com.hexvane.abilityapi.systems.AbilityConditionService;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.PhysicsDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.SoftBlockDropType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Grants extra block drops for players with mining_fortune when they break blocks that are in the
 * configured "fortune-affected" list (e.g. ores). Does not apply to every block.
 */
public class MiningFortuneEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public MiningFortuneEventSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {
        if (event.isCancelled()) return;

        if (!MiningFortuneConfig.getAffectedBlockIds().contains(event.getBlockType().getId())) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        PlayerRef playerRefComponent = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        var abilityValue = AbilityConditionService.getActiveAbilityValue(ref, store, world, playerRefComponent.getUuid(), "mining_fortune");
        if (abilityValue == null || !abilityValue.isPresent() || !(abilityValue.getRaw() instanceof Number n)) return;

        int level = n.intValue();
        if (level < 1) return;

        BlockType blockType = event.getBlockType();
        DropParams dropParams = getDropParams(blockType);
        if (dropParams == null) return;

        Vector3d position = event.getTargetBlock().toVector3d().add(0.5, 0.0, 0.5);

        for (int i = 0; i < level; i++) {
            List<ItemStack> drops = BlockHarvestUtils.getDrops(blockType, dropParams.quantity, dropParams.itemId, dropParams.dropListId);
            if (drops.isEmpty()) continue;
            Holder<EntityStore>[] holders = ItemComponent.generateItemDrops(store, drops, position, Vector3f.ZERO);
            if (holders != null && holders.length > 0) {
                commandBuffer.addEntities(holders, AddReason.SPAWN);
            }
        }
    }

    @Nullable
    private static DropParams getDropParams(@Nonnull BlockType blockType) {
        BlockGathering gathering = blockType.getGathering();
        if (gathering == null) return null;

        int quantity = 1;
        String itemId = null;
        String dropListId = null;

        PhysicsDropType physics = gathering.getPhysics();
        BlockBreakingDropType breaking = gathering.getBreaking();
        SoftBlockDropType soft = gathering.getSoft();
        HarvestingDropType harvest = gathering.getHarvest();

        if (physics != null) {
            itemId = physics.getItemId();
            dropListId = physics.getDropListId();
        } else if (breaking != null) {
            quantity = breaking.getQuantity();
            itemId = breaking.getItemId();
            dropListId = breaking.getDropListId();
        } else if (soft != null) {
            itemId = soft.getItemId();
            dropListId = soft.getDropListId();
        } else if (harvest != null) {
            itemId = harvest.getItemId();
            dropListId = harvest.getDropListId();
        }

        if (itemId == null && dropListId == null) {
            return null;
        }
        return new DropParams(quantity, itemId, dropListId);
    }

    private static final class DropParams {
        final int quantity;
        final String itemId;
        final String dropListId;

        DropParams(int quantity, String itemId, String dropListId) {
            this.quantity = quantity;
            this.itemId = itemId;
            this.dropListId = dropListId;
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
