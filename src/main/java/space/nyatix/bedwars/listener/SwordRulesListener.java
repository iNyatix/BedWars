package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.game.GameManager;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.GameState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SwordRulesListener implements Listener {

    private final BedWarsPlugin plugin;

    private final Set<UUID> pending = new HashSet<>();

    private int task = -1;

    private boolean stopped;

    public SwordRulesListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (this.task != -1) {
            return;
        }
        this.stopped = false;
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                ensureSword(player);
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        this.stopped = true;
        if (this.task != -1) {
            Bukkit.getScheduler().cancelTask(this.task);
        }
        this.task = -1;
        this.pending.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drop(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        if (!eligible(player) || !isSword(event.getItemDrop().getItemStack())) {
            return;
        }
        if (isWood(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
        checkLater(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        if (!eligible(player)) {
            return;
        }
        if (!event.isCancelled() && movesWoodOutside(event, player)) {
            event.setCancelled(true);
        }
        checkLater(player);
    }

    private boolean movesWoodOutside(final InventoryClickEvent event, final Player player) {
        final InventoryAction action = event.getAction();
        final boolean own = event.getClickedInventory() instanceof PlayerInventory;
        if ((action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT) && own && isWood(event.getCurrentItem())) {
            return true;
        }
        if ((action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR) && isWood(event.getCursor())) {
            return true;
        }
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && own && isWood(event.getCurrentItem())) {
            final InventoryType top = event.getView().getTopInventory().getType();
            return top != InventoryType.CRAFTING && top != InventoryType.PLAYER;
        }
        if (!own && (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR) && isWood(event.getCursor())) {
            return true;
        }
        final int hotbar = event.getHotbarButton();
        return !own && (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) && hotbar >= 0 && hotbar < 9 && isWood(player.getInventory().getItem(hotbar));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        if (!eligible(player)) {
            return;
        }
        if (isWood(event.getOldCursor())) {
            final int topSize = event.getView().getTopInventory().getSize();
            for (final int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
        checkLater(player);
    }

    @EventHandler
    public void close(final InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            checkLater((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void pickup(final PlayerPickupItemEvent event) {
        if (!event.isCancelled() && isSword(event.getItem().getItemStack())) {
            checkLater(event.getPlayer());
        }
    }

    @EventHandler
    public void broken(final PlayerItemBreakEvent event) {
        if (isSword(event.getBrokenItem())) {
            checkLater(event.getPlayer());
        }
    }

    private void checkLater(final Player player) {
        if (!eligible(player)) {
            return;
        }
        final UUID playerId = player.getUniqueId();
        if (!this.pending.add(playerId)) {
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            this.pending.remove(playerId);
            ensureSword(Bukkit.getPlayer(playerId));
        });
    }

    private boolean eligible(final Player player) {
        if (this.stopped || player == null || !player.isOnline() || player.isDead()) {
            return false;
        }
        final GameManager game = this.plugin.game();
        if (game == null) {
            return false;
        }
        if (game.state() != GameState.RUNNING && (game.state() != GameState.PAUSED || game.isStarting())) {
            return false;
        }
        final UUID playerId = player.getUniqueId();
        return game.participants().contains(playerId) && game.teamOf(playerId) != null && !game.isSpectator(playerId) && !game.isRespawning(playerId) && !game.isFinalDead(playerId);
    }

    private void ensureSword(final Player player) {
        if (!eligible(player)) {
            return;
        }
        final PlayerInventory inventory = player.getInventory();
        final ItemStack cursor = player.getItemOnCursor();
        boolean sword = isSword(cursor), better = sword && !isWood(cursor);
        for (int slot = 0; slot < 36; slot++) {
            final ItemStack item = inventory.getItem(slot);
            if (isSword(item)) {
                sword = true;
                if (!isWood(item)) {
                    better = true;
                }
            }
        }
        if (better) {
            boolean changed = false;
            for (int slot = 0; slot < 36; slot++) {
                if (isWood(inventory.getItem(slot))) {
                    inventory.setItem(slot, null);
                    changed = true;
                }
            }
            if (isWood(cursor)) {
                player.setItemOnCursor(null);
                changed = true;
            }
            if (changed) {
                player.updateInventory();
            }
            return;
        }
        if (sword) {
            return;
        }
        int slot = inventory.getHeldItemSlot();
        if (!empty(inventory.getItem(slot))) {
            slot = -1;
            for (int i = 0; i < 36; i++) {
                if (empty(inventory.getItem(i))) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot == -1) {
            slot = 35;
            for (int i = 35; i >= 0; i--) {
                final Material material = inventory.getItem(i).getType();
                if (material != Material.SHEARS && !material.name().endsWith("_PICKAXE") && !material.name().endsWith("_AXE")) {
                    slot = i;
                    break;
                }
            }
            player.getWorld().dropItemNaturally(player.getLocation(), inventory.getItem(slot).clone());
            Messages.send(player, Messages.get("sword-rules.ensure-sword.brak-miejsca-jeden-przedmiot-wypadl-na"));
        }
        inventory.setItem(slot, new ItemStack(Material.WOOD_SWORD));
        this.plugin.game().applyTeamEnchantments(player);
        player.updateInventory();
    }

    private static boolean empty(final ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private static boolean isWood(final ItemStack item) {
        return !empty(item) && item.getType() == Material.WOOD_SWORD;
    }

    private static boolean isSword(final ItemStack item) {
        if (empty(item)) {
            return false;
        }
        final Material type = item.getType();
        return type == Material.WOOD_SWORD || type == Material.STONE_SWORD || type == Material.IRON_SWORD || type == Material.GOLD_SWORD || type == Material.DIAMOND_SWORD;
    }
}
