package com.hexvane.abilityapi.systems;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Applies the dark_vision entity effect to players who have the ability, giving a
 * full-screen ScreenEffect overlay (gamma-style brightening). Removes the effect when
 * they no longer have the ability. The overlay can white-wash; use a subtle PNG in
 * Common/ScreenEffects/hexvane_abilityapi_dark_vision.png. Improve when true per-player
 * night vision (or client support) becomes available in the API.
 */
public final class DarkVisionSystem extends EntityTickingSystem<EntityStore> {

    private static final String DARK_VISION_EFFECT_ID = "hexvane_abilityapi_dark_vision";
    private static final float EFFECT_DURATION_SEC = 3600f;

    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    private static final int CHECK_INTERVAL = 20;
    private static int tickCounter = 0;

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
            @Nonnull com.hypixel.hytale.component.Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (index == 0) {
            tickCounter = (tickCounter + 1) % CHECK_INTERVAL;
        }
        if (tickCounter != 0) return;

        World world = store.getExternalData().getWorld();
        if (world == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) return;

        String worldName = world.getName();
        AbilityValue ability = PlayerAbilityStorage.getAbility(playerRef.getUuid(), worldName, "dark_vision");
        boolean hasAbility = ability != null && ability.isPresent() && ability.asBoolean();

        EntityEffect effect = EntityEffect.getAssetMap().getAsset(DARK_VISION_EFFECT_ID);
        if (effect == null) return;

        EffectControllerComponent effectController = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (effectController == null) return;

        int effectIndex = EntityEffect.getAssetMap().getIndex(effect.getId());
        if (effectIndex == Integer.MIN_VALUE) return;

        boolean hasEffect = effectController.getActiveEffects().containsKey(effectIndex);

        if (hasAbility) {
            if (!hasEffect) {
                effectController.addEffect(ref, effectIndex, effect, EFFECT_DURATION_SEC, OverlapBehavior.OVERWRITE, commandBuffer);
            }
        } else {
            if (hasEffect) {
                effectController.removeEffect(ref, effectIndex, commandBuffer);
            }
        }
    }
}
