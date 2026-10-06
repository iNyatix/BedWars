package space.nyatix.bedwars.message;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import space.nyatix.bedwars.BedWarsPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Messages {

    private static final Map<String, Object> DEFAULTS = loadDefaults();

    private static volatile Map<String, Object> values = DEFAULTS;

    private final BedWarsPlugin plugin;

    private final File file;

    public Messages(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void reload() throws IOException, InvalidConfigurationException {
        final YamlConfiguration configuration = new YamlConfiguration();
        if (this.file.isFile()) {
            configuration.load(this.file);
        }
        final Map<String, Object> loaded = new LinkedHashMap<>(DEFAULTS);
        boolean changed = !this.file.isFile();
        for (final Map.Entry<String, Object> entry : DEFAULTS.entrySet()) {
            if (!configuration.isSet(entry.getKey())) {
                Object value = entry.getValue();
                if (entry.getKey().equals("titles.victory.text")) {
                    value = this.plugin.getConfig().getString("messages.victoryTitle", String.valueOf(value));
                }
                configuration.set(entry.getKey(), value);
                changed = true;
            }
            final Object value = configuration.get(entry.getKey());
            if (!(value instanceof String) && !(value instanceof List)) {
                throw new InvalidConfigurationException("messages.yml: " + entry.getKey() + " musi byc tekstem lub lista tekstow.");
            }
            if (value instanceof List) {
                final List<String> lines = new ArrayList<>();
                for (final Object line : (List<?>) value) {
                    if (!(line instanceof String)) {
                        throw new InvalidConfigurationException("messages.yml: " + entry.getKey() + " zawiera wartosc inna niz tekst.");
                    }
                    lines.add((String) line);
                }
                loaded.put(entry.getKey(), Collections.unmodifiableList(lines));
            } else {
                loaded.put(entry.getKey(), value);
            }
        }
        if (changed) {
            final File parent = this.file.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Nie mozna utworzyc folderu " + parent);
            }
            configuration.options().header("BedWars - wiadomosci i wyglad tekstow\nKolory: &0-&f, &l, &o, &r. Zmienne sa zapisane w {NAWIASACH}.\nPusty tekst wylacza komunikat. Zamiast tekstu mozna podac liste linii.\nPrzeladowanie: /bw przeladuj. Brakujace wpisy sa uzupelniane automatycznie.");
            configuration.save(this.file);
        }
        values = Collections.unmodifiableMap(loaded);
    }

    public static String get(final String key, final Object... replacements) {
        final Object value = values.get(key);
        if (value instanceof List) {
            final StringBuilder result = new StringBuilder();
            boolean first = true;
            for (final Object line : (List<?>) value) {
                if (!first) {
                    result.append('\n');
                }
                first = false;
                result.append(line);
            }
            return format(result.toString(), replacements);
        }
        return format(value == null ? key : String.valueOf(value), replacements);
    }

    public static String frame(final String key, final int age, final int interval, final Object... replacements) {
        final Object value = values.get(key);
        if (!(value instanceof List)) {
            return get(key, replacements);
        }
        final List<?> frames = (List<?>) value;
        if (frames.isEmpty()) {
            return "";
        }
        final int index = Math.floorMod(age / Math.max(1, interval), frames.size());
        return format(String.valueOf(frames.get(index)), replacements);
    }

    public static List<String> lines(final String key, final Object... replacements) {
        if (values.get(key) instanceof List && ((List<?>) values.get(key)).isEmpty()) {
            return Collections.emptyList();
        }
        final String rendered = get(key, replacements);
        final List<String> result = new ArrayList<>();
        Collections.addAll(result, rendered.split("\\n", -1));
        return result;
    }

    public static void send(final CommandSender recipient, final String message) {
        if (recipient == null || message == null || ChatColor.stripColor(message).isEmpty()) {
            return;
        }
        for (final String line : message.split("\\n", -1)) {
            recipient.sendMessage(line);
        }
    }

    public static void reset() {
        values = DEFAULTS;
    }

    private static String format(final String template, final Object... replacements) {
        final Map<String, String> tokens = new LinkedHashMap<>();
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            tokens.put(String.valueOf(replacements[index]), String.valueOf(replacements[index + 1]));
        }
        final StringBuilder result = new StringBuilder();
        for (int index = 0; index < template.length(); ) {
            if (template.charAt(index) == '{') {
                final int end = template.indexOf('}', index + 1);
                if (end > index) {
                    final String replacement = tokens.get(template.substring(index + 1, end));
                    if (replacement != null) {
                        result.append(replacement);
                        index = end + 1;
                        continue;
                    }
                }
            }
            result.append(template.charAt(index++));
        }
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private static Map<String, Object> loadDefaults() {
        try (final InputStream stream = Messages.class.getResourceAsStream("/messages.yml")) {
            if (stream == null) {
                throw new IllegalStateException("Brak messages.yml w JAR BedWars.");
            }
            final YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final String key : configuration.getKeys(true)) {
                if (!configuration.isConfigurationSection(key)) {
                    result.put(key, configuration.get(key));
                }
            }
            return Collections.unmodifiableMap(result);
        } catch (final IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Nie mozna odczytac domyslnych wiadomosci BedWars.", exception);
        }
    }
}
