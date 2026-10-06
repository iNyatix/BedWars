package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.util.ConfiguredSound;
import space.nyatix.bedwars.util.TitleSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MatchEffectsManager implements Listener {

    private static final String FIREWORK_TAG = "bw-victory-firework";

    private final BedWarsPlugin plugin;

    private final TitleSender titles;

    private final Map<UUID, Animation> animations = new HashMap<>();

    private final List<Firework> fireworks = new ArrayList<>();

    private final int task;

    private Arena celebrationArena;

    private TeamColor winner;

    private int celebrationAge;

    private int lobbyScanAge;

    public MatchEffectsManager(final BedWarsPlugin plugin) {
        this(plugin, new TitleSender(plugin));
    }

    public MatchEffectsManager(final BedWarsPlugin plugin, final TitleSender titles) {
        this.plugin = plugin;
        this.titles = titles;
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            tick();
        }, 4L, 4L);
    }

    public void waiting(final Player player) {
        if (!waitingAllowed(player)) {
            return;
        }
        final Animation current = this.animations.get(player.getUniqueId());
        if (current == null) {
            show(player, Kind.WAITING, 0, null);
        }
    }

    private boolean waitingAllowed(final Player player) {
        if (player == null || !player.isOnline() || player.isDead() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        final GameManager game = this.plugin.game();
        if (game == null) {
            return false;
        }
        final UUID playerId = player.getUniqueId();
        if (game.isSpectator(playerId) || game.isRespawning(playerId)) {
            return false;
        }
        final boolean assigned = game.teamOf(playerId) != null;
        if (assigned && game.state() == GameState.WAITING && !game.isFinalDead(playerId)) {
            final Location lobby = game.active() == null ? null : game.active().getLobby();
            return lobby != null && player.getWorld().equals(lobby.getWorld());
        }
        if (assigned && !game.isFinalDead(playerId)) {
            return false;
        }
        final LobbyManager lobby = this.plugin.lobby();
        if (lobby == null || !lobby.isJoinItem(player.getInventory().getItem(4))) {
            return false;
        }
        final Location location = lobby.getLobby();
        return location == null || player.getWorld().equals(location.getWorld());
    }

    public void countdown(final Player player) {
        show(player, Kind.COUNTDOWN, 0, null);
    }

    public void respawning(final Player player) {
        show(player, Kind.RESPAWN, 0, null);
    }

    public void started(final Player player) {
        show(player, Kind.START, 40, null);
        ConfiguredSound.play(this.plugin, player, "start", Sound.LEVEL_UP, .7F, 1.3F);
    }

    public void respawned(final Player player) {
        final boolean lostBed = bedMissing(this.plugin.game().teamOf(player.getUniqueId()));
        show(player, lostBed ? Kind.BED_LOST : Kind.RESPAWNED, lostBed ? 80 : 40, null);
        ConfiguredSound.play(this.plugin, player, "respawn", Sound.ORB_PICKUP, .7F, 1.2F);
    }

    public void eliminated(final Player player) {
        show(player, Kind.ELIMINATED, 70, null);
    }

    public void bedDestroyed(final TeamColor victim) {
        bedLost(victim);
        if (!this.plugin.settings().bedBreakDragonSoundEnabled() || !this.plugin.settings().effectSoundsEnabled()) {
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final UUID playerId = player.getUniqueId();
            if (this.plugin.game().participants().contains(playerId) || this.plugin.game().isSpectator(playerId)) {
                ConfiguredSound.play(this.plugin, player, "bed-destroyed", Sound.ENDERDRAGON_DEATH, 1F, 1F);
            }
        }
    }

    public void bedLost(final TeamColor victim) {
        final GameState state = this.plugin.game().state();
        if (victim == null || (state != GameState.RUNNING && state != GameState.PAUSED) || !bedMissing(victim)) {
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final UUID playerId = player.getUniqueId();
            if (this.plugin.game().teamOf(playerId) != victim || !this.plugin.game().participants().contains(playerId) || this.plugin.game().isSpectator(playerId) || this.plugin.game().isFinalDead(playerId)) {
                continue;
            }
            if (this.plugin.game().isRespawning(playerId)) {
                final Animation current = this.animations.get(playerId);
                if (this.plugin.settings().titlesEnabled() && current != null && current.kind == Kind.RESPAWN) {
                    render(player, current);
                }
            } else {
                show(player, Kind.BED_LOST, 80, null);
            }
        }
    }

    private boolean bedMissing(final TeamColor team) {
        final Arena arena = this.plugin.game().active();
        return team != null && arena != null && !arena.team(team).isBedAlive();
    }

    public void finalKill(final Location deathLocation) {
        if (!this.plugin.settings().finalKillLightningEnabled() || deathLocation == null || deathLocation.getWorld() == null) {
            return;
        }
        final Location effect = deathLocation.clone();
        if (effect.getY() < 0D) {
            effect.setY(0D);
        }
        effect.getWorld().strikeLightningEffect(effect);
    }

    public void countdownCancelled() {
        for (final UUID playerId : new ArrayList<>(this.animations.keySet())) {
            if (this.animations.get(playerId).kind == Kind.COUNTDOWN) {
                final Player player = Bukkit.getPlayer(playerId);
                clear(player);
                waiting(player);
            }
        }
    }

    public void victory(final TeamColor winningTeam) {
        reset();
        this.winner = winningTeam;
        this.celebrationArena = this.plugin.game().active();
        this.celebrationAge = 0;
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final UUID playerId = player.getUniqueId();
            if (!this.plugin.game().participants().contains(playerId) && !this.plugin.game().isSpectator(playerId)) {
                continue;
            }
            final boolean won = this.plugin.game().teamOf(playerId) == winningTeam;
            show(player, won ? Kind.VICTORY : this.plugin.game().participants().contains(playerId) ? Kind.DEFEAT : Kind.FINISH, 200, winningTeam);
            if (won) {
                ConfiguredSound.play(this.plugin, player, "victory", Sound.LEVEL_UP, .7F, 1.0F);
            }
        }
        launchFireworks();
    }

    private void show(final Player player, final Kind kind, final int duration, final TeamColor winningTeam) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!this.plugin.settings().titlesEnabled() || !Options.enabled(this.plugin, "animations." + kind
                .name().toLowerCase(Locale.ROOT).replace('_', '-') + ".enabled", true)) {
            clear(player);
            return;
        }
        final String path = "animations." + kind.name().toLowerCase(Locale.ROOT).replace('_', '-');
        final Animation animation = new Animation(kind, duration <= 0 ? 0 : Options.integer(this.plugin, path + ".durationTicks", duration, 4, 1200), winningTeam);
        this.animations.put(player.getUniqueId(), animation);
        render(player, animation);
    }

    private void tick() {
        this.lobbyScanAge += 4;
        if (this.lobbyScanAge >= 20) {
            this.lobbyScanAge = 0;
            if (this.plugin.settings().titlesEnabled()) {
                for (final Player player : Bukkit.getOnlinePlayers()) {
                    waiting(player);
                }
            }
        }
        for (final UUID playerId : new ArrayList<>(this.animations.keySet())) {
            final Player player = Bukkit.getPlayer(playerId);
            final Animation animation = this.animations.get(playerId);
            if (player == null || !player.isOnline()) {
                this.animations.remove(playerId);
                continue;
            }
            if (!this.plugin.settings().titlesEnabled() || !valid(playerId, animation)) {
                clear(player);
                continue;
            }
            animation.age += 4;
            if (animation.duration > 0 && animation.age >= animation.duration) {
                clear(player);
                continue;
            }
            render(player, animation);
        }
        if (this.winner != null) {
            this.celebrationAge += 4;
            if (this.plugin.game().state() != GameState.ENDED || this.plugin.game().active() != this.celebrationArena || this.celebrationAge >= Options.integer(this.plugin,
                    "fireworks.durationTicks", 200, 4, 1200)) {
                clearFireworks();
            } else if (this.celebrationAge % (4 * Math.max(1, Options.integer(this.plugin, "fireworks.intervalTicks", 20, 4, 1200) / 4)) == 0) {
                launchFireworks();
            }
        }
        for (final Iterator<Firework> it = this.fireworks.iterator(); it.hasNext(); ) {
            final Firework firework = it.next();
            if (firework.isDead() || !firework.isValid()) {
                it.remove();
            }
        }
    }

    private boolean valid(final UUID playerId, final Animation animation) {
        final GameState state = this.plugin.game().state();
        switch(animation.kind) {
            case WAITING:
                return waitingAllowed(Bukkit.getPlayer(playerId));
            case COUNTDOWN:
                return this.plugin.game().teamOf(playerId) != null && this.plugin.game().isStarting();
            case RESPAWN:
                return this.plugin.game().isRespawning(playerId) && (state == GameState.RUNNING || state == GameState.PAUSED);
            case BED_LOST:
                return (state == GameState.RUNNING || state == GameState.PAUSED) && bedMissing(this.plugin.game().teamOf(playerId)) && !this.plugin.game().isSpectator(playerId) && !this.plugin.game().isFinalDead(playerId) && !this.plugin.game().isRespawning(playerId);
            case START:
            case RESPAWNED:
                return state == GameState.RUNNING && this.plugin.game().teamOf(playerId) != null && !this.plugin.game().isFinalDead(playerId) && !this.plugin.game().isRespawning(playerId);
            case ELIMINATED:
                return (state == GameState.RUNNING || state == GameState.PAUSED) && this.plugin.game().isFinalDead(playerId);
            default:
                return state == GameState.ENDED;
        }
    }

    private void render(final Player player, final Animation animation) {
        final GameManager game = this.plugin.game();
        final TeamColor team = game.teamOf(player.getUniqueId());
        final String teamName = team == null ? "" : team.chat() + team.display();
        String key = animation.kind.name().toLowerCase(Locale.ROOT).replace('_', '-');
        int seconds = 0;
        String subtitleKey = null;
        if (animation.kind == Kind.WAITING) {
            if (team != null && game.state() == GameState.WAITING) {
                subtitleKey = "titles.waiting.joined-subtitle";
            } else if (game.state() == GameState.RUNNING || game.state() == GameState.PAUSED || game.state() == GameState.ENDED) {
                subtitleKey = "titles.waiting.next-game-subtitle";
            }
        } else if (animation.kind == Kind.COUNTDOWN) {
            seconds = game.countdown();
            key = game.isPaused() ? "countdown-paused" : seconds <= 3 ? "countdown-last" : "countdown";
            if (!game.isPaused() && animation.lastSecond != seconds) {
                if (seconds <= 5 || seconds % 10 == 0) {
                    ConfiguredSound.play(this.plugin, player, seconds <= 3 ? "countdown-last" : "countdown", Sound.NOTE_PLING, .7F, seconds <= 3 ? 1.5F : 1F);
                }
                animation.lastSecond = seconds;
            }
        } else if (animation.kind == Kind.RESPAWN) {
            seconds = game.respawnSecondsLeft(player.getUniqueId());
            if (bedMissing(team)) {
                key += "-bed-lost";
            }
            if (game.isPaused()) {
                key += "-paused";
            }
        }
        final int interval = Options.integer(this.plugin, "animations." + key + ".frameTicks", 20, 4, 1200);
        final Object[] variables = { "PLAYER", player.getName(), "TEAM", teamName, "SECONDS", seconds, "WINNER", animation.winner == null ? "" : animation.winner.chat() + animation.winner.display(),
                "TEXT", Messages.get("titles.victory.text"), "DOTS", Messages.frame("titles.waiting.dots", animation.age, Options.integer(this.plugin, "animations.waiting.dotsTicks",
                12, 4, 1200)) };
        final String title = Messages.frame("titles." + key + ".title", animation.age, interval, variables);
        final String subtitle = Messages.frame(subtitleKey == null ? "titles." + key + ".subtitle" : subtitleKey, animation.age, interval, variables);
        if (animation.lastTitle == null) {
            this.titles.send(player, title, subtitle, 0, 40, 0);
        } else {
            if (title.equals(animation.lastTitle) && subtitle.equals(animation.lastSubtitle) && animation.age - animation.lastSent < 20) {
                return;
            }
            this.titles.update(player, title, subtitle);
        }
        animation.lastTitle = title;
        animation.lastSubtitle = subtitle;
        animation.lastSent = animation.age;
    }

    private void launchFireworks() {
        if (!this.plugin.settings().victoryFireworksEnabled() || this.celebrationArena == null || this.winner == null) {
            return;
        }
        final Location arenaLocation = this.celebrationArena.getSpectator();
        if (arenaLocation == null || arenaLocation.getWorld() == null) {
            return;
        }
        for (final UUID playerId : this.celebrationArena.team(this.winner).getPlayers()) {
            final Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || !this.plugin.game().participants().contains(playerId) || !player.getWorld().equals(arenaLocation.getWorld())) {
                continue;
            }
            final double angle = Math.PI * this.celebrationAge / 40D;
            final Location location = player.getLocation().clone().add(Math.cos(angle) * Options.number(this.plugin, "fireworks.radius", 1.5D,
                    0D, 10D), 1D, Math.sin(angle) * Options.number(this.plugin, "fireworks.radius", 1.5D, 0D, 10D));
            final Firework firework = location.getWorld().spawn(location, Firework.class);
            this.fireworks.add(firework);
            firework.setMetadata(FIREWORK_TAG, new FixedMetadataValue(this.plugin, true));
            final FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(this.celebrationAge % 40 == 0 ? FireworkEffect.Type.STAR : FireworkEffect.Type.BALL).withColor(this.winner.dye().getColor(), Color.YELLOW).withFade(Color.WHITE).trail(true).flicker(true)
                    .build());
            meta.setPower(Options.integer(this.plugin, "fireworks.power", 0, 0, 4));
            firework.setFireworkMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void fireworkDamage(final EntityDamageByEntityEvent event) {
        if (event.getDamager().hasMetadata(FIREWORK_TAG)) {
            event.setCancelled(true);
        }
    }

    public void clear(final Player player) {
        if (player != null && this.animations.remove(player.getUniqueId()) != null) {
            this.titles.clear(player);
        }
    }

    private void clearFireworks() {
        this.winner = null;
        this.celebrationArena = null;
        for (final Firework firework : this.fireworks) {
            if (!firework.isDead()) {
                firework.remove();
            }
        }
        this.fireworks.clear();
    }

    public void reset() {
        for (final UUID playerId : new ArrayList<>(this.animations.keySet())) {
            clear(Bukkit.getPlayer(playerId));
        }
        this.animations.clear();
        clearFireworks();
    }

    public void shutdown() {
        reset();
        Bukkit.getScheduler().cancelTask(this.task);
    }

    private enum Kind {

        WAITING,
        COUNTDOWN,
        RESPAWN,
        START,
        RESPAWNED,
        BED_LOST,
        ELIMINATED,
        VICTORY,
        DEFEAT,
        FINISH
    }

    private static class Animation {

        final Kind kind;

        final int duration;

        final TeamColor winner;

        int age, lastSent, lastSecond = -1;

        String lastTitle, lastSubtitle;

        Animation(final Kind kind, final int duration, final TeamColor winner) {
            this.kind = kind;
            this.duration = duration;
            this.winner = winner;
        }
    }
}
