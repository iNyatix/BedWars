package space.nyatix.bedwars.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.GameState;

public class TournamentExtraRulesListener implements Listener {

    private final BedWarsPlugin plugin;

    public TournamentExtraRulesListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void pickup(final PlayerPickupItemEvent event) {
        if (this.plugin.game().isSpectator(event.getPlayer().getUniqueId()) && !this.plugin.game().canIntervene(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void drop(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        if (this.plugin.game().state() != GameState.RUNNING || this.plugin.game().isSpectator(player.getUniqueId())) {
            return;
        }
        final ItemStack stack = event.getItemDrop().getItemStack();
        if (stack == null || !resource(stack.getType())) {
            return;
        }
        if (overVoid(player)) {
            event.setCancelled(true);
            Messages.send(player, Messages.get("tournament-extra-rules.drop.nie-mozesz-wyrzucac-surowcow-podczas-spadania"));
        }
    }

    private boolean resource(final Material material) {
        return material == Material.IRON_INGOT || material == Material.GOLD_INGOT || material == Material.DIAMOND || material == Material.EMERALD;
    }

    private boolean overVoid(final Player player) {
        final Location location = player.getLocation();
        if (location == null || location.getWorld() == null) {
            return false;
        }
        final World world = location.getWorld();
        final int x = block(location, "getBlockX"), y = block(location, "getBlockY") - 1, z = block(location, "getBlockZ");
        final int min = Math.max(0, y - 16);
        try {
            for (int yy = y; yy >= min; yy--) {
                final Object b = world.getClass().getMethod("getBlockAt", int.class, int.class, int.class).invoke(world, x, yy, z);
                final Object type = b.getClass().getMethod("getType").invoke(b);
                if (type != Material.AIR) {
                    return false;
                }
            }
        } catch (final Throwable ignored) {
            return false;
        }
        return true;
    }

    private int block(final Location location, final String method) {
        try {
            return ((Number) location.getClass().getMethod(method).invoke(location)).intValue();
        } catch (final Throwable ignored) {
            return 0;
        }
    }
}
