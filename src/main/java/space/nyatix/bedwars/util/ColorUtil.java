package space.nyatix.bedwars.util;

import org.bukkit.ChatColor;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String c(final String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
