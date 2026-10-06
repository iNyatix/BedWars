package space.nyatix.bedwars.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.game.PartyManager;
import space.nyatix.bedwars.message.MatchViewText;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.util.ItemBuilder;

public class PartyColorMenu implements Listener {

    private final BedWarsPlugin plugin;

    public PartyColorMenu(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(final Player player) {
        final PartyManager.Party party = this.plugin.parties().ensureParty(player);
        final MenuHolder holder = new MenuHolder();
        final Inventory menu = Bukkit.createInventory(holder, 27, MatchViewText.limit(Messages.get("party-color-menu.open.party-wybor-koloru"), 32));
        holder.inventory = menu;
        final TeamColor selected = this.plugin.parties().preferredTeam(player.getUniqueId());
        final boolean leader = party.leader().equals(player.getUniqueId());
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, new ItemBuilder(Material.STAINED_GLASS_PANE).data((short) 7)
                    .name(Messages.get("party-color-menu.open.text"))
                    .appearance(this.plugin, "menus.icons.party-color-menu.open.text")
                    .build());
        }
        int slot = 10;
        for (final TeamColor color : TeamColor.values()) {
            final int used = this.plugin.game().active() == null ? 0 : this.plugin.game().active().team(color).getPlayers().size();
            menu.setItem(slot, new ItemBuilder(Material.WOOL).data(color.dye().getWoolData())
                    .name(Messages.get("party-color-menu.open.text-2", "VALUE1", color.chat(), "TEAM", color.display()))
                    .lore(selected == color ? Messages.get("party-color-menu.open.wybrany-kolor-party") : Messages.get("party-color-menu.open.kliknij-aby-wybrac-ten-kolor"), Messages.get("party-color-menu.open.zajete-miejsca", "USED", used, "VALUE2", this.plugin.settings().playersPerTeam()), Messages.get("party-color-menu.open.cale-party-otrzyma-ten-kolor"), Messages.get("party-color-menu.open.takze-w-kolejnych-meczach"), Messages.get("party-color-menu.open.pelna-druzyna-brak-dolaczenia"), leader ? Messages.get("party-color-menu.open.kliknij-aby-zatwierdzic") : Messages.get("party-color-menu.open.kolor-wybiera-lider-party"))
                    .appearance(this.plugin, "menus.icons.party-color-menu.open.text-2")
                    .build());
            slot += 2;
        }
        menu.setItem(22, new ItemBuilder(Material.COMPASS)
                .name(Messages.get("party-color-menu.open.automatyczny-kolor"))
                .lore(selected == null ? Messages.get("party-color-menu.open.aktywny") : Messages.get("party-color-menu.open.usun-staly-wybor-koloru"), Messages.get("party-color-menu.open.party-trafi-razem-do-wolnej-druzyny"))
                .appearance(this.plugin, "menus.icons.party-color-menu.open.automatyczny-kolor")
                .build());
        player.openInventory(menu);
    }

    @EventHandler
    public void click(final InventoryClickEvent event) {
        final Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final int slot = event.getRawSlot();
        TeamColor choice = null;
        if (slot == 10 || slot == 12 || slot == 14 || slot == 16) {
            choice = TeamColor.values()[(slot - 10) / 2];
        } else if (slot != 22) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        final String error = this.plugin.parties().selectColor(player, choice);
        if (error != null) {
            Messages.send(player, Messages.get("party-color-menu.click.text", "ERROR", error));
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory() == top) {
                player.closeInventory();
            }
        });
    }

    @EventHandler
    public void drag(final InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private static class MenuHolder implements InventoryHolder {

        private Inventory inventory;

        public Inventory getInventory() {
            return this.inventory;
        }
    }
}
