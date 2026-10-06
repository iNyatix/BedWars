package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServerDiagnostics {

    private final BedWarsPlugin plugin;

    private final TickHistory ticks = new TickHistory();

    private final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();

    private int task = -1, samples;

    private long lastCpu = -1, lastWall = -1, lastMain = -1;

    private double processCpu = Double.NaN, mainCpu = Double.NaN, systemCpu = Double.NaN;

    public ServerDiagnostics(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (this.task != -1) {
            return;
        }
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            final long now = System.nanoTime();
            this.ticks.record(now);
            if (this.samples++ % 20 == 0) {
                sampleCpu(now);
            }
        }, 1L, 1L);
    }

    public void shutdown() {
        if (this.task != -1) {
            Bukkit.getScheduler().cancelTask(this.task);
        }
        this.task = -1;
    }

    private void sampleCpu(final long now) {
        final long cpu = (long) osMetric("getProcessCpuTime");
        long main = -1;
        if (this.threads.isCurrentThreadCpuTimeSupported() && this.threads.isThreadCpuTimeEnabled()) {
            main = this.threads.getCurrentThreadCpuTime();
        }
        final int cores = Math.max(1, this.os.getAvailableProcessors());
        this.processCpu = this.lastCpu >= 0 && cpu >= 0 ? cpuPercent(cpu - this.lastCpu, now - this.lastWall, cores) : Double.NaN;
        this.mainCpu = this.lastMain >= 0 && main >= 0 ? cpuPercent(main - this.lastMain, now - this.lastWall, 1) : Double.NaN;
        final double system = osMetric("getSystemCpuLoad");
        this.systemCpu = system < 0 ? Double.NaN : system * 100D;
        this.lastCpu = cpu;
        this.lastMain = main;
        this.lastWall = now;
    }

    public static double cpuPercent(final long cpuNanos, final long wallNanos, final int processors) {
        if (cpuNanos < 0 || wallNanos <= 0 || processors < 1) {
            return Double.NaN;
        }
        return Math.min(100D, 100D * cpuNanos / wallNanos / processors);
    }

    private double osMetric(final String method) {
        try {
            return ((Number) Class.forName("com.sun.management.OperatingSystemMXBean").getMethod(method).invoke(this.os)).doubleValue();
        } catch (final Exception | LinkageError ex) {
            return -1;
        }
    }

    private double[] nativeTps() {
        try {
            final Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            for (Class<?> c = server.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    final Field field = c.getDeclaredField("recentTps");
                    field.setAccessible(true);
                    final double[] value = (double[]) field.get(server);
                    if (value.length >= 3) {
                        return value.clone();
                    }
                } catch (final NoSuchFieldException ignored) {
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }

    public void send(final CommandSender sender) {
        if (!sender.hasPermission("bedwars.admin")) {
            Messages.send(sender, Messages.get("server-diagnostics.send.brak-uprawnien"));
            return;
        }
        final List<String> lines = new ArrayList<>();
        final double[] nativeValues = nativeTps();
        lines.add(Messages.get("server-diagnostics.send.wydajnosc-serwera"));
        lines.add(Messages.get("server-diagnostics.send.tps-1-5-15-min", "VALUE1", tps(nativeValues == null ? this.ticks.tps(60) : nativeValues[0]),
                "VALUE2", tps(nativeValues == null ? this.ticks.tps(300) : nativeValues[1]), "VALUE3", tps(nativeValues == null ? this.ticks.tps(900) : nativeValues[2]),
                "VALUE4", (nativeValues == null ? Messages.get("diagnostics.tps-source", "SECONDS", (int) this.ticks.historySeconds()) : "Spigot")));
        lines.add(Messages.get("server-diagnostics.send.odstep-tickow-sr-max-10-s", "VALUE1", number(this.ticks.averageMillis(10)), "VALUE2",
                number(this.ticks.maxMillis(10))));
        lines.add(Messages.get("server-diagnostics.send.cpu-jvm-system-rdzeni-logicznych", "VALUE1", percent(this.processCpu), "VALUE2", percent(this.systemCpu),
                "VALUE3", this.os.getAvailableProcessors()));
        lines.add(Messages.get("server-diagnostics.send.cpu-glownego-watku-wzgledem-1-rdzenia", "VALUE1", percent(this.mainCpu)));
        final MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        lines.add(Messages.get("server-diagnostics.send.ram-jvm-heap-mib-uzyte-limit", "VALUE1", mib(heap.getUsed()), "VALUE2", mib(heap.getMax()),
                "VALUE3", (heap.getMax() > 0 ? percent(100D * heap.getUsed() / heap.getMax()) : Messages.get("diagnostics.unavailable"))));
        lines.add(Messages.get("server-diagnostics.send.ram-przydzielony-non-heap-mib", "VALUE1", mib(heap.getCommitted()), "VALUE2", mib(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed())));
        final double total = osMetric("getTotalPhysicalMemorySize"), free = osMetric("getFreePhysicalMemorySize");
        lines.add(Messages.get("server-diagnostics.send.ram-systemu-wolny-caly-mib-widok", "VALUE1", mib((long) free), "VALUE2", mib((long) total)));
        long collections = 0, gcMillis = 0;
        boolean gcAvailable = false;
        for (final GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() >= 0) {
                collections += gc.getCollectionCount();
                gcAvailable = true;
            }
            if (gc.getCollectionTime() >= 0) {
                gcMillis += gc.getCollectionTime();
            }
        }
        lines.add(Messages.get("server-diagnostics.send.gc-od-startu-watki-jvm", "VALUE1", (gcAvailable ? Messages.get("diagnostics.gc", "COUNT",
                collections, "MILLIS", gcMillis) : Messages.get("diagnostics.unavailable")), "VALUE2", this.threads.getThreadCount()));
        lines.add(Messages.get("server-diagnostics.send.uptime-jvm-dysk-wolny-mib", "VALUE1", duration(ManagementFactory.getRuntimeMXBean().getUptime() / 1000),
                "VALUE2", mib(this.plugin.getDataFolder().getUsableSpace())));
        lines.add(Messages.get("server-diagnostics.send.online-view-distance", "VALUE1", Bukkit.getOnlinePlayers().size(), "VALUE2", Bukkit.getMaxPlayers(),
                "VALUE3", Bukkit.getViewDistance()));
        long chunks = 0, entities = 0, items = 0;
        final List<String> worlds = new ArrayList<>();
        for (final World world : Bukkit.getWorlds()) {
            final int chunkCount = world.getLoadedChunks().length;
            int entityCount = 0;
            int itemCount = 0;
            for (final Entity entity : world.getEntities()) {
                entityCount++;
                if (entity instanceof Item) {
                    itemCount++;
                }
            }
            chunks += chunkCount;
            entities += entityCount;
            items += itemCount;
            worlds.add(Messages.get("server-diagnostics.send.graczy-chunkow-encji-dropow", "PLAYER", world.getName(), "VALUE2", world.getPlayers().size(),
                    "CHUNK_COUNT", chunkCount, "ENTITY_COUNT", entityCount, "ITEM_COUNT", itemCount));
        }
        lines.add(Messages.get("server-diagnostics.send.swiaty-chunki-encje-dropy", "VALUE1", worlds.size(), "CHUNKS", chunks, "ENTITIES", entities, "ITEMS", items));
        for (int i = 0; i < Math.min(8, worlds.size()); i++) {
            lines.add(worlds.get(i));
        }
        if (worlds.size() > 8) {
            lines.add(Messages.get("server-diagnostics.send.pozostale-swiaty-uwzglednione-w-sumie", "VALUE1", (worlds.size() - 8)));
        }
        final Arena arena = this.plugin.game().active();
        if (arena != null) {
            int spectators = 0;
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (this.plugin.game().isSpectator(player.getUniqueId())) {
                    spectators++;
                }
            }
            lines.add(Messages.get("server-diagnostics.send.czas-meczu-obserwatorzy-bloki-graczy", "VALUE1", duration(this.plugin.game().elapsed()),
                    "SPECTATORS", spectators, "VALUE3", this.plugin.game().placedBlocks().size()));
            final WorldBorderManager borders = this.plugin.borders();
            if (borders != null && borders.isConfigured(arena)) {
                lines.add(Messages.get("server-diagnostics.send.border-x-srodek", "VALUE1", number(borders.currentRadius(arena) * 2), "VALUE2",
                        number(borders.currentRadius(arena) * 2), "VALUE3", number(borders.centerX(arena)), "VALUE4", number(borders.centerZ(arena))));
            }
        }
        lines.add(Messages.get("server-diagnostics.send.serwer", "VERSION", Bukkit.getVersion()));
        lines.add(Messages.get("server-diagnostics.send.java-system", "VALUE1", System.getProperty("java.version", Messages.get("diagnostics.unavailable")),
                "PLAYER", this.os.getName(), "VALUE3", this.os.getArch()));
        for (final String line : lines) {
            Messages.send(sender, line);
        }
    }

    private static String tps(final double value) {
        return number(Double.isNaN(value) ? value : Math.min(20D, value));
    }

    private static String number(final double value) {
        return Double.isNaN(value) || Double.isInfinite(value) || value < 0 ? Messages.get("diagnostics.unavailable") : String.format(Locale.ROOT, "%.2f", value);
    }

    private static String percent(final double value) {
        return Double.isNaN(value) || value < 0 ? Messages.get("diagnostics.unavailable") : number(value) + "%";
    }

    private static String mib(final long bytes) {
        return bytes < 0 ? Messages.get("diagnostics.unavailable") : String.valueOf(bytes / (1024L * 1024L));
    }

    private static String duration(final long seconds) {
        return Messages.get("diagnostics.duration", "HOURS", seconds / 3600, "MINUTES", seconds % 3600 / 60, "SECONDS", seconds % 60);
    }

    public static final class TickHistory {

        private final long[] times = new long[18001];

        private int next, count;

        public void record(final long now) {
            this.times[this.next] = now;
            this.next = (this.next + 1) % this.times.length;
            if (this.count < this.times.length) {
                this.count++;
            }
        }

        private long ago(final int age) {
            return this.times[(this.next - 1 - age + this.times.length) % this.times.length];
        }

        public double historySeconds() {
            return this.count < 2 ? 0 : (ago(0) - ago(this.count - 1)) / 1_000_000_000D;
        }

        private int intervals(final int seconds) {
            int n = 0;
            while (n + 1 < this.count && ago(0) - ago(n) < seconds * 1_000_000_000L) n++;
            return n;
        }

        public double tps(final int seconds) {
            final int n = intervals(seconds);
            final long delta = n == 0 ? 0 : ago(0) - ago(n);
            return n < 20 || delta <= 0 ? Double.NaN : Math.min(20D, n * 1_000_000_000D / delta);
        }

        public double averageMillis(final int seconds) {
            final int n = intervals(seconds);
            return n < 1 ? Double.NaN : (ago(0) - ago(n)) / 1_000_000D / n;
        }

        public double maxMillis(final int seconds) {
            final int n = intervals(seconds);
            if (n < 1) {
                return Double.NaN;
            }
            long max = 0;
            for (int i = 0; i < n; i++) {
                max = Math.max(max, ago(i) - ago(i + 1));
            }
            return max / 1_000_000D;
        }
    }
}
