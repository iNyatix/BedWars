package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GeneratorType;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.model.TeamData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigHologramManager {

    private final BedWarsPlugin plugin;

    private final Map<String, List<UUID>> spawned = new HashMap<>();

    public ConfigHologramManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh(final Arena arena) {
        if (arena == null) {
            return;
        }
        clear(arena);
        if (!Options.enabled(this.plugin, "holograms.configuration.enabled", true)) {
            return;
        }
        World world = arena.getLobby() != null ? arena.getLobby().getWorld() : null;
        if (world == null && arena.getWorld() != null) {
            world = Bukkit.getWorld(arena.getWorld());
        }
        if (world == null) {
            return;
        }
        marker(arena, arena.getLobby(), Messages.get("config-hologram.refresh.ustawiono-poczekalnie"), 2.15D);
        marker(arena, arena.getSpectator(), Messages.get("config-hologram.refresh.ustawiono-spawn-obserwatorow"), 2.15D);
        for (final TeamColor teamColor : TeamColor.values()) {
            final TeamData teamData = arena.team(teamColor);
            marker(arena, teamData.getSpawn(), Messages.get("config-hologram.refresh.ustawiono-spawn", "VALUE1", teamColor.chat(), "TEAM",
                    teamColor.display().toUpperCase()), 2.15D);
            marker(arena, teamData.getBed(), Messages.get("config-hologram.refresh.ustawiono-lozko", "VALUE1", teamColor.chat(), "TEAM", teamColor.display().toUpperCase()),
                    
                    1.65D);
            marker(arena, teamData.getBaseGenerator(), Messages.get("config-hologram.refresh.generator-bazy", "VALUE1", teamColor.chat(),
                    "TEAM", teamColor.display().toUpperCase()), 1.65D);
            marker(arena, teamData.getProtectedPos1(), Messages.get("config-hologram.refresh.strefa-bez-budowania-punkt-1", "VALUE1", teamColor.chat()), 1.65D);
            marker(arena, teamData.getProtectedPos2(), Messages.get("config-hologram.refresh.strefa-bez-budowania-punkt-2", "VALUE1", teamColor.chat()), 1.65D);
        }
        for (final Location l : arena.getGenerators().get(GeneratorType.DIAMOND)) {
            marker(arena, l, Messages.get("config-hologram.refresh.generator-diamentow"), 1.65D);
        }
        for (final Location l : arena.getGenerators().get(GeneratorType.EMERALD)) {
            marker(arena, l, Messages.get("config-hologram.refresh.generator-szmaragdow"), 1.65D);
        }
        for (final Location l : arena.getItemShops()) {
            marker(arena, l, Messages.get("config-hologram.refresh.sklep-z-przedmiotami"), 2.25D);
        }
        for (final Location l : arena.getUpgradeShops()) {
            marker(arena, l, Messages.get("config-hologram.refresh.ulepszenia-druzyny"), 2.25D);
        }
    }

    private void marker(final Arena arena, final Location base, final String text, final double yOffset) {
        if (base == null || base.getWorld() == null) {
            return;
        }
        final Location loc = base.clone().add(0.0D, yOffset, 0.0D);
        final ArmorStand stand = base.getWorld().spawn(loc, ArmorStand.class);
        stand.setMetadata("bw-config-marker", new FixedMetadataValue(this.plugin, true));
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setBasePlate(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        final List<UUID> list = this.spawned.computeIfAbsent(arena.getName().toLowerCase(), k -> new ArrayList<>());
        list.add(stand.getUniqueId());
    }

    public void clear(final Arena arena) {
        if (arena == null) {
            return;
        }
        final String key = arena.getName().toLowerCase();
        final List<UUID> ids = this.spawned.remove(key);
        World source = null;
        if (arena.getWorld() != null) {
            source = Bukkit.getWorld(arena.getWorld());
        }
        removeTracked(ids);
        if (source != null) {
            purgeConfigMarkers(source);
        }
    }

    public void clear(final String arenaName) {
        if (arenaName == null) {
            return;
        }
        final List<UUID> ids = this.spawned.remove(arenaName.toLowerCase());
        removeTracked(ids);
        final World byName = Bukkit.getWorld(arenaName);
        if (byName != null) {
            purgeConfigMarkers(byName);
        }
    }

    private void removeTracked(final List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (final World world : Bukkit.getWorlds()) {
            for (final Entity entity : new ArrayList<>(world.getEntities())) {
                if (ids.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            }
        }
    }

    public void purgeConfigMarkers(final World world) {
        if (world == null) {
            return;
        }
        for (final Entity entity : new ArrayList<>(world.getEntities())) {
            if (isConfigMarker(entity)) {
                entity.remove();
            }
        }
    }

    private boolean isConfigMarker(final Entity entity) {
        if (!(entity instanceof ArmorStand)) {
            return false;
        }
        if (entity.hasMetadata("bw-config-marker")) {
            return true;
        }
        final String name = ((ArmorStand) entity).getCustomName();
        if (name == null) {
            return false;
        }
        final String plain = ChatColor.stripColor(name);
        if (plain == null) {
            return false;
        }
        return plain.startsWith("USTAWIONO ") || plain.startsWith("GENERATOR BAZY") || plain.startsWith("STREFA BEZ BUDOWANIA") || plain.startsWith("GENERATOR DIAMENTOW") || plain.startsWith("GENERATOR SZMARAGDOW") || plain.startsWith("SKLEP Z PRZEDMIOTAMI") || plain.startsWith("ULEPSZENIA DRUZYNY");
    }

    public void clearAll() {
        for (final String key : new ArrayList<>(this.spawned.keySet())) {
            clear(key);
        }
        this.spawned.clear();
        for (final World world : Bukkit.getWorlds()) {
            purgeConfigMarkers(world);
        }
    }
}
