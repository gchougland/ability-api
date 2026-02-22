package com.hexvane.abilityapi.listeners;

import com.hexvane.abilityapi.ability.AbilityValue;
import com.hexvane.abilityapi.data.PlayerAbilityStorage;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * When a player with unlimited_arrows consumes arrow ammo (detected via inventory change),
 * re-adds the consumed amount to the same container so they do not run out.
 * Handles both single SlotTransaction and ListTransaction (e.g. ListTransaction of MoveTransaction).
 */
public final class UnlimitedArrowsListener {

    private static boolean isArrowItem(@Nonnull String itemId) {
        return itemId != null && itemId.toLowerCase().contains("arrow");
    }

    public static void onLivingEntityInventoryChange(@Nonnull LivingEntityInventoryChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        if (player.getWorld() == null) return;
        com.hypixel.hytale.server.core.universe.PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) return;
        String worldName = player.getWorld().getName();
        AbilityValue ability = PlayerAbilityStorage.getAbility(playerRef.getUuid(), worldName, "unlimited_arrows");
        if (ability == null || !ability.isPresent() || !ability.asBoolean()) return;

        ItemContainer container = event.getItemContainer();
        if (container == null) return;

        Transaction transaction = event.getTransaction();
        if (transaction instanceof SlotTransaction slotTx) {
            refundArrowRemoval(slotTx, container);
        } else if (transaction instanceof ListTransaction<?> listTx) {
            for (Object raw : listTx.getList()) {
                if (raw instanceof SlotTransaction st) {
                    refundArrowRemoval(st, container);
                } else if (raw instanceof MoveTransaction<?> moveTx) {
                    refundArrowRemoval(moveTx.getRemoveTransaction(), container);
                }
            }
        }
    }

    private static void refundArrowRemoval(@Nonnull SlotTransaction slotTx, @Nonnull ItemContainer container) {
        if (slotTx.getAction() != ActionType.REMOVE) return;

        ItemStack before = slotTx.getSlotBefore();
        ItemStack after = slotTx.getSlotAfter();
        if (before == null || before.isEmpty()) return;
        if (!isArrowItem(before.getItemId())) return;

        int beforeQty = before.getQuantity();
        int afterQty = ItemStack.isEmpty(after) ? 0 : after.getQuantity();
        int consumed = beforeQty - afterQty;
        if (consumed <= 0) return;

        ItemStack toAdd = new ItemStack(before.getItemId(), consumed);
        container.addItemStack(toAdd);
    }
}
