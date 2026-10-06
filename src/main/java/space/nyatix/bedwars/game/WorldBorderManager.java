package space.nyatix.bedwars.game;

import java.util.Comparator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class WorldBorderManager {

    private static WorldBorderManager instance;

    private final BedWarsPlugin plugin;

    private final File file;

    private final Map<String, BorderData> borders = new HashMap<>();

    private final Set<UUID> warnedOutside = new HashSet<>();

    private Arena shrinkingArena;

    private int shrinkStart, shrinkEnd;

    private double startRadius;

    private boolean closureAnnounced;

    public WorldBorderManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "worldborders.properties");
        instance = this;
        load();
    }

    public static boolean isConfiguredStatic(final Arena arena) {
        return instance != null && instance.isConfigured(arena);
    }

    public boolean isConfigured(final Arena arena) {
        final BorderData d = data(arena, false);
        return d != null && d.centerSet && d.radius > 0.0D;
    }

    public boolean hasCenter(final Arena arena) {
        final BorderData d = data(arena, false);
        return d != null && d.centerSet;
    }

    public double radius(final Arena arena) {
        final BorderData d = data(arena, false);
        return d == null ? 0.0D : d.radius;
    }

    public double centerX(final Arena arena) {
        final BorderData d = data(arena, false);
        return d == null ? 0.0D : d.x;
    }

    public double centerZ(final Arena arena) {
        final BorderData d = data(arena, false);
        return d == null ? 0.0D : d.z;
    }

    public void beginSuddenDeath(final Arena arena, final int fromSecond, final int closeSecond) {
        if (!isConfigured(arena)) {
            return;
        }
        this.shrinkingArena = arena;
        this.shrinkStart = fromSecond;
        this.shrinkEnd = Math.max(fromSecond + 1, closeSecond);
        this.startRadius = Math.max(0.5D, radius(arena));
        this.closureAnnounced = false;
        this.warnedOutside.clear();
        applyActive();
    }

    public boolean isShrinking(final Arena arena) {
        return arena != null && arena == this.shrinkingArena;
    }

    public double currentRadius(final Arena arena) {
        if (!isShrinking(arena) || this.plugin.game() == null) {
            return radius(arena);
        }
        final double progress = Math.max(0D, Math.min(1D, (this.plugin.game().elapsed() - this.shrinkStart) / (double) (this.shrinkEnd - this.shrinkStart)));
        return this.startRadius + (Math.min(this.startRadius, Options.number(this.plugin, "suddenDeath.finalRadius", 0.5D, 0.5D, 32.0D)) - this.startRadius) * progress;
    }

    public int secondsUntilClosed() {
        return Math.max(0, this.shrinkEnd - this.plugin.game().elapsed());
    }

    public void resetMatch() {
        this.shrinkingArena = null;
        this.warnedOutside.clear();
        this.closureAnnounced = false;
        applyActive();
    }

    public void tickSuddenDeath() {
        final GameManager game = this.plugin.game();
        if (game == null || game.state() != GameState.RUNNING || !isShrinking(game.active())) {
            return;
        }
        applyActive();
        final boolean closed = game.elapsed() >= this.shrinkEnd;
        if (closed && !this.closureAnnounced) {
            this.closureAnnounced = true;
            game.broadcast(Messages.get("world-border.tick-sudden-death.border-zamkniety-wszyscy-traca-hp-obrazenia"));
        }
        final double damage = closed ? Math.min(Options.number(this.plugin, "suddenDeath.maximumDamage", 20.0D, 20.0D, 100.0D), Options.number(this.plugin,
                "suddenDeath.initialDamage", 4.0D, 0.1D, 100.0D) + (game.elapsed() - this.shrinkEnd) * Options.number(this.plugin, "suddenDeath.damageIncreasePerSecond",
                1.0D, 0.1D, 20.0D)) : Options.number(this.plugin, "suddenDeath.outsideDamage", 4.0D, 0.1D, 100.0D);
        final World matchWorld = runtimeOrEditor(this.shrinkingArena);
        final List<Player> threatened = new ArrayList<>();
        for (final UUID playerId : game.participants()) {
            final Player p = Bukkit.getPlayer(playerId);
            if (p == null || !p.isOnline() || p.isDead() || game.isFinalDead(playerId) || game.isSpectator(playerId) || game.isRespawning(playerId) || !p.getWorld().equals(matchWorld)) {
                continue;
            }
            if (closed || outside(this.shrinkingArena, p.getLocation())) {
                threatened.add(p);
                if (this.warnedOutside.add(playerId) || game.elapsed() % 5 == 0) {
                    Messages.send(p, closed ? Messages.get("world-border.tick-sudden-death.ostateczna-walka-rosnace-obrazenia-hp-s", "VALUE1",
                            (int) damage) : Messages.get("world-border.tick-sudden-death.za-borderem-tracisz-2-serca-s", "DAMAGE", damage));
                }
            } else {
                this.warnedOutside.remove(playerId);
            }
        }
        Collections.shuffle(threatened);
        threatened.sort(Comparator.comparingDouble(Damageable::getHealth));
        for (final Player p : threatened) {
            if (game.state() != GameState.RUNNING) {
                break;
            }
            if (p.getHealth() <= damage) {
                game.handleDeath(p, game.recentKiller(p));
            } else {
                p.setHealth(p.getHealth() - damage);
            }
        }
    }

    public void setCenter(final Arena arena, final Location location) {
        if (arena == null || location == null) {
            return;
        }
        final BorderData d = data(arena, true);
        d.x = location.getX();
        d.z = location.getZ();
        d.centerSet = true;
        save();
        apply(arena, location.getWorld());
    }

    public void setRadius(final Arena arena, final double radius) {
        if (arena == null) {
            return;
        }
        final BorderData d = data(arena, true);
        d.radius = Math.max(0.0D, radius);
        save();
        final World world = runtimeOrEditor(arena);
        if (world != null) {
            apply(arena, world);
        }
    }

    public void start() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::applyActive, 20L, 40L);
    }

    public void applyActive() {
        final Arena arena = this.plugin.game() == null ? null : this.plugin.game().active();
        final World world = runtimeOrEditor(arena);
        if (arena != null && world != null) {
            apply(arena, world);
        }
    }

    public boolean apply(final Arena arena, final World world) {
        final BorderData d = data(arena, false);
        if (d == null || !d.centerSet || d.radius <= 0.0D || world == null) {
            return false;
        }
        try {
            final WorldBorder border = world.getWorldBorder();
            if (border == null) {
                return false;
            }
            border.setCenter(d.x, d.z);
            border.setSize(isShrinking(arena) ? Math.max(1D, currentRadius(arena) * 2D) : Math.max(2D, d.radius * 2D));
            border.setWarningDistance(5);
            border.setDamageBuffer(isShrinking(arena) ? 0D : 5D);
            border.setDamageAmount(isShrinking(arena) ? 0D : 2D);
            return true;
        } catch (final Throwable ex) {
            this.plugin.getLogger().warning(Messages.get("world-border.apply.nie-udalo-sie-ustawic-worldbordera-dla", "MAP", arena.getName(), "ERROR", ex.getMessage()));
            return false;
        }
    }

    public boolean outside(final Arena arena, final Location location) {
        final BorderData d = data(arena, false);
        if (d == null || !d.centerSet || d.radius <= 0.0D || location == null) {
            return false;
        }
        if (location.getWorld() == null || !location.getWorld().equals(runtimeOrEditor(arena))) {
            return false;
        }
        final double r = currentRadius(arena);
        return Math.abs(location.getX() - d.x) > r || Math.abs(location.getZ() - d.z) > r;
    }

    private World runtimeOrEditor(final Arena arena) {
        if (arena == null) {
            return null;
        }
        if (arena.getRuntimeWorld() != null) {
            final World world = Bukkit.getWorld(arena.getRuntimeWorld());
            if (world != null) {
                return world;
            }
        }
        return arena.getWorld() == null ? null : Bukkit.getWorld(arena.getWorld());
    }

    private BorderData data(final Arena arena, final boolean create) {
        if (arena == null) {
            return null;
        }
        final String key = arena.getName().toLowerCase();
        BorderData d = this.borders.get(key);
        if (d == null && create) {
            d = new BorderData();
            this.borders.put(key, d);
        }
        return d;
    }

    private void load() {
        if (!this.file.isFile()) {
            return;
        }
        final Properties p = new Properties();
        try {
            final FileInputStream in = new FileInputStream(this.file);
            p.load(in);
            in.close();
        } catch (final Exception ex) {
            return;
        }
        for (final String key : p.stringPropertyNames()) {
            final int dot = key.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            final String map = key.substring(0, dot), field = key.substring(dot + 1);
            final BorderData d = this.borders.computeIfAbsent(map, k -> new BorderData());
            try {
                if (field.equals("centerSet")) {
                    d.centerSet = Boolean.parseBoolean(p.getProperty(key));
                    continue;
                }
                final double v = Double.parseDouble(p.getProperty(key));
                switch (field) {
                    case "x":
                        d.x = v;
                        d.centerSet = true;
                        break;
                    case "z":
                        d.z = v;
                        d.centerSet = true;
                        break;
                    case "radius":
                        d.radius = v;
                        break;
                }
            } catch (final Exception ignored) {
            }
        }
    }

    private void save() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        final Properties p = new Properties();
        for (final Map.Entry<String, BorderData> e : this.borders.entrySet()) {
            final String b = e.getKey() + ".";
            final BorderData d = e.getValue();
            p.setProperty(b + "x", String.valueOf(d.x));
            p.setProperty(b + "z", String.valueOf(d.z));
            p.setProperty(b + "centerSet", String.valueOf(d.centerSet));
            p.setProperty(b + "radius", String.valueOf(d.radius));
        }
        try {
            final FileOutputStream out = new FileOutputStream(this.file);
            p.store(out, Messages.get("world-border.save.bedwars-worldborder-per-mapa"));
            out.close();
        } catch (final Exception ex) {
            this.plugin.getLogger().warning(Messages.get("world-border.save.nie-udalo-sie-zapisac-worldborders-properties", "ERROR", ex.getMessage()));
        }
    }

    private static class BorderData {

        double x, z, radius;

        boolean centerSet;
    }
}
