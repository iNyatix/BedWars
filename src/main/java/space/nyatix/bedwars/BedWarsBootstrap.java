package space.nyatix.bedwars;

import org.bukkit.Bukkit;
import space.nyatix.bedwars.command.PartyCommand;
import space.nyatix.bedwars.game.WorldBorderManager;
import space.nyatix.bedwars.gui.PartyColorMenu;
import space.nyatix.bedwars.listener.InvisibilityRulesListener;
import space.nyatix.bedwars.listener.TournamentExtraRulesListener;
import space.nyatix.bedwars.listener.TournamentRulesListener;
import space.nyatix.bedwars.listener.WorldBorderConfigListener;
import space.nyatix.bedwars.message.Messages;

public class BedWarsBootstrap extends BedWarsPlugin {

    @Override
    public void onEnable() {
        super.onEnable();
        if (!isEnabled()) {
            return;
        }
        final PartyColorMenu colors = new PartyColorMenu(this);
        final PartyCommand partyCommand = new PartyCommand(parties(), colors);
        getCommand("party").setExecutor(partyCommand);
        getCommand("party").setTabCompleter(partyCommand);
        final WorldBorderManager borders = borders();
        Bukkit.getPluginManager().registerEvents(new InvisibilityRulesListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TournamentRulesListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TournamentExtraRulesListener(this), this);
        Bukkit.getPluginManager().registerEvents(colors, this);
        Bukkit.getPluginManager().registerEvents(new WorldBorderConfigListener(this, borders), this);
        getLogger().info(Messages.get("bed-wars-bootstrap.on-enable.bedwars-party-z-wyborem-koloru-efekty"));
    }
}
