package space.nyatix.bedwars.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TeamData {

    private final TeamColor color;

    private Location spawn, bed, baseGenerator, protectedPos1, protectedPos2;

    private boolean bedAlive = true;

    private final Set<UUID> players = new LinkedHashSet<>();

    private int protectionLevel, sharpnessLevel, hasteLevel, forgeLevel;

    private boolean healPool, dragonBuff;

    private final List<TrapType> traps = new ArrayList<>();

    public TeamData(final TeamColor teamColor) {
        this.color = teamColor;
    }

    public TeamColor getColor() {
        return this.color;
    }

    public Location getSpawn() {
        return this.spawn;
    }

    public void setSpawn(final Location location) {
        this.spawn = location;
    }

    public Location getBed() {
        return this.bed;
    }

    public void setBed(final Location location) {
        this.bed = location;
    }

    public Location getBaseGenerator() {
        return this.baseGenerator;
    }

    public void setBaseGenerator(final Location location) {
        this.baseGenerator = location;
    }

    public Location getProtectedPos1() {
        return this.protectedPos1;
    }

    public void setProtectedPos1(final Location location) {
        this.protectedPos1 = location;
    }

    public Location getProtectedPos2() {
        return this.protectedPos2;
    }

    public void setProtectedPos2(final Location location) {
        this.protectedPos2 = location;
    }

    public boolean hasProtectedRegion() {
        return this.protectedPos1 != null && this.protectedPos2 != null;
    }

    public boolean isInsideProtectedRegion(final Location location) {
        if (location == null || !hasProtectedRegion()) {
            return false;
        }
        if (this.protectedPos1.getWorld() != null && location.getWorld() != null && !this.protectedPos1.getWorld().equals(location.getWorld())) {
            return false;
        }
        final int minX = Math.min(this.protectedPos1.getBlockX(), this.protectedPos2.getBlockX());
        final int maxX = Math.max(this.protectedPos1.getBlockX(), this.protectedPos2.getBlockX());
        final int minZ = Math.min(this.protectedPos1.getBlockZ(), this.protectedPos2.getBlockZ());
        final int maxZ = Math.max(this.protectedPos1.getBlockZ(), this.protectedPos2.getBlockZ());
        return location.getBlockX() >= minX && location.getBlockX() <= maxX && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public boolean isBedAlive() {
        return this.bedAlive;
    }

    public void setBedAlive(final boolean v) {
        this.bedAlive = v;
    }

    public Set<UUID> getPlayers() {
        return this.players;
    }

    public int getProtectionLevel() {
        return this.protectionLevel;
    }

    public void setProtectionLevel(final int v) {
        this.protectionLevel = v;
    }

    public int getSharpnessLevel() {
        return this.sharpnessLevel;
    }

    public void setSharpnessLevel(final int v) {
        this.sharpnessLevel = v;
    }

    public int getHasteLevel() {
        return this.hasteLevel;
    }

    public void setHasteLevel(final int v) {
        this.hasteLevel = v;
    }

    public int getForgeLevel() {
        return this.forgeLevel;
    }

    public void setForgeLevel(final int v) {
        this.forgeLevel = v;
    }

    public boolean hasHealPool() {
        return this.healPool;
    }

    public void setHealPool(final boolean v) {
        this.healPool = v;
    }

    public boolean hasDragonBuff() {
        return this.dragonBuff;
    }

    public void setDragonBuff(final boolean v) {
        this.dragonBuff = v;
    }

    public List<TrapType> getTraps() {
        return this.traps;
    }

    public void resetUpgrades() {
        this.protectionLevel = this.sharpnessLevel = this.hasteLevel = this.forgeLevel = 0;
        this.healPool = this.dragonBuff = false;
        this.traps.clear();
    }
}
