package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;

import java.util.List;

public class ArenaStartupManager {

    private final BedWarsPlugin plugin;

    private int task = -1, attempts;

    public ArenaStartupManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        this.attempts = 0;
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::attempt, 20L, 40L);
    }

    private void attempt() {
        final Arena arena = this.plugin.game().active();
        if (arena == null || this.plugin.worlds().isEditing(arena) || this.plugin.game().state() == GameState.RUNNING || this.plugin.game().state() == GameState.ENDED) {
            stop();
            return;
        }
        if (this.plugin.worlds().isRuntimeReady(arena)) {
            stop();
            return;
        }
        if (this.plugin.worlds().isRuntimePreparing(arena)) {
            return;
        }
        if (++this.attempts > 30) {
            this.plugin.getLogger().warning(Messages.get("arena-startup.attempt.automatyczne-ladowanie-mapy-nie-powiodlo-sie", "MAP", arena.getName()));
            stop();
            return;
        }
        if (!this.plugin.worlds().available()) {
            return;
        }
        final List<String> missing = ArenaValidator.validate(arena);
        if (!missing.isEmpty()) {
            this.plugin.getLogger().warning(Messages.get("arena-startup.attempt.zapisana-mapa-nie-jest-kompletna", "MAP", arena.getName(), "MISSING", missing));
            stop();
            return;
        }
        this.plugin.game().rememberActiveArena();
        this.plugin.worlds().prepareRuntimeAsync(arena, (world, error) -> {
            if (plugin.game().active() != arena || plugin.worlds().isEditing(arena)) {
                return;
            }
            if (error == null && world != null) {
                if (plugin.npcs() != null) {
                    plugin.npcs().respawn();
                }
                stop();
            } else if (error != null) {
                plugin.getLogger().warning(Messages.get("arena-startup.finished.proba-zaladowania-zapisanej-mapy", "MAP", arena.getName(),
                        "ERROR", error.getMessage()));
            }
        });
    }

    public void stop() {
        if (this.task != -1) {
            Bukkit.getScheduler().cancelTask(this.task);
            this.task = -1;
        }
    }
}
