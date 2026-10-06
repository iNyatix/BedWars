package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.gui.ConfigMenu;
import space.nyatix.bedwars.gui.MenuHolder;
import space.nyatix.bedwars.model.GameState;

public final class MenuListener implements Listener {

    private final BedWarsPlugin plugin;

    public MenuListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void click(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        final Inventory inventory = event.getInventory();
        final int slot = event.getRawSlot();
        final MenuHolder holder = MenuHolder.get(inventory);
        final boolean admin = this.plugin.adminMenu() != null && this.plugin.adminMenu().isMenu(inventory);
        final boolean teleporter = this.plugin.spectators().isTeleporter(inventory);
        if (holder != null || admin || teleporter) {
            event.setCancelled(true);
            if (slot < 0 || slot >= inventory.getSize() || holder != null && !holder.belongsTo(player)) {
                return;
            }
            final boolean shift = event.isShiftClick();
            final boolean left = event.isLeftClick();
            final ItemStack clicked = event.getCurrentItem();
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                    return;
                }
                if (admin) {
                    this.plugin.adminMenu().click(player, inventory, slot);
                } else if (teleporter) {
                    this.plugin.spectators().clickTeleporter(player, inventory, slot);
                } else {
                    this.handleMenu(player, holder, slot, shift, left, clicked);
                }
            });
            return;
        }
        final ItemStack current = event.getCurrentItem();
        if (this.plugin.game().isSpectator(player.getUniqueId()) && !this.plugin.game().canIntervene(player) || this.plugin.adminMenu() != null && this.plugin.adminMenu().isControlItem(current) || this.plugin.lobby().isJoinItem(current) || this.plugin.lobby().isLeaveItem(current)) {
            event.setCancelled(true);
        }
    }

    private void handleMenu(final Player player, final MenuHolder holder, final int slot, final boolean shift, final boolean left, final ItemStack clicked) {
        final String type = holder.getType();
        if (type.startsWith("config-")) {
            if (!player.hasPermission("bedwars.admin")) {
                player.closeInventory();
                return;
            }
            final ConfigMenu menu = this.plugin.configMenu();
            switch(type) {
                case "config-main":
                    menu.clickMain(player, slot, shift);
                    break;
                case "config-points":
                    menu.clickPoints(player, slot);
                    break;
                case "config-teams":
                    menu.clickTeams(player, slot);
                    break;
                case "config-team":
                    menu.clickTeam(player, slot);
                    break;
                case "config-generators":
                    menu.clickGenerators(player, slot, shift);
                    break;
                case "config-shops":
                    menu.clickShops(player, slot, shift);
                    break;
                case "config-settings":
                    menu.clickSettings(player, slot, left);
                    break;
                case "config-timers":
                    menu.clickTimer(player, slot, left, shift);
                    break;
                default:
                    break;
            }
            return;
        }
        if (this.plugin.game().state() != GameState.RUNNING || this.plugin.game().teamOf(player.getUniqueId()) == null || this.plugin.game().isSpectator(player.getUniqueId()) || this.plugin.game().isRespawning(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        if (type.equals("shop-items")) {
            this.plugin.shop().clickShop(player, slot, shift, clicked);
        } else if (type.equals("shop-upgrades")) {
            this.plugin.shop().clickUpgrade(player, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void drag(final InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player && this.plugin.game().isSpectator(event.getWhoClicked().getUniqueId()) && !this.plugin.game().canIntervene((Player) event.getWhoClicked())) {
            event.setCancelled(true);
            return;
        }
        final Inventory inventory = event.getInventory();
        if (inventory != null && (MenuHolder.get(inventory) != null || this.plugin.spectators().isTeleporter(inventory) || this.plugin.adminMenu() != null && this.plugin.adminMenu().isMenu(inventory))) {
            event.setCancelled(true);
        }
    }
}
