package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.model.GameState;

public class EquipmentRulesListener implements Listener {

    private final BedWarsPlugin plugin;

    public EquipmentRulesListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void click(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        if (!locked(player)) {
            return;
        }
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            return;
        }
        final ItemStack current = event.getCurrentItem();
        if (event.isShiftClick() && isArmor(current)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void itemDamage(final PlayerItemDamageEvent event) {
        final Player player = event.getPlayer();
        if (!locked(player)) {
            return;
        }
        event.setCancelled(true);
        event.setDamage(0);
        final ItemStack item = event.getItem();
        if (item != null && item.getDurability() != 0) {
            item.setDurability((short) 0);
        }
        Bukkit.getScheduler().runTask(this.plugin, player::updateInventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void prepareCraft(final PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getView().getPlayer();
        if (!locked(player)) {
            return;
        }
        event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void drag(final InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player && locked((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    private boolean locked(final Player player) {
        return player != null && this.plugin.game() != null && this.plugin.game().state() == GameState.RUNNING && this.plugin.game().teamOf(player.getUniqueId()) != null && !this.plugin.game().isSpectator(player.getUniqueId());
    }

    private boolean isArmor(final ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        final String n = item.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }
}
