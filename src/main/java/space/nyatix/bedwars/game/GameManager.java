package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.GeneratorType;
import space.nyatix.bedwars.model.PlayerLoadout;
import space.nyatix.bedwars.model.PlayerStats;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.model.TeamData;
import space.nyatix.bedwars.model.TrapType;
import space.nyatix.bedwars.util.ConfiguredSound;
import space.nyatix.bedwars.util.Locations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class GameManager {

    private final BedWarsPlugin plugin;

    private final Map<String, Arena> arenas;

    private final Map<UUID, TeamColor> playerTeams = new LinkedHashMap<>();

    private final Map<UUID, PlayerStats> stats = new HashMap<>();

    private final Map<UUID, PlayerLoadout> loadouts = new HashMap<>();

    private final Set<UUID> participants = new LinkedHashSet<>();

    private final Set<UUID> finalDead = new HashSet<>();

    private final Set<UUID> spectators = new HashSet<>();

    private final Map<UUID, Long> disconnectedAt = new LinkedHashMap<>();

    private final Map<UUID, UUID> lastDamager = new HashMap<>();

    private final Map<UUID, Long> lastDamageAt = new HashMap<>();

    private final Map<UUID, Integer> respawning = new HashMap<>();

    private final Map<UUID, Object> pendingJoins = new HashMap<>();

    private final Map<UUID, Long> protectedUntil = new HashMap<>();

    private final Set<String> placedBlocks = new HashSet<>();

    private final Map<String, BlockState> originalBlocks = new LinkedHashMap<>();

    private final Map<UUID, Long> trapCooldown = new HashMap<>();

    private final Map<UUID, Long> magicMilkUntil = new HashMap<>();

    private final List<EnderDragon> dragons = new ArrayList<>();

    private final Set<TeamColor> eliminatedTeams = EnumSet.noneOf(TeamColor.class);

    private Arena active;

    private GameState state = GameState.WAITING, beforePause = GameState.RUNNING;

    private int elapsed = 0, countdown = -1, tickTask = -1, resetTask = -1;

    private boolean suddenDeathSpawned = false;

    private TeamColor lastEliminatedTeam;

    private boolean manualCountdown = false;

    public GameManager(final BedWarsPlugin plugin, final Map<String, Arena> arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
        final String selected = plugin.getConfig().getString("game.activeArena", "");
        this.active = arenas.get(selected.toLowerCase(Locale.ROOT));
        if (this.active == null) {
            for (final Arena arena : arenas.values()) {
                if (arena.isPlayable()) {
                    this.active = arena;
                    break;
                }
            }
        }
        if (this.active == null && !arenas.isEmpty()) {
            this.active = arenas.values().iterator().next();
        }
        this.tickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            tick();
        }, 20L, 20L);
    }

    public void shutdown() {
        if (this.tickTask != -1) {
            Bukkit.getScheduler().cancelTask(this.tickTask);
        }
        cancelScheduledReset();
        this.pendingJoins.clear();
        if (this.plugin.borders() != null) {
            this.plugin.borders().resetMatch();
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            player.getEnderChest().clear();
        }
        removeDragons();
        this.originalBlocks.clear();
        this.placedBlocks.clear();
    }

    public Map<String, Arena> arenas() {
        return this.arenas;
    }

    public Arena active() {
        return this.active;
    }

    public void setActive(final Arena arena) {
        if (this.state != GameState.WAITING && this.state != GameState.COUNTDOWN) {
            return;
        }
        if (this.plugin.borders() != null) {
            this.plugin.borders().resetMatch();
        }
        if (this.plugin.effects() != null) {
            this.plugin.effects().reset();
        }
        if (this.active != null) {
            for (final TeamData team : this.active.getTeams().values()) {
                team.getPlayers().clear();
            }
        }
        this.active = arena;
        this.state = GameState.WAITING;
        this.countdown = -1;
        this.manualCountdown = false;
        if (this.active != null) {
            for (final TeamData team : this.active.getTeams().values()) {
                team.getPlayers().clear();
            }
        }
        this.pendingJoins.clear();
        this.playerTeams.clear();
        this.participants.clear();
        rememberActiveArena();
    }

    public void rememberActiveArena() {
        final String name = this.active == null ? null : this.active.getName();
        if (!Objects.equals(name, this.plugin.getConfig().getString("game.activeArena"))) {
            this.plugin.getConfig().set("game.activeArena", name);
            this.plugin.saveConfig();
        }
    }

    public GameState state() {
        return this.state;
    }

    public int elapsed() {
        return this.elapsed;
    }

    public int countdown() {
        return Math.max(0, this.countdown);
    }

    public boolean isPaused() {
        return this.state == GameState.PAUSED;
    }

    public boolean isStarting() {
        return this.state == GameState.COUNTDOWN || (this.state == GameState.PAUSED && this.beforePause == GameState.COUNTDOWN);
    }

    public Set<UUID> participants() {
        return Collections.unmodifiableSet(this.participants);
    }

    public TeamColor teamOf(final UUID playerId) {
        return this.playerTeams.get(playerId);
    }

    public PlayerStats stats(final UUID playerId) {
        PlayerStats stats = this.stats.get(playerId);
        if (stats == null) {
            stats = new PlayerStats();
            this.stats.put(playerId, stats);
        }
        return stats;
    }

    public PlayerLoadout loadout(final UUID playerId) {
        PlayerLoadout loadout = this.loadouts.get(playerId);
        if (loadout == null) {
            loadout = new PlayerLoadout();
            this.loadouts.put(playerId, loadout);
        }
        return loadout;
    }

    public boolean isFinalDead(final UUID playerId) {
        return this.finalDead.contains(playerId);
    }

    public boolean isSpectator(final UUID playerId) {
        return this.spectators.contains(playerId);
    }

    public boolean isRespawning(final UUID playerId) {
        return this.respawning.containsKey(playerId);
    }

    public int respawnSecondsLeft(final UUID playerId) {
        final Integer left = this.respawning.get(playerId);
        return left == null ? 0 : Math.max(0, left);
    }

    public Set<String> placedBlocks() {
        return this.placedBlocks;
    }

    public boolean isProtected(final UUID playerId) {
        final Long until = this.protectedUntil.get(playerId);
        return until != null && System.currentTimeMillis() < until;
    }

    public void clearProtection(final UUID playerId) {
        this.protectedUntil.remove(playerId);
    }

    public void setMagicMilk(final Player player, final int seconds) {
        this.magicMilkUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        Messages.send(player, Messages.get("game.set-magic-milk.magiczne-mleko-aktywne-przez-s", "SECONDS", seconds));
    }

    public boolean hasMagicMilk(final UUID playerId) {
        final Long until = this.magicMilkUntil.get(playerId);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            this.magicMilkUntil.remove(playerId);
            return false;
        }
        return true;
    }

    public Arena createArena(final String name, final World world) {
        final String key = name.toLowerCase();
        if (this.arenas.containsKey(key)) {
            return this.arenas.get(key);
        }
        final Arena arena = new Arena(name);
        arena.setWorld(world.getName());
        this.arenas.put(key, arena);
        this.active = arena;
        rememberActiveArena();
        this.plugin.arenaStorage().saveArena(arena);
        return arena;
    }

    public void deleteArena(final String name) {
        if (this.state == GameState.RUNNING || this.state == GameState.PAUSED) {
            return;
        }
        final Arena arena = this.arenas.remove(name.toLowerCase());
        if (arena != null && arena == this.active) {
            this.active = this.arenas.isEmpty() ? null : this.arenas.values().iterator().next();
        }
        rememberActiveArena();
        this.plugin.arenaStorage().delete(name);
    }

    public boolean assign(final Player player, final TeamColor teamColor) {
        if (this.active == null || (this.state != GameState.WAITING && this.state != GameState.COUNTDOWN)) {
            return false;
        }
        final TeamColor old = this.playerTeams.get(player.getUniqueId());
        if (old == teamColor) {
            return true;
        }
        if (this.active.team(teamColor).getPlayers().size() >= this.plugin.settings().playersPerTeam() && old != teamColor) {
            return false;
        }
        if (old != null) {
            this.active.team(old).getPlayers().remove(player.getUniqueId());
        }
        this.playerTeams.put(player.getUniqueId(), teamColor);
        this.active.team(teamColor).getPlayers().add(player.getUniqueId());
        this.participants.add(player.getUniqueId());
        this.spectators.remove(player.getUniqueId());
        applyFormatting(player);
        if (this.plugin.scoreboards() != null) {
            this.plugin.scoreboards().update(player);
        }
        return true;
    }

    public boolean assignGroup(final Collection<Player> players, final TeamColor color) {
        if (this.active == null || color == null || (this.state != GameState.WAITING && this.state != GameState.COUNTDOWN)) {
            return false;
        }
        final Map<UUID, Player> group = new LinkedHashMap<>();
        for (final Player player : players) {
            if (player == null || !player.isOnline()) {
                return false;
            }
            group.put(player.getUniqueId(), player);
        }
        if (group.isEmpty()) {
            return false;
        }
        int occupied = 0;
        for (final UUID id : this.active.team(color).getPlayers()) {
            if (!group.containsKey(id)) {
                occupied++;
            }
        }
        if (occupied + group.size() > this.plugin.settings().playersPerTeam()) {
            return false;
        }
        for (final UUID id : group.keySet()) {
            for (final TeamData team : this.active.getTeams().values()) {
                team.getPlayers().remove(id);
            }
            this.active.team(color).getPlayers().add(id);
            this.playerTeams.put(id, color);
            this.participants.add(id);
            this.spectators.remove(id);
        }
        for (final Player player : group.values()) {
            applyFormatting(player);
            if (this.plugin.scoreboards() != null) {
                this.plugin.scoreboards().update(player);
            }
        }
        return true;
    }

    public TeamColor autoAssign(final Player player) {
        if (this.active == null) {
            return null;
        }
        TeamColor best = null;
        int min = Integer.MAX_VALUE;
        for (final TeamColor teamColor : TeamColor.values()) {
            final int n = this.active.team(teamColor).getPlayers().size();
            if (n < min && n < this.plugin.settings().playersPerTeam()) {
                best = teamColor;
                min = n;
            }
        }
        if (best != null) {
            assign(player, best);
        }
        return best;
    }

    public void removeFromTeam(final UUID playerId) {
        if (this.state == GameState.RUNNING || this.state == GameState.PAUSED) {
            return;
        }
        final TeamColor teamColor = this.playerTeams.remove(playerId);
        this.participants.remove(playerId);
        if (teamColor != null && this.active != null) {
            this.active.team(teamColor).getPlayers().remove(playerId);
        }
    }

    public int waitingPlayerCount() {
        return this.playerTeams.size();
    }

    public int matchPlayerCount() {
        int n = 0;
        for (final UUID playerId : this.participants) {
            if (!this.finalDead.contains(playerId)) {
                n++;
            }
        }
        return n;
    }

    public int aliveCount(final TeamColor teamColor) {
        if (this.active == null) {
            return 0;
        }
        int n = 0;
        for (final UUID playerId : this.active.team(teamColor).getPlayers()) {
            if (this.participants.contains(playerId) && !this.finalDead.contains(playerId)) {
                n++;
            }
        }
        return n;
    }

    public boolean teamAlive(final TeamColor teamColor) {
        return aliveCount(teamColor) > 0;
    }

    public boolean isTeamInMatch(final TeamColor color) {
        for (final UUID playerId : this.participants) {
            if (teamOf(playerId) == color) {
                return true;
            }
        }
        return false;
    }

    public boolean canStart() {
        if (this.active == null || !ArenaValidator.validate(this.active).isEmpty()) {
            return false;
        }
        if (this.plugin.worlds() != null && !this.plugin.worlds().isRuntimeReady(this.active)) {
            return false;
        }
        int online = 0;
        final Set<TeamColor> represented = EnumSet.noneOf(TeamColor.class);
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.playerTeams.containsKey(player.getUniqueId())) {
                online++;
                represented.add(this.playerTeams.get(player.getUniqueId()));
            }
        }
        return online >= this.plugin.settings().minPlayers() && represented.size() >= 2;
    }

    public void joinWaiting(final Player p) {
        if (this.state != GameState.WAITING && this.state != GameState.COUNTDOWN) {
            Messages.send(p, Messages.get("game.join-waiting.mecz-juz-trwa-poczekaj-na-nastepna"));
            return;
        }
        if (this.active == null) {
            Messages.send(p, Messages.get("game.join-waiting.nie-ustawiono-aktywnej-mapy-turniejowej"));
            return;
        }
        if (!ArenaValidator.validate(this.active).isEmpty()) {
            Messages.send(p, Messages.get("game.join-waiting.aktualna-mapa-nie-jest-jeszcze-gotowa"));
            return;
        }
        if (this.plugin.worlds() != null && !this.plugin.worlds().isRuntimeReady(this.active)) {
            Messages.send(p, Messages.get("game.join-waiting.przygotowuje-swieza-kopie-mapy-to-potrwa"));
            final UUID waitingPlayer = p.getUniqueId();
            if (this.pendingJoins.containsKey(waitingPlayer)) {
                return;
            }
            final Arena requestedArena = this.active;
            final Object request = new Object();
            this.pendingJoins.put(waitingPlayer, request);
            this.plugin.worlds().prepareRuntimeAsync(requestedArena, new SlimeWorldService.RuntimeCallback() {

                public void finished(final World world, final Throwable error) {
                    if (pendingJoins.get(waitingPlayer) != request) {
                        return;
                    }
                    pendingJoins.remove(waitingPlayer);
                    final Player player = Bukkit.getPlayer(waitingPlayer);
                    if (player == null || active != requestedArena) {
                        return;
                    }
                    if (error != null || world == null) {
                        Messages.send(player, Messages.get("game.finished.nie-udalo-sie-zaladowac-mapy-turniejowej"));
                        return;
                    }
                    if (plugin.npcs() != null) {
                        plugin.npcs().respawn();
                    }
                    if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
                        joinWaiting(player);
                    }
                }
            });
            return;
        }
        if (this.plugin.parties() != null && this.plugin.parties().inParty(p.getUniqueId())) {
            final List<Player> group = this.plugin.parties().onlineMembers(p);
            final String error = this.plugin.parties().assignForQueue(p, group);
            if (error != null) {
                Messages.send(p, Messages.get("game.join-waiting.text", "ERROR", error));
                return;
            }
            for (final Player member : group) {
                this.pendingJoins.remove(member.getUniqueId());
                enterWaiting(member);
            }
            return;
        }
        if (!this.playerTeams.containsKey(p.getUniqueId())) {
            final TeamColor teamColor = autoAssign(p);
            if (teamColor == null) {
                Messages.send(p, Messages.get("game.join-waiting.brak-wolnego-miejsca-w-druzynach"));
                return;
            }
        }
        enterWaiting(p);
    }

    private void enterWaiting(final Player player) {
        player.getEnderChest().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        if (this.active.getLobby() != null) {
            player.teleport(this.active.getLobby());
        }
        this.plugin.lobby().giveLeaveItem(player);
        applyFormatting(player);
        final TeamColor teamColor = teamOf(player.getUniqueId());
        Messages.send(player, Messages.get("game.enter-waiting.dolaczyles-do-meczu-turniejowego", "TEAM", (teamColor == null ? "." : Messages.get("game.enter-waiting.jako",
                
                "VALUE1", teamColor.chat(), "TEAM", teamColor.display()))));
        if (this.plugin.scoreboards() != null) {
            this.plugin.scoreboards().update(player);
        }
        if (this.plugin.effects() != null) {
            if (this.state == GameState.COUNTDOWN) {
                this.plugin.effects().countdown(player);
            } else {
                this.plugin.effects().waiting(player);
            }
        }
    }

    public void leaveWaiting(final Player player) {
        if (isSpectator(player.getUniqueId())) {
            leaveObservation(player);
            return;
        }
        if (this.state != GameState.WAITING && this.state != GameState.COUNTDOWN) {
            Messages.send(player, Messages.get("game.leave-waiting.nie-mozesz-opuscic-kolejki-bo-mecz"));
            return;
        }
        this.pendingJoins.remove(player.getUniqueId());
        removeFromTeam(player.getUniqueId());
        this.plugin.lobby().send(player);
        Messages.send(player, Messages.get("game.leave-waiting.opusciles-poczekalnie-meczu"));
    }

    public void startCountdown(final int sec) {
        if (this.state != GameState.WAITING && this.state != GameState.COUNTDOWN) {
            return;
        }
        this.state = GameState.COUNTDOWN;
        this.countdown = Math.max(1, sec);
        broadcast(Messages.get("game.start-countdown.mecz-rozpocznie-sie-za-s", "COUNTDOWN", this.countdown));
        if (this.plugin.effects() != null) {
            for (final UUID playerId : this.playerTeams.keySet()) {
                final Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    this.plugin.effects().countdown(player);
                }
            }
        }
    }

    public void forceStart() {
        if (this.active == null) {
            return;
        }
        if (!canStart()) {
            broadcast(Messages.get("game.force-start.nie-mozna-wystartowac-mapa-niekompletna-albo"));
            return;
        }
        this.manualCountdown = true;
        startCountdown(5);
    }

    private int lobbyCapacity() {
        return Math.max(4, this.plugin.settings().playersPerTeam() * TeamColor.values().length);
    }

    private void startGame() {
        if (!canStart()) {
            this.state = GameState.WAITING;
            if (this.plugin.effects() != null) {
                this.plugin.effects().countdownCancelled();
            }
            broadcast(Messages.get("game.start-game.start-meczu-anulowany-mapa-nie-jest"));
            return;
        }
        this.state = GameState.RUNNING;
        this.elapsed = 0;
        this.countdown = -1;
        this.suddenDeathSpawned = false;
        this.lastEliminatedTeam = null;
        if (this.plugin.borders() != null) {
            this.plugin.borders().resetMatch();
        }
        this.finalDead.clear();
        this.spectators.clear();
        this.disconnectedAt.clear();
        this.lastDamager.clear();
        this.lastDamageAt.clear();
        this.respawning.clear();
        this.protectedUntil.clear();
        this.magicMilkUntil.clear();
        this.trapCooldown.clear();
        this.eliminatedTeams.clear();
        this.placedBlocks.clear();
        this.originalBlocks.clear();
        removeDragons();
        this.plugin.generators().clear();
        this.participants.clear();
        for (final UUID playerId : this.playerTeams.keySet()) {
            final Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                this.participants.add(playerId);
            }
        }
        for (final TeamColor color : TeamColor.values()) {
            final TeamData team = this.active.team(color);
            team.setBedAlive(isTeamInMatch(color));
            team.resetUpgrades();
            if (!team.isBedAlive()) {
                removeBedBlocks(color);
            }
        }
        for (final UUID u : this.participants) {
            this.stats.put(u, new PlayerStats());
            loadout(u).reset();
        }
        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (this.participants.contains(p.getUniqueId())) {
                spawnPlayer(p, true);
            } else if (this.spectators.contains(p.getUniqueId())) {
                this.plugin.spectators().make(p);
            }
        }
        if (this.plugin.matchLogger() != null) {
            this.plugin.matchLogger().begin();
        }
        broadcastRaw(Messages.get("game.start-game.bed-wars"));
        broadcastRaw(Messages.get("game.start-game.chron-swoje-lozko-i-niszcz-lozka"));
    }

    public void pause() {
        if (this.state == GameState.RUNNING || this.state == GameState.COUNTDOWN) {
            this.beforePause = this.state;
            this.state = GameState.PAUSED;
            broadcast(Messages.get("game.pause.mecz-wstrzymany-przez-administratora"));
        }
    }

    public void resume() {
        if (this.state == GameState.PAUSED) {
            this.state = this.beforePause;
            broadcast(Messages.get("game.resume.mecz-wznowiony"));
        }
    }

    public void stopGame() {
        if (this.state == GameState.WAITING) {
            return;
        }
        this.state = GameState.ENDED;
        if (this.plugin.effects() != null) {
            this.plugin.effects().reset();
        }
        broadcast(Messages.get("game.stop-game.mecz-zostal-zatrzymany-przez-administratora"));
        if (this.plugin.matchLogger() != null) {
            this.plugin.matchLogger().finish(null);
        }
        scheduleReset(60L);
    }

    private void cancelScheduledReset() {
        if (this.resetTask != -1) {
            Bukkit.getScheduler().cancelTask(this.resetTask);
            this.resetTask = -1;
        }
    }

    private void scheduleReset(final long ticks) {
        cancelScheduledReset();
        this.resetTask = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            this.resetTask = -1;
            reset();
        }, ticks);
    }

    public void reset() {
        cancelScheduledReset();
        this.pendingJoins.clear();
        if (this.plugin.effects() != null) {
            this.plugin.effects().reset();
        }
        removeDragons();
        this.state = GameState.WAITING;
        this.elapsed = 0;
        this.countdown = -1;
        this.suddenDeathSpawned = false;
        this.manualCountdown = false;
        this.lastEliminatedTeam = null;
        if (this.plugin.borders() != null) {
            this.plugin.borders().resetMatch();
        }
        this.finalDead.clear();
        this.spectators.clear();
        this.disconnectedAt.clear();
        this.lastDamager.clear();
        this.lastDamageAt.clear();
        this.respawning.clear();
        this.protectedUntil.clear();
        this.magicMilkUntil.clear();
        this.trapCooldown.clear();
        this.eliminatedTeams.clear();
        this.stats.clear();
        this.loadouts.clear();
        this.participants.clear();
        if (this.active != null) {
            for (final TeamData teamData : this.active.getTeams().values()) {
                teamData.setBedAlive(true);
                teamData.resetUpgrades();
                teamData.getPlayers().clear();
            }
        }
        this.playerTeams.clear();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            player.getEnderChest().clear();
            if (this.plugin.spectators() != null) {
                this.plugin.spectators().clear(player);
            }
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.setDisplayName(ChatColor.GRAY + player.getName());
            player.setPlayerListName(ChatColor.GRAY + player.getName());
            this.plugin.lobby().send(player);
        }
        this.originalBlocks.clear();
        this.placedBlocks.clear();
        if (this.plugin.generators() != null) {
            this.plugin.generators().clear();
        }
        if (this.plugin.worlds() != null && this.active != null) {
            broadcastRaw(Messages.get("game.reset.przygotowywanie-swiezej-kopii-mapy"));
            this.plugin.worlds().resetRuntime(this.active);
        }
        if (this.plugin.npcs() != null) {
            this.plugin.npcs().respawn();
        }
    }

    public void prepareForConfiguration() {
        if (this.state == GameState.RUNNING || this.state == GameState.PAUSED) {
            return;
        }
        cancelScheduledReset();
        this.pendingJoins.clear();
        if (this.plugin.effects() != null) {
            this.plugin.effects().reset();
        }
        this.state = GameState.WAITING;
        this.countdown = -1;
        this.manualCountdown = false;
        if (this.plugin.borders() != null) {
            this.plugin.borders().resetMatch();
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.playerTeams.containsKey(player.getUniqueId()) || this.participants.contains(player.getUniqueId())) {
                this.plugin.lobby().send(player);
            }
        }
        if (this.active != null) {
            for (final TeamData teamData : this.active.getTeams().values()) {
                teamData.getPlayers().clear();
            }
        }
        this.playerTeams.clear();
        this.participants.clear();
        this.spectators.clear();
    }

    private void tick() {
        expireReconnects();
        if (this.plugin.spectators() != null) {
            this.plugin.spectators().tick();
        }
        if (this.state == GameState.PAUSED) {
            updateAll();
            return;
        }
        if (this.state == GameState.WAITING) {
            if (this.plugin.settings().autoStartWhenFull() && waitingPlayerCount() >= lobbyCapacity() && canStart()) {
                this.manualCountdown = false;
                startCountdown(this.plugin.settings().countdown());
            }
            updateAll();
            return;
        }
        if (this.state == GameState.COUNTDOWN) {
            if (!canStart() || (!this.manualCountdown && this.plugin.settings().autoStartWhenFull() && waitingPlayerCount() < lobbyCapacity())) {
                this.state = GameState.WAITING;
                this.countdown = -1;
                this.manualCountdown = false;
                if (this.plugin.effects() != null) {
                    this.plugin.effects().countdownCancelled();
                }
                broadcast(Messages.get("game.tick.odliczanie-zostalo-anulowane-poczekalnia-nie-jest"));
                updateAll();
                return;
            }
            this.countdown--;
            if (this.countdown <= 0) {
                this.manualCountdown = false;
                startGame();
                return;
            }
            if (this.countdown <= 5 || this.countdown % 10 == 0) {
                broadcast(Messages.get("game.tick.mecz-rozpocznie-sie-za", "COUNTDOWN", this.countdown));
            }
            updateAll();
            return;
        }
        if (this.state != GameState.RUNNING) {
            updateAll();
            return;
        }
        this.elapsed++;
        this.plugin.generators().tick();
        tickRespawns();
        applyTeamEffects();
        triggerTraps();
        checkVoid();
        if (this.state != GameState.RUNNING) {
            updateAll();
            return;
        }
        eventAnnouncements();
        if (this.elapsed >= this.plugin.settings().timer("suddenDeath") && !this.suddenDeathSpawned) {
            spawnSuddenDeath();
        }
        if (this.plugin.borders() != null) {
            this.plugin.borders().tickSuddenDeath();
        }
        checkEliminations();
        checkWinner();
        updateAll();
    }

    private void checkVoid() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.participants.contains(player.getUniqueId()) && !this.finalDead.contains(player.getUniqueId()) && !this.respawning.containsKey(player.getUniqueId()) && player.getLocation().getY() < this.plugin.settings().voidY()) {
                final Player killer = recentKiller(player);
                handleDeath(player, killer);
            }
        }
    }

    private void eventAnnouncements() {
        if (this.elapsed == this.plugin.settings().timer("diamond2")) {
            broadcast(Messages.get("game.event-announcements.generatory-diamentow-zostaly-ulepszone-do-poziomu"));
        }
        if (this.elapsed == this.plugin.settings().timer("emerald2")) {
            broadcast(Messages.get("game.event-announcements.generatory-szmaragdow-zostaly-ulepszone-do-poziomu"));
        }
        if (this.elapsed == this.plugin.settings().timer("diamond3")) {
            broadcast(Messages.get("game.event-announcements.generatory-diamentow-zostaly-ulepszone-do-poziomu-2"));
        }
        if (this.elapsed == this.plugin.settings().timer("emerald3")) {
            broadcast(Messages.get("game.event-announcements.generatory-szmaragdow-zostaly-ulepszone-do-poziomu-2"));
        }
        if (this.elapsed == this.plugin.settings().timer("bedsGone")) {
            for (final TeamColor color : TeamColor.values()) {
                if (this.active.team(color).isBedAlive()) {
                    this.active.team(color).setBedAlive(false);
                    if (this.plugin.effects() != null) {
                        this.plugin.effects().bedLost(color);
                    }
                }
            }
            broadcast(Messages.get("game.event-announcements.wszystkie-lozka-zostaly-zniszczone"));
        }
    }

    public String nextEventText() {
        if (this.suddenDeathSpawned) {
            final int r = this.plugin.borders() != null ? this.plugin.borders().secondsUntilClosed() : Math.max(0, this.plugin.settings().timer("end") - this.elapsed);
            return r > 0 ? Messages.get("game.next-event-text.zamkniecie-bordera-za", "VALUE1", (r / 60), "VALUE2", (r % 60 < 10 ? "0" : ""),
                    "VALUE3", (r % 60)) : Messages.get("game.next-event-text.walka-o-zwyciestwo");
        }
        final int[] values = { this.plugin.settings().timer("diamond2"), this.plugin.settings().timer("emerald2"), this.plugin.settings().timer("diamond3"),
                this.plugin.settings().timer("emerald3"), this.plugin.settings().timer("bedsGone"), this.plugin.settings().timer("suddenDeath") };
        final String[] names = { Messages.get("game.next-event-text.diamenty-ii"), Messages.get("game.next-event-text.szmaragdy-ii"), Messages.get("game.next-event-text.diamenty-iii"),
                
                Messages.get("game.next-event-text.szmaragdy-iii"), Messages.get("game.next-event-text.zniszczenie-lozek"), Messages.get("game.next-event-text.nagla-smierc") };
        for (int i = 0; i < values.length; i++) {
            if (this.elapsed < values[i]) {
                final int r = values[i] - this.elapsed;
                return Messages.get("game.next-event-text.za", "VALUE1", names[i], "VALUE2", (r / 60), "VALUE3", (r % 60 < 10 ? "0" : ""), "VALUE4", (r % 60));
            }
        }
        return Messages.get("game.next-event-text.walka-o-zwyciestwo");
    }

    public boolean skipNextPhase() {
        if (this.state != GameState.RUNNING) {
            return false;
        }
        final int[] values = { this.plugin.settings().timer("diamond2"), this.plugin.settings().timer("emerald2"), this.plugin.settings().timer("diamond3"),
                this.plugin.settings().timer("emerald3"), this.plugin.settings().timer("bedsGone"), this.plugin.settings().timer("suddenDeath"),
                        this.plugin.settings().timer("end") };
        for (final int v : values) {
            if (this.elapsed < v) {
                this.elapsed = v - 1;
                return true;
            }
        }
        return false;
    }

    public void spawnPlayer(final Player player, final boolean initial) {
        final TeamColor teamColor = teamOf(player.getUniqueId());
        if (teamColor == null || this.active == null) {
            return;
        }
        if (initial) {
            player.getEnderChest().clear();
        }
        this.spectators.remove(player.getUniqueId());
        this.respawning.remove(player.getUniqueId());
        this.plugin.spectators().clear(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setFireTicks(0);
        for (final PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        if (this.active.team(teamColor).getSpawn() != null) {
            player.teleport(this.active.team(teamColor).getSpawn());
        }
        giveBaseKit(player);
        this.plugin.shop().restorePermanent(player);
        applyFormatting(player);
        applyTeamEnchantments(player);
        if (!initial) {
            this.protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + this.plugin.settings().respawnProtectionSeconds() * 1000L);
            Messages.send(player, Messages.get("game.spawn-player.odrodziles-sie-s-ochrony", "VALUE1", this.plugin.settings().respawnProtectionSeconds()));
        }
        if (this.plugin.effects() != null) {
            if (initial) {
                this.plugin.effects().started(player);
            } else {
                this.plugin.effects().respawned(player);
            }
        }
    }

    private void giveBaseKit(final Player player) {
        player.getInventory().setItem(0, new ItemStack(Material.WOOD_SWORD));
    }

    public void applyFormatting(final Player player) {
        final TeamColor teamColor = teamOf(player.getUniqueId());
        final ChatColor col = teamColor == null ? ChatColor.GRAY : teamColor.chat();
        player.setDisplayName(col + player.getName());
        player.setPlayerListName(col + player.getName());
    }

    public void applyTeamEnchantments(final TeamColor teamColor) {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (teamOf(player.getUniqueId()) == teamColor) {
                applyTeamEnchantments(player);
            }
        }
    }

    public void applyTeamEnchantments(final Player player) {
        final TeamColor teamColor = teamOf(player.getUniqueId());
        if (teamColor == null || this.active == null) {
            return;
        }
        final TeamData teamData = this.active.team(teamColor);
        for (final ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.getType().name().endsWith("_SWORD") && teamData.getSharpnessLevel() > 0) {
                s.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
            }
        }
        for (final ItemStack s : player.getInventory().getArmorContents()) {
            if (s != null && s.getType() != Material.AIR && teamData.getProtectionLevel() > 0) {
                s.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, teamData.getProtectionLevel());
            }
        }
    }

    public void markDamage(final Player victim, final Player damager) {
        if (victim == null || damager == null || victim.equals(damager)) {
            return;
        }
        final TeamColor teamColor = teamOf(victim.getUniqueId()), b = teamOf(damager.getUniqueId());
        if (teamColor != null && teamColor == b && this.plugin.settings().friendlyFire()) {
            return;
        }
        if (isProtected(victim.getUniqueId())) {
            clearProtection(victim.getUniqueId());
        }
        this.lastDamager.put(victim.getUniqueId(), damager.getUniqueId());
        this.lastDamageAt.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    public Player recentKiller(final Player victim) {
        final Long when = this.lastDamageAt.get(victim.getUniqueId());
        if (when == null || System.currentTimeMillis() - when > this.plugin.settings().combatTagSeconds() * 1000L) {
            return null;
        }
        final UUID playerId = this.lastDamager.get(victim.getUniqueId());
        return playerId == null ? null : Bukkit.getPlayer(playerId);
    }

    public void handleDeath(final Player dead, final Player killer) {
        handleDeath(dead, killer, dead.getLocation().clone());
    }

    public void handleDeath(final Player dead, Player killer, final Location deathLocation) {
        if (this.state != GameState.RUNNING || isFinalDead(dead.getUniqueId()) || this.respawning.containsKey(dead.getUniqueId())) {
            return;
        }
        final TeamColor teamColor = teamOf(dead.getUniqueId());
        if (teamColor == null) {
            return;
        }
        stats(dead.getUniqueId()).death();
        loadout(dead.getUniqueId()).downgradeTools();
        if (killer == null) {
            killer = recentKiller(dead);
        }
        if (killer != null && killer.equals(dead)) {
            killer = null;
        }
        final boolean fin = !this.active.team(teamColor).isBedAlive();
        if (killer != null) {
            stats(killer.getUniqueId()).kill();
            if (fin) {
                stats(killer.getUniqueId()).finalKill();
            }
            transferResources(dead, killer);
        } else {
            dropResources(dead);
        }
        final TeamColor kt = killer == null ? null : teamOf(killer.getUniqueId());
        final String deathMsg = Messages.get("game.handle-death.zginal", "VICTIM", teamColor.chat() + dead.getName(), "KILLER", (killer == null ? "." : Messages.get("death.killer",
                
                "COLOR", kt == null ? "§7" : kt.chat(), "PLAYER", killer.getName())), "VALUE3", (fin ? Messages.get("death.final") : ""));
        broadcastRaw(deathMsg);
        dead.getInventory().clear();
        dead.getInventory().setArmorContents(new ItemStack[4]);
        this.lastDamager.remove(dead.getUniqueId());
        this.lastDamageAt.remove(dead.getUniqueId());
        this.protectedUntil.remove(dead.getUniqueId());
        if (fin) {
            this.finalDead.add(dead.getUniqueId());
            this.lastEliminatedTeam = teamColor;
            if (this.plugin.effects() != null) {
                this.plugin.effects().finalKill(deathLocation);
            }
            makeSpectator(dead);
            if (this.plugin.effects() != null) {
                this.plugin.effects().eliminated(dead);
            }
            checkEliminations();
            checkWinner();
            return;
        }
        this.respawning.put(dead.getUniqueId(), Math.max(1, this.plugin.settings().respawnSeconds()));
        waitForRespawn(dead);
    }

    private void waitForRespawn(final Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        if (this.active.getSpectator() != null) {
            player.teleport(this.active.getSpectator());
        }
        if (this.plugin.effects() != null) {
            this.plugin.effects().respawning(player);
        }
    }

    private void tickRespawns() {
        for (final Map.Entry<UUID, Integer> e : new HashMap<>(this.respawning).entrySet()) {
            final Player player = Bukkit.getPlayer(e.getKey());
            final int left = e.getValue() - 1;
            if (player == null) {
                continue;
            }
            if (left <= 0) {
                this.respawning.remove(e.getKey());
                spawnPlayer(player, false);
            } else {
                if (left <= 5) {
                    Messages.send(player, Messages.get("game.tick-respawns.odrodzisz-sie-za-s", "LEFT", left));
                }
                this.respawning.put(e.getKey(), left);
            }
        }
    }

    private boolean isResource(final Material material) {
        return material == Material.IRON_INGOT || material == Material.GOLD_INGOT || material == Material.DIAMOND || material == Material.EMERALD;
    }

    private void transferResources(final Player dead, final Player killer) {
        for (final ItemStack item : dead.getInventory().getContents()) {
            if (item != null && isResource(item.getType())) {
                killer.getInventory().addItem(item.clone());
                stats(killer.getUniqueId()).resource(item.getAmount());
            }
        }
    }

    private void dropResources(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item != null && isResource(item.getType())) {
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
            }
        }
    }

    public void resourceCollected(final Player player, final int amount) {
        if (this.participants.contains(player.getUniqueId())) {
            stats(player.getUniqueId()).resource(amount);
        }
    }

    public boolean joinObservation(final Player player) {
        if (!player.hasPermission("bedwars.admin")) {
            Messages.send(player, Messages.get("game.join-observation.brak-uprawnien"));
            return false;
        }
        if (!isLiveMatch()) {
            Messages.send(player, Messages.get("game.join-observation.obecnie-nie-trwa-zaden-mecz-do"));
            return false;
        }
        if (this.active == null || this.active.getSpectator() == null || this.active.getSpectator().getWorld() == null || (this.plugin.worlds() != null && !this.plugin.worlds().isRuntimeReady(this.active))) {
            Messages.send(player, Messages.get("game.join-observation.swiat-meczu-nie-jest-gotowy-do"));
            return false;
        }
        final UUID playerId = player.getUniqueId();
        if (this.participants.contains(playerId) && !this.finalDead.contains(playerId) && !this.spectators.contains(playerId)) {
            Messages.send(player, Messages.get("game.join-observation.jestes-zawodnikiem-tego-meczu-obserwacja-jest"));
            return false;
        }
        this.pendingJoins.remove(playerId);
        if (!this.spectators.contains(playerId)) {
            makeSpectator(player);
        }
        Messages.send(player, Messages.get("game.join-observation.tryb-obserwacji-wlaczony-kompas-zawodnicy-komparator"));
        this.plugin.spectators().openTeleporter(player);
        return true;
    }

    public void leaveObservation(final Player player) {
        if (!this.spectators.remove(player.getUniqueId())) {
            return;
        }
        if (this.plugin.effects() != null) {
            this.plugin.effects().clear(player);
        }
        this.plugin.spectators().clear(player);
        this.plugin.lobby().send(player);
        Messages.send(player, Messages.get("game.leave-observation.zakonczono-obserwacje-meczu"));
    }

    public void makeSpectator(final Player player) {
        if (this.plugin.effects() != null) {
            this.plugin.effects().clear(player);
        }
        this.spectators.add(player.getUniqueId());
        this.respawning.remove(player.getUniqueId());
        this.protectedUntil.remove(player.getUniqueId());
        this.plugin.spectators().make(player);
    }

    public boolean canRevive(final Player player) {
        if (player == null || !player.isOnline() || !isLiveMatch() || this.active == null) {
            return false;
        }
        final UUID playerId = player.getUniqueId();
        return this.participants.contains(playerId) && teamOf(playerId) != null && (isFinalDead(playerId) || isRespawning(playerId) || isSpectator(playerId));
    }

    public boolean revive(final Player player) {
        if (!canRevive(player)) {
            return false;
        }
        this.finalDead.remove(player.getUniqueId());
        this.spectators.remove(player.getUniqueId());
        this.disconnectedAt.remove(player.getUniqueId());
        this.eliminatedTeams.remove(teamOf(player.getUniqueId()));
        spawnPlayer(player, false);
        return true;
    }

    private void removeBedBlocks(final TeamColor color) {
        final Location location = this.active.team(color).getBed();
        if (location == null || location.getWorld() == null) {
            return;
        }
        final Block bed = location.getBlock();
        if (bed == null || bed.getType() != Material.BED_BLOCK) {
            return;
        }
        final Block other = otherBedHalf(bed);
        if (other != null) {
            recordChanged(other);
            other.setType(Material.AIR, false);
        }
        recordChanged(bed);
        bed.setType(Material.AIR, false);
    }

    private Block otherBedHalf(final Block bed) {
        if (bed == null || bed.getType() != Material.BED_BLOCK) {
            return null;
        }
        final Bed data = new Bed(Material.BED_BLOCK, (byte) (bed.getData() & 11));
        final Block other = bed.getRelative(data.isHeadOfBed() ? data.getFacing().getOppositeFace() : data.getFacing());
        if (other == null || other.getType() != Material.BED_BLOCK) {
            return null;
        }
        final Bed otherData = new Bed(Material.BED_BLOCK, (byte) (other.getData() & 11));
        return otherData.isHeadOfBed() != data.isHeadOfBed() && otherData.getFacing() == data.getFacing() ? other : null;
    }

    public void adminRemoveBed(final Player admin, final TeamColor color) {
        if (!canIntervene(admin) || color == null) {
            return;
        }
        final boolean alive = this.active.team(color).isBedAlive();
        this.active.team(color).setBedAlive(false);
        removeBedBlocks(color);
        if (alive) {
            if (this.plugin.effects() != null) {
                this.plugin.effects().bedDestroyed(color);
            }
            broadcast(Messages.get("game.admin-remove-bed.admin-usunal-lozko-druzyny", "PLAYER", admin.getName(), "VALUE2", color.chat(), "TEAM", color.display()));
        }
    }

    public boolean canIntervene(final Player player) {
        return this.plugin.spectators() != null && this.plugin.spectators().isIntervening(player);
    }

    public TeamColor bedTeam(final Location location) {
        if (this.active == null || location == null) {
            return null;
        }
        for (final TeamColor teamColor : TeamColor.values()) {
            final Location b = this.active.team(teamColor).getBed();
            if (Locations.sameBlock(location, b)) {
                return teamColor;
            }
            if (b != null && b.getWorld() != null) {
                final Block other = otherBedHalf(b.getBlock());
                if (other != null && Locations.sameBlock(location, other.getLocation())) {
                    return teamColor;
                }
            }
        }
        return null;
    }

    public boolean destroyBed(final Player breaker, final Location loc) {
        final TeamColor victim = bedTeam(loc);
        if (victim == null) {
            return false;
        }
        final TeamColor own = teamOf(breaker.getUniqueId());
        if (!isTeamInMatch(victim)) {
            return true;
        }
        if (victim == own) {
            Messages.send(breaker, Messages.get("game.destroy-bed.nie-mozesz-zniszczyc-wlasnego-lozka"));
            return true;
        }
        final TeamData teamData = this.active.team(victim);
        if (!teamData.isBedAlive()) {
            return true;
        }
        teamData.setBedAlive(false);
        stats(breaker.getUniqueId()).bed();
        broadcastRaw(Messages.get("game.destroy-bed.zniszczenie-lozka-lozko-zniszczyl", "VALUE1", victim.chat(), "TEAM", victim.display(),
                "VALUE3", (own == null ? "§7" : own.chat()), "PLAYER", breaker.getName()));
        if (this.plugin.effects() != null) {
            this.plugin.effects().bedDestroyed(victim);
        }
        for (final UUID playerId : teamData.getPlayers()) {
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                ConfiguredSound.play(this.plugin, player, "bed-lost", Sound.ENDERMAN_TELEPORT, 1F, 1F);
            }
        }
        ConfiguredSound.play(this.plugin, breaker, "bed-broken", Sound.LEVEL_UP, 1F, 1F);
        return false;
    }

    public boolean canBreak(final Player player, final Block block) {
        if (canIntervene(player)) {
            return block.getWorld().equals(player.getWorld());
        }
        if (isSpectator(player.getUniqueId())) {
            return false;
        }
        if (this.state != GameState.RUNNING) {
            return player.hasPermission("bedwars.admin") && this.plugin.settings().allowAdminBuild();
        }
        final TeamColor teamColor = bedTeam(block.getLocation());
        if (teamColor != null) {
            return teamColor != teamOf(player.getUniqueId());
        }
        return this.placedBlocks.contains(Locations.key(block.getLocation()));
    }

    public boolean canPlace(final Player player, final Block block) {
        if (canIntervene(player)) {
            return block.getWorld().equals(player.getWorld());
        }
        if (isSpectator(player.getUniqueId())) {
            return false;
        }
        if (this.state != GameState.RUNNING) {
            return player.hasPermission("bedwars.admin") && this.plugin.settings().allowAdminBuild();
        }
        if (block.getLocation().getY() > this.plugin.settings().buildHeight()) {
            return false;
        }
        return !isNoBuildLocation(block.getLocation());
    }

    public boolean isNoBuildLocation(final Location location) {
        if (this.active == null || location == null) {
            return false;
        }
        for (final TeamColor teamColor : TeamColor.values()) {
            if (this.active.team(teamColor).isInsideProtectedRegion(location)) {
                return true;
            }
        }
        return isGeneratorProtectedLocation(location);
    }

    public boolean isGeneratorProtectedLocation(final Location location) {
        if (this.active == null || location == null || location.getWorld() == null) {
            return false;
        }
        final int radius = Math.max(0, this.plugin.settings().generatorProtectionSize() / 2);
        for (final GeneratorType type : new GeneratorType[] { GeneratorType.DIAMOND, GeneratorType.EMERALD }) {
            final List<Location> list = this.active.getGenerators().get(type);
            if (list == null) {
                continue;
            }
            for (final Location generator : list) {
                if (generator == null || generator.getWorld() == null) {
                    continue;
                }
                if (!generator.getWorld().getName().equals(location.getWorld().getName())) {
                    continue;
                }
                if (Math.abs(location.getBlockX() - generator.getBlockX()) <= radius && Math.abs(location.getBlockZ() - generator.getBlockZ()) <= radius) {
                    return true;
                }
            }
        }
        return false;
    }

    public void recordPlaced(final Block block) {
        final String k = Locations.key(block.getLocation());
        if (!this.originalBlocks.containsKey(k)) {
            this.originalBlocks.put(k, block.getState());
        }
        this.placedBlocks.add(k);
    }

    public void recordChanged(final Block block) {
        final String k = Locations.key(block.getLocation());
        if (!this.originalBlocks.containsKey(k)) {
            this.originalBlocks.put(k, block.getState());
        }
        if (this.state == GameState.RUNNING && block.getType() == Material.AIR && isNoBuildLocation(block.getLocation())) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR);
                }
                removePlaced(block);
            }, 1L);
        }
    }

    public void removePlaced(final Block block) {
        this.placedBlocks.remove(Locations.key(block.getLocation()));
    }

    private void resetWorld() {
        final List<BlockState> states = new ArrayList<>(this.originalBlocks.values());
        Collections.reverse(states);
        for (final BlockState s : states) {
            try {
                s.update(true, false);
            } catch (final Exception ignored) {
            }
        }
        this.originalBlocks.clear();
        this.placedBlocks.clear();
    }

    private void applyTeamEffects() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final TeamColor teamColor = teamOf(player.getUniqueId());
            if (teamColor == null || this.finalDead.contains(player.getUniqueId()) || this.spectators.contains(player.getUniqueId())) {
                continue;
            }
            final TeamData teamData = this.active.team(teamColor);
            if (teamData.getHasteLevel() > 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 40, teamData.getHasteLevel() - 1), true);
            }
            if (teamData.hasHealPool() && teamData.getSpawn() != null && player.getWorld().equals(teamData.getSpawn().getWorld()) && player.getLocation().distanceSquared(teamData.getSpawn()) <= this.plugin.settings().baseRadius() * this.plugin.settings().baseRadius()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0), true);
            }
        }
    }

    private void triggerTraps() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.finalDead.contains(player.getUniqueId()) || this.spectators.contains(player.getUniqueId())) {
                continue;
            }
            final TeamColor own = teamOf(player.getUniqueId());
            if (own == null) {
                continue;
            }
            for (final TeamColor enemy : TeamColor.values()) {
                if (enemy == own) {
                    continue;
                }
                final TeamData teamData = this.active.team(enemy);
                if (teamData.getTraps().isEmpty() || teamData.getSpawn() == null) {
                    continue;
                }
                if (!player.getWorld().equals(teamData.getSpawn().getWorld()) || player.getLocation().distanceSquared(teamData.getSpawn()) > this.plugin.settings().baseRadius() * this.plugin.settings().baseRadius()) {
                    continue;
                }
                final Long cd = this.trapCooldown.get(player.getUniqueId());
                if (cd != null && System.currentTimeMillis() - cd < 1000L * Options.integer(this.plugin, "traps.cooldownSeconds", 10, 1, 120)) {
                    continue;
                }
                if (hasMagicMilk(player.getUniqueId())) {
                    continue;
                }
                final TrapType trap = teamData.getTraps().remove(0);
                this.trapCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                activateTrap(enemy, player, trap);
                break;
            }
        }
    }

    private void activateTrap(final TeamColor team, final Player player, final TrapType trap) {
        for (final UUID u : this.active.team(team).getPlayers()) {
            final Player p = Bukkit.getPlayer(u);
            if (p != null) {
                Messages.send(p, Messages.get("game.activate-trap.uruchomiono-pulapke-uruchomiona-przez", "TEAM", trap.display(), "PLAYER", player.getName()));
                ConfiguredSound.play(this.plugin, p, "trap", Sound.ENDERMAN_TELEPORT, 1F, 1F);
            }
        }
        if (trap == TrapType.ITS_A_TRAP) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * Options.integer(this.plugin, "traps.blindness.seconds", 8, 1, 120), 0), true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * Options.integer(this.plugin, "traps.blindness.seconds", 8, 1, 120), 0), true);
        } else if (trap == TrapType.MINER_FATIGUE) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * Options.integer(this.plugin, "traps.fatigue.seconds", 10, 1, 120), 0), true);
        } else if (trap == TrapType.ALARM) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        } else if (trap == TrapType.COUNTER_OFFENSIVE) {
            for (final UUID u : this.active.team(team).getPlayers()) {
                final Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * Options.integer(this.plugin, "traps.counter.seconds", 15, 1, 120), 0), true);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * Options.integer(this.plugin, "traps.counter.seconds", 15, 1, 120), 1), true);
                }
            }
        }
    }

    private void spawnSuddenDeath() {
        this.suddenDeathSpawned = true;
        for (final TeamColor color : TeamColor.values()) {
            if (this.active.team(color).isBedAlive()) {
                this.active.team(color).setBedAlive(false);
                if (this.plugin.effects() != null) {
                    this.plugin.effects().bedLost(color);
                }
            }
        }
        if (this.plugin.borders() != null) {
            this.plugin.borders().beginSuddenDeath(this.active, this.elapsed, this.plugin.settings().timer("end"));
        }
        broadcast(Messages.get("game.spawn-sudden-death.nagla-smierc-smoki-i-kurczacy-sie"));
        expireReconnects();
        if (this.state != GameState.RUNNING) {
            return;
        }
        final Location center = this.active.getSpectator() != null ? this.active.getSpectator() : this.active.getLobby();
        if (center == null) {
            return;
        }
        for (final TeamColor teamColor : TeamColor.values()) {
            if (teamAlive(teamColor)) {
                final int n = this.active.team(teamColor).hasDragonBuff() ? Options.integer(this.plugin, "suddenDeath.dragonsWithBuff", 2,
                        0, 8) : Options.integer(this.plugin, "suddenDeath.dragonsPerTeam", 1, 0, 8);
                for (int x = 0; x < n; x++) {
                    final Location location = center.clone().add((x * 4) - 2, 12, 0);
                    final EnderDragon d = this.plugin.worldRules() == null ? location.getWorld().spawn(location, EnderDragon.class) : this.plugin.worldRules().spawnGameMob(location,
                            
                            EnderDragon.class);
                    d.setCustomName(Messages.get("mobs.dragon.name", "COLOR", teamColor.chat(), "TEAM", teamColor.display()));
                    d.setCustomNameVisible(true);
                    this.dragons.add(d);
                }
            }
        }
    }

    private void removeDragons() {
        for (final EnderDragon d : this.dragons) {
            if (d != null && !d.isDead()) {
                d.remove();
            }
        }
        this.dragons.clear();
    }

    private void checkEliminations() {
        if (this.active == null) {
            return;
        }
        for (final TeamColor teamColor : TeamColor.values()) {
            if (isTeamInMatch(teamColor) && !this.eliminatedTeams.contains(teamColor) && !this.active.team(teamColor).isBedAlive() && aliveCount(teamColor) == 0) {
                this.eliminatedTeams.add(teamColor);
                broadcastRaw(Messages.get("game.check-eliminations.druzyna-wyeliminowana-zostali-wyeliminowani", "VALUE1", teamColor.chat(),
                        "TEAM", teamColor.display()));
            }
        }
    }

    private void checkWinner() {
        if (this.state != GameState.RUNNING) {
            return;
        }
        TeamColor alive = null;
        int teams = 0;
        for (final TeamColor teamColor : TeamColor.values()) {
            if (teamAlive(teamColor)) {
                alive = teamColor;
                teams++;
            }
        }
        if (teams == 0) {
            alive = this.lastEliminatedTeam;
        }
        if (teams <= 1 && alive != null) {
            this.state = GameState.ENDED;
            for (final UUID playerId : this.active.team(alive).getPlayers()) {
                if (this.participants.contains(playerId)) {
                    stats(playerId).win();
                }
            }
            broadcastRaw(Messages.get("game.check-winner.wygrywaja-mecz", "VALUE1", Messages.get("titles.victory.text"), "VALUE2", alive.chat(),
                    "TEAM", alive.display().toUpperCase()));
            broadcastTopPlayers();
            if (this.plugin.effects() != null) {
                this.plugin.effects().victory(alive);
            }
            if (this.plugin.matchLogger() != null) {
                this.plugin.matchLogger().finish(alive);
            }
            scheduleReset(20L * Options.integer(this.plugin, "game.returnToLobbySeconds", 10, 1, 120));
        }
    }

    private void broadcastTopPlayers() {
        final List<UUID> list = new ArrayList<>(this.participants);
        list.sort((a, b) -> Integer.compare(stats(b).getFinalKills(), stats(a).getFinalKills()));
        broadcastRaw(Messages.get("game.broadcast-top-players.top-3-finalne-zabojstwa"));
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            final UUID playerId = list.get(i);
            final TeamColor teamColor = teamOf(playerId);
            broadcastRaw(Messages.get("game.broadcast-top-players.text", "VALUE1", (i + 1), "VALUE2", (teamColor == null ? "§7" : teamColor.chat()),
                    "VALUE3", name(playerId), "VALUE4", stats(playerId).getFinalKills()));
        }
    }

    public void onQuit(final Player player) {
        this.pendingJoins.remove(player.getUniqueId());
        if (this.plugin.effects() != null) {
            this.plugin.effects().clear(player);
        }
        if (this.spectators.remove(player.getUniqueId()) && this.plugin.spectators() != null) {
            this.plugin.spectators().clear(player);
        }
        if (isLiveMatch() && this.participants.contains(player.getUniqueId()) && !this.finalDead.contains(player.getUniqueId())) {
            this.disconnectedAt.put(player.getUniqueId(), System.currentTimeMillis());
            if (this.state == GameState.RUNNING && this.suddenDeathSpawned) {
                expireReconnects();
            }
        } else if ((this.state == GameState.WAITING || this.state == GameState.COUNTDOWN) && this.playerTeams.containsKey(player.getUniqueId())) {
            removeFromTeam(player.getUniqueId());
        }
    }

    public void onJoin(final Player player) {
        if (this.disconnectedAt.remove(player.getUniqueId()) != null && isLiveMatch() && !isFinalDead(player.getUniqueId())) {
            Messages.send(player, Messages.get("game.on-join.wrociles-do-trwajacego-meczu"));
            if (isRespawning(player.getUniqueId())) {
                waitForRespawn(player);
            } else {
                spawnPlayer(player, false);
            }
        } else {
            player.getEnderChest().clear();
            this.plugin.lobby().send(player);
        }
        if (this.plugin.spectators() != null) {
            this.plugin.spectators().refreshVisibility(player);
        }
    }

    private boolean isLiveMatch() {
        return this.state == GameState.RUNNING || (this.state == GameState.PAUSED && this.beforePause == GameState.RUNNING);
    }

    private void expireReconnects() {
        if (this.state != GameState.RUNNING) {
            return;
        }
        final long now = System.currentTimeMillis();
        for (final Map.Entry<UUID, Long> e : new LinkedHashMap<>(this.disconnectedAt).entrySet()) {
            if (this.state != GameState.RUNNING) {
                break;
            }
            if (this.suddenDeathSpawned || now - e.getValue() > this.plugin.settings().reconnectSeconds() * 1000L) {
                this.disconnectedAt.remove(e.getKey());
                this.finalDead.add(e.getKey());
                final TeamColor teamColor = teamOf(e.getKey());
                this.lastEliminatedTeam = teamColor;
                broadcastRaw(Messages.get("game.expire-reconnects.finalne-zabojstwo", "VALUE1", (teamColor == null ? "§7" : teamColor.chat()) + name(e.getKey()) + (this.suddenDeathSpawned ? Messages.get("game.expire-reconnects.zostal-wyeliminowany-za-rozlaczenie-podczas-naglej") : Messages.get("game.expire-reconnects.zostal-wyeliminowany-po-przekroczeniu-czasu-na"))));
                checkEliminations();
                checkWinner();
            }
        }
    }

    private String name(final UUID playerId) {
        final OfflinePlayer p = Bukkit.getOfflinePlayer(playerId);
        return p == null || p.getName() == null ? playerId.toString() : p.getName();
    }

    private void updateAll() {
        if (this.plugin.scoreboards() != null) {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                this.plugin.scoreboards().update(player);
            }
        }
    }

    public void broadcast(final String s) {
        broadcastRaw(ChatColor.translateAlternateColorCodes('&', s));
    }

    public void broadcastRaw(final String s) {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final UUID playerId = player.getUniqueId();
            if (this.playerTeams.containsKey(playerId) || this.participants.contains(playerId) || this.spectators.contains(playerId) || player.hasPermission("bedwars.admin")) {
                Messages.send(player, s);
            }
        }
    }
}
