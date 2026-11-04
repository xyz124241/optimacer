package de.neptun.optimize;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Serverseitige "Entity Culling light": Mobs, die weit von jedem Spieler entfernt sind, bekommen
 * ihre Kollisionsabfrage abgeschaltet (setCollidable(false)). Das spart dem Server die laufende
 * Berechnung, wer wen gerade schubst - eine echte, clientunabhaengige Rechenlast. Eine echte
 * Sichtbarkeits-Kulisse (Mobs hinter Waenden ausblenden) kann ein Server-Plugin dagegen nicht
 * leisten, das muesste auf dem Client passieren.
 */
public final class EntityOptimizer {
    private final NeptunOptimize module;
    private BukkitTask task;

    public EntityOptimizer(NeptunOptimize module) {
        this.module = module;
    }

    private boolean enabled() {
        return module.getConfig().getBoolean("entities.reduce-collision", true);
    }

    public void start() {
        long period = Math.max(10, module.getConfig().getLong("entities.check-interval-ticks", 40));
        task = module.host().getServer().getScheduler().runTaskTimer(module.host(), () -> tick(), period, period);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    public void reload() {
        stop();
        start();
    }

    private void tick() {
        if (!enabled()) return;
        double distance = module.getConfig().getDouble("entities.collision-distance", 24.0);
        double sq = distance * distance;

        for (World world : module.host().getServer().getWorlds()) {
            if (world.getPlayers().isEmpty()) continue;
            for (Entity e : world.getEntities()) {
                if (!(e instanceof LivingEntity living) || e instanceof Player) continue;
                boolean nearPlayer = false;
                Location loc = living.getLocation();
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) <= sq) {
                        nearPlayer = true;
                        break;
                    }
                }
                living.setCollidable(nearPlayer);
            }
        }
    }
}
