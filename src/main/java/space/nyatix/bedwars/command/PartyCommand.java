package space.nyatix.bedwars.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.game.PartyManager;
import space.nyatix.bedwars.gui.PartyColorMenu;
import space.nyatix.bedwars.message.Messages;

import java.util.ArrayList;
import java.util.List;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final PartyManager parties;

    private final PartyColorMenu colors;

    public PartyCommand(final PartyManager parties, final PartyColorMenu colors) {
        this.parties = parties;
        this.colors = colors;
    }

    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, Messages.get("party-command.on-command.ta-komenda-jest-tylko-dla-gracza"));
            return true;
        }
        final Player player = (Player) sender;
        if (args.length == 0) {
            help(player);
            return true;
        }
        final String subcommand = args[0].toLowerCase();
        if (subcommand.equals("kolor") || subcommand.equals("color")) {
            this.colors.open(player);
            return true;
        }
        if (subcommand.equals("zapros") || subcommand.equals("invite")) {
            if (args.length < 2) {
                Messages.send(player, Messages.get("party-command.on-command.party-zapros-nick"));
                return true;
            }
            final Player target = Bukkit.getPlayerExact(args[1]);
            final String result = this.parties.invite(player, target);
            if (result != null) {
                Messages.send(player, result);
            }
            return true;
        }
        if (subcommand.equals("akceptuj") || subcommand.equals("accept")) {
            final String result = this.parties.accept(player);
            if (result != null) {
                Messages.send(player, Messages.get("party-command.on-command.text", "RESULT", result));
            }
            return true;
        }
        if (subcommand.equals("opusc") || subcommand.equals("leave")) {
            final String result = this.parties.leave(player);
            if (result != null) {
                Messages.send(player, Messages.get("party-command.on-command.text-2", "RESULT", result));
            }
            return true;
        }
        if (subcommand.equals("wyrzuc") || subcommand.equals("kick")) {
            if (args.length < 2) {
                Messages.send(player, Messages.get("party-command.on-command.party-wyrzuc-nick"));
                return true;
            }
            final Player target = Bukkit.getPlayerExact(args[1]);
            final String result = this.parties.kick(player, target);
            if (result != null) {
                Messages.send(player, Messages.get("party-command.on-command.text", "RESULT", result));
            }
            return true;
        }
        if (subcommand.equals("rozwiaz") || subcommand.equals("disband")) {
            final String result = this.parties.disband(player);
            if (result != null) {
                Messages.send(player, Messages.get("party-command.on-command.text", "RESULT", result));
            }
            return true;
        }
        if (subcommand.equals("lista") || subcommand.equals("info")) {
            this.parties.sendInfo(player);
            return true;
        }
        help(player);
        return true;
    }

    private void help(final Player player) {
        Messages.send(player, Messages.get("party-command.help.text"));
        Messages.send(player, Messages.get("party-command.help.party-komendy"));
        Messages.send(player, Messages.get("party-command.help.party-zapros-nick-zapros-gracza"));
        Messages.send(player, Messages.get("party-command.help.party-akceptuj-przyjmij-zaproszenie"));
        Messages.send(player, Messages.get("party-command.help.party-lista-pokaz-party"));
        Messages.send(player, Messages.get("party-command.help.party-kolor-wybierz-staly-kolor-party"));
        Messages.send(player, Messages.get("party-command.help.party-wyrzuc-nick-lider-wyrzuca-gracza"));
        Messages.send(player, Messages.get("party-command.help.party-opusc-opusc-party"));
        Messages.send(player, Messages.get("party-command.help.party-rozwiaz-lider-rozwiazuje-party"));
        Messages.send(player, Messages.get("party-command.help.party-trafia-razem-do-tej-samej"));
        Messages.send(player, Messages.get("party-command.help.text"));
    }

    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (final String x : new String[] { "zapros", "akceptuj", "lista", "kolor", "wyrzuc", "opusc", "rozwiaz" }) {
                if (x.startsWith(args[0].toLowerCase())) {
                    out.add(x);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("zapros") || args[0].equalsIgnoreCase("wyrzuc"))) {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(player.getName());
                }
            }
        }
        return out;
    }
}
