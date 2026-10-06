package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.game.WorldBorderManager;
import space.nyatix.bedwars.gui.MenuHolder;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.util.ItemBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WorldBorderConfigListener implements Listener {

    private final BedWarsPlugin plugin;

    private final WorldBorderManager borders;

    private final Set<UUID> waitingRadius = new HashSet<>();

    public WorldBorderConfigListener(final BedWarsPlugin plugin, final WorldBorderManager borders) {
        this.plugin = plugin;
        this.borders = borders;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void open(final InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getPlayer();
        decorate(player, event.getInventory());
    }

    private void decorate(final Player player, final Inventory inventory) {
        if (inventory == null) {
            return;
        }
        final MenuHolder holder = MenuHolder.get(inventory);
        if (holder == null || !holder.belongsTo(player) || !player.hasPermission("bedwars.admin")) {
            return;
        }
        final String title = holder.getType();
        final Arena arena = this.plugin.configMenu().arena(player);
        if (arena == null) {
            return;
        }
        if (title.equals("config-points")) {
            final boolean centerSet = this.borders.hasCenter(arena);
            final boolean configured = this.borders.isConfigured(arena);
            inventory.setItem(11, new ItemBuilder(centerSet ? Material.COMPASS : Material.REDSTONE)
                    .name(Messages.get("world-border-config.decorate.srodek-worldbordera"))
                    .lore(centerSet ? Messages.get("world-border-config.decorate.ustawiono-x-z", "VALUE1", fmt(this.borders.centerX(arena)), "VALUE2", fmt(this.borders.centerZ(arena))) : Messages.get("world-border-config.decorate.nie-ustawiono"), Messages.get("world-border-config.decorate.text"), Messages.get("world-border-config.decorate.stan-na-srodku-mapy"), Messages.get("world-border-config.decorate.kliknij-ustaw-srodek-tutaj"))
                    .build());
            inventory.setItem(15, new ItemBuilder(configured ? Material.EMERALD : Material.REDSTONE)
                    .name(Messages.get("world-border-config.decorate.zasieg-worldbordera"))
                    .lore(configured ? Messages.get("world-border-config.decorate.promien-blokow", "VALUE1", fmt(this.borders.radius(arena))) : Messages.get("world-border-config.decorate.nie-ustawiono"), Messages.get("world-border-config.decorate.promien-to-odleglosc-od-srodka-do"), Messages.get("world-border-config.decorate.np-120-daje-border-240x240"), Messages.get("world-border-config.decorate.text"), Messages.get("world-border-config.decorate.kliknij-i-wpisz-liczbe-na-czacie"))
                    .build());
        } else if (title.equals("config-main")) {
            final boolean centerSet = this.borders.hasCenter(arena);
            final boolean configured = this.borders.isConfigured(arena);
            final double radius = this.borders.radius(arena);
            inventory.setItem(35, new ItemBuilder(configured ? Material.EMERALD : Material.REDSTONE)
                    .name(configured ? Messages.get("world-border-config.decorate.worldborder-gotowy") : Messages.get("world-border-config.decorate.worldborder-niekompletny"))
                    .lore(centerSet ? Messages.get("world-border-config.decorate.srodek", "VALUE1", fmt(this.borders.centerX(arena)), "VALUE2", fmt(this.borders.centerZ(arena))) : Messages.get("world-border-config.decorate.srodek-nie-ustawiono"), radius > 0.0D ? Messages.get("world-border-config.decorate.promien-blokow", "VALUE1", fmt(radius)) : Messages.get("world-border-config.decorate.promien-nie-ustawiono"), Messages.get("world-border-config.decorate.text"), Messages.get("world-border-config.decorate.kliknij-otworz-punkty-glowne"))
                    .build());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void click(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getInventory() == null) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        final MenuHolder holder = MenuHolder.get(event.getInventory());
        if (holder == null || !holder.belongsTo(player) || !player.hasPermission("bedwars.admin")) {
            return;
        }
        final String title = holder.getType();
        final Arena arena = this.plugin.configMenu().arena(player);
        if (arena == null) {
            return;
        }
        final int slot = event.getRawSlot();
        if (title.equals("config-main") && slot == 35) {
            event.setCancelled(true);
            this.plugin.configMenu().openPoints(player);
            return;
        }
        if (!title.equals("config-points")) {
            return;
        }
        if (slot == 11) {
            event.setCancelled(true);
            this.borders.setCenter(arena, player.getLocation());
            Messages.send(player, Messages.get("world-border-config.click.worldborder-ustawiono-srodek-na-x-z", "VALUE1", fmt(player.getLocation().getX()),
                    "VALUE2", fmt(player.getLocation().getZ())));
            this.plugin.configMenu().openPoints(player);
            return;
        }
        if (slot == 15) {
            event.setCancelled(true);
            this.waitingRadius.add(player.getUniqueId());
            player.closeInventory();
            Messages.send(player, Messages.get("world-border-config.click.text"));
            Messages.send(player, Messages.get("world-border-config.click.worldborder-wpisz-na-czacie-promien-w"));
            Messages.send(player, Messages.get("world-border-config.click.np-120-border-240x240-wpisz-anuluj"));
            Messages.send(player, Messages.get("world-border-config.click.text"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void chat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (!this.waitingRadius.contains(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        final String msg = event.getMessage();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            handleRadius(player, msg);
        });
    }

    private void handleRadius(final Player player, final String msg) {
        if (!this.waitingRadius.remove(player.getUniqueId())) {
            return;
        }
        final Arena arena = this.plugin.configMenu().arena(player);
        if (arena == null) {
            return;
        }
        if (msg == null || msg.equalsIgnoreCase("anuluj") || msg.equalsIgnoreCase("cancel")) {
            Messages.send(player, Messages.get("world-border-config.handle-radius.anulowano-ustawianie-worldbordera"));
            this.plugin.configMenu().openPoints(player);
            return;
        }
        try {
            final double radius = Double.parseDouble(msg.replace(',', '.'));
            if (radius < 10.0D || radius > 5000.0D) {
                Messages.send(player, Messages.get("world-border-config.handle-radius.podaj-liczbe-od-10-do-5000"));
                this.waitingRadius.add(player.getUniqueId());
                return;
            }
            this.borders.setRadius(arena, radius);
            Messages.send(player, Messages.get("world-border-config.handle-radius.worldborder-ustawiono-promien-na-blokow", "VALUE1", fmt(radius)));
            this.plugin.configMenu().openPoints(player);
        } catch (final Exception ex) {
            Messages.send(player, Messages.get("world-border-config.handle-radius.to-nie-jest-liczba-wpisz-np"));
            this.waitingRadius.add(player.getUniqueId());
        }
    }

    private String plain(final String s) {
        final String x = ChatColor.stripColor(s == null ? "" : s);
        return x == null ? "" : x;
    }

    private String fmt(final double d) {
        return String.valueOf((int) Math.round(d));
    }
}
