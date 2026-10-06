package space.nyatix.bedwars.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ConfigurationValidator {

    private ConfigurationValidator() {
    }

    public static void validate(final YamlConfiguration configuration) throws IOException, InvalidConfigurationException {
        final YamlConfiguration defaults = new YamlConfiguration();
        try (final InputStream stream = ConfigurationValidator.class.getResourceAsStream("/config.yml")) {
            if (stream == null) {
                throw new IOException("Brak config.yml w JAR BedWars.");
            }
            defaults.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        for (final String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key) || !configuration.isSet(key)) {
                continue;
            }
            final Object expected = defaults.get(key);
            final Object value = configuration.get(key);
            final boolean valid = expected instanceof Boolean ? value instanceof Boolean : expected instanceof Number ? value instanceof Number && Double.isFinite(((Number) value).doubleValue()) : expected instanceof List ? value instanceof List : value instanceof String;
            if (!valid) {
                throw new InvalidConfigurationException("config.yml: nieprawidlowy typ wartosci " + key);
            }
            if (expected instanceof List && !((List<?>) expected).isEmpty()) {
                final Object element = ((List<?>) expected).get(0);
                for (final Object entry : (List<?>) value) {
                    if (element instanceof Number ? !(entry instanceof Number) : !(entry instanceof String)) {
                        throw new InvalidConfigurationException("config.yml: nieprawidlowy element listy " + key);
                    }
                }
            }
        }
        range(configuration, "game.playersPerTeam", 4, 1, 64);
        range(configuration, "game.minPlayers", 2, 2, 256);
        range(configuration, "game.countdownSeconds", 20, 1, 3600);
        range(configuration, "game.respawnSeconds", 5, 1, 120);
        range(configuration, "game.reconnectSeconds", 180, 0, 3600);
        range(configuration, "game.respawnProtectionSeconds", 3, 0, 120);
        if (configuration.getInt("game.minPlayers", 2) > configuration.getInt("game.playersPerTeam", 4) * 4) {
            throw new InvalidConfigurationException("config.yml: minPlayers przekracza liczbe miejsc w druzynach.");
        }
    }

    private static void range(final YamlConfiguration configuration, final String path, final int fallback, final int minimum, final int maximum) throws InvalidConfigurationException {
        final int value = configuration.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new InvalidConfigurationException("config.yml: " + path + " musi miescic sie w zakresie " + minimum + "-" + maximum);
        }
    }
}
