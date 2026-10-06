package space.nyatix.bedwars.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;

import java.util.Locale;

public final class ConfiguredSound {

    private ConfiguredSound() {
    }

    public static void play(final BedWarsPlugin plugin, final Player player, final String event, final Sound fallback, final float volume, final float pitch) {
        final String path = "sounds." + event;
        if (!plugin.settings().effectSoundsEnabled() || !Options.enabled(plugin, path + ".enabled", true)) {
            return;
        }
        Sound sound = fallback;
        try {
            sound = Sound.valueOf(plugin.getConfig().getString(path + ".name", fallback.name()).toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
        }
        player.playSound(player.getLocation(), sound, (float) Options.number(plugin, path + ".volume", volume, 0D, 10D), (float) Options.number(plugin,
                path + ".pitch", pitch, 0.5D, 2D));
    }
}
