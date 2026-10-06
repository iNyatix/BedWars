package space.nyatix.bedwars.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.game.GameManager;
import space.nyatix.bedwars.model.GameState;

import java.util.UUID;

public class RegenerationListener implements Listener {

    private final BedWarsPlugin plugin;

    public RegenerationListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void regenerate(final EntityRegainHealthEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        final EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason != EntityRegainHealthEvent.RegainReason.SATIATED && reason != EntityRegainHealthEvent.RegainReason.MAGIC_REGEN && reason != EntityRegainHealthEvent.RegainReason.REGEN) {
            return;
        }
        final GameManager game = this.plugin.game();
        final UUID playerId = event.getEntity().getUniqueId();
        if (game == null || !game.participants().contains(playerId) || game.teamOf(playerId) == null) {
            return;
        }
        if (game.state() != GameState.RUNNING && game.state() != GameState.PAUSED) {
            return;
        }
        if (game.state() == GameState.PAUSED || game.isSpectator(playerId) || game.isFinalDead(playerId) || game.isRespawning(playerId)) {
            event.setCancelled(true);
            return;
        }
        event.setAmount(event.getAmount() * this.plugin.settings().regenerationMultiplier());
    }
}
