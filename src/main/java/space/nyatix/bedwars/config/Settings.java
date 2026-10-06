package space.nyatix.bedwars.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.shop.ShopItem;
import space.nyatix.bedwars.util.ColorUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    private final BedWarsPlugin plugin;

    private int announcementIndex = 0;

    private long nextAnnouncementAt = 0L;

    public Settings(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        boolean changed = false;
        try (final InputStream stream = plugin.getResource("config.yml")) {
            if (stream != null) {
                final YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                for (final String key : defaults.getKeys(true)) {
                    if (!defaults.isConfigurationSection(key)) {
                        changed |= putDefault(key, defaults.get(key));
                    }
                }
            }
        } catch (final IOException exception) {
            throw new IllegalStateException("Nie mozna odczytac domyslnego config.yml", exception);
        }
        changed |= putDefault("game.autoStartWhenFull", false);
        changed |= putDefault("game.generatorProtectionSize", 3);
        changed |= putDefault("game.baseGeneratorSpeed", 1.5D);
        changed |= putDefault("game.regenerationMultiplier", 0.75D);
        changed |= putDefault("effects.titles", true);
        changed |= putDefault("effects.victoryFireworks", true);
        changed |= putDefault("effects.sounds", true);
        changed |= putDefault("effects.bedBreakDragonSound", true);
        changed |= putDefault("effects.finalKillLightning", true);
        changed |= putDefault("jumps.tnt.enabled", true);
        changed |= putDefault("jumps.tnt.radius", 5.0D);
        changed |= putDefault("jumps.tnt.horizontal", 1.15D);
        changed |= putDefault("jumps.tnt.vertical", 0.90D);
        changed |= putDefault("jumps.tnt.maxHorizontal", 1.90D);
        changed |= putDefault("jumps.fireball.enabled", true);
        changed |= putDefault("jumps.fireball.radius", 4.5D);
        changed |= putDefault("jumps.fireball.horizontal", 1.45D);
        changed |= putDefault("jumps.fireball.vertical", 0.75D);
        changed |= putDefault("jumps.fireball.maxHorizontal", 2.20D);
        changed |= putDefault("announcements.enabled", true);
        changed |= putDefault("announcements.intervalSeconds", 180);
        changed |= putDefault("announcements.prefix", "&6&lELEKTRONIK &8» &f");
        changed |= putDefault("announcements.messages.1", "Organizatorem turnieju jest &eSamorzad Uczniowski Elektronika&f.");
        changed |= putDefault("announcements.messages.2", "Serwer zostal stworzony przez &eMateusza Nowosielskiego &fwe wspolpracy z &eSamorzadem Uczniowskim&f.");
        changed |= putDefault("announcements.messages.3", "Powodzenia wszystkim uczestnikom turnieju BedWars!");
        changed |= putDefault("announcements.messages.4", "W razie problemow technicznych zglos sie do organizatorow turnieju.");
        for (final ShopItem item : ShopItem.values()) {
            final String base = shopPath(item);
            changed |= putDefault(base + ".enabled", true);
            changed |= putDefault(base + ".price", item.price());
            changed |= putDefault(base + ".amount", item.amount());
            changed |= putDefault(base + ".currency", item.currency().name());
        }
        final int[] toolPrices = { 10, 10, 3, 6 };
        final Material[] toolCurrencies = { Material.IRON_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT, Material.GOLD_INGOT };
        for (final String tool : new String[] { "pickaxe", "axe" }) {
            for (int tier = 1; tier <= 4; tier++) {
                changed |= putDefault("shop.tiered." + tool + "." + tier + ".price", toolPrices[tier - 1]);
                changed |= putDefault("shop.tiered." + tool + "." + tier + ".currency", toolCurrencies[tier - 1].name());
            }
        }
        changed |= putDefault("upgrades.sharpness.price", 8);
        final int[] protection = { 5, 10, 20, 30 };
        final int[] haste = { 4, 6 };
        final int[] forge = { 4, 8, 12, 16 };
        final int[] traps = { 1, 2, 4 };
        for (int i = 0; i < protection.length; i++) {
            changed |= putDefault("upgrades.protection." + (i + 1), protection[i]);
        }
        for (int i = 0; i < haste.length; i++) {
            changed |= putDefault("upgrades.haste." + (i + 1), haste[i]);
        }
        for (int i = 0; i < forge.length; i++) {
            changed |= putDefault("upgrades.forge." + (i + 1), forge[i]);
        }
        changed |= putDefault("upgrades.healPool.price", 3);
        changed |= putDefault("upgrades.dragonBuff.price", 5);
        for (int i = 0; i < traps.length; i++) {
            changed |= putDefault("upgrades.traps." + (i + 1), traps[i]);
        }
        if (changed) {
            plugin.saveConfig();
        }
        startAnnouncementTask();
    }

    private void startAnnouncementTask() {
        this.nextAnnouncementAt = System.currentTimeMillis() + announcementIntervalSeconds() * 1000L;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::announcementTick, 20L, 20L);
    }

    private void announcementTick() {
        if (!announcementsEnabled()) {
            this.nextAnnouncementAt = System.currentTimeMillis() + 1000L;
            return;
        }
        final long now = System.currentTimeMillis();
        if (now < this.nextAnnouncementAt) {
            return;
        }
        final List<String> messages = announcementMessages();
        if (!messages.isEmpty()) {
            if (this.announcementIndex >= messages.size()) {
                this.announcementIndex = 0;
            }
            final String line = ColorUtil.c(announcementPrefix() + messages.get(this.announcementIndex++));
            for (final Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage("");
                player.sendMessage(line);
                player.sendMessage("");
            }
        }
        this.nextAnnouncementAt = now + announcementIntervalSeconds() * 1000L;
    }

    private boolean putDefault(final String path, final Object value) {
        if (this.plugin.getConfig().isSet(path)) {
            return false;
        }
        this.plugin.getConfig().set(path, value);
        return true;
    }

    public int playersPerTeam() {
        return this.plugin.getConfig().getInt("game.playersPerTeam", 4);
    }

    public int minPlayers() {
        return this.plugin.getConfig().getInt("game.minPlayers", 2);
    }

    public int reconnectSeconds() {
        return this.plugin.getConfig().getInt("game.reconnectSeconds", 180);
    }

    public int countdown() {
        return this.plugin.getConfig().getInt("game.countdownSeconds", 20);
    }

    public int respawnSeconds() {
        return this.plugin.getConfig().getInt("game.respawnSeconds", 5);
    }

    public int respawnProtectionSeconds() {
        return this.plugin.getConfig().getInt("game.respawnProtectionSeconds", 3);
    }

    public int combatTagSeconds() {
        return this.plugin.getConfig().getInt("game.combatTagSeconds", 15);
    }

    public double baseRadius() {
        return this.plugin.getConfig().getDouble("game.baseRadius", 16.0);
    }

    public double buildHeight() {
        return this.plugin.getConfig().getDouble("game.buildHeight", 120.0);
    }

    public double voidY() {
        return this.plugin.getConfig().getDouble("game.voidY", 0.0);
    }

    public boolean friendlyFire() {
        return !this.plugin.getConfig().getBoolean("game.friendlyFire", false);
    }

    public boolean autoJoin() {
        return this.plugin.getConfig().getBoolean("game.autoJoin", true);
    }

    public boolean autoStartWhenFull() {
        return this.plugin.getConfig().getBoolean("game.autoStartWhenFull", false);
    }

    public int generatorProtectionSize() {
        int v = this.plugin.getConfig().getInt("game.generatorProtectionSize", 3);
        if (v < 1) {
            v = 1;
        }
        if (v % 2 == 0) {
            v++;
        }
        return Math.min(9, v);
    }

    public boolean tntJumpEnabled() {
        return this.plugin.getConfig().getBoolean("jumps.tnt.enabled", true);
    }

    public double tntJumpRadius() {
        return this.plugin.getConfig().getDouble("jumps.tnt.radius", 5.0D);
    }

    public double tntJumpHorizontal() {
        return this.plugin.getConfig().getDouble("jumps.tnt.horizontal", 1.15D);
    }

    public double tntJumpVertical() {
        return this.plugin.getConfig().getDouble("jumps.tnt.vertical", 0.90D);
    }

    public double tntJumpMaxHorizontal() {
        return this.plugin.getConfig().getDouble("jumps.tnt.maxHorizontal", 1.90D);
    }

    public boolean fireballJumpEnabled() {
        return this.plugin.getConfig().getBoolean("jumps.fireball.enabled", true);
    }

    public double fireballJumpRadius() {
        return this.plugin.getConfig().getDouble("jumps.fireball.radius", 4.5D);
    }

    public double fireballJumpHorizontal() {
        return this.plugin.getConfig().getDouble("jumps.fireball.horizontal", 1.45D);
    }

    public double fireballJumpVertical() {
        return this.plugin.getConfig().getDouble("jumps.fireball.vertical", 0.75D);
    }

    public double fireballJumpMaxHorizontal() {
        return this.plugin.getConfig().getDouble("jumps.fireball.maxHorizontal", 2.20D);
    }

    public boolean allowAdminBuild() {
        return this.plugin.getConfig().getBoolean("game.allowAdminBuild", true);
    }

    public boolean logMatches() {
        return this.plugin.getConfig().getBoolean("logging.matches", true);
    }

    public int timer(final String key) {
        return this.plugin.getConfig().getInt("game.timers." + key);
    }

    public String serverLine() {
        return this.plugin.getConfig().getString("scoreboard.serverLine", "Elektronik");
    }

    public String victoryTitle() {
        return this.plugin.getConfig().getString("messages.victoryTitle", "ZWYCIESTWO!");
    }

    public boolean titlesEnabled() {
        return this.plugin.getConfig().getBoolean("effects.titles", true);
    }

    public boolean victoryFireworksEnabled() {
        return this.plugin.getConfig().getBoolean("effects.victoryFireworks", true);
    }

    public boolean effectSoundsEnabled() {
        return this.plugin.getConfig().getBoolean("effects.sounds", true);
    }

    public boolean bedBreakDragonSoundEnabled() {
        return this.plugin.getConfig().getBoolean("effects.bedBreakDragonSound", true);
    }

    public boolean finalKillLightningEnabled() {
        return this.plugin.getConfig().getBoolean("effects.finalKillLightning", true);
    }

    public double baseGeneratorSpeed() {
        return rate("game.baseGeneratorSpeed", 1.5D, 0.1D, 10D);
    }

    public double regenerationMultiplier() {
        return rate("game.regenerationMultiplier", 0.75D, 0D, 1D);
    }

    private double rate(final String path, final double fallback, final double min, final double max) {
        final double value = this.plugin.getConfig().getDouble(path, fallback);
        return Double.isNaN(value) || Double.isInfinite(value) ? fallback : Math.max(min, Math.min(max, value));
    }

    public boolean announcementsEnabled() {
        return this.plugin.getConfig().getBoolean("announcements.enabled", true);
    }

    public int announcementIntervalSeconds() {
        return Math.max(30, this.plugin.getConfig().getInt("announcements.intervalSeconds", 180));
    }

    public String announcementPrefix() {
        return this.plugin.getConfig().getString("announcements.prefix", "&6&lELEKTRONIK &8» &f");
    }

    public List<String> announcementMessages() {
        final List<String> out = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            final String value = this.plugin.getConfig().getString("announcements.messages." + i);
            if (value != null && !value.trim().isEmpty()) {
                out.add(value);
            }
        }
        return out;
    }

    private String shopPath(final ShopItem item) {
        return "shop.items." + item.name().toLowerCase();
    }

    public boolean shopEnabled(final ShopItem item) {
        return this.plugin.getConfig().getBoolean(shopPath(item) + ".enabled", true);
    }

    public int shopPrice(final ShopItem item) {
        return Math.max(0, this.plugin.getConfig().getInt(shopPath(item) + ".price", item.price()));
    }

    public int shopAmount(final ShopItem item) {
        return Math.max(1, Math.min(64, this.plugin.getConfig().getInt(shopPath(item) + ".amount", item.amount())));
    }

    public Material shopCurrency(final ShopItem item) {
        return material(this.plugin.getConfig().getString(shopPath(item) + ".currency", item.currency().name()), item.currency());
    }

    public void setShopEnabled(final ShopItem item, final boolean value) {
        setBoolean(shopPath(item) + ".enabled", value);
    }

    public void setShopPrice(final ShopItem item, final int value) {
        setInt(shopPath(item) + ".price", Math.max(0, value));
    }

    public void setShopAmount(final ShopItem item, final int value) {
        setInt(shopPath(item) + ".amount", Math.max(1, Math.min(64, value)));
    }

    public void setShopCurrency(final ShopItem item, final Material value) {
        setString(shopPath(item) + ".currency", value.name());
    }

    public int toolPrice(final ShopItem item, final int tier) {
        final String tool = item == ShopItem.PICKAXE ? "pickaxe" : "axe";
        return Math.max(0, this.plugin.getConfig().getInt("shop.tiered." + tool + "." + clampTier(tier) + ".price", tier <= 2 ? 10 : (tier == 3 ? 3 : 6)));
    }

    public Material toolCurrency(final ShopItem item, final int tier) {
        final String tool = item == ShopItem.PICKAXE ? "pickaxe" : "axe";
        final Material def = tier <= 2 ? Material.IRON_INGOT : Material.GOLD_INGOT;
        return material(this.plugin.getConfig().getString("shop.tiered." + tool + "." + clampTier(tier) + ".currency", def.name()), def);
    }

    public void setToolPrice(final ShopItem item, final int tier, final int value) {
        final String tool = item == ShopItem.PICKAXE ? "pickaxe" : "axe";
        setInt("shop.tiered." + tool + "." + clampTier(tier) + ".price", Math.max(0, value));
    }

    public void setToolCurrency(final ShopItem item, final int tier, final Material value) {
        final String tool = item == ShopItem.PICKAXE ? "pickaxe" : "axe";
        setString("shop.tiered." + tool + "." + clampTier(tier) + ".currency", value.name());
    }

    public int upgradePrice(final String key, final int level, final int fallback) {
        return Math.max(0, this.plugin.getConfig().getInt("upgrades." + key + (level > 0 ? "." + level : ".price"), fallback));
    }

    public void setUpgradePrice(final String key, final int level, final int value) {
        setInt("upgrades." + key + (level > 0 ? "." + level : ".price"), Math.max(0, value));
    }

    private int clampTier(final int tier) {
        return Math.max(1, Math.min(4, tier));
    }

    private Material material(final String name, final Material fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (final Exception ignored) {
            return fallback;
        }
    }

    public void setInt(final String path, final int value) {
        this.plugin.getConfig().set(path, value);
        this.plugin.saveConfig();
    }

    public void setDouble(final String path, final double value) {
        this.plugin.getConfig().set(path, value);
        this.plugin.saveConfig();
    }

    public void setBoolean(final String path, final boolean value) {
        this.plugin.getConfig().set(path, value);
        this.plugin.saveConfig();
    }

    public void setString(final String path, final String value) {
        this.plugin.getConfig().set(path, value);
        this.plugin.saveConfig();
    }

    public void apply(final YamlConfiguration configuration) throws InvalidConfigurationException {
        this.plugin.getConfig().loadFromString(configuration.saveToString());
        this.nextAnnouncementAt = System.currentTimeMillis() + announcementIntervalSeconds() * 1000L;
    }

    public void reload() throws IOException, InvalidConfigurationException {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(new File(this.plugin.getDataFolder(), "config.yml"));
        ConfigurationValidator.validate(configuration);
        this.apply(configuration);
    }
}
