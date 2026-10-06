package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.MatchViewText;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.util.ItemBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpectatorManager {

    private final BedWarsPlugin plugin;

    private final Map<UUID, UUID> following = new HashMap<>();

    private final Set<UUID> intervening = new HashSet<>();

    public SpectatorManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void make(final Player player) {
        this.intervening.remove(player.getUniqueId());
        this.following.remove(player.getUniqueId());
        player.closeInventory();
        player.setItemOnCursor(null);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setFireTicks(0);
        for (final PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.getInventory().setItem(0, new ItemBuilder(Material.COMPASS)
                .tag("spectator-teleporter")
                .name(Messages.get("spectator.make.teleporter-ppm"))
                .lore(Messages.get("spectator.make.wybierz-zawodnika-do-obserwowania"))
                .appearance(this.plugin, "menus.icons.spectator.make.teleporter-ppm")
                .build());
        player.getInventory().setItem(4, new ItemBuilder(Material.REDSTONE_COMPARATOR)
                .tag("spectator-flight")
                .name(Messages.get("spectator.make.swobodny-lot-ppm"))
                .lore(Messages.get("spectator.make.zakoncz-podglad-zawodnika-i-wroc"), Messages.get("spectator.make.na-punkt-obserwacyjny-mapy"))
                .appearance(this.plugin, "menus.icons.spectator.make.swobodny-lot-ppm")
                .build());
        this.plugin.lobby().giveLeaveItem(player);
        if (player.hasPermission("bedwars.admin") && this.plugin.adminMenu() != null) {
            player.getInventory().setItem(7, this.plugin.adminMenu().controlItem());
        }
        teleportToSpawn(player);
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.hidePlayer(player);
            }
        }
        refreshVisibility(player);
        updateScoreboard(player);
    }

    public void clear(final Player player) {
        this.following.remove(player.getUniqueId());
        if (this.intervening.remove(player.getUniqueId())) {
            player.closeInventory();
            player.setItemOnCursor(null);
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.setGameMode(GameMode.ADVENTURE);
        }
        player.setFlying(false);
        player.setAllowFlight(false);
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(player);
        }
    }

    public void refreshVisibility(final Player viewer) {
        for (final Player subject : Bukkit.getOnlinePlayers()) {
            if (!subject.equals(viewer) && this.plugin.game().isSpectator(subject.getUniqueId())) {
                if (isIntervening(subject)) {
                    viewer.showPlayer(subject);
                } else {
                    viewer.hidePlayer(subject);
                }
            }
        }
    }

    public boolean canManageMatch(final Player player) {
        if (player == null || !player.isOnline() || !player.hasPermission("bedwars.admin")) {
            return false;
        }
        final GameManager game = this.plugin.game();
        final Arena arena = game.active();
        final UUID playerId = player.getUniqueId();
        return game.isSpectator(playerId) && (game.state() == GameState.RUNNING || game.state() == GameState.PAUSED && !game.isStarting()) && (!game.participants().contains(playerId) || game.isFinalDead(playerId)) && arena != null && arena.getSpectator() != null && player.getWorld().equals(arena.getSpectator().getWorld());
    }

    public boolean isIntervening(final Player player) {
        return player != null && this.intervening.contains(player.getUniqueId()) && canManageMatch(player);
    }

    public boolean toggleIntervention(final Player player) {
        if (!canManageMatch(player)) {
            Messages.send(player, Messages.get("spectator.toggle-intervention.ingerencja-jest-dostepna-tylko-dla-admina"));
            return false;
        }
        if (this.intervening.contains(player.getUniqueId())) {
            make(player);
            this.plugin.game().broadcast(Messages.get("spectator.toggle-intervention.admin-zakonczyl-ingerencje", "PLAYER", player.getName()));
            return true;
        }
        this.following.remove(player.getUniqueId());
        this.intervening.add(player.getUniqueId());
        player.closeInventory();
        player.setItemOnCursor(null);
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(player);
        }
        this.plugin.game().broadcast(Messages.get("spectator.toggle-intervention.admin-wlaczyl-ingerencje-w-mecz", "PLAYER", player.getName()));
        Messages.send(player, Messages.get("spectator.toggle-intervention.ingerencja-mozesz-budowac-usuwac-bloki-i"));
        updateScoreboard(player);
        return true;
    }

    public void openTeleporter(final Player player) {
        openTeleporter(player, 0);
    }

    private void openTeleporter(final Player player, final int requestedPage) {
        if (!this.plugin.game().isSpectator(player.getUniqueId())) {
            return;
        }
        final List<Player> targets = new ArrayList<>();
        for (final Player target : Bukkit.getOnlinePlayers()) {
            if (canFollow(target)) {
                targets.add(target);
            }
        }
        targets.sort((left, right) -> {
            final int team = this.plugin.game().teamOf(left.getUniqueId()).compareTo(this.plugin.game().teamOf(right.getUniqueId()));
            return team != 0 ? team : left.getName().compareToIgnoreCase(right.getName());
        });
        final int pages = Math.max(1, (targets.size() + 44) / 45), page = Math.max(0, Math.min(requestedPage, pages - 1));
        final TeleporterHolder holder = new TeleporterHolder(player.getUniqueId(), page);
        final Inventory menu = Bukkit.createInventory(holder, 54, MatchViewText.limit(Messages.get("menus.spectator.title"), 32));
        holder.inventory = menu;
        for (int n = page * 45; n < Math.min(targets.size(), (page + 1) * 45); n++) {
            final Player target = targets.get(n);
            final TeamColor team = this.plugin.game().teamOf(target.getUniqueId());
            final ItemStack head = new ItemBuilder(Material.SKULL_ITEM).data((short) 3)
                    .name(team.chat() + target.getName())

                            .lore(Messages.get("spectator.open-teleporter.druzyna", "VALUE1", team.chat(), "TEAM", team.display()), Messages.get("spectator.open-teleporter.hp", "VALUE1", (int) Math.ceil(target.getHealth()), "VALUE2", (int) target.getMaxHealth()), Messages.get("spectator.open-teleporter.kliknij-aby-obserwowac"))
                    .build();
            if (head.getItemMeta() instanceof SkullMeta) {
                final SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwner(target.getName());
                head.setItemMeta(meta);
            }
            final int slot = n - page * 45;
            holder.targets.put(slot, target.getUniqueId());
            menu.setItem(slot, head);
        }
        if (targets.isEmpty()) {
            menu.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(Messages.get("spectator.open-teleporter.brak-dostepnych-zawodnikow"))
                    .lore(Messages.get("spectator.open-teleporter.gracze-moga-sie-wlasnie-odradzac"), Messages.get("spectator.open-teleporter.odswiez-liste-za-chwile"))
                    .appearance(this.plugin, "menus.icons.spectator.open-teleporter.brak-dostepnych-zawodnikow")
                    .build());
        }
        menu.setItem(45, new ItemBuilder(Material.FEATHER)
                .name(Messages.get("spectator.open-teleporter.swobodny-lot"))
                .lore(Messages.get("spectator.open-teleporter.wroc-na-punkt-obserwacyjny"))
                .appearance(this.plugin, "menus.icons.spectator.open-teleporter.swobodny-lot")
                .build());
        if (page > 0) {
            menu.setItem(48, new ItemBuilder(Material.ARROW)
                    .name(Messages.get("spectator.open-teleporter.poprzednia-strona"))
                    .appearance(this.plugin, "menus.icons.spectator.open-teleporter.poprzednia-strona")
                    .build());
        }
        menu.setItem(49, new ItemBuilder(Material.WATCH)
                .name(Messages.get("spectator.open-teleporter.odswiez-liste"))
                .lore(Messages.get("spectator.open-teleporter.strona", "VALUE1", (page + 1), "PAGES", pages))
                .appearance(this.plugin, "menus.icons.spectator.open-teleporter.odswiez-liste")
                .build());
        if (page + 1 < pages) {
            menu.setItem(50, new ItemBuilder(Material.ARROW)
                    .name(Messages.get("spectator.open-teleporter.nastepna-strona"))
                    .appearance(this.plugin, "menus.icons.spectator.open-teleporter.nastepna-strona")
                    .build());
        }
        menu.setItem(53, new ItemBuilder(Material.BED)
                .name(Messages.get("spectator.open-teleporter.wroc-do-lobby"))
                .lore(Messages.get("spectator.open-teleporter.zakoncz-obserwacje-meczu"))
                .appearance(this.plugin, "menus.icons.spectator.open-teleporter.wroc-do-lobby")
                .build());
        player.openInventory(menu);
    }

    public boolean isTeleporter(final Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof TeleporterHolder;
    }

    public void clickTeleporter(final Player player, final Inventory inventory, final int slot) {
        if (!isTeleporter(inventory) || !this.plugin.game().isSpectator(player.getUniqueId()) || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        final TeleporterHolder holder = (TeleporterHolder) inventory.getHolder();
        if (!holder.owner.equals(player.getUniqueId())) {
            return;
        }
        final UUID target = holder.targets.get(slot);
        if (target != null) {
            follow(player, Bukkit.getPlayer(target));
            return;
        }
        if (slot == 45) {
            player.closeInventory();
            stopFollowing(player);
        } else if (slot == 48 && inventory.getItem(slot) != null) {
            openTeleporter(player, holder.page - 1);
        } else if (slot == 49) {
            openTeleporter(player, holder.page);
        } else if (slot == 50 && inventory.getItem(slot) != null) {
            openTeleporter(player, holder.page + 1);
        } else if (slot == 53) {
            this.plugin.game().leaveObservation(player);
        }
    }

    private boolean canFollow(final Player target) {
        if (target == null || !target.isOnline()) {
            return false;
        }
        final UUID playerId = target.getUniqueId();
        final Arena arena = this.plugin.game().active();
        return arena != null && arena.getSpectator() != null && target.getWorld().equals(arena.getSpectator().getWorld()) && this.plugin.game().participants().contains(playerId) && this.plugin.game().teamOf(playerId) != null && !this.plugin.game().isFinalDead(playerId) && !this.plugin.game().isSpectator(playerId) && !this.plugin.game().isRespawning(playerId);
    }

    public void follow(final Player spectator, final Player target) {
        if (!this.plugin.game().isSpectator(spectator.getUniqueId())) {
            return;
        }
        if (isIntervening(spectator)) {
            Messages.send(spectator, Messages.get("spectator.follow.najpierw-wylacz-ingerencje-w-panelu-admina"));
            return;
        }
        if (!canFollow(target)) {
            Messages.send(spectator, Messages.get("spectator.follow.ten-zawodnik-jest-teraz-niedostepny-odswiez"));
            return;
        }
        this.following.put(spectator.getUniqueId(), target.getUniqueId());
        spectator.closeInventory();
        spectator.teleport(target.getLocation().clone().add(0, 2, 0));
        Messages.send(spectator, Messages.get("spectator.follow.obserwujesz-teraz-komparator-swobodny-lot-lozko", "PLAYER", target.getName()));
        updateScoreboard(spectator);
    }

    public Player target(final Player player) {
        final UUID playerId = this.following.get(player.getUniqueId());
        final Player target = playerId == null ? null : Bukkit.getPlayer(playerId);
        return canFollow(target) ? target : null;
    }

    public void stopFollowing(final Player spectator) {
        if (!this.plugin.game().isSpectator(spectator.getUniqueId())) {
            return;
        }
        this.following.remove(spectator.getUniqueId());
        teleportToSpawn(spectator);
        updateScoreboard(spectator);
        Messages.send(spectator, Messages.get("spectator.stop-following.swobodny-lot-kompas-otwiera-liste-zawodnikow"));
    }

    public void tick() {
        for (final UUID playerId : new HashSet<>(this.intervening)) {
            final Player admin = Bukkit.getPlayer(playerId);
            if (admin == null) {
                this.intervening.remove(playerId);
                continue;
            }
            if (!canManageMatch(admin)) {
                if (this.plugin.game().isSpectator(playerId)) {
                    make(admin);
                } else {
                    clear(admin);
                }
            }
        }
        for (final Map.Entry<UUID, UUID> entry : new HashMap<>(this.following).entrySet()) {
            final Player spectator = Bukkit.getPlayer(entry.getKey()), target = Bukkit.getPlayer(entry.getValue());
            if (spectator == null || !this.plugin.game().isSpectator(entry.getKey())) {
                this.following.remove(entry.getKey());
                continue;
            }
            if (!canFollow(target)) {
                stopFollowing(spectator);
                continue;
            }
            if (!spectator.getWorld().equals(target.getWorld()) || spectator.getLocation().distanceSquared(target.getLocation()) > 2500) {
                spectator.teleport(target.getLocation().clone().add(0, 2, 0));
            }
            updateScoreboard(spectator);
        }
    }

    private void teleportToSpawn(final Player player) {
        final Arena arena = this.plugin.game().active();
        if (arena != null && arena.getSpectator() != null && arena.getSpectator().getWorld() != null) {
            player.teleport(arena.getSpectator());
        }
    }

    private void updateScoreboard(final Player player) {
        if (this.plugin.scoreboards() != null) {
            this.plugin.scoreboards().update(player);
        }
    }

    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.game().isSpectator(player.getUniqueId())) {
                clear(player);
            }
        }
        this.following.clear();
    }

    private static class TeleporterHolder implements InventoryHolder {

        final UUID owner;

        final int page;

        final Map<Integer, UUID> targets = new HashMap<>();

        Inventory inventory;

        TeleporterHolder(final UUID owner, final int page) {
            this.owner = owner;
            this.page = page;
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }
}
