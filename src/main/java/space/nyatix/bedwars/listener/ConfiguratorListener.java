package space.nyatix.bedwars.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;

public class ConfiguratorListener implements Listener {

    private final BedWarsPlugin plugin;

    public ConfiguratorListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void interact(final PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        if (!this.plugin.configMenu().isConfiguratorTool(item)) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            this.plugin.configMenu().useTool(event.getPlayer(), event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void breakBlock(final BlockBreakEvent event) {
        if (this.plugin.configMenu().isConfiguratorTool(event.getPlayer().getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void drop(final PlayerDropItemEvent event) {
        if (this.plugin.configMenu().isConfiguratorTool(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void inventory(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (this.plugin.configMenu().isConfiguratorTool(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void drag(final InventoryDragEvent event) {
        if (this.plugin.configMenu().isConfiguratorTool(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }
}
