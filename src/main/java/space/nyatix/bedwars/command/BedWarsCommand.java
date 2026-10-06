package space.nyatix.bedwars.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.command.subcommand.ReloadCommand;
import space.nyatix.bedwars.command.subcommand.StatusCommand;
import space.nyatix.bedwars.game.ArenaValidator;
import space.nyatix.bedwars.game.SlimeWorldService;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.GeneratorType;
import space.nyatix.bedwars.model.TeamColor;

public class BedWarsCommand implements CommandExecutor, TabCompleter {

    private final BedWarsPlugin plugin;

    public BedWarsCommand(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        final String subcommand = normalize(args[0]);
        if (subcommand.equals("join") && sender instanceof Player) {
            this.plugin.game().joinWaiting((Player) sender);
            return true;
        }
        if (subcommand.equals("leave") && sender instanceof Player) {
            this.plugin.game().leaveWaiting((Player) sender);
            return true;
        }
        if (subcommand.equals("team") && sender instanceof Player) {
            final TeamColor t = this.plugin.game().teamOf(((Player) sender).getUniqueId());
            Messages.send(sender, Messages.get("bed-wars-command.on-command.twoja-druzyna", "TEAM", (t == null ? Messages.get("bed-wars-command.on-command.brak") : t.chat() + t.display())));
            return true;
        }
        if (!sender.hasPermission("bedwars.admin")) {
            Messages.send(sender, Messages.get("bed-wars-command.on-command.brak-uprawnien"));
            return true;
        }
        if (subcommand.equals("panel") && sender instanceof Player) {
            this.plugin.adminMenu().open((Player) sender);
            return true;
        }
        if (subcommand.equals("observe")) {
            if (!(sender instanceof Player)) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.tej-komendy-mozna-uzyc-tylko-w"));
                return true;
            }
            if (args.length != 1) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.uzycie-bw-obserwuj"));
                return true;
            }
            this.plugin.game().joinObservation((Player) sender);
            return true;
        }
        if (subcommand.equals("setlobby") && sender instanceof Player) {
            this.plugin.lobby().setLobby(((Player) sender).getLocation());
            Messages.send(sender, Messages.get("bed-wars-command.on-command.ustawiono-glowne-lobby-serwera-nowi-gracze"));
            return true;
        }
        if (subcommand.equals("create") && sender instanceof Player && args.length >= 2) {
            final Player p = (Player) sender;
            final Arena ar = this.plugin.game().createArena(args[1], p.getWorld());
            Messages.send(sender, Messages.get("bed-wars-command.on-command.utworzono-i-wybrano-mape-dalsza-konfiguracja", "PLAYER", ar.getName(),
                    "PLAYER2", ar.getName()));
            return true;
        }
        if (subcommand.equals("delete") && args.length >= 2) {
            this.plugin.game().deleteArena(args[1]);
            Messages.send(sender, Messages.get("bed-wars-command.on-command.usunieto-mape"));
            return true;
        }
        if (subcommand.equals("select") && args.length >= 2) {
            final Arena ar = this.plugin.game().arenas().get(args[1].toLowerCase());
            if (ar == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.nie-ma-takiej-mapy"));
                return true;
            }
            if (this.plugin.game().state() != GameState.WAITING && this.plugin.game().state() != GameState.COUNTDOWN) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.najpierw-zakoncz-i-zresetuj-obecny-mecz"));
                return true;
            }
            final Arena old = this.plugin.game().active();
            if (old != null && old != ar && this.plugin.worlds() != null && !this.plugin.worlds().unloadRuntime(old)) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.nie-udalo-sie-wyladowac-poprzedniej-mapy"));
                return true;
            }
            this.plugin.game().setActive(ar);
            Messages.send(sender, Messages.get("bed-wars-command.on-command.wybrano-mape", "PLAYER", ar.getName()));
            final List<String> missing = ArenaValidator.validate(ar);
            if (!missing.isEmpty()) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.konfiguracja-mapy-jest-niekompletna", "MISSING", missing));
                return true;
            }
            if (this.plugin.worlds() != null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.przygotowuje-swieza-kopie-zapisanej-mapy"));
                this.plugin.worlds().prepareRuntimeAsync(ar, (world, error) -> {
                    if (plugin.game().active() != ar) {
                        return;
                    }
                    if (error != null || world == null) {
                        Messages.send(sender, Messages.get("bed-wars-command.finished.nie-udalo-sie-zaladowac-zapisanej-mapy"));
                        return;
                    }
                    if (plugin.npcs() != null) {
                        plugin.npcs().respawn();
                    }
                    Messages.send(sender, Messages.get("bed-wars-command.finished.kopia-turniejowa-mapy-jest-gotowa"));
                });
            }
            return true;
        }
        if (subcommand.equals("list")) {
            Messages.send(sender, Messages.get("bed-wars-command.on-command.mapy", "VALUE1", this.plugin.game().arenas().keySet()));
            return true;
        }
        if (subcommand.equals("validate")) {
            final List<String> problems = ArenaValidator.validate(this.plugin.game().active());
            if (problems.isEmpty()) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.mapa-jest-kompletna-i-gotowa-do"));
            } else {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.mapa-jest-niekompletna"));
                for (final String x : problems) {
                    Messages.send(sender, Messages.get("bed-wars-command.on-command.text", "VALUE1", x));
                }
            }
            return true;
        }
        if (subcommand.equals("setteam") && args.length >= 3) {
            final Player p = Bukkit.getPlayer(args[1]);
            if (p == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.gracz-jest-offline"));
                return true;
            }
            final TeamColor t = parseTeam(args[2]);
            if (t == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.druzyna-czerwoni-zieloni-niebiescy-biali"));
                return true;
            }
            if (!this.plugin.game().assign(p, t)) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.nie-udalo-sie-przypisac-gracza-druzyna"));
            } else {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.text-4", "PLAYER", p.getName(), "VALUE2", t.chat(), "TEAM", t.display()));
            }
            return true;
        }
        if (subcommand.equals("teams")) {
            for (final TeamColor tc : TeamColor.values()) {
                final StringBuilder b = new StringBuilder(Messages.get("bed-wars-command.on-command.text-2", "TEAM", tc.chat() + tc.display()));
                boolean first = true;
                final Arena ar = this.plugin.game().active();
                if (ar != null) {
                    for (final UUID playerId : ar.team(tc).getPlayers()) {
                        if (!first) {
                            b.append(Messages.get("bed-wars-command.on-command.text-3"));
                        }
                        b.append(tc.chat()).append(name(playerId));
                        first = false;
                    }
                }
                if (first) {
                    b.append(Messages.get("bed-wars-command.on-command.brak-2"));
                }
                Messages.send(sender, b.toString());
            }
            return true;
        }
        if (subcommand.equals("forcestart") || subcommand.equals("start")) {
            this.plugin.game().forceStart();
            return true;
        }
        if (subcommand.equals("stop")) {
            this.plugin.game().stopGame();
            return true;
        }
        if (subcommand.equals("reset")) {
            this.plugin.game().reset();
            return true;
        }
        if (subcommand.equals("pause")) {
            this.plugin.game().pause();
            return true;
        }
        if (subcommand.equals("resume")) {
            this.plugin.game().resume();
            return true;
        }
        if (subcommand.equals("skipphase")) {
            Messages.send(sender, this.plugin.game().skipNextPhase() ? Messages.get("bed-wars-command.on-command.przeskoczono-do-nastepnej-fazy") : Messages.get("bed-wars-command.on-command.nie-mozna-teraz-przeskoczyc-fazy"));
            return true;
        }
        if (subcommand.equals("spectator") && args.length >= 2) {
            final Player p = Bukkit.getPlayer(args[1]);
            if (p == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.gracz-jest-offline"));
            } else {
                this.plugin.game().makeSpectator(p);
                Messages.send(sender, Messages.get("bed-wars-command.on-command.ustawiono-gracza-jako-obserwatora", "PLAYER", p.getName()));
            }
            return true;
        }
        if (subcommand.equals("revive") && args.length >= 2) {
            final Player p = Bukkit.getPlayer(args[1]);
            if (p == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.gracz-jest-offline"));
            } else {
                Messages.send(sender, this.plugin.game().revive(p) ? Messages.get("bed-wars-command.on-command.przywrocono-gracza", "PLAYER",
                        p.getName()) : Messages.get("bed-wars-command.on-command.mozna-wskrzesic-tylko-wyeliminowanego-lub-odradzajacego"));
            }
            return true;
        }
        if (subcommand.equals("spawnshop") && sender instanceof Player && args.length >= 2) {
            final Player p = (Player) sender;
            final Arena ar = this.plugin.game().active();
            if (ar == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.brak-aktywnej-mapy"));
                return true;
            }
            final String typ = args[1].toLowerCase();
            if (typ.equals("przedmioty") || typ.equals("item")) {
                ar.getItemShops().add(p.getLocation());
            } else if (typ.equals("ulepszenia") || typ.equals("upgrades")) {
                ar.getUpgradeShops().add(p.getLocation());
            } else {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.bw-ustawsklep-przedmioty-ulepszenia"));
                return true;
            }
            this.plugin.arenaStorage().saveArena(ar);
            this.plugin.npcs().respawn();
            Messages.send(sender, Messages.get("bed-wars-command.on-command.dodano-npc-sklepu"));
            return true;
        }
        if (subcommand.equals("addgen") && sender instanceof Player && args.length >= 2) {
            final Player p = (Player) sender;
            final Arena ar = this.plugin.game().active();
            if (ar == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.brak-aktywnej-mapy"));
                return true;
            }
            final GeneratorType type = parseGenerator(args[1]);
            if (type == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.bw-dodajgenerator-diamenty-szmaragdy"));
                return true;
            }
            ar.getGenerators().get(type).add(p.getLocation());
            this.plugin.arenaStorage().saveArena(ar);
            Messages.send(sender, Messages.get("bed-wars-command.on-command.dodano-generator", "VALUE1", (type == GeneratorType.DIAMOND ? Messages.get("currency.diamond") : Messages.get("currency.emerald"))));
            return true;
        }
        if (subcommand.equals("setspawn") && sender instanceof Player && args.length >= 2) {
            final TeamColor tc = parseTeam(args[1]);
            if (tc == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.bw-ustawspawn-czerwoni-zieloni-niebiescy-biali"));
                return true;
            }
            if (this.plugin.game().active() == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.brak-aktywnej-mapy"));
                return true;
            }
            this.plugin.game().active().team(tc).setSpawn(((Player) sender).getLocation());
            this.plugin.arenaStorage().saveArena(this.plugin.game().active());
            Messages.send(sender, Messages.get("bed-wars-command.on-command.ustawiono-spawn-druzyny", "VALUE1", tc.chat(), "TEAM", tc.display()));
            return true;
        }
        if (subcommand.equals("setbed") && sender instanceof Player && args.length >= 2) {
            final TeamColor tc = parseTeam(args[1]);
            if (tc == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.bw-ustawlozko-czerwoni-zieloni-niebiescy-biali"));
                return true;
            }
            if (this.plugin.game().active() == null) {
                Messages.send(sender, Messages.get("bed-wars-command.on-command.brak-aktywnej-mapy"));
                return true;
            }
            this.plugin.game().active().team(tc).setBed(((Player) sender).getLocation().getBlock().getLocation());
            this.plugin.arenaStorage().saveArena(this.plugin.game().active());
            Messages.send(sender, Messages.get("bed-wars-command.on-command.ustawiono-lozko-druzyny", "VALUE1", tc.chat(), "TEAM", tc.display()));
            return true;
        }
        if (subcommand.equals("reload")) {
            new ReloadCommand(this.plugin).execute(sender);
            return true;
        }
        if (subcommand.equals("status")) {
            new StatusCommand(this.plugin).execute(sender);
            return true;
        }
        help(sender);
        return true;
    }

    private String normalize(final String raw) {
        final String x = raw.toLowerCase();
        if (x.equals("dolacz")) {
            return "join";
        }
        if (x.equals("opusc")) {
            return "leave";
        }
        if (x.equals("druzyna")) {
            return "team";
        }
        if (x.equals("obserwuj")) {
            return "observe";
        }
        if (x.equals("ustawlobby")) {
            return "setlobby";
        }
        if (x.equals("utworz")) {
            return "create";
        }
        if (x.equals("usun")) {
            return "delete";
        }
        if (x.equals("wybierz")) {
            return "select";
        }
        if (x.equals("lista")) {
            return "list";
        }
        if (x.equals("sprawdz")) {
            return "validate";
        }
        if (x.equals("ustawdruzyne")) {
            return "setteam";
        }
        if (x.equals("druzyny")) {
            return "teams";
        }
        if (x.equals("wymusstart")) {
            return "forcestart";
        }
        if (x.equals("zatrzymaj")) {
            return "stop";
        }
        if (x.equals("pauza")) {
            return "pause";
        }
        if (x.equals("wznow")) {
            return "resume";
        }
        if (x.equals("nastepnafaza")) {
            return "skipphase";
        }
        if (x.equals("obserwator")) {
            return "spectator";
        }
        if (x.equals("wskrzes")) {
            return "revive";
        }
        if (x.equals("ustawsklep")) {
            return "spawnshop";
        }
        if (x.equals("dodajgenerator")) {
            return "addgen";
        }
        if (x.equals("ustawspawn")) {
            return "setspawn";
        }
        if (x.equals("ustawlozko")) {
            return "setbed";
        }
        if (x.equals("przeladuj")) {
            return "reload";
        }
        return x;
    }

    private TeamColor parseTeam(final String raw) {
        final String x = raw.toLowerCase();
        if (x.equals("red") || x.equals("czerwoni") || x.equals("czerwony")) {
            return TeamColor.RED;
        }
        if (x.equals("green") || x.equals("zieloni") || x.equals("zielony")) {
            return TeamColor.GREEN;
        }
        if (x.equals("blue") || x.equals("niebiescy") || x.equals("niebieski")) {
            return TeamColor.BLUE;
        }
        if (x.equals("white") || x.equals("biali") || x.equals("bialy")) {
            return TeamColor.WHITE;
        }
        return null;
    }

    private GeneratorType parseGenerator(final String raw) {
        final String x = raw.toLowerCase();
        if (x.equals("diamond") || x.equals("diament") || x.equals("diamenty")) {
            return GeneratorType.DIAMOND;
        }
        if (x.equals("emerald") || x.equals("szmaragd") || x.equals("szmaragdy")) {
            return GeneratorType.EMERALD;
        }
        return null;
    }

    private String name(final UUID playerId) {
        final String n = Bukkit.getOfflinePlayer(playerId).getName();
        return n == null ? playerId.toString() : n;
    }

    private void help(final CommandSender sender) {
        Messages.send(sender, Messages.get("bed-wars-command.help.bedwars-komendy"));
        Messages.send(sender, Messages.get("bed-wars-command.help.bw-dolacz-opusc-druzyna"));
        if (sender.hasPermission("bedwars.admin")) {
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-ustawlobby-ustawia-glowne-lobby-serwera"));
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-obserwuj-dolacz-do-obserwacji-z"));
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-panel-panel-ingerencji-admina-obserwujacego"));
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-utworz-mapa-wybierz-mapa-lista"));
            Messages.send(sender, Messages.get("bed-wars-command.help.konfiguruj-nazwa-mapy-laduje-mape-i"));
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-ustawdruzyne-nick-czerwoni-zieloni-niebiescy"));
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-ustawspawn-druzyna-ustawlozko-druzyna-dodajgenerator"));
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-ustawsklep-przedmioty-ulepszenia-wymusstart-pauza"));
            Messages.send(sender, Messages.get("bed-wars-command.help.bw-zatrzymaj-reset-wskrzes-nick-obserwator"));
        }
    }

    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final List<String> out = new ArrayList<>();
        if (args.length == 1) {
            final String[] vals = sender.hasPermission("bedwars.admin") ? new String[] {"dolacz", "opusc", "druzyna", "obserwuj", "panel",
                    "ustawlobby", "utworz", "usun", "wybierz", "lista", "sprawdz", "ustawdruzyne", "druzyny", "wymusstart", "pauza", "wznow", "nastepnafaza",
                    "zatrzymaj", "reset", "obserwator", "wskrzes", "ustawsklep", "dodajgenerator", "ustawspawn", "ustawlozko", "status", "przeladuj"} : new String[] {"dolacz",
                    "opusc", "druzyna"};
            for (final String v : vals) {
                if (v.startsWith(args[0].toLowerCase())) {
                    out.add(v);
                }
            }
            return out;
        }
        final String subcommand = normalize(args[0]);
        if (args.length == 2 && (subcommand.equals("select") || subcommand.equals("delete"))) {
            for (final String n : this.plugin.game().arenas().keySet()) {
                if (n.startsWith(args[1].toLowerCase())) {
                    out.add(n);
                }
            }
        } else if (args.length == 2 && (subcommand.equals("setteam") || subcommand.equals("spectator") || subcommand.equals("revive"))) {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(player.getName());
                }
            }
        } else if ((args.length == 3 && subcommand.equals("setteam")) || (args.length == 2 && (subcommand.equals("setspawn") || subcommand.equals("setbed")))) {
            out.add("czerwoni");
            out.add("zieloni");
            out.add("niebiescy");
            out.add("biali");
        } else if (args.length == 2 && subcommand.equals("spawnshop")) {
            out.add("przedmioty");
            out.add("ulepszenia");
        } else if (args.length == 2 && subcommand.equals("addgen")) {
            out.add("diamenty");
            out.add("szmaragdy");
        }
        return out;
    }
}
