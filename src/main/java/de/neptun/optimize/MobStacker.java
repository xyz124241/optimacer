package de.neptun.optimize;

import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.EntityType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ThreadLocalRandom;

/**
 * Mobs vom gleichen, konfigurierten Typ, die nah beieinander stehen, werden in regelmaessigen
 * Abstaenden zu EINEM Mob zusammengefasst: er bewegt sich nicht mehr (spart Pathfinding/AI) und
 * traegt ein "xN"-Namensschild. Toedlicher Treffer zieht nur EINEN davon ab (Loot+XP fuer einen
 * normalen Mob dieses Typs), der Rest bleibt stehen, bis der Stack aufgebraucht ist.
 */
public final class MobStacker implements Listener {
    private final NeptunOptimize module;
    private final NamespacedKey countKey;
    private BukkitTask task;

    private static final Map<EntityType, LootTables> LOOT = new EnumMap<>(EntityType.class);
    private static final Map<EntityType, Integer> XP = new EnumMap<>(EntityType.class);
    static {
        LOOT.put(EntityType.ZOMBIE, LootTables.ZOMBIE);
        LOOT.put(EntityType.SKELETON, LootTables.SKELETON);
        LOOT.put(EntityType.WITHER_SKELETON, LootTables.WITHER_SKELETON);
        LOOT.put(EntityType.SPIDER, LootTables.SPIDER);
        LOOT.put(EntityType.CREEPER, LootTables.CREEPER);
        LOOT.put(EntityType.HUSK, LootTables.HUSK);
        LOOT.put(EntityType.DROWNED, LootTables.DROWNED);
        LOOT.put(EntityType.STRAY, LootTables.STRAY);
        for (EntityType t : LOOT.keySet()) XP.put(t, 5);
    }

    public MobStacker(NeptunOptimize module) {
        this.module = module;
        this.countKey = new NamespacedKey(module.host(), "mob_stack_count");
    }

    private boolean enabled() {
        return module.getConfig().getBoolean("mobs.stack", true);
    }

    private Set<EntityType> configuredTypes() {
        Set<EntityType> out = EnumSet.noneOf(EntityType.class);
        for (String s : module.getConfig().getStringList("mobs.types")) {
            try {
                out.add(EntityType.valueOf(s.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                module.getLogger().warning("[optimize] Unbekannter Mob-Typ in mobs.types: " + s);
            }
        }
        return out;
    }

    public void start() {
        long period = Math.max(20, module.getConfig().getLong("mobs.scan-interval-ticks", 100));
        task = module.host().getServer().getScheduler().runTaskTimer(module.host(), () -> scan(), period, period);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    public void reload() {
        stop();
        start();
    }

    private int countOf(LivingEntity e) {
        Integer v = e.getPersistentDataContainer().get(countKey, PersistentDataType.INTEGER);
        return v != null ? v : 1;
    }

    private void setCount(LivingEntity e, int count) {
        e.getPersistentDataContainer().set(countKey, PersistentDataType.INTEGER, count);
        String name = prettyName(e.getType()) + " " + ChatColor.YELLOW + "x" + count;
        if (count > 1) {
            e.setCustomName(name);
            e.setCustomNameVisible(true);
            if (e instanceof Mob mob) mob.setAware(false);
            e.setAI(false);
        } else {
            e.setCustomName(null);
            e.setCustomNameVisible(false);
            e.setAI(true);
            if (e instanceof Mob mob) mob.setAware(true);
        }
    }

    private String prettyName(EntityType type) {
        String s = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private boolean eligible(LivingEntity e) {
        if (!(e instanceof Mob)) return false;
        if (!LOOT.containsKey(e.getType())) return false;
        if (e.isDead()) return false;
        if (e.getScoreboardTags().contains("no_stack")) return false;
        return true;
    }

    private void scan() {
        if (!enabled()) return;
        Set<EntityType> types = configuredTypes();
        if (types.isEmpty()) return;
        double radius = module.getConfig().getDouble("mobs.stack-radius", 4.0);
        int max = Math.max(1, module.getConfig().getInt("mobs.max-stack", 200));

        for (org.bukkit.World world : module.host().getServer().getWorlds()) {
            Set<LivingEntity> handled = new HashSet<>();
            for (Entity e : world.getEntities()) {
                if (!(e instanceof LivingEntity base) || !types.contains(e.getType())) continue;
                if (handled.contains(base) || !eligible(base)) continue;

                int total = countOf(base);
                boolean merged = false;
                for (Entity near : base.getNearbyEntities(radius, radius, radius)) {
                    if (!(near instanceof LivingEntity other) || !types.contains(other.getType())) continue;
                    if (other.equals(base) || handled.contains(other) || !eligible(other)) continue;
                    if (total >= max) break;
                    total += countOf(other);
                    handled.add(other);
                    other.remove();
                    merged = true;
                }
                if (merged) setCount(base, Math.min(total, max));
                handled.add(base);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!enabled()) return;
        if (!(e.getEntity() instanceof LivingEntity living)) return;
        int count = countOf(living);
        if (count <= 1) return;
        double after = living.getHealth() - e.getFinalDamage();
        if (after > 0) return; // ueberlebt den Treffer, normal weiterverarbeiten

        e.setCancelled(true);
        int remaining = count - 1;
        Location loc = living.getLocation();

        dropLootFor(living, loc);

        if (remaining <= 0) {
            living.getPersistentDataContainer().remove(countKey);
            living.setAI(true);
            living.remove();
            return;
        }
        setCount(living, remaining);
        living.setHealth(Math.max(1.0, living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : living.getHealth()));
        living.playEffect(EntityEffect.HURT);
    }

    private void dropLootFor(LivingEntity living, Location loc) {
        LootTables table = LOOT.get(living.getType());
        if (table != null) {
            try {
                LootTable lt = table.getLootTable();
                LootContext ctx = new LootContext.Builder(loc).lootedEntity(living).build();
                Collection<org.bukkit.inventory.ItemStack> drops = lt.populateLoot(ThreadLocalRandom.current(), ctx);
                for (org.bukkit.inventory.ItemStack it : drops) {
                    loc.getWorld().dropItemNaturally(loc, it);
                }
            } catch (Throwable t) {
                module.getLogger().warning("[optimize] Konnte Loot fuer " + living.getType() + " nicht erzeugen: " + t);
            }
        }
        int xp = XP.getOrDefault(living.getType(), 5);
        if (xp > 0) {
            org.bukkit.entity.ExperienceOrb orb = loc.getWorld().spawn(loc, org.bukkit.entity.ExperienceOrb.class);
            orb.setExperience(xp);
        }
    }
}
