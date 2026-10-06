package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.util.ItemTag;
import space.nyatix.bedwars.util.Locations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class GameListener implements Listener {

    private final BedWarsPlugin plugin;

    private final Map<UUID, UUID> petOwners = new HashMap<>();

    public GameListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void join(final PlayerJoinEvent event) {
        event.setJoinMessage(Messages.get("game.join.stanowisko"));
        this.plugin.game().onJoin(event.getPlayer());
    }

    @EventHandler
    public void quit(final PlayerQuitEvent event) {
        this.plugin.game().onQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void damage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager && this.plugin.npcs().isShop((Villager) event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getEntity();
        if (this.plugin.game().teamOf(player.getUniqueId()) == null && !this.plugin.game().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.game().state() == GameState.PAUSED || this.plugin.game().isSpectator(player.getUniqueId()) || this.plugin.game().isRespawning(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.game().state() != GameState.RUNNING && !player.hasPermission("bedwars.admin")) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.game().isProtected(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void combat(final EntityDamageByEntityEvent event) {
        final Player damager = resolveDamager(event.getDamager());
        if (damager != null && this.plugin.game().isSpectator(damager.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        final Player victim = (Player) event.getEntity();
        if (this.plugin.game().isProtected(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (damager != null) {
            final TeamColor teamColor = this.plugin.game().teamOf(victim.getUniqueId()), b = this.plugin.game().teamOf(damager.getUniqueId());
            if (teamColor != null && teamColor == b && this.plugin.settings().friendlyFire()) {
                event.setCancelled(true);
                return;
            }
            this.plugin.game().markDamage(victim, damager);
            this.plugin.game().clearProtection(damager.getUniqueId());
        } else if (event.getDamager() instanceof Creature) {
            final UUID owner = this.petOwners.get(event.getDamager().getUniqueId());
            if (owner != null) {
                final Player player = Bukkit.getPlayer(owner);
                if (player != null) {
                    this.plugin.game().markDamage(victim, player);
                }
            }
        }
    }

    private Player resolveDamager(final Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }
        if (entity instanceof Projectile) {
            final ProjectileSource s = ((Projectile) entity).getShooter();
            if (s instanceof Player) {
                return (Player) s;
            }
        }
        final UUID owner = this.petOwners.get(entity.getUniqueId());
        return owner == null ? null : Bukkit.getPlayer(owner);
    }

    @EventHandler
    public void death(final PlayerDeathEvent event) {
        event.setDeathMessage(null);
        event.setKeepLevel(true);
        event.getDrops().clear();
        final Player player = event.getEntity();
        final Player killer = resolveDamagerFromPlayer(player);
        final Location deathLocation = player.getLocation().clone();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
                player.spigot().respawn();
            } catch (final Exception ignored) {
            }
            this.plugin.game().handleDeath(player, killer, deathLocation);
        });
    }

    private Player resolveDamagerFromPlayer(final Player player) {
        final Player k = player.getKiller();
        return k != null ? k : this.plugin.game().recentKiller(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void place(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        if (!this.plugin.game().canPlace(player, event.getBlockPlaced())) {
            event.setCancelled(true);
            if (event.getBlockPlaced().getLocation().getY() > this.plugin.settings().buildHeight()) {
                Messages.send(player, Messages.get("game.place.osiagnales-limit-wysokosci-budowania"));
            } else if (this.plugin.game().isNoBuildLocation(event.getBlockPlaced().getLocation())) {
                Messages.send(player, Messages.get("game.place.w-tej-strefie-bazy-nie-mozna"));
            }
            return;
        }
        if (this.plugin.game().canIntervene(player)) {
            this.plugin.game().recordPlaced(event.getBlockPlaced());
            return;
        }
        this.plugin.game().recordPlaced(event.getBlockPlaced());
        this.plugin.game().clearProtection(player.getUniqueId());
        if (event.getBlockPlaced().getType() == Material.TNT) {
            final Location location = event.getBlockPlaced().getLocation();
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                final Block block = location.getBlock();
                block.setType(Material.AIR);
                final TNTPrimed t = location.getWorld().spawn(location.clone().add(.5, .2, .5), TNTPrimed.class);
                t.setFuseTicks(Options.integer(this.plugin, "items.tnt.fuseTicks", 40, 1, 1200));
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void breakBlock(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (this.plugin.game().canIntervene(player)) {
            final TeamColor bed = this.plugin.game().bedTeam(block.getLocation());
            if (bed != null) {
                event.setCancelled(true);
                this.plugin.game().adminRemoveBed(player, bed);
            } else {
                this.plugin.game().recordChanged(block);
                this.plugin.game().removePlaced(block);
            }
            return;
        }
        if (this.plugin.game().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.game().state() != GameState.RUNNING) {
            if (!player.hasPermission("bedwars.admin") || !this.plugin.settings().allowAdminBuild()) {
                event.setCancelled(true);
            }
            return;
        }
        final TeamColor bed = this.plugin.game().bedTeam(block.getLocation());
        if (bed != null) {
            if (bed == this.plugin.game().teamOf(player.getUniqueId())) {
                event.setCancelled(true);
                Messages.send(player, Messages.get("game.break-block.nie-mozesz-zniszczyc-wlasnego-lozka"));
                return;
            }
            this.plugin.game().recordChanged(block);
            this.plugin.game().destroyBed(player, block.getLocation());
            this.plugin.game().clearProtection(player.getUniqueId());
            return;
        }
        if (!this.plugin.game().canBreak(player, block)) {
            event.setCancelled(true);
            return;
        }
        this.plugin.game().removePlaced(block);
        this.plugin.game().clearProtection(player.getUniqueId());
    }

    @EventHandler
    public void bucket(final PlayerBucketEmptyEvent event) {
        if (this.plugin.game().canIntervene(event.getPlayer())) {
            this.plugin.game().recordPlaced(event.getBlockClicked().getRelative(event.getBlockFace()));
            return;
        }
        if (this.plugin.game().state() != GameState.RUNNING || this.plugin.game().isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        final Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (this.plugin.game().isNoBuildLocation(target.getLocation())) {
            event.setCancelled(true);
            Messages.send(event.getPlayer(), Messages.get("game.bucket.w-tym-miejscu-nie-mozna-budowac"));
            return;
        }
        this.plugin.game().recordChanged(target);
        this.plugin.game().placedBlocks().add(Locations.key(target.getLocation()));
        this.plugin.game().clearProtection(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void bucketFill(final PlayerBucketFillEvent event) {
        if (this.plugin.game().isSpectator(event.getPlayer().getUniqueId()) && !this.plugin.game().canIntervene(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void explode(final EntityExplodeEvent event) {
        final Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            final Block block = it.next();
            if (block.getType() == Material.GLASS || this.plugin.game().bedTeam(block.getLocation()) != null || !this.plugin.game().placedBlocks().contains(Locations.key(block.getLocation()))) {
                it.remove();
                continue;
            }
            this.plugin.game().recordChanged(block);
            this.plugin.game().removePlaced(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interactEntity(final PlayerInteractEntityEvent event) {
        if (this.plugin.game().isSpectator(event.getPlayer().getUniqueId()) && !this.plugin.game().canIntervene(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getRightClicked() instanceof Villager)) {
            return;
        }
        final Villager villager = (Villager) event.getRightClicked();
        if (!this.plugin.npcs().isShop(villager)) {
            return;
        }
        event.setCancelled(true);
        if (this.plugin.game().isSpectator(event.getPlayer().getUniqueId()) || this.plugin.game().state() != GameState.RUNNING) {
            return;
        }
        if (this.plugin.npcs().isItemShop(villager)) {
            this.plugin.shop().open(event.getPlayer());
        } else {
            this.plugin.shop().openUpgrades(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();
        if (this.plugin.game().isSpectator(player.getUniqueId())) {
            if (item != null && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                if (this.plugin.adminMenu() != null && this.plugin.adminMenu().isControlItem(item)) {
                    event.setCancelled(true);
                    this.plugin.adminMenu().open(player);
                    return;
                }
                if (ItemTag.is(item, "spectator-teleporter") || item.getType() == Material.COMPASS) {
                    event.setCancelled(true);
                    this.plugin.spectators().openTeleporter(player);
                    return;
                }
                if (ItemTag.is(item, "spectator-flight") || item.getType() == Material.REDSTONE_COMPARATOR) {
                    event.setCancelled(true);
                    this.plugin.spectators().stopFollowing(player);
                    return;
                }
                if (this.plugin.lobby().isLeaveItem(item)) {
                    event.setCancelled(true);
                    this.plugin.game().leaveObservation(player);
                    return;
                }
            }
            if (!this.plugin.game().canIntervene(player)) {
                event.setCancelled(true);
            }
            return;
        }
        if (item == null) {
            return;
        }
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && this.plugin.lobby().isJoinItem(item)) {
            event.setCancelled(true);
            this.plugin.game().joinWaiting(player);
            return;
        }
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && this.plugin.lobby().isLeaveItem(item)) {
            event.setCancelled(true);
            this.plugin.game().leaveWaiting(player);
            return;
        }
        if (this.plugin.game().state() != GameState.RUNNING) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        this.plugin.game().clearProtection(player.getUniqueId());
        if (item.getType() == Material.FIREBALL) {
            event.setCancelled(true);
            consume(player);
            final Fireball f = player.launchProjectile(Fireball.class);
            f.setYield((float) Options.number(this.plugin, "items.fireball.explosionPower", 2.2D, 0.0D, 10.0D));
            f.setIsIncendiary(Options.enabled(this.plugin, "items.fireball.incendiary", false));
        } else if (item.getType() == Material.EGG) {
            event.setCancelled(true);
            consume(player);
            final Egg egg = player.launchProjectile(Egg.class);
            startBridgeEgg(player, egg);
        } else if (item.getType() == Material.SNOW_BALL) {
            event.setCancelled(true);
            consume(player);
            final Snowball s = player.launchProjectile(Snowball.class);
            s.setMetadata("bw-bedbug", new FixedMetadataValue(this.plugin, player.getUniqueId().toString()));
        } else if (item.getType() == Material.MONSTER_EGG) {
            event.setCancelled(true);
            consume(player);
            final Location location = event.getClickedBlock() != null ? event.getClickedBlock().getLocation().add(0, 1, 0) : player.getLocation().add(player.getLocation().getDirection().multiply(2));
            final IronGolem g = this.plugin.worldRules() == null ? location.getWorld().spawn(location, IronGolem.class) : this.plugin.worldRules().spawnGameMob(location,
                    IronGolem.class);
            final TeamColor team = this.plugin.game().teamOf(player.getUniqueId());
            g.setCustomName(Messages.get("game.interact.straznik-snow", "VALUE1", (team == null ? "§7" : team.chat()), "PLAYER", player.getName()));
            g.setPlayerCreated(true);
            this.petOwners.put(g.getUniqueId(), player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                g.remove();
                this.petOwners.remove(g.getUniqueId());
            }, 20L * Options.integer(this.plugin, "items.defender.lifetimeSeconds", 240, 1, 3600));
        }
    }

    private void consume(final Player player) {
        final ItemStack item = player.getItemInHand();
        if (item.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void startBridgeEgg(final Player owner, final Egg egg) {
        final TeamColor teamColor = this.plugin.game().teamOf(owner.getUniqueId());
        if (teamColor == null) {
            return;
        }
        new BukkitRunnable() {

            int age = 0;

            public void run() {
                if (egg.isDead() || !egg.isValid() || age++ > Options.integer(plugin, "items.bridgeEgg.lifetimeTicks", 80, 1, 1200)) {
                    cancel();
                    return;
                }
                final Location location = egg.getLocation().clone().subtract(0, 2, 0);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > 1) {
                            continue;
                        }
                        final Block block = location.clone().add(dx, 0, dz).getBlock();
                        if (block.getType() == Material.AIR && !plugin.game().isNoBuildLocation(block.getLocation())) {
                            plugin.game().recordChanged(block);
                            block.setType(Material.WOOL);
                            block.setData(teamColor.dye().getWoolData());
                            plugin.game().placedBlocks().add(Locations.key(block.getLocation()));
                        }
                    }
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    @EventHandler
    public void projectileHit(final ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball && event.getEntity().hasMetadata("bw-bedbug")) {
            final String id = event.getEntity().getMetadata("bw-bedbug").get(0).asString();
            Player owner = null;
            try {
                owner = Bukkit.getPlayer(UUID.fromString(id));
            } catch (final Exception ignored) {
            }
            if (owner != null) {
                final Silverfish s = this.plugin.worldRules() == null ? event.getEntity().getWorld().spawn(event.getEntity().getLocation(),
                        Silverfish.class) : this.plugin.worldRules().spawnGameMob(event.getEntity().getLocation(), Silverfish.class);
                final TeamColor team = this.plugin.game().teamOf(owner.getUniqueId());
                s.setCustomName(Messages.get("mobs.bedbug.name", "COLOR", team == null ? "§7" : team.chat(), "PLAYER", owner.getName()));
                this.petOwners.put(s.getUniqueId(), owner.getUniqueId());
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    s.remove();
                    this.petOwners.remove(s.getUniqueId());
                }, 20L * Options.integer(this.plugin, "items.bedbug.lifetimeSeconds", 15, 1, 3600));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drop(final PlayerDropItemEvent event) {
        if (this.plugin.game().isSpectator(event.getPlayer().getUniqueId()) && !this.plugin.game().canIntervene(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        final ItemStack item = event.getItemDrop().getItemStack();
        if (this.plugin.adminMenu() != null && this.plugin.adminMenu().isControlItem(item)) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.lobby().isJoinItem(item) || this.plugin.lobby().isLeaveItem(item)) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.game().canIntervene(event.getPlayer())) {
            return;
        }
        if (this.plugin.game().state() != GameState.RUNNING) {
            return;
        }
        final Material material = item.getType();
        final String n = material.name();
        if (material == Material.WOOD_SWORD || material == Material.SHEARS || n.endsWith("_PICKAXE") || n.endsWith("_AXE")) {
            event.setCancelled(true);
            Messages.send(event.getPlayer(), Messages.get("game.drop.nie-mozesz-wyrzucac-stalych-przedmiotow"));
        }
    }

    @EventHandler
    public void consumeEvent(final PlayerItemConsumeEvent event) {
        final ItemStack item = event.getItem();
        if (ItemTag.is(item, "magic-milk") || item.hasItemMeta() && item.getItemMeta().hasDisplayName() && ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Magiczne mleko")) {
            this.plugin.game().setMagicMilk(event.getPlayer(), Options.integer(this.plugin, "items.magicMilk.seconds", 30, 1, 3600));
        }
    }

    @EventHandler
    public void hunger(final FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void creatureTarget(final EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player && this.plugin.game().isSpectator(event.getTarget().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        final UUID owner = this.petOwners.get(event.getEntity().getUniqueId());
        if (owner == null || !(event.getTarget() instanceof Player)) {
            return;
        }
        final TeamColor teamColor = this.plugin.game().teamOf(owner), b = this.plugin.game().teamOf(event.getTarget().getUniqueId());
        if (teamColor != null && teamColor == b) {
            event.setCancelled(true);
        }
    }
}
