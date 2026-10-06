package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

public class SlimeWorldService {

    public interface RuntimeCallback {

        void finished(final World world, final Throwable error);
    }

    private final BedWarsPlugin plugin;

    private Plugin swm;

    private Object loader;

    private final Map<String, String> runtimes = new HashMap<>();

    private final Map<String, Object> runtimeSlimes = new HashMap<>();

    private final Map<String, Object> templates = new HashMap<>();

    private final Set<String> editing = new HashSet<>();

    private final Map<String, String> pendingRuntimes = new HashMap<>();

    private final Map<String, Object> pendingSlimes = new HashMap<>();

    private final Map<String, List<RuntimeCallback>> pendingCallbacks = new HashMap<>();

    private long sequence = 0L;

    public SlimeWorldService(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        detect();
    }

    public void detect() {
        this.swm = Bukkit.getPluginManager().getPlugin("SwoftyWorldManager");
        if (this.swm == null) {
            this.swm = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        }
        this.loader = null;
        if (this.swm != null && this.swm.isEnabled()) {
            try {
                this.loader = invokeCompatible(this.swm, "getLoader", new Object[] { "file" });
            } catch (final Throwable ex) {
                this.plugin.getLogger().warning(Messages.get("slime-world-service.detect.wykryto-slimeworldmanager-ale-nie-udalo-sie", "VALUE1", rootMessage(ex)));
            }
        }
    }

    public boolean available() {
        if (this.swm == null || !this.swm.isEnabled() || this.loader == null) {
            detect();
        }
        return this.swm != null && this.swm.isEnabled() && this.loader != null;
    }

    public String providerName() {
        return this.swm == null ? Messages.get("slime-world-service.provider-name.brak") : this.swm.getName();
    }

    public String providerVersion() {
        if (this.swm == null) {
            return Messages.get("slime-world-service.provider-version.brak");
        }
        try {
            return this.swm.getDescription().getVersion();
        } catch (final Throwable ignored) {
            return "?";
        }
    }

    public String templateName(final Arena arena) {
        return "bw_tpl_" + safe(arena == null ? "mapa" : arena.getName());
    }

    public String runtimeName(final Arena arena) {
        this.sequence++;
        return "bw_" + safe(arena.getName()) + "_" + Long.toString(System.currentTimeMillis(), 36) + "_" + this.sequence;
    }

    private String arenaKey(final Arena arena) {
        return arena == null ? "" : arena.getName().toLowerCase(Locale.ROOT);
    }

    private String safe(final String s) {
        String out = s == null ? "mapa" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (out.length() > 28) {
            out = out.substring(0, 28);
        }
        return out;
    }

    public boolean hasTemplate(final Arena arena) {
        if (arena == null || !available()) {
            return false;
        }
        if (this.templates.containsKey(templateName(arena))) {
            return true;
        }
        try {
            return worldExists(templateName(arena));
        } catch (final Exception ex) {
            this.plugin.getLogger().warning(Messages.get("slime-world-service.has-template.nie-mozna-sprawdzic-zapisanego-szablonu", "MAP",
                    arena.getName(), "VALUE2", rootMessage(ex)));
            return false;
        }
    }

    public boolean isEditing(final Arena arena) {
        return arena != null && this.editing.contains(arenaKey(arena));
    }

    public void leaveConfiguration(final Arena arena) {
        if (arena != null) {
            this.editing.remove(arenaKey(arena));
        }
    }

    public synchronized boolean isRuntimePreparing(final Arena arena) {
        return arena != null && this.pendingRuntimes.containsKey(arenaKey(arena));
    }

    public synchronized String pendingRuntimeName(final Arena arena) {
        return arena == null ? null : this.pendingRuntimes.get(arenaKey(arena));
    }

    public boolean isRuntimeReady(final Arena arena) {
        if (arena == null) {
            return false;
        }
        String name = arena.getRuntimeWorld();
        if (name == null) {
            name = this.runtimes.get(arenaKey(arena));
        }
        return name != null && Bukkit.getWorld(name) != null;
    }

    public World runtimeWorld(final Arena arena) {
        if (arena == null) {
            return null;
        }
        String name = arena.getRuntimeWorld();
        if (name == null) {
            name = this.runtimes.get(arenaKey(arena));
        }
        return name == null ? null : Bukkit.getWorld(name);
    }

    public synchronized World prepareRuntime(final Arena arena) {
        if (arena == null || isEditing(arena)) {
            return null;
        }
        final World current = runtimeWorld(arena);
        if (current != null) {
            return current;
        }
        final String key = arenaKey(arena);
        final String pending = this.pendingRuntimes.get(key);
        if (pending != null) {
            final World already = Bukkit.getWorld(pending);
            if (already != null) {
                completeRuntimeSuccess(arena, pending, this.pendingSlimes.get(key), already);
                return already;
            }
            return null;
        }
        if (!available()) {
            this.plugin.getLogger().warning(Messages.get("slime-world-service.prepare-runtime.nie-mozna-przygotowac-mapy-brak-dzialajacego", "MAP", arena.getName()));
            return null;
        }
        startRuntimeGeneration(arena);
        return runtimeWorld(arena);
    }

    public void prepareRuntimeAsync(final Arena arena, final RuntimeCallback callback) {
        if (arena == null) {
            if (callback != null) {
                callback.finished(null, new IllegalArgumentException(Messages.get("slime-world-service.prepare-runtime-async.brak-mapy")));
            }
            return;
        }
        if (isEditing(arena)) {
            if (callback != null) {
                callback.finished(null, new IllegalStateException(Messages.get("slime-world-service.prepare-runtime-async.mapa-jest-obecnie-edytowana-zakoncz-konfiguracje")));
            }
            return;
        }
        final World ready = runtimeWorld(arena);
        if (ready != null) {
            if (callback != null) {
                callback.finished(ready, null);
            }
            return;
        }
        synchronized (this) {
            final String key = arenaKey(arena);
            if (callback != null) {
                final List<RuntimeCallback> list = this.pendingCallbacks.computeIfAbsent(key, k -> new ArrayList<>());
                list.add(callback);
            }
            if (this.pendingRuntimes.containsKey(key)) {
                return;
            }
        }
        if (!available()) {
            failPending(arena, new IllegalStateException(Messages.get("slime-world-service.prepare-runtime-async.brak-dzialajacego-slime-world-managera")));
            return;
        }
        synchronized (this) {
            if (!this.pendingRuntimes.containsKey(arenaKey(arena))) {
                startRuntimeGeneration(arena);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void startRuntimeGeneration(final Arena arena) {
        final String key = arenaKey(arena);
        final String runtime = runtimeName(arena);
        try {
            ensureStoredTemplate(arena);
            final String templateKey = templateName(arena);
            Object template = this.templates.get(templateKey);
            if (template == null) {
                template = loadSlimeWorld(templateKey);
                if (template != null) {
                    this.templates.put(templateKey, template);
                }
            }
            if (template == null) {
                throw new IllegalStateException(Messages.get("slime-world-service.start-runtime-generation.cswm-zwrocil-pusty-szablon"));
            }
            final Object clone = invokeCompatible(template, "clone", new Object[] { runtime });
            if (clone == null) {
                throw new IllegalStateException(Messages.get("slime-world-service.start-runtime-generation.nie-udalo-sie-utworzyc-klona-swiata"));
            }
            this.pendingRuntimes.put(key, runtime);
            this.pendingSlimes.put(key, clone);
            this.plugin.getLogger().info(Messages.get("slime-world-service.start-runtime-generation.przygotowywanie-swiezej-kopii-mapy-jako",
                    "MAP", arena.getName(), "RUNTIME", runtime));
            final Object generation = invokeCompatible(this.swm, "generateWorld", new Object[] { clone });
            if (generation instanceof CompletionStage) {
                final CompletionStage<Object> stage = (CompletionStage<Object>) generation;
                stage.whenComplete(new BiConsumer<Object, Throwable>() {

                    public void accept(final Object ignored, final Throwable error) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            finishGeneratedRuntime(arena, runtime, clone, error, 0);
                        }, 0L);
                    }
                });
            } else {
                finishGeneratedRuntime(arena, runtime, clone, null, 0);
            }
        } catch (final Throwable ex) {
            this.plugin.getLogger().severe(Messages.get("slime-world-service.start-runtime-generation.nie-udalo-sie-rozpoczac-tworzenia-runtime",
                    "MAP", arena.getName(), "VALUE2", providerName(), "VALUE3", rootMessage(ex)));
            failPending(arena, ex);
        }
    }

    private void finishGeneratedRuntime(final Arena arena, final String runtime, final Object clone, final Throwable error, final int attempt) {
        final String key = arenaKey(arena);
        synchronized (this) {
            final String expected = this.pendingRuntimes.get(key);
            if (expected == null || !expected.equals(runtime)) {
                final World orphan = Bukkit.getWorld(runtime);
                if (orphan != null) {
                    Bukkit.unloadWorld(orphan, false);
                }
                return;
            }
        }
        if (error != null) {
            this.plugin.getLogger().severe(Messages.get("slime-world-service.finish-generated-runtime.cswm-nie-wygenerowal-runtime", "RUNTIME",
                    runtime, "VALUE2", rootMessage(error)));
            failPending(arena, error);
            return;
        }
        final World world = Bukkit.getWorld(runtime);
        if (world == null && attempt < 40) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                finishGeneratedRuntime(arena, runtime, clone, null, attempt + 1);
            }, 1L);
            return;
        }
        if (world == null) {
            final IllegalStateException ex = new IllegalStateException(Messages.get("slime-world-service.finish-generated-runtime.cswm-zakonczyl-generowanie-ale-bukkit-nadal",
                    
                    "RUNTIME", runtime));
            this.plugin.getLogger().severe(ex.getMessage());
            failPending(arena, ex);
            return;
        }
        completeRuntimeSuccess(arena, runtime, clone, world);
    }

    private void completeRuntimeSuccess(final Arena arena, final String runtime, final Object clone, final World world) {
        List<RuntimeCallback> callbacks;
        synchronized (this) {
            final String key = arenaKey(arena);
            final String expected = this.pendingRuntimes.get(key);
            if (expected != null && !expected.equals(runtime)) {
                return;
            }
            this.pendingRuntimes.remove(key);
            this.pendingSlimes.remove(key);
            callbacks = this.pendingCallbacks.remove(key);
            arena.setRuntimeWorld(runtime);
            this.runtimes.put(key, runtime);
            if (clone != null) {
                this.runtimeSlimes.put(key, clone);
            }
            arena.bindWorld(world);
        }
        world.setAutoSave(false);
        if (this.plugin.worldRules() != null) {
            this.plugin.worldRules().prepare(world);
        }
        this.plugin.getLogger().info(Messages.get("slime-world-service.complete-runtime-success.zaladowano-swieza-kopie-mapy-jako-przez",
                "MAP", arena.getName(), "RUNTIME", runtime, "VALUE3", providerName(), "VALUE4", providerVersion()));
        fireCallbacks(callbacks, world, null);
    }

    private void failPending(final Arena arena, final Throwable error) {
        List<RuntimeCallback> callbacks;
        synchronized (this) {
            final String key = arenaKey(arena);
            this.pendingRuntimes.remove(key);
            this.pendingSlimes.remove(key);
            callbacks = this.pendingCallbacks.remove(key);
        }
        fireCallbacks(callbacks, null, error);
    }

    private void fireCallbacks(final List<RuntimeCallback> callbacks, final World world, final Throwable error) {
        if (callbacks == null) {
            return;
        }
        for (final RuntimeCallback callback : new ArrayList<>(callbacks)) {
            if (callback == null) {
                continue;
            }
            try {
                callback.finished(world, error);
            } catch (final Throwable ex) {
                this.plugin.getLogger().warning(Messages.get("slime-world-service.fire-callbacks.blad-callbacku-ladowania-mapy", "VALUE1", rootMessage(ex)));
            }
        }
    }

    public synchronized boolean unloadRuntime(final Arena arena) {
        if (arena == null) {
            return true;
        }
        final String key = arenaKey(arena);
        final String pending = this.pendingRuntimes.remove(key);
        this.pendingSlimes.remove(key);
        final List<RuntimeCallback> cancelledCallbacks = this.pendingCallbacks.remove(key);
        if (cancelledCallbacks != null) {
            fireCallbacks(cancelledCallbacks, null, new CancellationException(Messages.get("slime-world-service.unload-runtime.przygotowywanie-mapy-zostalo-anulowane")));
        }
        final World world = runtimeWorld(arena);
        final Object slime = this.runtimeSlimes.remove(key);
        if (world == null) {
            arena.setRuntimeWorld(null);
            this.runtimes.remove(key);
            return true;
        }
        final Location lobby = this.plugin.lobby() == null ? null : this.plugin.lobby().getLobby();
        for (final Player player : new ArrayList<>(world.getPlayers())) {
            if (lobby != null && lobby.getWorld() != null && !lobby.getWorld().equals(world)) {
                player.teleport(lobby);
            }
        }
        if (this.plugin.npcs() != null) {
            this.plugin.npcs().remove();
        }
        boolean ok = false;
        if (slime != null) {
            try {
                final String fallback = lobby != null && lobby.getWorld() != null ? lobby.getWorld().getName() : null;
                try {
                    invokeCompatible(slime, "unloadWorld", new Object[] { Boolean.FALSE, fallback });
                } catch (final NoSuchMethodException oldApi) {
                    invokeCompatible(slime, "unloadWorld", new Object[] { Boolean.FALSE });
                }
                ok = Bukkit.getWorld(world.getName()) == null;
            } catch (final Throwable ex) {
                this.plugin.getLogger().warning(Messages.get("slime-world-service.unload-runtime.cswm-nie-wyladowal-runtime-przez-swoje", "VALUE1", rootMessage(ex)));
            }
        }
        if (!ok) {
            ok = Bukkit.unloadWorld(world, false);
        }
        if (ok) {
            this.plugin.getLogger().info(Messages.get("slime-world-service.unload-runtime.usunieto-zuzyta-kopie-mapy-bez-zapisu", "PLAYER", world.getName()));
            arena.setRuntimeWorld(null);
            this.runtimes.remove(key);
            arena.bindWorld(null);
        } else {
            this.plugin.getLogger().warning(Messages.get("slime-world-service.unload-runtime.nie-udalo-sie-wyladowac-runtime-swiata", "PLAYER", world.getName()));
        }
        return ok;
    }

    public boolean resetRuntime(final Arena arena) {
        if (arena == null) {
            return false;
        }
        if (!unloadRuntime(arena)) {
            return false;
        }
        prepareRuntimeAsync(arena, (world, error) -> {
            if (error != null) {
                plugin.getLogger().severe(Messages.get("slime-world-service.finished.nie-udalo-sie-przygotowac-swiezej-mapy", "VALUE1", rootMessage(error)));
                return;
            }
            if (world != null && plugin.npcs() != null) {
                plugin.npcs().respawn();
            }
        });
        return isRuntimeReady(arena) || isRuntimePreparing(arena);
    }

    public synchronized boolean publishTemplate(final Player admin, final Arena arena) {
        if (admin == null || arena == null) {
            return false;
        }
        if (!available()) {
            Messages.send(admin, Messages.get("slime-world-service.publish-template.brak-slime-world-managera"));
            Messages.send(admin, Messages.get("slime-world-service.publish-template.uruchom-poprawnie-swoftyworldmanager-slimeworldmanager-dla-minecraft"));
            return false;
        }
        final Location mainLobby = this.plugin.lobby() == null ? null : this.plugin.lobby().getLobby();
        if (mainLobby == null || mainLobby.getWorld() == null) {
            Messages.send(admin, Messages.get("slime-world-service.publish-template.najpierw-ustaw-glowne-lobby-serwera-bw"));
            Messages.send(admin, Messages.get("slime-world-service.publish-template.musze-miec-bezpieczne-miejsce-do-ktorego"));
            return false;
        }
        final World source = Bukkit.getWorld(arena.getWorld());
        if (source == null) {
            Messages.send(admin, Messages.get("slime-world-service.publish-template.swiat-zrodlowy-nie-jest-zaladowany-wejdz", "VALUE1", arena.getWorld(),
                    "MAP", arena.getName()));
            return false;
        }
        if (source.equals(mainLobby.getWorld())) {
            Messages.send(admin, Messages.get("slime-world-service.publish-template.ta-mapa-jest-jednoczesnie-glownym-lobby"));
            Messages.send(admin, Messages.get("slime-world-service.publish-template.ustaw-lobby-na-innym-swiecie-przez"));
            return false;
        }
        try {
            if (this.plugin.npcs() != null) {
                this.plugin.npcs().remove();
            }
            source.setAutoSave(true);
            source.save();
            for (final Player p : new ArrayList<>(source.getPlayers())) {
                p.teleport(mainLobby);
            }
            if (!Bukkit.unloadWorld(source, true)) {
                Messages.send(admin, Messages.get("slime-world-service.publish-template.nie-udalo-sie-wyladowac-swiata-mapy"));
                return false;
            }
            final File worldFolder = new File(this.plugin.getServer().getWorldContainer(), arena.getWorld());
            if (!worldFolder.isDirectory()) {
                Messages.send(admin, Messages.get("slime-world-service.publish-template.nie-znaleziono-folderu-swiata", "PLAYER", worldFolder.getName()));
                restoreEditorWorld(admin, arena);
                return false;
            }
            final String template = templateName(arena);
            this.templates.remove(template);
            if (worldExists(template)) {
                deleteStoredWorld(template);
            }
            invokeCompatible(this.swm, "importWorld", new Object[] { worldFolder, template, this.loader });
            if (!worldExists(template)) {
                throw new IllegalStateException(Messages.get("slime-world-service.publish-template.import-zakonczyl-sie-bez-bledu-ale", "TEMPLATE", template));
            }
            final Object freshTemplate = loadSlimeWorld(template);
            if (freshTemplate == null) {
                throw new IllegalStateException(Messages.get("slime-world-service.publish-template.szablon-zapisano-ale-slime-world-manager"));
            }
            this.templates.put(template, freshTemplate);
            this.editing.remove(arenaKey(arena));
            this.plugin.arenaStorage().saveArena(arena);
            final UUID adminId = admin.getUniqueId();
            Messages.send(admin, Messages.get("slime-world-service.publish-template.mapa-zatwierdzona"));
            Messages.send(admin, Messages.get("slime-world-service.publish-template.czysty-szablon", "TEMPLATE", template));
            Messages.send(admin, Messages.get("slime-world-service.publish-template.przygotowuje-teraz-swieza-kopie-meczu-w"));
            prepareRuntimeAsync(arena, new RuntimeCallback() {

                public void finished(final World world, final Throwable error) {
                    final Player player = Bukkit.getPlayer(adminId);
                    if (error != null) {
                        plugin.getLogger().severe(Messages.get("slime-world-service.finished.szablon-zapisano-ale-runtime-nie-wystartowal",
                                "MAP", arena.getName(), "VALUE2", rootMessage(error)));
                        if (player != null) {
                            Messages.send(player, Messages.get("slime-world-service.finished.szablon-mapy-zostal-zapisany-ale-nie"));
                            Messages.send(player, Messages.get("slime-world-service.finished.sprawdz-konsole-nie-musisz-konfigurowac-mapy"));
                        }
                        return;
                    }
                    if (plugin.npcs() != null) {
                        plugin.npcs().respawn();
                    }
                    if (player != null && world != null) {
                        Messages.send(player, Messages.get("slime-world-service.finished.swieza-kopia-mapy-jest-gotowa", "MAP", arena.getName(),
                                "PLAYER", world.getName()));
                    }
                }
            });
            return true;
        } catch (final Throwable ex) {
            this.plugin.getLogger().severe(Messages.get("slime-world-service.publish-template.publikacja-szablonu-nie-powiodla-sie", "MAP",
                    arena.getName(), "VALUE2", rootMessage(ex)));
            ex.printStackTrace();
            Messages.send(admin, Messages.get("slime-world-service.publish-template.nie-udalo-sie-utworzyc-szablonu", "VALUE1", rootMessage(ex)));
            Messages.send(admin, Messages.get("slime-world-service.publish-template.szczegoly-sa-w-konsoli-serwera-przywracam"));
            restoreEditorWorld(admin, arena);
            return false;
        }
    }

    private void restoreEditorWorld(final Player admin, final Arena arena) {
        try {
            World source = Bukkit.getWorld(arena.getWorld());
            if (source == null) {
                source = new WorldCreator(arena.getWorld()).createWorld();
            }
            if (source == null) {
                return;
            }
            enterConfiguration(arena, source);
            Location target = arena.getLobby();
            if (target == null || target.getWorld() == null) {
                target = source.getSpawnLocation().clone().add(.5D, 0D, .5D);
            }
            admin.teleport(target);
            if (this.plugin.configMenu() != null) {
                this.plugin.configMenu().giveTool(admin);
            }
        } catch (final Throwable recover) {
            this.plugin.getLogger().warning(Messages.get("slime-world-service.restore-editor-world.nie-udalo-sie-automatycznie-przywrocic-mapy",
                    "MAP", arena.getName(), "VALUE2", rootMessage(recover)));
        }
    }

    public void enterConfiguration(final Arena arena, final World source) {
        if (arena == null || source == null) {
            return;
        }
        this.editing.add(arenaKey(arena));
        unloadRuntime(arena);
        arena.setWorld(source.getName());
        arena.setRuntimeWorld(null);
        this.runtimes.remove(arenaKey(arena));
        arena.bindWorld(source);
        if (this.plugin.worldRules() != null) {
            this.plugin.worldRules().prepare(source);
        }
    }

    private void ensureStoredTemplate(final Arena arena) throws Exception {
        final String template = templateName(arena);
        if (this.templates.containsKey(template) || worldExists(template)) {
            return;
        }
        if (isEditing(arena)) {
            throw new IllegalStateException(Messages.get("slime-world-service.ensure-stored-template.mapa-jest-w-konfiguratorze"));
        }
        final List<String> missing = ArenaValidator.validate(arena);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(Messages.get("slime-world-service.ensure-stored-template.niekompletna-konfiguracja-mapy", "MISSING", missing));
        }
        final String sourceName = arena.getWorld();
        if (sourceName == null || !sourceName.matches("[A-Za-z0-9._-]{1,48}") || sourceName.contains("..") || sourceName.startsWith("bw_")) {
            throw new IllegalStateException(Messages.get("slime-world-service.ensure-stored-template.brak-poprawnej-nazwy-mapy-zrodlowej"));
        }
        final File container = this.plugin.getServer().getWorldContainer().getCanonicalFile();
        final File source = new File(container, sourceName).getCanonicalFile();
        if (!container.equals(source.getParentFile()) || !new File(source, "level.dat").isFile()) {
            throw new IllegalStateException(Messages.get("slime-world-service.ensure-stored-template.brak-zapisanego-szablonu-i-folderu-zrodlowego",
                    "SOURCE_NAME", sourceName));
        }
        final World loaded = Bukkit.getWorld(sourceName);
        if (loaded != null) {
            final Location lobby = this.plugin.lobby() == null ? null : this.plugin.lobby().getLobby();
            if (!loaded.getPlayers().isEmpty() || (lobby != null && loaded.equals(lobby.getWorld()))) {
                throw new IllegalStateException(Messages.get("slime-world-service.ensure-stored-template.nie-mozna-odtworzyc-szablonu-na-swiecie"));
            }
            if (!Bukkit.unloadWorld(loaded, false)) {
                throw new IllegalStateException(Messages.get("slime-world-service.ensure-stored-template.nie-mozna-wyladowac-swiata-zrodlowego",
                        "SOURCE_NAME", sourceName));
            }
        }
        invokeCompatible(this.swm, "importWorld", new Object[] { source, template, this.loader });
        if (!worldExists(template)) {
            throw new IllegalStateException(Messages.get("slime-world-service.ensure-stored-template.cswm-nie-zapisal-odtwarzanego-szablonu", "TEMPLATE", template));
        }
        this.plugin.getLogger().info(Messages.get("slime-world-service.ensure-stored-template.odtworzono-brakujacy-szablon-z-zapisanej-mapy",
                "TEMPLATE", template, "SOURCE_NAME", sourceName));
    }

    private Object loadSlimeWorld(final String name) throws Exception {
        final Method[] methods = this.swm.getClass().getMethods();
        for (final Method m : methods) {
            if (!m.getName().equals("loadWorld")) {
                continue;
            }
            final Class<?>[] p = m.getParameterTypes();
            if (p.length == 4 && compatible(p[0], this.loader) && p[1] == String.class && (p[2] == boolean.class || p[2] == Boolean.class)) {
                final Object props = newEmpty(p[3]);
                return m.invoke(this.swm, this.loader, name, true, props);
            }
        }
        for (final Method m : methods) {
            if (!m.getName().equals("loadWorld")) {
                continue;
            }
            final Class<?>[] p = m.getParameterTypes();
            if (p.length == 3 && compatible(p[0], this.loader) && p[1] == String.class) {
                final Object props = newEmpty(p[2]);
                return m.invoke(this.swm, this.loader, name, props);
            }
        }
        throw new NoSuchMethodException(Messages.get("slime-world-service.load-slime-world.nie-znaleziono-obslugiwanej-metody-loadworld-w",
                "PLAYER", this.swm.getClass().getName()));
    }

    private Object newEmpty(final Class<?> type) throws Exception {
        final Constructor<?> c = type.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }

    private boolean worldExists(final String name) throws Exception {
        final Object result = invokeCompatible(this.loader, "worldExists", new Object[] { name });
        return result instanceof Boolean && (Boolean) result;
    }

    private void deleteStoredWorld(final String name) throws Exception {
        try {
            invokeCompatible(this.loader, "unlockWorld", new Object[] { name });
        } catch (Throwable ignored) {
        }
        invokeCompatible(this.loader, "deleteWorld", new Object[] { name });
    }

    private Object invokeCompatible(final Object target, final String name, final Object[] args) throws Exception {
        if (target == null) {
            throw new IllegalStateException(Messages.get("slime-world-service.invoke-compatible.brak-obiektu-dla-metody", "NAME", name));
        }
        Method best = null;
        for (final Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                continue;
            }
            final Class<?>[] types = method.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < types.length; i++) {
                if (!compatible(types[i], args[i])) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                best = method;
                break;
            }
        }
        if (best == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + name + "(" + args.length + " args)");
        }
        try {
            best.setAccessible(true);
            return best.invoke(target, args);
        } catch (final InvocationTargetException ex) {
            final Throwable c = ex.getCause();
            if (c instanceof Exception) {
                throw (Exception) c;
            }
            if (c instanceof Error) {
                throw (Error) c;
            }
            throw ex;
        }
    }

    private boolean compatible(final Class<?> type, final Object value) {
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isInstance(value)) {
            return true;
        }
        if (type == boolean.class && value instanceof Boolean) {
            return true;
        }
        if (type == int.class && value instanceof Integer) {
            return true;
        }
        if (type == long.class && value instanceof Long) {
            return true;
        }
        if (type == double.class && value instanceof Double) {
            return true;
        }
        return type == float.class && value instanceof Float;
    }

    private String rootMessage(Throwable t) {
        if (t == null) {
            return Messages.get("slime-world-service.root-message.nieznany-blad");
        }
        while (true) {
            if (t instanceof InvocationTargetException && ((InvocationTargetException) t).getCause() != null) {
                t = ((InvocationTargetException) t).getCause();
                continue;
            }
            if ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
                t = t.getCause();
                continue;
            }
            break;
        }
        final String msg = t.getMessage();
        return t.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
    }
}
