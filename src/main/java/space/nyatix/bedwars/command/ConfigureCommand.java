package space.nyatix.bedwars.command;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConfigureCommand implements CommandExecutor, TabCompleter {

    private final BedWarsPlugin plugin;

    public ConfigureCommand(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, Messages.get("configure-command.on-command.tej-komendy-moze-uzyc-tylko-gracz"));
            return true;
        }
        final Player player = (Player) sender;
        if (!player.hasPermission("bedwars.admin")) {
            Messages.send(player, Messages.get("configure-command.on-command.nie-masz-uprawnien-do-konfiguracji-bedwars"));
            return true;
        }
        if (args.length != 1) {
            Messages.send(player, Messages.get("configure-command.on-command.text"));
            Messages.send(player, Messages.get("configure-command.on-command.konfigurator-bedwars"));
            Messages.send(player, Messages.get("configure-command.on-command.wpisz-tylko-nazwe-folderu-mapy"));
            Messages.send(player, Messages.get("configure-command.on-command.konfiguruj-nazwa-mapy"));
            Messages.send(player, Messages.get("configure-command.on-command.przyklad-konfiguruj-rooftop"));
            Messages.send(player, Messages.get("configure-command.on-command.text"));
            return true;
        }
        if (this.plugin.game().state() == GameState.RUNNING || this.plugin.game().state() == GameState.PAUSED) {
            Messages.send(player, Messages.get("configure-command.on-command.nie-mozna-edytowac-mapy-podczas-trwajacego"));
            Messages.send(player, Messages.get("configure-command.on-command.najpierw-zakoncz-rozgrywke-przez-bw-zatrzymaj"));
            return true;
        }
        final String mapName = args[0];
        if (!mapName.matches("[A-Za-z0-9._-]{1,48}") || mapName.contains("..")) {
            Messages.send(player, Messages.get("configure-command.on-command.nieprawidlowa-nazwa-mapy-uzywaj-tylko-liter"));
            return true;
        }
        if (mapName.startsWith("bw_") || mapName.startsWith("bw-tpl-") || mapName.startsWith("bw_tpl_")) {
            Messages.send(player, Messages.get("configure-command.on-command.nie-konfiguruj-swiata-runtime-template-podaj"));
            return true;
        }
        final File worldFolder = new File(this.plugin.getServer().getWorldContainer(), mapName);
        if (!worldFolder.isDirectory() || !new File(worldFolder, "level.dat").isFile()) {
            Messages.send(player, Messages.get("configure-command.on-command.nie-znaleziono-poprawnej-mapy"));
            Messages.send(player, Messages.get("configure-command.on-command.nie-ma-poprawnego-folderu-swiata-z", "MAP_NAME", mapName));
            Messages.send(player, Messages.get("configure-command.on-command.wrzuc-mape-obok-folderu-world-i"));
            Messages.send(player, Messages.get("configure-command.on-command.konfiguruj", "MAP_NAME", mapName));
            return true;
        }
        this.plugin.game().prepareForConfiguration();
        if (this.plugin.startup() != null) {
            this.plugin.startup().stop();
        }
        final Arena existing = this.plugin.game().arenas().get(mapName.toLowerCase());
        if (existing != null && this.plugin.worlds() != null) {
            this.plugin.worlds().unloadRuntime(existing);
        }
        World world = Bukkit.getWorld(mapName);
        if (world == null) {
            Messages.send(player, Messages.get("configure-command.on-command.ladowanie-swiata", "MAP_NAME", mapName));
            try {
                world = new WorldCreator(mapName).createWorld();
            } catch (Throwable ex) {
                this.plugin.getLogger().warning(Messages.get("configure-command.on-command.nie-udalo-sie-zaladowac-swiata", "MAP_NAME", mapName,
                        "ERROR", ex.getMessage()));
                Messages.send(player, Messages.get("configure-command.on-command.nie-udalo-sie-zaladowac-mapy-szczegoly"));
                return true;
            }
        }
        if (world == null) {
            Messages.send(player, Messages.get("configure-command.on-command.bukkit-nie-zaladowal-swiata", "MAP_NAME", mapName));
            return true;
        }
        final Map<String, Arena> refreshed = this.plugin.arenaStorage().loadAll();
        Arena arena = refreshed.get(mapName.toLowerCase());
        if (arena != null) {
            this.plugin.game().arenas().put(mapName.toLowerCase(), arena);
        } else {
            arena = this.plugin.game().arenas().get(mapName.toLowerCase());
        }
        if (arena == null) {
            arena = this.plugin.game().createArena(mapName, world);
        }
        arena.setWorld(mapName);
        arena.bindWorld(world);
        this.plugin.game().arenas().put(mapName.toLowerCase(), arena);
        this.plugin.game().setActive(arena);
        if (this.plugin.worlds() != null) {
            this.plugin.worlds().enterConfiguration(arena, world);
        }
        this.plugin.arenaStorage().saveArena(arena);
        if (this.plugin.npcs() != null) {
            this.plugin.npcs().respawn();
        }
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(false);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        try {
            player.setHealth(player.getMaxHealth());
        } catch (Exception ignored) {
        }
        Location target = arena.getLobby();
        if (target == null || target.getWorld() == null) {
            target = world.getSpawnLocation().clone().add(.5D, 0D, .5D);
        }
        player.teleport(target);
        this.plugin.configMenu().startSession(player, arena);
        Messages.send(player, Messages.get("configure-command.on-command.text"));
        Messages.send(player, Messages.get("configure-command.on-command.konfigurator-uruchomiony"));
        Messages.send(player, Messages.get("configure-command.on-command.mapa", "MAP", arena.getName()));
        Messages.send(player, Messages.get("configure-command.on-command.nie-musisz-znac-zadnych-komend-ustaw"));
        Messages.send(player, Messages.get("configure-command.on-command.diamentowy-kilof-otwiera-menu-prawym-przyciskiem"));
        if (this.plugin.worlds() == null || !this.plugin.worlds().available()) {
            Messages.send(player, Messages.get("configure-command.on-command.uwaga-brak-continued-slime-world-manager"));
        }
        Messages.send(player, Messages.get("configure-command.on-command.text"));
        return true;
    }

    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final List<String> out = new ArrayList<>();
        if (args.length != 1 || !sender.hasPermission("bedwars.admin")) {
            return out;
        }
        final File root = this.plugin.getServer().getWorldContainer();
        final File[] files = root.listFiles();
        if (files == null) {
            return out;
        }
        final String prefix = args[0].toLowerCase();
        for (final File file : files) {
            if (!file.isDirectory() || !new File(file, "level.dat").isFile()) {
                continue;
            }
            final String name = file.getName();
            final String lower = name.toLowerCase();
            if (lower.startsWith("bw_") || lower.startsWith("bw_tpl_")) {
                continue;
            }
            if (lower.startsWith(prefix)) {
                out.add(name);
            }
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }
}
