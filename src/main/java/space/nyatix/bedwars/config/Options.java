package space.nyatix.bedwars.config;

import org.bukkit.Material;
import space.nyatix.bedwars.BedWarsPlugin;

import java.util.List;
import java.util.Locale;

public final class Options {

    private Options() {
    }

    public static boolean enabled(final BedWarsPlugin plugin, final String path, final boolean fallback) {
        return plugin.getConfig().getBoolean(path, fallback);
    }

    public static int integer(final BedWarsPlugin plugin, final String path, final int fallback, final int minimum, final int maximum) {
        return Math.max(minimum, Math.min(maximum, plugin.getConfig().getInt(path, fallback)));
    }

    public static double number(final BedWarsPlugin plugin, final String path, final double fallback, final double minimum, final double maximum) {
        final double value = plugin.getConfig().getDouble(path, fallback);
        return Double.isNaN(value) || Double.isInfinite(value) ? fallback : Math.max(minimum, Math.min(maximum, value));
    }

    public static int tier(final BedWarsPlugin plugin, final String path, final int tier, final int[] fallback, final int minimum, final int maximum) {
        final List<Integer> values = plugin.getConfig().getIntegerList(path);
        final int index = Math.max(0, Math.min(fallback.length - 1, tier));
        final int value = index < values.size() ? values.get(index) : fallback[index];
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static Material material(final BedWarsPlugin plugin, final String path, final Material fallback) {
        final String value = plugin.getConfig().getString(path, fallback.name());
        final Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        return material == null || material == Material.AIR ? fallback : material;
    }
}
