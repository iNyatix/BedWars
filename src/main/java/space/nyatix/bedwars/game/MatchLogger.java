package space.nyatix.bedwars.game;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.PlayerStats;
import space.nyatix.bedwars.model.TeamColor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MatchLogger {

    private final BedWarsPlugin plugin;

    private final File file;

    private long startedAt;

    public MatchLogger(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "historia-meczow.csv");
        ensureHeader();
    }

    private void ensureHeader() {
        if (this.file.exists()) {
            return;
        }
        try {
            if (this.file.getParentFile() != null) {
                this.file.getParentFile().mkdirs();
            }
            final Writer w = new OutputStreamWriter(Files.newOutputStream(this.file.toPath()), StandardCharsets.UTF_8);
            w.write("czas,mapa,czas_trwania_sekundy,zwyciezca,gracz,druzyna,zabojstwa,finalne_zabojstwa,zniszczone_lozka,smierci,zebrane_surowce\n");
            w.close();
        } catch (final IOException ex) {
            this.plugin.getLogger().warning(Messages.get("match-logger.ensure-header.nie-mozna-utworzyc-historia-meczow-csv", "ERROR", ex.getMessage()));
        }
    }

    public void begin() {
        this.startedAt = System.currentTimeMillis();
    }

    public void finish(final TeamColor winner) {
        if (!this.plugin.settings().logMatches()) {
            return;
        }
        final String ts = new SimpleDateFormat(Messages.get("match-logger.finish.yyyy-mm-dd-hh-mm-ss")).format(new Date());
        final String map = this.plugin.game().active() == null ? Messages.get("match-logger.finish.brak") : this.plugin.game().active().getName();
        final int duration = (int) Math.max(0, (System.currentTimeMillis() - this.startedAt) / 1000L);
        try {
            final Writer w = new OutputStreamWriter(new FileOutputStream(this.file, true), StandardCharsets.UTF_8);
            for (final UUID playerId : this.plugin.game().participants()) {
                final PlayerStats stats = this.plugin.game().stats(playerId);
                final TeamColor teamColor = this.plugin.game().teamOf(playerId);
                final Player online = Bukkit.getPlayer(playerId);
                String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(playerId).getName();
                if (name == null) {
                    name = playerId.toString();
                }
                w.write(csv(ts) + "," + csv(map) + "," + duration + "," + csv(winner == null ? "ANULOWANY" : winner.display()) + "," + csv(name) + "," + csv(teamColor == null ? Messages.get("match-logger.finish.brak-2") : teamColor.display()) + "," + stats.getKills() + "," + stats.getFinalKills() + "," + stats.getBedsBroken() + "," + stats.getDeaths() + "," + stats.getResourcesCollected() + "\n");
            }
            w.close();
        } catch (final IOException ex) {
            this.plugin.getLogger().warning(Messages.get("match-logger.finish.nie-mozna-zapisac-historii-meczu", "ERROR", ex.getMessage()));
        }
    }

    private String csv(final String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }
}
