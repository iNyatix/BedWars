package space.nyatix.bedwars.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GeneratorType;
import space.nyatix.bedwars.model.TeamColor;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArenaStorage {

    private final File file;

    private YamlConfiguration cfg;

    public ArenaStorage(final BedWarsPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        this.cfg = YamlConfiguration.loadConfiguration(this.file);
    }

    public void saveArena(final Arena arena) {
        final String p = "arenas." + arena.getName() + ".";
        this.cfg.set(p + "world", arena.getWorld());
        saveLoc(p + "lobby", arena.getLobby());
        saveLoc(p + "spectator", arena.getSpectator());
        for (final TeamColor teamColor : TeamColor.values()) {
            saveLoc(p + "teams." + teamColor.name() + ".spawn", arena.team(teamColor).getSpawn());
            saveLoc(p + "teams." + teamColor.name() + ".bed", arena.team(teamColor).getBed());
            saveLoc(p + "teams." + teamColor.name() + ".baseGenerator", arena.team(teamColor).getBaseGenerator());
            saveLoc(p + "teams." + teamColor.name() + ".protected.pos1", arena.team(teamColor).getProtectedPos1());
            saveLoc(p + "teams." + teamColor.name() + ".protected.pos2", arena.team(teamColor).getProtectedPos2());
        }
        this.cfg.set(p + "generators", null);
        for (final GeneratorType generatorType : GeneratorType.values()) {
            int i = 0;
            for (final Location l : arena.getGenerators().get(generatorType)) {
                saveLoc(p + "generators." + generatorType.name() + "." + (i++), l);
            }
        }
        this.cfg.set(p + "shops", null);
        int i = 0;
        for (final Location l : arena.getItemShops()) {
            saveLoc(p + "shops.item." + (i++), l);
        }
        i = 0;
        for (final Location l : arena.getUpgradeShops()) {
            saveLoc(p + "shops.upgrades." + (i++), l);
        }
        save();
    }

    public Map<String, Arena> loadAll() {
        this.cfg = YamlConfiguration.loadConfiguration(this.file);
        final Map<String, Arena> out = new LinkedHashMap<>();
        if (this.cfg.getConfigurationSection("arenas") == null) {
            return out;
        }
        for (final String name : this.cfg.getConfigurationSection("arenas").getKeys(false)) {
            final String p = "arenas." + name + ".";
            final Arena arena = new Arena(name);
            arena.setWorld(this.cfg.getString(p + "world", name));
            arena.setLobby(loadLoc(p + "lobby"));
            arena.setSpectator(loadLoc(p + "spectator"));
            for (final TeamColor teamColor : TeamColor.values()) {
                arena.team(teamColor).setSpawn(loadLoc(p + "teams." + teamColor.name() + ".spawn"));
                arena.team(teamColor).setBed(loadLoc(p + "teams." + teamColor.name() + ".bed"));
                arena.team(teamColor).setBaseGenerator(loadLoc(p + "teams." + teamColor.name() + ".baseGenerator"));
                arena.team(teamColor).setProtectedPos1(loadLoc(p + "teams." + teamColor.name() + ".protected.pos1"));
                arena.team(teamColor).setProtectedPos2(loadLoc(p + "teams." + teamColor.name() + ".protected.pos2"));
            }
            for (final GeneratorType generatorType : GeneratorType.values()) {
                loadLocList(p + "generators." + generatorType.name(), arena.getGenerators().get(generatorType));
            }
            loadLocList(p + "shops.item", arena.getItemShops());
            loadLocList(p + "shops.upgrades", arena.getUpgradeShops());
            final World loaded = Bukkit.getWorld(arena.getWorld());
            if (loaded != null) {
                arena.bindWorld(loaded);
            }
            out.put(name.toLowerCase(), arena);
        }
        return out;
    }

    public void delete(final String name) {
        this.cfg.set("arenas." + name, null);
        save();
    }

    private void loadLocList(final String path, final List<Location> list) {
        if (this.cfg.getConfigurationSection(path) == null) {
            return;
        }
        for (final String k : this.cfg.getConfigurationSection(path).getKeys(false)) {
            final Location location = loadLoc(path + "." + k);
            if (location != null) {
                list.add(location);
            }
        }
    }

    private void saveLoc(final String path, final Location location) {
        if (location == null) {
            this.cfg.set(path, null);
            return;
        }
        if (location.getWorld() != null) {
            this.cfg.set(path + ".world", location.getWorld().getName());
        }
        this.cfg.set(path + ".x", location.getX());
        this.cfg.set(path + ".y", location.getY());
        this.cfg.set(path + ".z", location.getZ());
        this.cfg.set(path + ".yaw", location.getYaw());
        this.cfg.set(path + ".pitch", location.getPitch());
    }

    private Location loadLoc(final String path) {
        if (!this.cfg.contains(path + ".x")) {
            return null;
        }
        return new Location(null, this.cfg.getDouble(path + ".x"), this.cfg.getDouble(path + ".y"), this.cfg.getDouble(path + ".z"), (float) this.cfg.getDouble(path + ".yaw"),
                
                (float) this.cfg.getDouble(path + ".pitch"));
    }

    private void save() {
        try {
            this.cfg.save(this.file);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
