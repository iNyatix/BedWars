package space.nyatix.bedwars.config;

import org.bukkit.configuration.file.YamlConfiguration;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.shop.ShopItem;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class QuickBuyStorage {

    private final File file;

    private final YamlConfiguration cfg;

    public QuickBuyStorage(final BedWarsPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "quickbuy.yml");
        this.cfg = YamlConfiguration.loadConfiguration(this.file);
    }

    public ShopItem[] load(final UUID playerId, final ShopItem[] defaults) {
        final String raw = this.cfg.getString("players." + playerId.toString(), "");
        if (raw == null || raw.trim().isEmpty()) {
            return defaults.clone();
        }
        final String[] values = raw.split(",", -1);
        final ShopItem[] out = new ShopItem[21];
        for (int i = 0; i < Math.min(out.length, values.length); i++) {
            final String v = values[i];
            if (v == null || v.isEmpty() || v.equalsIgnoreCase("null")) {
                continue;
            }
            try {
                out[i] = ShopItem.valueOf(v);
            } catch (final Exception ignored) {
            }
        }
        return out;
    }

    public void save(final UUID playerId, final ShopItem[] items) {
        final StringBuilder raw = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                raw.append(',');
            }
            raw.append(items[i] == null ? "null" : items[i].name());
        }
        this.cfg.set("players." + playerId.toString(), raw.toString());
        flush();
    }

    private void flush() {
        try {
            if (this.file.getParentFile() != null) {
                this.file.getParentFile().mkdirs();
            }
            this.cfg.save(this.file);
        } catch (final IOException ignored) {
        }
    }
}
