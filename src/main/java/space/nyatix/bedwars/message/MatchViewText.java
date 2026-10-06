package space.nyatix.bedwars.message;

import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.game.GameManager;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.PlayerStats;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.model.TeamData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class MatchViewText {

    private final BedWarsPlugin plugin;

    public MatchViewText(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public Object[] variables(final Player viewer, final Player subject) {
        final GameManager game = this.plugin.game();
        final TeamColor team = game.teamOf(subject.getUniqueId());
        final PlayerStats stats = game.stats(subject.getUniqueId());
        final boolean spectator = game.isSpectator(viewer.getUniqueId());
        final String next = game.state() == GameState.RUNNING || game.state() == GameState.PAUSED ? game.nextEventText() : game.state() == GameState.COUNTDOWN ? Messages.get("view.countdown",
                "SECONDS", game.countdown()) : Messages.get("view.waiting");
        final List<Object> values = new ArrayList<>(Arrays.asList("PLAYER", viewer.getName(), "SUBJECT", subject.getName(), "DATE", this.date(),
                "MAP", game.active() == null ? Messages.get("view.no-map") : game.active().getName(), "PLAYERS", game.state() == GameState.WAITING ? game.waitingPlayerCount() : game.matchPlayerCount(),
                "MAX_PLAYERS", this.plugin.settings().playersPerTeam() * 4, "SECONDS", game.countdown(), "TEAM", team == null ? Messages.get("view.spectator") : team.chat() + team.display(),
                "COLOR", team == null ? "§7" : team.chat(), "NEXT_EVENT", next, "KILLS", stats.getKills(), "FINAL_KILLS", stats.getFinalKills(), "BEDS",
                stats.getBedsBroken(), "OBSERVER", spectator ? Messages.get(game.canIntervene(viewer) ? "view.admin" : "view.observer") : "", "WATCHING",
                subject.equals(viewer) ? "" : Messages.get("view.watching", "PLAYER", subject.getName()), "TEAMS", this.teams(subject), "SERVER", this.plugin.settings().serverLine()));
        for (final TeamColor color : TeamColor.values()) {
            values.add(color.name() + "_NAME");
            values.add(color.chat() + color.display());
            values.add(color.name() + "_ALIVE");
            values.add(game.aliveCount(color));
        }
        return values.toArray();
    }

    private String teams(final Player subject) {
        final GameManager game = this.plugin.game();
        if (game.active() == null) {
            return "";
        }
        final StringBuilder result = new StringBuilder();
        for (final TeamColor color : TeamColor.values()) {
            final TeamData team = game.active().team(color);
            final String status = !game.isTeamInMatch(color) ? "empty" : team.isBedAlive() ? "bed" : game.teamAlive(color) ? "alive" : "eliminated";
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(Messages.get("scoreboard.team", "COLOR", color.chat(), "SHORT", color.shortName(), "TEAM", color.display(), "STATUS",
                    Messages.get("scoreboard.status." + status, "ALIVE", game.aliveCount(color)), "YOU", game.teamOf(subject.getUniqueId()) == color ? Messages.get("scoreboard.you") : ""));
        }
        return result.toString();
    }

    private String date() {
        final String pattern = this.plugin.getConfig().getString("scoreboard.dateFormat", "dd/MM/yy");
        try {
            return new SimpleDateFormat(pattern).format(new Date());
        } catch (final IllegalArgumentException exception) {
            return new SimpleDateFormat("dd/MM/yy").format(new Date());
        }
    }

    public static String limit(final String text, final int maximum) {
        final String value = text == null ? "" : text.replace('\n', ' ').replace('\r', ' ');
        final String limited = value.length() > maximum ? value.substring(0, maximum) : value;
        return limited.endsWith("§") ? limited.substring(0, limited.length() - 1) : limited;
    }
}
