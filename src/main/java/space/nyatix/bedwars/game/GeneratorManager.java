package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.listener.ExplosionJumpListener;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.GeneratorType;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.model.TeamData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GeneratorManager {

    private final BedWarsPlugin plugin;

    private final Map<String, Integer> counters = new HashMap<>();

    private final Map<String, Double> baseProgress = new HashMap<>();

    private final int baseTask;

    private final Map<String, GeneratorHologram> holograms = new HashMap<>();

    private String hologramWorld;

    public GeneratorManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new ExplosionJumpListener(plugin), plugin);
        this.baseTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::tickBaseGenerators, 1L, 1L);
    }

    public void tick() {
        if (this.plugin.game().state() != GameState.RUNNING || this.plugin.game().active() == null) {
            clearHolograms();
            return;
        }
        final Arena arena = this.plugin.game().active();
        for (final GeneratorType type : new GeneratorType[] { GeneratorType.DIAMOND, GeneratorType.EMERALD }) {
            for (final Location location : arena.getGenerators().get(type)) {
                spawnIfDue(location, type, interval(type), Options.integer(this.plugin, "generators.neutral.cap", 48, 1, 4096), false);
            }
        }
        if (Options.enabled(this.plugin, "generators.holograms.enabled", true)) {
            refreshHolograms(arena);
        } else {
            clearHolograms();
        }
    }

    private void tickBaseGenerators() {
        if (this.plugin.game().state() != GameState.RUNNING || this.plugin.game().active() == null) {
            return;
        }
        for (final TeamColor color : TeamColor.values()) {
            final TeamData team = this.plugin.game().active().team(color);
            final Location location = team.getBaseGenerator();
            if (location == null) {
                continue;
            }
            final int forge = team.getForgeLevel();
            spawnBaseIfDue(location, GeneratorType.IRON, Options.tier(this.plugin, "generators.base.ironSeconds", forge, new int[] { 2, 1,
                    1, 1, 1 }, 1, 3600), Options.integer(this.plugin, "generators.base.ironCap", 64, 1, 4096));
            spawnBaseIfDue(location, GeneratorType.GOLD, Options.tier(this.plugin, "generators.base.goldSeconds", forge, new int[] { 8, 7,
                    6, 5, 4 }, 1, 3600), Options.integer(this.plugin, "generators.base.goldCap", 32, 1, 4096));
            if (forge >= Options.integer(this.plugin, "generators.base.emeraldForgeLevel", 4, 0, 4)) {
                spawnBaseIfDue(location, GeneratorType.EMERALD, Options.integer(this.plugin, "generators.base.emeraldSeconds", 35, 1, 3600),
                        Options.integer(this.plugin, "generators.base.emeraldCap", 8, 1, 4096));
            }
        }
    }

    private void spawnBaseIfDue(final Location location, final GeneratorType type, final int seconds, final int cap) {
        if (location.getWorld() == null) {
            return;
        }
        final String key = counterKey(location, type);
        final Double old = this.baseProgress.get(key);
        final double progress = (old == null ? 0D : old) + this.plugin.settings().baseGeneratorSpeed();
        final double interval = seconds * 20D;
        if (progress < interval) {
            this.baseProgress.put(key, progress);
            return;
        }
        this.baseProgress.put(key, progress % interval);
        spawnResource(location, type, cap, true);
    }

    private int interval(final GeneratorType generatorType) {
        final int e = this.plugin.game().elapsed();
        if (generatorType == GeneratorType.DIAMOND) {
            return Options.tier(this.plugin, "generators.neutral.diamondSeconds", tier(generatorType) - 1, new int[] { 30, 18, 12 }, 1, 3600);
        }
        return Options.tier(this.plugin, "generators.neutral.emeraldSeconds", tier(generatorType) - 1, new int[] { 65, 45, 35 }, 1, 3600);
    }

    private int tier(final GeneratorType generatorType) {
        final int e = this.plugin.game().elapsed();
        if (generatorType == GeneratorType.DIAMOND) {
            return e >= this.plugin.settings().timer("diamond3") ? 3 : e >= this.plugin.settings().timer("diamond2") ? 2 : 1;
        }
        return e >= this.plugin.settings().timer("emerald3") ? 3 : e >= this.plugin.settings().timer("emerald2") ? 2 : 1;
    }

    private void spawnIfDue(final Location location, final GeneratorType generatorType, final int seconds, final int cap, final boolean splitPlayers) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        final String k = counterKey(location, generatorType);
        final int n = this.counters.containsKey(k) ? this.counters.get(k) + 1 : 1;
        if (n < seconds) {
            this.counters.put(k, n);
            return;
        }
        this.counters.put(k, 0);
        spawnResource(location, generatorType, cap, splitPlayers);
    }

    private void spawnResource(final Location location, final GeneratorType generatorType, final int cap, final boolean splitPlayers) {
        final Material material = generatorType == GeneratorType.IRON ? Material.IRON_INGOT : generatorType == GeneratorType.GOLD ? Material.GOLD_INGOT : generatorType == GeneratorType.DIAMOND ? Material.DIAMOND : Material.EMERALD;
        int nearby = 0;
        for (final Entity entity : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
            if (entity instanceof Item && ((Item) entity).getItemStack().getType() == material) {
                nearby += ((Item) entity).getItemStack().getAmount();
            }
        }
        if (nearby >= cap) {
            return;
        }
        if (splitPlayers && Options.enabled(this.plugin, "generators.sharing.enabled", true) && giveSplitResource(location, material)) {
            return;
        }
        location.getWorld().dropItem(location.clone().add(0, .2, 0), new ItemStack(material));
    }

    private boolean giveSplitResource(final Location generator, final Material material) {
        if (generator == null || generator.getWorld() == null) {
            return false;
        }
        final double radius = Options.number(this.plugin, "generators.sharing.radius", 2.35D, 0.0D, 16.0D);
        final double radiusSq = radius * radius;
        final List<Player> eligible = new ArrayList<>();
        for (final Player player : generator.getWorld().getPlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            final UUID playerId = player.getUniqueId();
            if (!this.plugin.game().participants().contains(playerId)) {
                continue;
            }
            if (this.plugin.game().isFinalDead(playerId) || this.plugin.game().isSpectator(playerId) || this.plugin.game().isRespawning(playerId)) {
                continue;
            }
            final Location location = player.getLocation();
            if (location == null || location.getWorld() == null || !location.getWorld().equals(generator.getWorld())) {
                continue;
            }
            final double dx = location.getX() - generator.getX();
            final double dy = location.getY() - generator.getY();
            final double dz = location.getZ() - generator.getZ();
            if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                eligible.add(player);
            }
        }
        if (eligible.isEmpty()) {
            return false;
        }
        for (final Player player : eligible) {
            final ItemStack one = new ItemStack(material, 1);
            final Map<Integer, ItemStack> left = player.getInventory().addItem(one);
            if (left != null && !left.isEmpty()) {
                for (final ItemStack stack : left.values()) {
                    if (stack != null && stack.getAmount() > 0) {
                        generator.getWorld().dropItem(player.getLocation(), stack);
                    }
                }
            }
        }
        return true;
    }

    private String counterKey(final Location location, final GeneratorType generatorType) {
        return generatorType.name() + ":" + location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private int secondsLeft(final Location location, final GeneratorType generatorType) {
        final int seconds = interval(generatorType);
        final Integer n = this.counters.get(counterKey(location, generatorType));
        return Math.max(1, seconds - (n == null ? 0 : n));
    }

    private void refreshHolograms(final Arena arena) {
        final World world = runtimeWorld(arena);
        if (world == null) {
            clearHolograms();
            return;
        }
        if (this.hologramWorld == null || !this.hologramWorld.equals(world.getName())) {
            clearHolograms();
            if (this.plugin.configHolograms() != null) {
                this.plugin.configHolograms().purgeConfigMarkers(world);
            }
            this.hologramWorld = world.getName();
        }
        final Set<String> wanted = new HashSet<>();
        for (final GeneratorType type : new GeneratorType[] { GeneratorType.DIAMOND, GeneratorType.EMERALD }) {
            for (final Location location : arena.getGenerators().get(type)) {
                if (location == null || location.getWorld() == null) {
                    continue;
                }
                final String key = counterKey(location, type);
                wanted.add(key);
                GeneratorHologram holo = this.holograms.get(key);
                if (holo == null || !holo.valid()) {
                    if (holo != null) {
                        holo.remove();
                    }
                    holo = createHologram(location, type);
                    this.holograms.put(key, holo);
                }
                holo.update(type, tier(type), secondsLeft(location, type));
            }
        }
        final Iterator<Map.Entry<String, GeneratorHologram>> it = this.holograms.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, GeneratorHologram> e = it.next();
            if (!wanted.contains(e.getKey())) {
                e.getValue().remove();
                it.remove();
            }
        }
    }

    private World runtimeWorld(final Arena arena) {
        if (arena == null) {
            return null;
        }
        if (arena.getRuntimeWorld() != null) {
            final World world = Bukkit.getWorld(arena.getRuntimeWorld());
            if (world != null) {
                return world;
            }
        }
        return arena.getLobby() == null ? null : arena.getLobby().getWorld();
    }

    private GeneratorHologram createHologram(final Location base, final GeneratorType type) {
        final ArmorStand top = stand(base.clone().add(0.5D, 3.10D, 0.5D));
        final ArmorStand middle = stand(base.clone().add(0.5D, 2.82D, 0.5D));
        final ArmorStand bottom = stand(base.clone().add(0.5D, 2.54D, 0.5D));
        return new GeneratorHologram(top, middle, bottom);
    }

    private ArmorStand stand(final Location location) {
        final ArmorStand s = location.getWorld().spawn(location, ArmorStand.class);
        s.setVisible(false);
        s.setGravity(false);
        s.setSmall(true);
        s.setMarker(true);
        s.setBasePlate(false);
        s.setCustomNameVisible(true);
        return s;
    }

    public void clear() {
        this.counters.clear();
        this.baseProgress.clear();
        clearHolograms();
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTask(this.baseTask);
        clear();
    }

    public void clearHolograms() {
        for (final GeneratorHologram h : new ArrayList<>(this.holograms.values())) {
            h.remove();
        }
        this.holograms.clear();
        this.hologramWorld = null;
    }

    private static class GeneratorHologram {

        private final ArmorStand top, middle, bottom;

        GeneratorHologram(final ArmorStand a, final ArmorStand b, final ArmorStand c) {
            this.top = a;
            this.middle = b;
            this.bottom = c;
        }

        boolean valid() {
            return this.top != null && this.middle != null && this.bottom != null && !this.top.isDead() && !this.middle.isDead() && !this.bottom.isDead();
        }

        void update(final GeneratorType type, final int tier, final int seconds) {
            this.top.setCustomName(Messages.get("generator.update.poziom", "VALUE1", roman(tier)));
            this.middle.setCustomName(type == GeneratorType.DIAMOND ? Messages.get("generator.update.diament") : Messages.get("generator.update.szmaragd"));
            this.bottom.setCustomName(Messages.get("generator.update.pojawi-sie-za-s", "SECONDS", seconds));
        }

        void remove() {
            try {
                if (this.top != null) {
                    this.top.remove();
                }
            } catch (final Throwable ignored) {
            }
            try {
                if (this.middle != null) {
                    this.middle.remove();
                }
            } catch (final Throwable ignored) {
            }
            try {
                if (this.bottom != null) {
                    this.bottom.remove();
                }
            } catch (final Throwable ignored) {
            }
        }

        private static String roman(final int i) {
            return i >= 3 ? "III" : i == 2 ? "II" : "I";
        }
    }
}
