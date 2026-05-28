package com.example.soulitem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.IronGolem;
import org.bukkit.block.data.BlockData;
import org.bukkit.FluidCollisionMode;
import org.bukkit.util.RayTraceResult;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager implements Listener {

    private final firepickaxePlugin plugin;
    // Per-player, per-ability cooldown tracking.
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public AbilityManager(firepickaxePlugin plugin) {
        this.plugin = plugin;
        startPassiveTask();
    }

    private boolean onCooldown(Player p, String ab, int seconds) {
        if (seconds <= 0) return false;
        Map<String, Long> map = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long now = System.currentTimeMillis();
        Long until = map.get(ab);
        if (until != null && until > now) return true;
        map.put(ab, now + seconds * 1000L);
        return false;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isCustomItem(item)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isCustomItem(item)) return;
        LivingEntity target = e.getEntity();

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isCustomItem(item)) return;
        Action a = e.getAction();

        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            if (p.isSneaking()) {
                // Portable Workbench
                if (!onCooldown(p, "OPEN_WORKBENCH", 0)) p.openWorkbench(null, true);
            } else {

            }
        } else if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {

        }
    }

    @EventHandler
    public void onHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isCustomItem(item)) return;

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isCustomItem(item)) return;
        Block block = e.getBlock();
        // FORTUNE - generic ability (auto-generated fallback)
        if (!onCooldown(p, "FORTUNE", 0)) {
            // Visual feedback that ability triggered
            p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0,1,0), 20, 0.5, 0.5, 0.5, 0.1);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            // Optional: deal damage to nearby enemies
            for (Entity nearby : p.getNearbyEntities(3, 3, 3)) {
                if (nearby instanceof LivingEntity le && le != p) {
                    le.damage(3, p);
                }
            }
        }
        // Auto Smelt (basic ores)
        Material drop = block.getType();
        ItemStack smelted = switch (drop) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> new ItemStack(Material.COPPER_INGOT);
            case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_SCRAP);
            default -> null;
        };
        if (smelted != null) {
            e.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation(), smelted);
        }
    }

    private void startPassiveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ItemStack item = p.getInventory().getItemInMainHand();
                    if (!plugin.getItemManager().isCustomItem(item)) continue;

                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private static PotionEffectType effect(String name) {
        try { return PotionEffectType.getByName(name); } catch (Throwable ignored) {}
        try {
            // 1.20.5+ uses Registry lookups; fall back to reflection.
            var f = PotionEffectType.class.getField(name);
            return (PotionEffectType) f.get(null);
        } catch (Throwable ignored) {}
        return null;
    }

    /** Resilient max-health lookup across MC versions (Attribute names changed in 1.21.3+). */
    private static double getMaxHealth(LivingEntity le) {
        Attribute a = null;
        try { a = Attribute.valueOf("GENERIC_MAX_HEALTH"); } catch (Throwable ignored) {}
        if (a == null) try { a = Attribute.valueOf("MAX_HEALTH"); } catch (Throwable ignored) {}
        if (a != null) {
            var inst = le.getAttribute(a);
            if (inst != null) return inst.getValue();
        }
        return 20.0;
    }
}
