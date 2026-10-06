package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.MatchViewText;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.TeamColor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ScoreboardManager {

    private final BedWarsPlugin plugin;

    private final TabManager tabs;

    public ScoreboardManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.tabs = new TabManager(plugin);
    }

    public void update(final Player viewer) {
        final GameManager game = this.plugin.game();
        final Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        if (game.teamOf(viewer.getUniqueId()) == null && !game.isSpectator(viewer.getUniqueId())) {
            viewer.setScoreboard(board);
            this.tabs.update(viewer);
            return;
        }
        Player subject = this.plugin.spectators() == null ? viewer : this.plugin.spectators().target(viewer);
        if (subject == null) {
            subject = viewer;
        }
        final Object[] variables = new MatchViewText(this.plugin).variables(viewer, subject);
        if (Options.enabled(this.plugin, "scoreboard.enabled", true)) {
            final Objective sidebar = board.registerNewObjective("bw", "dummy");
            sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
            sidebar.setDisplayName(MatchViewText.limit(Messages.get("scoreboard.title", variables), 32));
            final String state = game.state() == GameState.ENDED ? "running" : game.state().name().toLowerCase(Locale.ROOT);
            final Set<String> used = new HashSet<>();
            int score = 15;
            for (final String line : Messages.lines("scoreboard.layouts." + state, variables)) {
                if (score < 1) {
                    break;
                }
                String text = MatchViewText.limit(line, 40);
                if (used.contains(text)) {
                    text = MatchViewText.limit(text, 38) + ChatColor.values()[score];
                }
                used.add(text);
                sidebar.getScore(text).setScore(score--);
            }
        }
        for (final TeamColor color : TeamColor.values()) {
            final Team team = board.registerNewTeam("t" + color.name());
            team.setPrefix(MatchViewText.limit(Messages.get("scoreboard.name-prefix", "COLOR", color.chat(), "SHORT", color.shortName()), 16));
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (game.teamOf(player.getUniqueId()) == color) {
                    this.addTeamMemberCompat(team, player);
                }
            }
        }
        if (Options.enabled(this.plugin, "scoreboard.healthEnabled", true)) {
            final Objective health = board.registerNewObjective("hp", "dummy");
            health.setDisplaySlot(DisplaySlot.BELOW_NAME);
            health.setDisplayName(MatchViewText.limit(Messages.get("scoreboard.health"), 32));
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (game.teamOf(player.getUniqueId()) != null || game.isSpectator(player.getUniqueId())) {
                    health.getScore(player.getName()).setScore(Math.max(0, (int) Math.ceil(player.getHealth())));
                }
            }
        }
        viewer.setScoreboard(board);
        this.tabs.update(viewer);
    }

    private void addTeamMemberCompat(final Team team, final Player player) {
        if (team == null || player == null) {
            return;
        }
        try {
            final Method m = team.getClass().getMethod("addEntry", String.class);
            m.invoke(team, player.getName());
            return;
        } catch (final Throwable ignored) {
        }
        try {
            for (final Method m : team.getClass().getMethods()) {
                if (!"addPlayer".equals(m.getName()) || m.getParameterTypes().length != 1) {
                    continue;
                }
                final Class<?> type = m.getParameterTypes()[0];
                final Object argument;
                if (type == String.class) {
                    argument = player.getName();
                } else if (type.isInstance(player)) {
                    argument = player;
                } else {
                    continue;
                }
                m.invoke(team, argument);
                return;
            }
        } catch (final Throwable ignored) {
        }
    }
}
