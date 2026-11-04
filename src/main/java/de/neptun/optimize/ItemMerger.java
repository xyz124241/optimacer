package de.neptun.optimize;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Legt gleiche Items am Boden zu EINEM Entity zusammen und zeigt die wahre Menge als Namensschild
 * ("x15") an - auch fuer Items, die eigentlich gar nicht stapeln (Werkzeuge, Waffen, Ruestung).
 * Beim Aufsammeln bekommt der Spieler trotzdem die volle, echte Menge.
 */
public final class ItemMerger implements Listener {
    private final NeptunOptimize module;
    private final NamespacedKey countKey;

    public ItemMerger(NeptunOptimize module) {
        this.module = module;
        this.countKey = new NamespacedKey(module.host(), "stack_count");
    }

    private int totalOf(Item item) {
        Integer v = item.getPersistentDataContainer().get(countKey, PersistentDataType.INTEGER);
        return v != null ? v : item.getItemStack().getAmount();
    }

    private void setTotal(Item item, int total) {
        item.getPersistentDataContainer().set(countKey, PersistentDataType.INTEGER, total);
        ItemStack st = item.getItemStack();
        st.setAmount(Math.max(1, Math.min(total, st.getType().getMaxStackSize())));
        item.setItemStack(st);
        if (total > 1) {
            item.setCustomName(ChatColor.YELLOW + "x" + total);
            item.setCustomNameVisible(true);
        } else {
            item.setCustomNameVisible(false);
        }
    }

    @EventHandler
    public void onSpawn(ItemSpawnEvent e) {
        if (!module.getConfig().getBoolean("items.merge", true)) return;
        Item item = e.getEntity();
        ItemStack stack = item.getItemStack();
        if (stack.getType().isAir()) return;

        double radius = module.getConfig().getDouble("items.merge-radius", 2.5);
        int cap = module.getConfig().getInt("items.merge-cap", 6400);

        for (Entity near : item.getNearbyEntities(radius, radius, radius)) {
            if (!(near instanceof Item other) || other.equals(item)) continue;
            if (!other.getItemStack().isSimilar(stack)) continue;
            int combined = totalOf(other) + stack.getAmount();
            if (combined > cap) continue;
            setTotal(other, combined);
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!module.getConfig().getBoolean("items.merge", true)) return;
        if (!(e.getEntity() instanceof Player p)) return;
        Item item = e.getItem();
        int total = totalOf(item);
        ItemStack base = item.getItemStack();
        if (total <= base.getAmount()) return; // nichts Besonderes gespeichert, normal weiterlaufen lassen

        e.setCancelled(true);
        item.remove();

        int max = Math.max(1, base.getType().getMaxStackSize());
        List<ItemStack> stacks = new ArrayList<>();
        int left = total;
        while (left > 0) {
            int n = Math.min(max, left);
            ItemStack copy = base.clone();
            copy.setAmount(n);
            stacks.add(copy);
            left -= n;
        }
        Location loc = p.getLocation();
        for (ItemStack s : stacks) {
            for (ItemStack leftover : p.getInventory().addItem(s).values()) {
                p.getWorld().dropItemNaturally(loc, leftover);
            }
        }
        p.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
    }
}
