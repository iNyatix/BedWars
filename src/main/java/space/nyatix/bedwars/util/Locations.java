package space.nyatix.bedwars.util;

import org.bukkit.Location;

public final class Locations {

    private Locations() {
    }

    public static boolean sameBlock(final Location location, final Location b) {
        return location != null && b != null && location.getWorld() != null && b.getWorld() != null && location.getWorld().equals(b.getWorld()) && location.getBlockX() == b.getBlockX() && location.getBlockY() == b.getBlockY() && location.getBlockZ() == b.getBlockZ();
    }

    public static String key(final Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
