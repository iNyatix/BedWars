package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.GameState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class TournamentRulesListener implements Listener {

    private final BedWarsPlugin plugin;

    private final Map<UUID, Long> lastHeightWarning = new HashMap<>();

    public TournamentRulesListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickHeightLimit, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void death(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        clearCursor(player);
        if (player != null) {
            this.lastHeightWarning.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void respawn(final PlayerRespawnEvent event) {
        clearCursor(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void quit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (player != null && this.plugin.game() != null && this.plugin.game().state() == GameState.RUNNING && this.plugin.game().teamOf(player.getUniqueId()) != null) {
            clearCursor(player);
        }
        if (player != null) {
            this.lastHeightWarning.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void achievement(final PlayerAchievementAwardedEvent event) {
        event.setCancelled(true);
    }

    private void tickHeightLimit() {
        if (!Options.enabled(this.plugin, "heightPenalty.enabled", true) || this.plugin.game() == null || this.plugin.game().state() != GameState.RUNNING) {
            this.lastHeightWarning.clear();
            return;
        }
        for (final UUID playerId : new HashSet<>(this.plugin.game().participants())) {
            final Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || this.plugin.game().teamOf(playerId) == null || this.plugin.game().isSpectator(playerId) || this.plugin.game().isFinalDead(playerId) || this.plugin.game().isRespawning(playerId)) {
                this.lastHeightWarning.remove(playerId);
                continue;
            }
            if (player.getLocation() == null || player.getLocation().getY() < Options.number(this.plugin, "heightPenalty.y", 105.0D, 0.0D, 256.0D)) {
                this.lastHeightWarning.remove(playerId);
                continue;
            }
            final long now = System.currentTimeMillis();
            final Long last = this.lastHeightWarning.get(playerId);
            if (last == null || now - last >= 1000L * Options.integer(this.plugin, "heightPenalty.warningSeconds", 3, 1, 120)) {
                Messages.send(player, Messages.get("height-penalty.warning", "Y", Options.integer(this.plugin, "heightPenalty.y", 105, 0,
                        256), "DAMAGE", Options.number(this.plugin, "heightPenalty.damage", 4.0D, 0.1D, 100.0D)));
                this.lastHeightWarning.put(playerId, now);
            }
            player.damage(Options.number(this.plugin, "heightPenalty.damage", 4.0D, 0.1D, 100.0D));
        }
    }

    private void clearCursor(final Player player) {
        if (player == null) {
            return;
        }
        try {
            player.setItemOnCursor(null);
        } catch (final Throwable ignored) {
        }
        try {
            player.closeInventory();
        } catch (final Throwable ignored) {
        }
        try {
            player.updateInventory();
        } catch (final Throwable ignored) {
        }
    }
}
