package de.neptun.optimize;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Eigenständiges Performance-Plugin: unendliches Dörfler-Handeln, XP-Orb-Zusammenlegen,
 * Item- und Mob-Stacking (gleiche Items/Mobs werden zu einem mit "xN"-Anzeige zusammengefasst)
 * und etwas weniger Kollisions-Rechenlast für weit entfernte Mobs.
 *
 * Für das Vorladen/Vorgenerieren von Chunks gibt es bewusst keinen eigenen Code hier - dafür
 * ist das bewährte Chunky-Plugin die bessere Wahl.
 */
public final class NeptunOptimize extends JavaPlugin {

    private VillagerTrades villagerTrades;
    private XpMerger xpMerger;
    private ItemMerger itemMerger;
    private MobStacker mobStacker;
    private EntityOptimizer entityOptimizer;

    /** Für die Unterklassen: dieselbe Methode wie bei einem NeptunCore-Modul, gibt einfach sich selbst zurück. */
    public NeptunOptimize host() {
        return this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        villagerTrades = new VillagerTrades(this);
        xpMerger = new XpMerger(this);
        itemMerger = new ItemMerger(this);
        mobStacker = new MobStacker(this);
        entityOptimizer = new EntityOptimizer(this);

        getServer().getPluginManager().registerEvents(villagerTrades, this);
        getServer().getPluginManager().registerEvents(xpMerger, this);
        getServer().getPluginManager().registerEvents(itemMerger, this);
        getServer().getPluginManager().registerEvents(mobStacker, this);

        mobStacker.start();
        entityOptimizer.start();

        getLogger().info("NeptunOptimize aktiv.");
    }

    @Override
    public void onDisable() {
        if (mobStacker != null) mobStacker.stop();
        if (entityOptimizer != null) entityOptimizer.stop();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("neptunoptimize.admin")) {
            sender.sendMessage(color("&cKeine Berechtigung."));
            return true;
        }
        reloadConfig();
        if (mobStacker != null) mobStacker.reload();
        if (entityOptimizer != null) entityOptimizer.reload();
        sender.sendMessage(color("&a[NeptunOptimize] Konfiguration neu geladen."));
        return true;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
