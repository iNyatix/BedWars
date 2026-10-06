package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.util.ItemBuilder;
import space.nyatix.bedwars.util.ItemTag;

public class LobbyManager {

    public static final String JOIN_ITEM_NAME = "§a§lDolacz do meczu turniejowego §7(PPM)";

    public static final String LEAVE_ITEM_NAME = "§c§lWroc do lobby §7(PPM)";

    private final BedWarsPlugin plugin;

    public LobbyManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public Location getLobby() {
        final String worldName = this.plugin.getConfig().getString("lobby.joinLocation.world");
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, this.plugin.getConfig().getDouble("lobby.joinLocation.x"), this.plugin.getConfig().getDouble("lobby.joinLocation.y"),
                this.plugin.getConfig().getDouble("lobby.joinLocation.z"), (float) this.plugin.getConfig().getDouble("lobby.joinLocation.yaw"),
                        (float) this.plugin.getConfig().getDouble("lobby.joinLocation.pitch"));
    }

    public void setLobby(final Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        this.plugin.getConfig().set("lobby.joinLocation.world", location.getWorld().getName());
        this.plugin.getConfig().set("lobby.joinLocation.x", location.getX());
        this.plugin.getConfig().set("lobby.joinLocation.y", location.getY());
        this.plugin.getConfig().set("lobby.joinLocation.z", location.getZ());
        this.plugin.getConfig().set("lobby.joinLocation.yaw", location.getYaw());
        this.plugin.getConfig().set("lobby.joinLocation.pitch", location.getPitch());
        this.plugin.saveConfig();
        if (this.plugin.worldRules() != null) {
            this.plugin.worldRules().prepare(location.getWorld());
        }
    }

    public void send(final Player player) {
        if (player == null) {
            return;
        }
        if (this.plugin.effects() != null) {
            this.plugin.effects().clear(player);
        }
        if (this.plugin.spectators() != null) {
            this.plugin.spectators().clear(player);
        }
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        try {
            player.setHealth(player.getMaxHealth());
        } catch (final Exception ignored) {
        }
        for (final PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setDisplayName(ChatColor.GRAY + player.getName());
        player.setPlayerListName(ChatColor.GRAY + player.getName());
        final Location lobby = getLobby();
        if (lobby != null) {
            player.teleport(lobby);
        }
        giveJoinItem(player);
        final Scoreboard blank = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(blank);
        if (this.plugin.effects() != null) {
            this.plugin.effects().waiting(player);
        }
    }

    public void giveJoinItem(final Player player) {
        player.getInventory().setItem(4, new ItemBuilder(Material.COMPASS)
                .tag("lobby-join")
                .name(Messages.get("lobby.give-join-item.dolacz-do-meczu-turniejowego-ppm"))

                        .lore(Messages.get("lobby.give-join-item.kliknij-prawym-przyciskiem-aby-wejsc"), Messages.get("lobby.give-join-item.do-poczekalni-aktualnego-meczu-bedwars"))
                .appearance(this.plugin, "menus.icons.lobby.give-join-item.dolacz-do-meczu-turniejowego-ppm")
                .build());
    }

    public void giveLeaveItem(final Player player) {
        player.getInventory().setItem(8, new ItemBuilder(Material.BED)
                .tag("lobby-leave")
                .name(Messages.get("lobby.give-leave-item.wroc-do-lobby-ppm"))
                .lore(Messages.get("lobby.give-leave-item.kliknij-prawym-przyciskiem-aby-opuscic"), Messages.get("lobby.give-leave-item.poczekalnie-meczu"))
                .appearance(this.plugin, "menus.icons.lobby.give-leave-item.wroc-do-lobby-ppm")
                .build());
    }

    public boolean isJoinItem(final ItemStack item) {
        return ItemTag.is(item, "lobby-join") || hasName(item, JOIN_ITEM_NAME);
    }

    public boolean isLeaveItem(final ItemStack item) {
        return ItemTag.is(item, "lobby-leave") || hasName(item, LEAVE_ITEM_NAME);
    }

    private boolean hasName(final ItemStack item, final String name) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && name.equals(item.getItemMeta().getDisplayName());
    }
}
