package space.nyatix.bedwars.command.subcommand;

import org.bukkit.command.CommandSender;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;

public final class ReloadCommand {

    private final BedWarsPlugin plugin;

    public ReloadCommand(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(final CommandSender sender) {
        try {
            this.plugin.reloadFiles();
            Messages.send(sender, Messages.get("configuration.reloaded"));
        } catch (Exception exception) {
            Messages.send(sender, Messages.get("configuration.reload-failed", "ERROR", exception.getMessage()));
        }
    }
}
