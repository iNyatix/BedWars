package space.nyatix.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import space.nyatix.bedwars.command.BedWarsCommand;
import space.nyatix.bedwars.command.ConfigureCommand;
import space.nyatix.bedwars.config.ArenaStorage;
import space.nyatix.bedwars.config.ConfigurationValidator;
import space.nyatix.bedwars.config.Settings;
import space.nyatix.bedwars.game.ArenaStartupManager;
import space.nyatix.bedwars.game.ConfigHologramManager;
import space.nyatix.bedwars.game.GameManager;
import space.nyatix.bedwars.game.GeneratorManager;
import space.nyatix.bedwars.game.InvisibilityManager;
import space.nyatix.bedwars.game.LobbyManager;
import space.nyatix.bedwars.game.MatchEffectsManager;
import space.nyatix.bedwars.game.MatchLogger;
import space.nyatix.bedwars.game.NpcManager;
import space.nyatix.bedwars.game.PartyManager;
import space.nyatix.bedwars.game.ScoreboardManager;
import space.nyatix.bedwars.game.ServerDiagnostics;
import space.nyatix.bedwars.game.SlimeWorldService;
import space.nyatix.bedwars.game.SpectatorManager;
import space.nyatix.bedwars.game.WorldBorderManager;
import space.nyatix.bedwars.game.WorldRulesManager;
import space.nyatix.bedwars.gui.AdminMatchMenu;
import space.nyatix.bedwars.gui.ConfigMenu;
import space.nyatix.bedwars.listener.EquipmentRulesListener;
import space.nyatix.bedwars.listener.GameListener;
import space.nyatix.bedwars.listener.MenuListener;
import space.nyatix.bedwars.listener.RegenerationListener;
import space.nyatix.bedwars.listener.SwordRulesListener;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.shop.ShopManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BedWarsPlugin extends JavaPlugin {

    private Settings settings;

    private Messages messages;

    private ArenaStorage arenaStorage;

    private GameManager game;

    private ConfigMenu configMenu;

    private ShopManager shop;

    private ScoreboardManager scoreboards;

    private SpectatorManager spectators;

    private GeneratorManager generators;

    private NpcManager npcs;

    private MatchLogger matchLogger;

    private LobbyManager lobby;

    private SlimeWorldService worlds;

    private ConfigHologramManager configHolograms;

    private InvisibilityManager invisibility;

    private PartyManager parties;

    private MatchEffectsManager effects;

    private WorldRulesManager worldRules;

    private ArenaStartupManager startup;

    private WorldBorderManager borders;

    private ServerDiagnostics diagnostics;

    private SwordRulesListener swordRules;

    private AdminMatchMenu adminMenu;

    public void onEnable() {
        migrateLegacyData();
        saveDefaultConfig();
        try {
            final YamlConfiguration checked = new YamlConfiguration();
            checked.load(new File(getDataFolder(), "config.yml"));
            ConfigurationValidator.validate(checked);
        } catch (final Exception exception) {
            getLogger().severe("Nie mozna wczytac config.yml: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        migratePolishDefaults();
        this.messages = new Messages(this);
        try {
            this.messages.reload();
        } catch (final Exception exception) {
            getLogger().severe(Messages.get("bed-wars-plugin.on-enable.nie-mozna-wczytac-messages-yml", "ERROR", exception.getMessage()));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.settings = new Settings(this);
        this.arenaStorage = new ArenaStorage(this);
        this.borders = new WorldBorderManager(this);
        this.game = new GameManager(this, this.arenaStorage.loadAll());
        this.worldRules = new WorldRulesManager(this);
        this.parties = new PartyManager(this);
        this.effects = new MatchEffectsManager(this);
        this.shop = new ShopManager(this);
        this.scoreboards = new ScoreboardManager(this);
        this.spectators = new SpectatorManager(this);
        this.adminMenu = new AdminMatchMenu(this);
        this.diagnostics = new ServerDiagnostics(this);
        this.diagnostics.start();
        this.generators = new GeneratorManager(this);
        this.npcs = new NpcManager(this);
        this.matchLogger = new MatchLogger(this);
        this.lobby = new LobbyManager(this);
        this.worlds = new SlimeWorldService(this);
        this.configHolograms = new ConfigHologramManager(this);
        this.invisibility = new InvisibilityManager(this);
        if (!this.worlds.available()) {
            getLogger().warning(Messages.get("bed-wars-plugin.on-enable.brak-continued-slime-world-manager-mecze"));
        }
        this.configMenu = new ConfigMenu(this);
        final ConfigureCommand configureCommand = new ConfigureCommand(this);
        final BedWarsCommand bwCommand = new BedWarsCommand(this);
        getCommand("konfiguruj").setExecutor(configureCommand);
        getCommand("konfiguruj").setTabCompleter(configureCommand);
        getCommand("bw").setExecutor(bwCommand);
        getCommand("bw").setTabCompleter(bwCommand);
        configureCommands();
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EquipmentRulesListener(this), this);
        this.swordRules = new SwordRulesListener(this);
        Bukkit.getPluginManager().registerEvents(this.swordRules, this);
        this.swordRules.start();
        Bukkit.getPluginManager().registerEvents(this.effects, this);
        Bukkit.getPluginManager().registerEvents(new RegenerationListener(this), this);
        Bukkit.getPluginManager().registerEvents(this.worldRules, this);
        this.worldRules.start();
        this.borders.start();
        this.startup = new ArenaStartupManager(this);
        this.startup.start();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                this.game.onJoin(player);
            }
        }, 20L);
        getLogger().info(Messages.get("bed-wars-plugin.on-enable.bedwars-uruchomiony-autor-mateusz-nowosielski-spigot", "VERSION", getDescription().getVersion()));
    }

    private void migrateLegacyData() {
        final File current = getDataFolder();
        final File parent = current.getParentFile();
        if (parent == null) {
            return;
        }
        final File legacy = new File(parent, "BedWars2026");
        if (current.exists() || !legacy.isDirectory()) {
            return;
        }
        if (!current.mkdirs()) {
            return;
        }
        copyLegacy(legacy, current, "config.yml", "config.yml");
        copyLegacy(legacy, current, "arenas.yml", "arenas.yml");
        copyLegacy(legacy, current, "quickbuy.yml", "quickbuy.yml");
        copyLegacy(legacy, current, "match-history.csv", "historia-meczow.csv");
        getLogger().info(Messages.get("bed-wars-plugin.migrate-legacy-data.przeniesiono-dane-z-poprzedniego-folderu-plugins"));
    }

    private void copyLegacy(final File fromDir, final File toDir, final String from, final String to) {
        final File source = new File(fromDir, from);
        if (!source.isFile()) {
            return;
        }
        try {
            Files.copy(source.toPath(), new File(toDir, to).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException ex) {
            getLogger().warning(Messages.get("bed-wars-plugin.copy-legacy.nie-udalo-sie-przeniesc", "FROM", from, "ERROR", ex.getMessage()));
        }
    }

    private void migratePolishDefaults() {
        boolean changed = false;
        final String line = getConfig().getString("scoreboard.serverLine");
        if (line == null || "SCHOOL TOURNAMENT".equalsIgnoreCase(line) || "TURNIEJ SZKOLNY".equalsIgnoreCase(line)) {
            getConfig().set("scoreboard.serverLine", "Elektronik");
            changed = true;
        }
        if ("VICTORY!".equalsIgnoreCase(getConfig().getString("messages.victoryTitle"))) {
            getConfig().set("messages.victoryTitle", "ZWYCIESTWO!");
            changed = true;
        }
        if (changed) {
            saveConfig();
        }
    }

    public void onDisable() {
        Messages.reset();
        if (this.swordRules != null) {
            this.swordRules.shutdown();
        }
        if (this.diagnostics != null) {
            this.diagnostics.shutdown();
        }
        if (this.startup != null) {
            this.startup.stop();
        }
        if (this.worldRules != null) {
            this.worldRules.shutdown();
        }
        if (this.effects != null) {
            this.effects.shutdown();
        }
        if (this.spectators != null) {
            this.spectators.shutdown();
        }
        if (this.generators != null) {
            this.generators.shutdown();
        }
        if (this.configHolograms != null) {
            this.configHolograms.clearAll();
        }
        if (this.invisibility != null) {
            this.invisibility.shutdown();
        }
        if (this.npcs != null) {
            this.npcs.remove();
        }
        if (this.game != null) {
            this.game.shutdown();
        }
        if (this.worlds != null && this.game != null && this.game.active() != null) {
            this.worlds.unloadRuntime(this.game.active());
        }
    }

    private void configureCommands() {
        for (final String name : new String[] { "bw", "party", "konfiguruj" }) {
            final PluginCommand command = getCommand(name);
            if (command == null) {
                continue;
            }
            command.setDescription(Messages.get("commands." + name + ".description"));
            command.setUsage(Messages.get("commands." + name + ".usage"));
            command.setPermissionMessage(Messages.get("commands.no-permission"));
        }
    }

    public Messages messages() {
        return this.messages;
    }

    public void reloadFiles() throws Exception {
        final YamlConfiguration checked = new YamlConfiguration();
        checked.load(new File(getDataFolder(), "config.yml"));
        ConfigurationValidator.validate(checked);
        this.messages.reload();
        this.settings.apply(checked);
        configureCommands();
        try {
            if (this.npcs != null) {
                this.npcs.respawn();
            }
            for (final Player player : Bukkit.getOnlinePlayers()) {
                player.closeInventory();
                if (this.scoreboards != null) {
                    this.scoreboards.update(player);
                }
            }
        } catch (final Exception exception) {
            getLogger().warning(Messages.get("configuration.ui-refresh-failed", "ERROR", exception.getMessage()));
        }
    }

    public Settings settings() {
        return this.settings;
    }

    public ArenaStorage arenaStorage() {
        return this.arenaStorage;
    }

    public GameManager game() {
        return this.game;
    }

    public ConfigMenu configMenu() {
        return this.configMenu;
    }

    public ShopManager shop() {
        return this.shop;
    }

    public ScoreboardManager scoreboards() {
        return this.scoreboards;
    }

    public SpectatorManager spectators() {
        return this.spectators;
    }

    public GeneratorManager generators() {
        return this.generators;
    }

    public NpcManager npcs() {
        return this.npcs;
    }

    public MatchLogger matchLogger() {
        return this.matchLogger;
    }

    public LobbyManager lobby() {
        return this.lobby;
    }

    public SlimeWorldService worlds() {
        return this.worlds;
    }

    public ConfigHologramManager configHolograms() {
        return this.configHolograms;
    }

    public InvisibilityManager invisibility() {
        return this.invisibility;
    }

    public PartyManager parties() {
        return this.parties;
    }

    public MatchEffectsManager effects() {
        return this.effects;
    }

    public WorldRulesManager worldRules() {
        return this.worldRules;
    }

    public ArenaStartupManager startup() {
        return this.startup;
    }

    public WorldBorderManager borders() {
        return this.borders;
    }

    public ServerDiagnostics diagnostics() {
        return this.diagnostics;
    }

    public AdminMatchMenu adminMenu() {
        return this.adminMenu;
    }
}
