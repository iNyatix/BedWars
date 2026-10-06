package space.nyatix.bedwars.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Arena {

    private final String name;

    private String world;

    private transient String runtimeWorld;

    private Location lobby, spectator;

    private final Map<TeamColor, TeamData> teams = new EnumMap<>(TeamColor.class);

    private final Map<GeneratorType, List<Location>> generators = new EnumMap<>(GeneratorType.class);

    private final List<Location> itemShops = new ArrayList<>(), upgradeShops = new ArrayList<>();

    public Arena(final String name) {
        this.name = name;
        for (final TeamColor teamColor : TeamColor.values()) {
            this.teams.put(teamColor, new TeamData(teamColor));
        }
        for (final GeneratorType generatorType : GeneratorType.values()) {
            this.generators.put(generatorType, new ArrayList<>());
        }
    }

    public String getName() {
        return this.name;
    }

    public String getWorld() {
        return this.world;
    }

    public void setWorld(final String w) {
        this.world = w;
    }

    public String getRuntimeWorld() {
        return this.runtimeWorld;
    }

    public void setRuntimeWorld(final String w) {
        this.runtimeWorld = w;
    }

    public Location getLobby() {
        return this.lobby;
    }

    public void setLobby(final Location location) {
        this.lobby = location;
    }

    public Location getSpectator() {
        return this.spectator;
    }

    public void setSpectator(final Location location) {
        this.spectator = location;
    }

    public Map<TeamColor, TeamData> getTeams() {
        return this.teams;
    }

    public TeamData team(final TeamColor teamColor) {
        return this.teams.get(teamColor);
    }

    public Map<GeneratorType, List<Location>> getGenerators() {
        return this.generators;
    }

    public List<Location> getItemShops() {
        return this.itemShops;
    }

    public List<Location> getUpgradeShops() {
        return this.upgradeShops;
    }

    public void bindWorld(final World world) {
        bind(this.lobby, world);
        bind(this.spectator, world);
        for (final TeamColor teamColor : TeamColor.values()) {
            bind(team(teamColor).getSpawn(), world);
            bind(team(teamColor).getBed(), world);
            bind(team(teamColor).getBaseGenerator(), world);
            bind(team(teamColor).getProtectedPos1(), world);
            bind(team(teamColor).getProtectedPos2(), world);
        }
        for (final List<Location> list : this.generators.values()) {
            for (final Location l : list) {
                bind(l, world);
            }
        }
        for (final Location l : this.itemShops) {
            bind(l, world);
        }
        for (final Location l : this.upgradeShops) {
            bind(l, world);
        }
    }

    private void bind(final Location location, final World world) {
        if (location != null) {
            location.setWorld(world);
        }
    }

    public boolean isPlayable() {
        if (this.lobby == null || this.spectator == null) {
            return false;
        }
        for (final TeamColor teamColor : TeamColor.values()) {
            if (team(teamColor).getSpawn() == null || team(teamColor).getBed() == null || team(teamColor).getBaseGenerator() == null || !team(teamColor).hasProtectedRegion()) {
                return false;
            }
        }
        return !this.generators.get(GeneratorType.DIAMOND).isEmpty() && !this.generators.get(GeneratorType.EMERALD).isEmpty();
    }
}
