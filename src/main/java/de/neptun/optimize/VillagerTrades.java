package de.neptun.optimize;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

/**
 * Macht Dörfler-(und Wanderhändler-)Handel unendlich: kein "ausverkauft", keine steigenden
 * Preise nach mehrfachem Handeln. Greift sowohl beim Erzeugen neuer Angebote als auch direkt
 * nach jedem abgeschlossenen Handel (dort würde Minecraft sonst den Preis nachträglich erhöhen).
 */
public final class VillagerTrades implements Listener {
    private final de.neptun.optimize.NeptunOptimize module;

    public VillagerTrades(de.neptun.optimize.NeptunOptimize module) {
        this.module = module;
    }

    private boolean enabled() {
        return module.getConfig().getBoolean("villagers.infinite-trades", true);
    }

    private MerchantRecipe unlimited(MerchantRecipe r) {
        MerchantRecipe copy = new MerchantRecipe(r.getResult(), 0, Integer.MAX_VALUE,
                r.hasExperienceReward(), r.getVillagerExperience(), 0f, 0, 0, r.shouldIgnoreDiscounts());
        copy.setIngredients(r.getIngredients());
        return copy;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAcquire(VillagerAcquireTradeEvent e) {
        if (!enabled()) return;
        e.setRecipe(unlimited(e.getRecipe()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onReplenish(VillagerReplenishTradeEvent e) {
        if (!enabled()) return;
        e.setRecipe(unlimited(e.getRecipe()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTrade(InventoryClickEvent e) {
        if (!enabled()) return;
        if (!(e.getInventory() instanceof MerchantInventory mi)) return;
        AbstractVillager villager = mi.getMerchant() instanceof AbstractVillager v ? v : null;
        if (villager == null) return;
        // Einen Tick warten, bis Minecraft Nutzung/Preis selbst erhoeht hat, dann zuruecksetzen.
        Bukkit.getScheduler().runTask(module.host(), () -> {
            for (MerchantRecipe r : villager.getRecipes()) {
                r.setUses(0);
                r.setDemand(0);
                r.setSpecialPrice(0);
            }
        });
    }
}
