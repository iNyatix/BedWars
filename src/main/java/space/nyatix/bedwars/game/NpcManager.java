package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.listener.BedBreakEffectsListener;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NpcManager {

    public static final String ITEM_SHOP_NAME = "§b§lSKLEP Z PRZEDMIOTAMI §7(PPM)";

    public static final String UPGRADE_SHOP_NAME = "§b§lULEPSZENIA DRUZYNY §7(PPM)";

    private final BedWarsPlugin plugin;

    private final List<Villager> spawned = new ArrayList<>();

    private final Map<UUID, Location> anchors = new HashMap<>();

    private int task = -1;

    public NpcManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new BedBreakEffectsListener(plugin), plugin);
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::watchdog, 40L, 20L);
    }

    public void respawn() {
        remove();
        final Arena arena = this.plugin.game().active();
        if (arena == null) {
            return;
        }
        for (final Location l : arena.getItemShops()) {
            spawn(l, true);
        }
        for (final Location l : arena.getUpgradeShops()) {
            spawn(l, false);
        }
        this.plugin.getLogger().info(Messages.get("npc.respawn.npc-sklepow-utworzono", "VALUE1", this.spawned.size(), "VALUE2", (arena.getItemShops().size() + arena.getUpgradeShops().size())));
    }

    public void spawn(final Location configured, final boolean item) {
        if (configured == null || configured.getWorld() == null) {
            return;
        }
        final Location loc = new Location(configured.getWorld(), configured.getBlockX() + 0.5D, configured.getY(), configured.getBlockZ() + 0.5D,
                configured.getYaw(), configured.getPitch());
        try {
            final Villager villager = this.plugin.worldRules() == null ? loc.getWorld().spawn(loc, Villager.class) : this.plugin.worldRules().spawnGameMob(loc,
                    Villager.class);
            villager.setAdult();
            villager.setProfession(Villager.Profession.BLACKSMITH);
            villager.setCustomNameVisible(true);
            villager.setMetadata("bw-shop-kind", new FixedMetadataValue(this.plugin, item ? "items" : "upgrades"));
            villager.setCustomName(item ? Messages.get("npc.items.name") : Messages.get("npc.upgrades.name"));
            villager.setCanPickupItems(false);
            invokeOptional(villager, "setRemoveWhenFarAway", new Class[] { boolean.class }, new Object[] { Boolean.FALSE });
            invokeOptional(villager, "setAI", new Class[] { boolean.class }, new Object[] { Boolean.FALSE });
            this.spawned.add(villager);
            this.anchors.put(villager.getUniqueId(), loc.clone());
        } catch (final Throwable ex) {
            this.plugin.getLogger().warning(Messages.get("npc.spawn.nie-udalo-sie-zespawnowac-npc-sklepu", "VALUE1", ex.getClass().getSimpleName(),
                    "ERROR", ex.getMessage()));
        }
    }

    private void watchdog() {
        final Arena arena = this.plugin.game() == null ? null : this.plugin.game().active();
        if (arena == null) {
            return;
        }
        final int expected = arena.getItemShops().size() + arena.getUpgradeShops().size();
        boolean bound = false;
        for (final Location l : arena.getItemShops()) {
            if (l != null && l.getWorld() != null) {
                bound = true;
                break;
            }
        }
        if (!bound) {
            for (final Location l : arena.getUpgradeShops()) {
                if (l != null && l.getWorld() != null) {
                    bound = true;
                    break;
                }
            }
        }
        if (expected > 0 && !bound) {
            return;
        }
        int alive = 0;
        for (final Villager villager : new ArrayList<>(this.spawned)) {
            if (villager == null || villager.isDead() || !villager.isValid()) {
                continue;
            }
            alive++;
            final Location anchor = this.anchors.get(villager.getUniqueId());
            if (anchor != null && anchor.getWorld() != null && villager.getWorld().equals(anchor.getWorld())) {
                try {
                    if (villager.getLocation().distanceSquared(anchor) > .04D) {
                        villager.teleport(anchor);
                    }
                } catch (final Throwable ignored) {
                }
            }
        }
        if (expected > 0 && alive != expected) {
            respawn();
        }
    }

    public boolean isShop(final Villager villager) {
        final String n = villager == null ? null : villager.getCustomName();
        return villager != null && villager.hasMetadata("bw-shop-kind") || ITEM_SHOP_NAME.equals(n) || UPGRADE_SHOP_NAME.equals(n);
    }

    public boolean isItemShop(final Villager villager) {
        return villager != null && (villager.hasMetadata("bw-shop-kind") ? villager.getMetadata("bw-shop-kind").get(0).asString().equals("items") : ITEM_SHOP_NAME.equals(villager.getCustomName()));
    }

    public void remove() {
        for (final Villager villager : new ArrayList<>(this.spawned)) {
            try {
                if (villager != null && !villager.isDead()) {
                    villager.remove();
                }
            } catch (final Throwable ignored) {
            }
        }
        this.spawned.clear();
        this.anchors.clear();
    }

    public void shutdown() {
        if (this.task != -1) {
            Bukkit.getScheduler().cancelTask(this.task);
        }
        remove();
    }

    private void invokeOptional(final Object target, final String name, final Class<?>[] types, final Object[] args) {
        try {
            final Method method = target.getClass().getMethod(name, types);
            method.invoke(target, args);
        } catch (final Throwable ignored) {
        }
    }
}
