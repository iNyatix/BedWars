package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;

import java.util.Locale;

public class WorldRulesManager implements Listener {

    private static final String GAME_MOB = "bw-game-mob";

    private final BedWarsPlugin plugin;

    private final java.util.Set<String> restoredWeather = new java.util.HashSet<>();

    private final java.util.Set<String> frozenTime = new java.util.HashSet<>();

    private int task = -1, spawningGameMob;

    public WorldRulesManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        for (final World world : Bukkit.getWorlds()) {
            if (managed(world)) {
                prepare(world);
            }
        }
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            for (final World world : Bukkit.getWorlds()) {
                if (managed(world)) {
                    apply(world);
                }
            }
        }, 200L, 200L);
    }

    public boolean managed(final World world) {
        if (world == null) {
            return false;
        }
        final String name = world.getName();
        if (name.equals(this.plugin.getConfig().getString("lobby.joinLocation.world", "world"))) {
            return true;
        }
        if (this.plugin.game() == null) {
            return false;
        }
        for (final Arena arena : this.plugin.game().arenas().values()) {
            if (name.equals(arena.getWorld()) || name.equals(arena.getRuntimeWorld())) {
                return true;
            }
            String safe = arena.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
            if (safe.length() > 28) {
                safe = safe.substring(0, 28);
            }
            if (name.equals("bw_tpl_" + safe) || name.startsWith("bw_" + safe + "_")) {
                return true;
            }
        }
        return false;
    }

    public void prepare(final World world) {
        if (!managed(world)) {
            return;
        }
        apply(world);
        for (final Entity entity : world.getEntities()) {
            removeUnwantedMob(entity);
        }
    }

    private void apply(final World world) {
        if (Options.enabled(this.plugin, "world.disableWeather", true)) {
            this.restoredWeather.remove(world.getName());
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
            world.setThunderDuration(Integer.MAX_VALUE);
            if (world.isGameRule("doWeatherCycle")) {
                world.setGameRuleValue("doWeatherCycle", "false");
            }
            for (final Player player : world.getPlayers()) {
                player.resetPlayerWeather();
            }
        }
        else if (this.restoredWeather.add(world.getName())) {
            world.setWeatherDuration(6000);
            world.setThunderDuration(6000);
            if (world.isGameRule("doWeatherCycle")) world.setGameRuleValue("doWeatherCycle", "true");
        }
        final boolean mobs = !Options.enabled(this.plugin, "world.disableMobs", true);
        world.setSpawnFlags(mobs, mobs);
        world.setGameRuleValue("doMobSpawning", String.valueOf(mobs));
        if (Options.enabled(this.plugin, "world.freezeTime", false)) {
            this.frozenTime.add(world.getName());
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setTime(Options.integer(this.plugin, "world.time", 6000, 0, 23999));
        } else if (this.frozenTime.remove(world.getName())) {
            world.setGameRuleValue("doDaylightCycle", "true");
        }
    }

    public <T extends LivingEntity> T spawnGameMob(final Location location, final Class<T> type) {
        this.spawningGameMob++;
        try {
            final T entity = location.getWorld().spawn(location, type);
            entity.setMetadata(GAME_MOB, new FixedMetadataValue(this.plugin, true));
            return entity;
        } finally {
            this.spawningGameMob--;
        }
    }

    private void removeUnwantedMob(final Entity entity) {
        if (!Options.enabled(this.plugin, "world.disableMobs", true) || !(entity instanceof LivingEntity) || entity instanceof Player || entity instanceof ArmorStand) {
            return;
        }
        if (entity.hasMetadata(GAME_MOB)) {
            return;
        }
        final String name = ChatColor.stripColor(entity.getCustomName());
        if (entity instanceof Villager && this.plugin.npcs() != null && this.plugin.npcs().isShop((Villager) entity)) {
            return;
        }
        final GameState state = this.plugin.game().state();
        if ((state == GameState.RUNNING || state == GameState.PAUSED) && name != null) {
            if (entity instanceof IronGolem && name.startsWith("Straznik snow - ")) {
                return;
            }
            if (entity instanceof Silverfish && name.startsWith("Pluskwa - ")) {
                return;
            }
            if (entity instanceof EnderDragon && name.startsWith("Smok - ")) {
                return;
            }
        }
        entity.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void weather(final WeatherChangeEvent event) {
        if (Options.enabled(this.plugin, "world.disableWeather", true) && managed(event.getWorld()) && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void thunder(final ThunderChangeEvent event) {
        if (Options.enabled(this.plugin, "world.disableWeather", true) && managed(event.getWorld()) && event.toThunderState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void lightning(final LightningStrikeEvent event) {
        if (Options.enabled(this.plugin, "world.disableWeather", true) && managed(event.getWorld()) && !event.getLightning().isEffect()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void spawn(final CreatureSpawnEvent event) {
        if (!Options.enabled(this.plugin, "world.disableMobs", true) || !managed(event.getLocation().getWorld()) || event.getEntity() instanceof ArmorStand) {
            return;
        }
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM && this.spawningGameMob > 0) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void worldInit(final WorldInitEvent event) {
        if (managed(event.getWorld())) {
            apply(event.getWorld());
        }
    }

    @EventHandler
    public void worldLoad(final WorldLoadEvent event) {
        prepare(event.getWorld());
    }

    @EventHandler
    public void chunkLoad(final ChunkLoadEvent event) {
        if (managed(event.getWorld())) {
            for (final Entity entity : event.getChunk().getEntities()) {
                removeUnwantedMob(entity);
            }
        }
    }

    public void shutdown() {
        if (this.task != -1) {
            Bukkit.getScheduler().cancelTask(this.task);
            this.task = -1;
        }
    }
}
