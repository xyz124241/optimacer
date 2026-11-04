package de.neptun.optimize;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Wenn ein neuer XP-Orb spawnt und in der Naehe schon einer liegt, wird die Erfahrung einfach
 * in den vorhandenen gepackt statt einen zweiten Entity-Objekt zu erzeugen. Spart bei grossen
 * Grindspots (XP-Farmen, Mob-Stacks) sehr viele gleichzeitig existierende Entities.
 */
public final class XpMerger implements Listener {
    private final NeptunOptimize module;

    public XpMerger(NeptunOptimize module) {
        this.module = module;
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent e) {
        if (!module.getConfig().getBoolean("xp.merge", true)) return;
        if (!(e.getEntity() instanceof ExperienceOrb orb)) return;

        double radius = module.getConfig().getDouble("xp.merge-radius", 3.0);
        for (Entity near : orb.getNearbyEntities(radius, radius, radius)) {
            if (near instanceof ExperienceOrb other) {
                other.setExperience(other.getExperience() + orb.getExperience());
                e.setCancelled(true);
                return;
            }
        }
    }
}
