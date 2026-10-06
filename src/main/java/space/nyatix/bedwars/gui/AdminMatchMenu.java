package space.nyatix.bedwars.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.MatchViewText;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.util.ItemBuilder;
import space.nyatix.bedwars.util.ItemTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminMatchMenu {

    private final BedWarsPlugin plugin;

    public AdminMatchMenu(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack controlItem() {
        return new ItemBuilder(Material.NETHER_STAR)
                .tag("admin-panel")
                .name(Messages.get("admin-match-menu.control-item.panel-admina-ppm"))
                .lore(Messages.get("admin-match-menu.control-item.ingerencja-pauza-fazy-i-wskrzeszanie"))
                .appearance(this.plugin, "menus.icons.admin-match-menu.control-item.panel-admina-ppm")
                .build();
    }

    public boolean isControlItem(final ItemStack item) {
        return ItemTag.is(item, "admin-panel") || item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && "Panel admina (PPM)".equals(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
    }

    public boolean allowed(final Player player) {
        return this.plugin.spectators().canManageMatch(player);
    }

    public void open(final Player player) {
        if (!allowed(player)) {
            Messages.send(player, Messages.get("admin-match-menu.open.panel-jest-dostepny-dla-administratora-obserwujacego"));
            return;
        }
        final Holder holder = new Holder(player.getUniqueId(), false, 0);
        final Inventory menu = Bukkit.createInventory(holder, 27, MatchViewText.limit(Messages.get("admin-match-menu.open.panel-admina-mecz"), 32));
        holder.inventory = menu;
        final boolean editing = this.plugin.spectators().isIntervening(player), paused = this.plugin.game().isPaused();
        menu.setItem(10, new ItemBuilder(editing ? Material.REDSTONE_BLOCK : Material.DIAMOND_PICKAXE)
                .name(editing ? Messages.get("admin-match-menu.open.wylacz-ingerencje") : Messages.get("admin-match-menu.open.wlacz-ingerencje"))
                .lore(Messages.get("admin-match-menu.open.creative-budowanie-skrzynie-i-przedmioty"), Messages.get("admin-match-menu.open.w-tym-trybie-jestes-widoczny"), Messages.get("admin-match-menu.open.nie-jestes-zawodnikiem-pvp-zablokowane"), Messages.get("admin-match-menu.open.kliknij-aby-przelaczyc-tryb"))
                .build());
        menu.setItem(12, new ItemBuilder(paused ? Material.EMERALD_BLOCK : Material.WATCH)
                .name(paused ? Messages.get("admin-match-menu.open.wznow-mecz") : Messages.get("admin-match-menu.open.wstrzymaj-mecz"))
                .lore(Messages.get("admin-match-menu.open.pauza-zatrzymuje-czas-i-border"))
                .build());
        menu.setItem(14, new ItemBuilder(Material.REDSTONE_TORCH_ON)
                .name(Messages.get("admin-match-menu.open.nastepna-faza"))
                .lore(Messages.get("admin-match-menu.open.text", "VALUE1", this.plugin.game().nextEventText()), paused ? Messages.get("admin-match-menu.open.najpierw-wznow-mecz") : Messages.get("admin-match-menu.open.kliknij-przejdz-do-kolejnej-fazy"))
                .appearance(this.plugin, "menus.icons.admin-match-menu.open.nastepna-faza")
                .build());
        menu.setItem(16, new ItemBuilder(Material.GOLDEN_APPLE)
                .name(Messages.get("admin-match-menu.open.wskrzes-zawodnika"))
                .lore(Messages.get("admin-match-menu.open.lista-wyeliminowanych-i-odradzajacych-sie"), Messages.get("admin-match-menu.open.dziala-takze-w-pauzie"))
                .appearance(this.plugin, "menus.icons.admin-match-menu.open.wskrzes-zawodnika")
                .build());
        menu.setItem(22, new ItemBuilder(Material.PAPER)
                .name(Messages.get("admin-match-menu.open.wydajnosc-serwera"))
                .lore(Messages.get("admin-match-menu.open.tps-cpu-ram-i-swiaty-na"))
                .appearance(this.plugin, "menus.icons.admin-match-menu.open.wydajnosc-serwera")
                .build());
        player.openInventory(menu);
    }

    public boolean isMenu(final Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof Holder;
    }

    public void click(final Player player, final Inventory menu, final int slot) {
        if (!isMenu(menu) || !allowed(player) || slot < 0 || slot >= menu.getSize()) {
            return;
        }
        final Holder holder = (Holder) menu.getHolder();
        if (!holder.owner.equals(player.getUniqueId())) {
            return;
        }
        if (holder.revive) {
            final UUID playerId = holder.players.get(slot);
            if (playerId != null) {
                final Player target = Bukkit.getPlayer(playerId);
                if (this.plugin.game().revive(target)) {
                    announce(player, Messages.get("admin-match-menu.click.wskrzesil-zawodnika", "PLAYER", target.getName()));
                } else {
                    Messages.send(player, Messages.get("admin-match-menu.click.ten-zawodnik-nie-jest-juz-dostepny"));
                }
                openRevive(player, holder.page);
                return;
            }
            if (slot == 48 && menu.getItem(slot) != null) {
                openRevive(player, holder.page - 1);
            } else if (slot == 50 && menu.getItem(slot) != null) {
                openRevive(player, holder.page + 1);
            } else if (slot == 49) {
                openRevive(player, holder.page);
            } else if (slot == 53) {
                open(player);
            }
            return;
        }
        if (slot == 10) {
            player.closeInventory();
            this.plugin.spectators().toggleIntervention(player);
        } else if (slot == 12) {
            if (this.plugin.game().isPaused()) {
                this.plugin.game().resume();
                announce(player, Messages.get("admin-match-menu.click.wznowil-mecz"));
            } else {
                this.plugin.game().pause();
                announce(player, Messages.get("admin-match-menu.click.wstrzymal-mecz"));
            }
            open(player);
        } else if (slot == 14) {
            if (this.plugin.game().skipNextPhase()) {
                announce(player, Messages.get("admin-match-menu.click.przeskoczyl-do-nastepnej-fazy"));
            } else {
                Messages.send(player, Messages.get("admin-match-menu.click.nie-mozna-teraz-przeskoczyc-fazy"));
            }
            open(player);
        } else if (slot == 16) {
            openRevive(player, 0);
        } else if (slot == 22) {
            player.closeInventory();
            if (this.plugin.diagnostics() != null) {
                this.plugin.diagnostics().send(player);
            }
        }
    }

    private void openRevive(final Player admin, final int requestedPage) {
        if (!allowed(admin)) {
            return;
        }
        final List<Player> players = new ArrayList<>();
        for (final UUID playerId : this.plugin.game().participants()) {
            final Player p = Bukkit.getPlayer(playerId);
            if (this.plugin.game().canRevive(p)) {
                players.add(p);
            }
        }
        players.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        final int pages = Math.max(1, (players.size() + 44) / 45), page = Math.max(0, Math.min(requestedPage, pages - 1));
        final Holder holder = new Holder(admin.getUniqueId(), true, page);
        final Inventory menu = Bukkit.createInventory(holder, 54, MatchViewText.limit(Messages.get("admin-match-menu.open-revive.wskrzeszanie-zawodnika"), 32));
        holder.inventory = menu;
        for (int n = page * 45; n < Math.min(players.size(), (page + 1) * 45); n++) {
            final Player p = players.get(n);
            final TeamColor team = this.plugin.game().teamOf(p.getUniqueId());
            final int slot = n - page * 45;
            holder.players.put(slot, p.getUniqueId());
            menu.setItem(slot, new ItemBuilder(Material.SKULL_ITEM).data((short) 3)
                    .name(team.chat() + p.getName())
                    .lore(Messages.get("admin-match-menu.open-revive.druzyna", "VALUE1", team.chat(), "TEAM", team.display()), Messages.get("admin-match-menu.open-revive.kliknij-aby-przywrocic-do-gry"), Messages.get("admin-match-menu.open-revive.lozko-nie-zostanie-odbudowane"))
                    .build());
        }
        if (players.isEmpty()) {
            menu.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(Messages.get("admin-match-menu.open-revive.brak-zawodnikow-do-wskrzeszenia"))
                    .appearance(this.plugin, "menus.icons.admin-match-menu.open-revive.brak-zawodnikow-do-wskrzeszenia")
                    .build());
        }
        if (page > 0) {
            menu.setItem(48, new ItemBuilder(Material.ARROW)
                    .name(Messages.get("admin-match-menu.open-revive.poprzednia-strona"))
                    .appearance(this.plugin, "menus.icons.admin-match-menu.open-revive.poprzednia-strona")
                    .build());
        }
        menu.setItem(49, new ItemBuilder(Material.WATCH)
                .name(Messages.get("admin-match-menu.open-revive.odswiez"))
                .lore(Messages.get("admin-match-menu.open-revive.strona", "VALUE1", (page + 1), "PAGES", pages))
                .appearance(this.plugin, "menus.icons.admin-match-menu.open-revive.odswiez")
                .build());
        if (page + 1 < pages) {
            menu.setItem(50, new ItemBuilder(Material.ARROW)
                    .name(Messages.get("admin-match-menu.open-revive.nastepna-strona"))
                    .appearance(this.plugin, "menus.icons.admin-match-menu.open-revive.nastepna-strona")
                    .build());
        }
        menu.setItem(53, new ItemBuilder(Material.ARROW)
                .name(Messages.get("admin-match-menu.open-revive.wroc-do-panelu"))
                .appearance(this.plugin, "menus.icons.admin-match-menu.open-revive.wroc-do-panelu")
                .build());
        admin.openInventory(menu);
    }

    private void announce(final Player player, final String action) {
        this.plugin.game().broadcast(Messages.get("admin-match-menu.announce.admin", "PLAYER", player.getName(), "ACTION", action));
    }

    private static final class Holder implements InventoryHolder {

        final UUID owner;

        final boolean revive;

        final int page;

        final Map<Integer, UUID> players = new HashMap<>();

        Inventory inventory;

        Holder(final UUID owner, final boolean revive, final int page) {
            this.owner = owner;
            this.revive = revive;
            this.page = page;
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }
}
