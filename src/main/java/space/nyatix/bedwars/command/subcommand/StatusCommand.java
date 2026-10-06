package space.nyatix.bedwars.command.subcommand;

import org.bukkit.command.CommandSender;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.game.ArenaValidator;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;

public final class StatusCommand {

    private final BedWarsPlugin plugin;

    public StatusCommand(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(final CommandSender sender) {
        final Arena arena = this.plugin.game().active();
        final boolean ready = arena != null && ArenaValidator.validate(arena).isEmpty();
        final boolean swm = this.plugin.worlds() != null && this.plugin.worlds().available();
        final boolean tpl = arena != null && swm && this.plugin.worlds().hasTemplate(arena);
        final boolean runtime = arena != null && swm && this.plugin.worlds().isRuntimeReady(arena);
        final boolean preparing = arena != null && swm && this.plugin.worlds().isRuntimePreparing(arena);
        Messages.send(sender, Messages.get("bed-wars-command.on-command.bedwars-stan-systemu"));
        Messages.send(sender, Messages.get("bed-wars-command.on-command.stan-meczu-gracze", "VALUE1", stateName(this.plugin.game().state()),
                "VALUE2", this.plugin.game().matchPlayerCount()));
        Messages.send(sender, Messages.get("bed-wars-command.on-command.mapa-konfiguracja", "PLAYER", (arena == null ? Messages.get("bed-wars-command.on-command.brak-3") : arena.getName()),
                "VALUE2", (ready ? Messages.get("bed-wars-command.on-command.kompletna") : Messages.get("bed-wars-command.on-command.niekompletna"))));
        Messages.send(sender, Messages.get("bed-wars-command.on-command.cswm-szablon-runtime", "VALUE1", (swm ? this.plugin.worlds().providerName() + " " + this.plugin.worlds().providerVersion() : Messages.get("bed-wars-command.on-command.brak-4")),
                "VALUE2", (tpl ? Messages.get("bed-wars-command.on-command.tak") : Messages.get("bed-wars-command.on-command.nie")), "VALUE3", (runtime ? Messages.get("bed-wars-command.on-command.zaladowany") : preparing ? Messages.get("bed-wars-command.on-command.ladowanie") : Messages.get("bed-wars-command.on-command.brak-3"))));
        if (this.plugin.diagnostics() != null) {
            this.plugin.diagnostics().send(sender);
        }
    }

    private String stateName(final GameState gameState) {
        if (gameState == GameState.WAITING) {
            return Messages.get("states.waiting");
        }
        if (gameState == GameState.COUNTDOWN) {
            return Messages.get("states.countdown");
        }
        if (gameState == GameState.RUNNING) {
            return Messages.get("states.running");
        }
        if (gameState == GameState.PAUSED) {
            return Messages.get("states.paused");
        }
        return Messages.get("states.ended");
    }
}
