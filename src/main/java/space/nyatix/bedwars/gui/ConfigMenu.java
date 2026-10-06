package space.nyatix.bedwars.gui;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.game.ArenaValidator;
import space.nyatix.bedwars.listener.ConfiguratorListener;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GeneratorType;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.model.TeamData;
import space.nyatix.bedwars.shop.ShopItem;
import space.nyatix.bedwars.util.ItemBuilder;
import space.nyatix.bedwars.util.ItemTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConfigMenu {

    public static final String TOOL_NAME = "§b§lKONFIGURATOR MAPY §7(PPM)";

    private final BedWarsPlugin plugin;

    private final Map<UUID, String> sessions = new HashMap<>();

    private final Map<UUID, String> pendingSelection = new HashMap<>();

    private final Map<UUID, TeamColor> selectedTeam = new HashMap<>();

    private final Set<String> dirtyArenas = new HashSet<>();

    private final Map<UUID, Long> publishConfirmUntil = new HashMap<>();

    private final Map<UUID, String> shopEditorMode = new HashMap<>();

    private final Map<UUID, ShopItem> selectedShopItem = new HashMap<>();

    private final Map<UUID, Integer> selectedToolTier = new HashMap<>();

    public ConfigMenu(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new ConfiguratorListener(plugin), plugin);
    }

    public void startSession(final Player player, final Arena arena) {
        if (player == null || arena == null) {
            return;
        }
        this.sessions.put(player.getUniqueId(), arena.getName().toLowerCase());
        this.pendingSelection.remove(player.getUniqueId());
        this.publishConfirmUntil.remove(player.getUniqueId());
        this.selectedTeam.remove(player.getUniqueId());
        clearShopEditor(player);
        giveTool(player);
        if (this.plugin.configHolograms() != null) {
            this.plugin.configHolograms().refresh(arena);
        }
        open(player);
    }

    public void endSession(final Player player) {
        if (player == null) {
            return;
        }
        final Arena configured = arena(player);
        this.sessions.remove(player.getUniqueId());
        this.pendingSelection.remove(player.getUniqueId());
        this.publishConfirmUntil.remove(player.getUniqueId());
        this.selectedTeam.remove(player.getUniqueId());
        clearShopEditor(player);
        if (this.plugin.configHolograms() != null && configured != null) {
            this.plugin.configHolograms().clear(configured);
        }
        player.closeInventory();
        Messages.send(player, Messages.get("config-menu.end-session.zakonczono-konfiguracje-mapy"));
        this.plugin.lobby().send(player);
        if (configured != null && !isDirty(configured) && !this.sessions.containsValue(configured.getName().toLowerCase())) {
            if (this.plugin.worlds() != null) {
                this.plugin.worlds().leaveConfiguration(configured);
            }
            if (this.plugin.startup() != null) {
                this.plugin.startup().start();
            }
        }
    }

    public boolean hasSession(final Player player) {
        return player != null && this.sessions.containsKey(player.getUniqueId());
    }

    public boolean isDirty(final Arena arena) {
        return arena != null && this.dirtyArenas.contains(arena.getName().toLowerCase());
    }

    public void completePublished(final Player player, final Arena arena) {
        if (arena != null) {
            this.dirtyArenas.remove(arena.getName().toLowerCase());
        }
        if (player != null) {
            this.sessions.remove(player.getUniqueId());
            this.pendingSelection.remove(player.getUniqueId());
            this.publishConfirmUntil.remove(player.getUniqueId());
            this.selectedTeam.remove(player.getUniqueId());
            clearShopEditor(player);
            if (this.plugin.configHolograms() != null && arena != null) {
                this.plugin.configHolograms().clear(arena);
            }
            player.closeInventory();
            this.plugin.lobby().send(player);
            Messages.send(player, Messages.get("config-menu.complete-published.konfiguracja-zakonczona-czysty-szablon-mapy-zostal"));
        }
    }

    public Arena arena(final Player player) {
        if (player != null) {
            final String name = this.sessions.get(player.getUniqueId());
            if (name != null) {
                final Arena arena = this.plugin.game().arenas().get(name);
                if (arena != null) {
                    return arena;
                }
            }
        }
        return this.plugin.game().active();
    }

    public void giveTool(final Player player) {
        final ItemStack tool = new ItemBuilder(Material.DIAMOND_PICKAXE)
                .name(Messages.get("config-menu.give-tool.konfigurator-mapy-ppm"))
                .lore(Messages.get("config-menu.give-tool.kliknij-ppm-aby-otworzyc-konfigurator"), Messages.get("config-menu.give-tool.po-wybraniu-opcji-wskazywania-kliknij"), Messages.get("config-menu.give-tool.tym-kilofem-odpowiedni-blok-na-mapie"))
                .enchant(Enchantment.DURABILITY, 1)
                .tag("config-tool")
                .appearance(this.plugin, "menus.icons.config-menu.give-tool.konfigurator-mapy-ppm")
                .build();
        player.getInventory().setItem(4, tool);
        player.getInventory().setHeldItemSlot(4);
        try {
            player.updateInventory();
        } catch (Exception ignored) {
        }
    }

    public boolean isConfiguratorTool(final ItemStack item) {
        if (ItemTag.is(item, "config-tool")) {
            return true;
        }
        if (item == null || item.getType() != Material.DIAMOND_PICKAXE || !item.hasItemMeta()) {
            return false;
        }
        final ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && TOOL_NAME.equals(meta.getDisplayName());
    }

    public void useTool(final Player player, final Block clicked) {
        final Arena arena = arena(player);
        if (arena == null) {
            Messages.send(player, Messages.get("config-menu.use-tool.brak-wybranej-mapy-uzyj-konfiguruj-nazwa"));
            return;
        }
        if (!hasSession(player)) {
            this.sessions.put(player.getUniqueId(), arena.getName().toLowerCase());
        }
        final String mode = this.pendingSelection.get(player.getUniqueId());
        if (mode == null) {
            open(player);
            return;
        }
        if (clicked == null) {
            Messages.send(player, Messages.get("config-menu.use-tool.kliknij-ppm-kilofem-bezposrednio-w-odpowiedni"));
            return;
        }
        if (mode.startsWith("bed:")) {
            if (clicked.getType() != Material.BED_BLOCK) {
                Messages.send(player, Messages.get("config-menu.use-tool.to-nie-jest-lozko-kliknij-ppm"));
                return;
            }
            final TeamColor color = parseTeam(mode.substring(4));
            if (color == null) {
                return;
            }
            arena.team(color).setBed(clicked.getLocation());
            save(arena);
            this.pendingSelection.remove(player.getUniqueId());
            Messages.send(player, Messages.get("config-menu.use-tool.ustawiono-lozko", "VALUE1", color.chat(), "TEAM", color.display()));
            openTeam(player, color);
            return;
        }
        if (mode.startsWith("basegen:")) {
            final TeamColor color = parseTeam(mode.substring(8));
            if (color == null) {
                return;
            }
            arena.team(color).setBaseGenerator(centerAbove(clicked, player));
            save(arena);
            this.pendingSelection.remove(player.getUniqueId());
            Messages.send(player, Messages.get("config-menu.use-tool.ustawiono-generator-bazy", "VALUE1", color.chat(), "TEAM", color.display()));
            openTeam(player, color);
            return;
        }
        if (mode.startsWith("protect1:") || mode.startsWith("protect2:")) {
            final boolean first = mode.startsWith("protect1:");
            final TeamColor color = parseTeam(mode.substring(first ? 9 : 9));
            if (color == null) {
                return;
            }
            final Location point = clicked.getLocation();
            if (first) {
                arena.team(color).setProtectedPos1(point);
            } else {
                arena.team(color).setProtectedPos2(point);
            }
            save(arena);
            this.pendingSelection.remove(player.getUniqueId());
            Messages.send(player, Messages.get("config-menu.use-tool.ustawiono-punkt-strefy-bez-budowania-druzyny", "VALUE1", (first ? "1" : "2"),
                    "VALUE2", color.chat(), "TEAM", color.display()));
            if (arena.team(color).hasProtectedRegion()) {
                Messages.send(player, Messages.get("config-menu.use-tool.strefa-gotowa-zaznaczony-prostokat-jest-chroniony"));
            }
            openTeam(player, color);
            return;
        }
        if (mode.equals("gen:diamond") || mode.equals("gen:emerald")) {
            final GeneratorType type = mode.endsWith("diamond") ? GeneratorType.DIAMOND : GeneratorType.EMERALD;
            final Location loc = centerAbove(clicked, player);
            if (containsBlock(arena.getGenerators().get(type), loc)) {
                Messages.send(player, Messages.get("config-menu.use-tool.w-tym-miejscu-jest-juz-generator"));
                return;
            }
            arena.getGenerators().get(type).add(loc);
            save(arena);
            this.pendingSelection.remove(player.getUniqueId());
            Messages.send(player, Messages.get("config-menu.use-tool.ustawiono-generator", "VALUE1", (type == GeneratorType.DIAMOND ? Messages.get("config-menu.use-tool.diamentow") : Messages.get("config-menu.use-tool.szmaragdow"))));
            openGenerators(player);
            return;
        }
        if (mode.equals("shop:item") || mode.equals("shop:upgrade")) {
            final Location loc = centerAbove(clicked, player);
            final boolean item = mode.equals("shop:item");
            final List<Location> list = item ? arena.getItemShops() : arena.getUpgradeShops();
            if (containsBlock(list, loc)) {
                Messages.send(player, Messages.get("config-menu.use-tool.w-tym-miejscu-jest-juz-taki"));
                return;
            }
            list.add(loc);
            save(arena);
            this.plugin.game().setActive(arena);
            this.plugin.npcs().respawn();
            this.pendingSelection.remove(player.getUniqueId());
            Messages.send(player, item ? Messages.get("config-menu.use-tool.ustawiono-sklep-z-przedmiotami") : Messages.get("config-menu.use-tool.ustawiono-ulepszenia-druzyny"));
            openShops(player);
        }
    }

    public void open(final Player player) {
        final Arena arena = arena(player);
        final Inventory inventory = MenuHolder.create(player, "config-main", 54, Messages.get("menus.configuration.main.title"));
        fill(inventory, (short) 15);
        final List<String> problems = ArenaValidator.validate(arena);
        final boolean ready = problems.isEmpty();
        final boolean pointsOk = arena != null && arena.getLobby() != null && arena.getSpectator() != null;
        final boolean teamsOk = teamsComplete(arena);
        final boolean generatorsOk = arena != null && !arena.getGenerators().get(GeneratorType.DIAMOND).isEmpty() && !arena.getGenerators().get(GeneratorType.EMERALD).isEmpty();
        final boolean shopsOk = arena != null && arena.getItemShops().size() >= 4 && arena.getUpgradeShops().size() >= 4;
        final String world = arena == null || arena.getWorld() == null ? Messages.get("config-menu.open.brak") : arena.getWorld();
        inventory.setItem(0, progressPane(pointsOk, Messages.get("config-menu.open.1-punkty-glowne")));
        inventory.setItem(1, progressPane(teamsOk, Messages.get("config-menu.open.2-druzyny")));
        inventory.setItem(2, progressPane(generatorsOk, Messages.get("config-menu.open.3-generatory")));
        inventory.setItem(3, progressPane(shopsOk, Messages.get("config-menu.open.4-sklepy-npc")));
        inventory.setItem(5, progressPane(true, Messages.get("config-menu.open.5-ustawienia-gry")));
        inventory.setItem(6, progressPane(true, Messages.get("config-menu.open.6-czasy-faz")));
        inventory.setItem(7, progressPane(ready, Messages.get("config-menu.open.7-sprawdzenie-mapy")));
        inventory.setItem(8, progressPane(ready, Messages.get("config-menu.open.8-publikacja")));
        inventory.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name(Messages.get("config-menu.open.konfigurator-mapy"))
                .lore(Messages.get("config-menu.open.mapa", "MAP", (arena == null ? Messages.get("config-menu.open.brak-2") : arena.getName())), Messages.get("config-menu.open.swiat-edycyjny", "WORLD", world), ready ? Messages.get("config-menu.open.konfiguracja-kompletna") : Messages.get("config-menu.open.brakuje-elementow", "VALUE1", problems.size()), isDirty(arena) ? Messages.get("config-menu.open.sa-niezapisane-zmiany-do-publikacji") : Messages.get("config-menu.open.brak-zmian-do-publikacji"), Messages.get("config-menu.open.text-2"), Messages.get("config-menu.open.najprosciej-klikaj-po-kolei-kroki-1"), Messages.get("config-menu.open.na-koncu-uzyj-zatwierdz-mape"))
                .appearance(this.plugin, "menus.icons.config-menu.open.konfigurator-mapy")
                .build());
        inventory.setItem(10, stepItem(Material.BEACON, 1, Messages.get("config-menu.open.punkty-glowne"), pointsOk, configured(arena != null && arena.getLobby() != null,
                Messages.get("config-menu.open.poczekalnia-mapy")), configured(arena != null && arena.getSpectator() != null, Messages.get("config-menu.open.spawn-obserwatorow")),
                "", Messages.get("config-menu.open.kliknij-aby-przejsc-do-kroku-1")));
        inventory.setItem(12, stepItem(Material.WOOL, 2, Messages.get("config-menu.open.druzyny"), teamsOk, teamProgress(arena), Messages.get("config-menu.open.spawn-lozko-generator-bazy-i-strefa"),
                "", Messages.get("config-menu.open.kliknij-aby-przejsc-do-kroku-2")));
        inventory.setItem(14, stepItem(Material.DIAMOND, 3, Messages.get("config-menu.open.generatory"), generatorsOk, Messages.get("config-menu.open.diamenty",
                "VALUE1", count(arena, GeneratorType.DIAMOND)), Messages.get("config-menu.open.szmaragdy", "VALUE1", count(arena, GeneratorType.EMERALD)),
                generatorsOk ? Messages.get("config-menu.open.oba-typy-generatorow-ustawione") : Messages.get("config-menu.open.ustaw-co-najmniej-po-1-generatorze"),
                "", Messages.get("config-menu.open.kliknij-aby-przejsc-do-kroku-3")));
        inventory.setItem(16, stepItem(Material.EMERALD, 4, Messages.get("config-menu.open.sklepy-npc"), shopsOk, Messages.get("config-menu.open.sklepy-przedmiotow-4",
                "VALUE1", (arena == null ? 0 : arena.getItemShops().size())), Messages.get("config-menu.open.sklepy-ulepszen-4", "VALUE1", (arena == null ? 0 : arena.getUpgradeShops().size())),
                shopsOk ? Messages.get("config-menu.open.sklepy-dla-wszystkich-baz-ustawione") : Messages.get("config-menu.open.potrzebujesz-po-4-npc-kazdego-typu"),
                "", Messages.get("config-menu.open.kliknij-aby-przejsc-do-kroku-4")));
        inventory.setItem(19, statusPane(pointsOk, Messages.get("config-menu.open.krok-1-ukonczony"), Messages.get("config-menu.open.krok-1-wymaga-ustawienia")));
        inventory.setItem(21, statusPane(teamsOk, Messages.get("config-menu.open.krok-2-ukonczony"), Messages.get("config-menu.open.krok-2-wymaga-ustawienia")));
        inventory.setItem(23, statusPane(generatorsOk, Messages.get("config-menu.open.krok-3-ukonczony"), Messages.get("config-menu.open.krok-3-wymaga-ustawienia")));
        inventory.setItem(25, statusPane(shopsOk, Messages.get("config-menu.open.krok-4-ukonczony"), Messages.get("config-menu.open.krok-4-wymaga-ustawienia")));
        inventory.setItem(28, stepItem(Material.REDSTONE, 5, Messages.get("config-menu.open.ustawienia-rozgrywki"), true, Messages.get("config-menu.open.graczy-w-druzynie",
                "VALUE1", this.plugin.settings().playersPerTeam()), Messages.get("config-menu.open.minimalnie-graczy", "VALUE1", this.plugin.settings().minPlayers()),
                Messages.get("config-menu.open.odrodzenie-s", "VALUE1", this.plugin.settings().respawnSeconds()), "", Messages.get("config-menu.open.kliknij-aby-edytowac")));
        inventory.setItem(30, stepItem(Material.WATCH, 6, Messages.get("config-menu.open.czasy-faz"), true, Messages.get("config-menu.open.diamenty-ii",
                "VALUE1", fmt(this.plugin.settings().timer("diamond2"))), Messages.get("config-menu.open.zniszczenie-lozek", "VALUE1", fmt(this.plugin.settings().timer("bedsGone"))),
                Messages.get("config-menu.open.nagla-smierc", "VALUE1", fmt(this.plugin.settings().timer("suddenDeath"))), "", Messages.get("config-menu.open.kliknij-aby-edytowac")));
        final List<String> checkLore = new ArrayList<>();
        if (ready) {
            checkLore.add(Messages.get("config-menu.open.wszystkie-wymagane-elementy-sa-ustawione"));
            checkLore.add(Messages.get("config-menu.open.mozesz-przejsc-do-publikacji-mapy"));
        } else {
            checkLore.add(Messages.get("config-menu.open.mapa-nie-jest-jeszcze-gotowa"));
            checkLore.add(Messages.get("config-menu.open.najwazniejsze-braki"));
            int shown = 0;
            for (final String problem : problems) {
                if (shown++ >= 5) {
                    checkLore.add(Messages.get("config-menu.open.i-jeszcze", "VALUE1", (problems.size() - 5)));
                    break;
                }
                checkLore.add(Messages.get("config-menu.open.text", "PROBLEM", problem));
            }
        }
        checkLore.add("");
        checkLore.add(Messages.get("config-menu.open.kliknij-aby-dostac-pelny-raport-na"));
        inventory.setItem(32, new ItemBuilder(ready ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .name(ready ? Messages.get("config-menu.open.mapa-gotowa") : Messages.get("config-menu.open.sprawdz-mape"))
                .lore(checkLore.toArray(new String[checkLore.size()]))
                .build());
        final boolean swmOk = this.plugin.worlds() != null && this.plugin.worlds().available();
        final boolean template = swmOk && this.plugin.worlds().hasTemplate(arena);
        final boolean mainLobbyOk = this.plugin.lobby() != null && this.plugin.lobby().getLobby() != null && this.plugin.lobby().getLobby().getWorld() != null && (arena == null || !this.plugin.lobby().getLobby().getWorld().getName().equalsIgnoreCase(arena.getWorld()));
        inventory.setItem(34, new ItemBuilder(swmOk ? Material.SLIME_BALL : Material.REDSTONE_BLOCK)
                .name(swmOk ? Messages.get("config-menu.open.system-map-gotowy") : Messages.get("config-menu.open.system-map-niedostepny"))
                .lore(swmOk ? new String[] { Messages.get("config-menu.open.slime-world-manager-dziala"), Messages.get("config-menu.open.wersja", "VALUE1", this.plugin.worlds().providerName(), "VALUE2", this.plugin.worlds().providerVersion()), template ? Messages.get("config-menu.open.czysty-szablon-mapy-istnieje") : Messages.get("config-menu.open.szablon-utworzy-sie-przy-zatwierdzaniu"), mainLobbyOk ? Messages.get("config-menu.open.glowne-lobby-jest-na-innym-swiecie") : Messages.get("config-menu.open.ustaw-glowne-lobby-na-innym-swiecie"), "", Messages.get("config-menu.open.po-kazdym-meczu-swiat-jest-wyrzucany"), Messages.get("config-menu.open.i-ladowany-od-nowa-z-czystego") } : new String[] { Messages.get("config-menu.open.nie-wykryto-slime-world-managera"), Messages.get("config-menu.open.bez-niego-mapa-nie-zostanie-opublikowana"), Messages.get("config-menu.open.administrator-serwera-musi-go-poprawnie-uruchomic") })
                .build());
        inventory.setItem(37, statusPane(true, Messages.get("config-menu.open.ustawienia-gry-dostepne"), ""));
        inventory.setItem(39, statusPane(true, Messages.get("config-menu.open.czasy-faz-dostepne"), ""));
        inventory.setItem(41, statusPane(ready, Messages.get("config-menu.open.walidacja-zaliczona"), Messages.get("config-menu.open.najpierw-popraw-czerwone-bledy")));
        inventory.setItem(43, statusPane(swmOk && mainLobbyOk, Messages.get("config-menu.open.system-map-gotowy-2"), Messages.get("config-menu.open.sprawdz-slime-world-manager-i-lobby")));
        inventory.setItem(45, new ItemBuilder(Material.BARRIER)
                .name(Messages.get("config-menu.open.wyjdz-z-konfiguratora"))
                .lore(Messages.get("config-menu.open.wraca-do-glownego-lobby-serwera"), isDirty(arena) ? Messages.get("config-menu.open.uwaga-masz-nieopublikowane-zmiany") : Messages.get("config-menu.open.nie-masz-nieopublikowanych-zmian"), isDirty(arena) ? Messages.get("config-menu.open.shift-klik-wyjdz-mimo-zmian") : Messages.get("config-menu.open.kliknij-aby-zakonczyc"))
                .appearance(this.plugin, "menus.icons.config-menu.open.wyjdz-z-konfiguratora")
                .build());
        inventory.setItem(47, new ItemBuilder(Material.BOOK)
                .name(Messages.get("config-menu.open.pomoc-co-mam-robic"))
                .lore(Messages.get("config-menu.open.1-ustaw-punkty-glowne"), Messages.get("config-menu.open.2-ustaw-4-bazy-spawn-lozko"), Messages.get("config-menu.open.3-dodaj-generatory-diamentow-i-szmaragdow"), Messages.get("config-menu.open.4-dodaj-sklepy-npc"), Messages.get("config-menu.open.5-sprawdz-ustawienia-i-czasy"), Messages.get("config-menu.open.6-kliknij-zatwierdz-mape"), Messages.get("config-menu.open.text-2"), Messages.get("config-menu.open.nie-musisz-wpisywac-zadnych-dodatkowych-komend"))
                .appearance(this.plugin, "menus.icons.config-menu.open.pomoc-co-mam-robic")
                .build());
        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name(ready ? Messages.get("config-menu.open.nastepny-krok-zatwierdz-mape") : Messages.get("config-menu.open.nastepny-krok"))
                .lore(nextStepLore(arena, pointsOk, teamsOk, generatorsOk, shopsOk, ready))
                .build());
        final boolean canPublish = ready && swmOk && mainLobbyOk;
        final Long confirmUntil = this.publishConfirmUntil.get(player.getUniqueId());
        final boolean confirming = canPublish && confirmUntil != null && confirmUntil.longValue() >= System.currentTimeMillis();
        inventory.setItem(51, new ItemBuilder(confirming ? Material.GOLD_BLOCK : (canPublish ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK))
                .name(confirming ? Messages.get("config-menu.open.potwierdz-publikacje") : (canPublish ? Messages.get("config-menu.open.zatwierdz-mape") : Messages.get("config-menu.open.nie-mozna-jeszcze-zatwierdzic")))
                .lore(confirming ? new String[] { Messages.get("config-menu.open.kliknij-jeszcze-raz-w-ciagu-10"), Messages.get("config-menu.open.to-zapisze-czysty-szablon-mapy") } : (canPublish ? new String[] { Messages.get("config-menu.open.ostatni-krok-konfiguracji"), Messages.get("config-menu.open.mapa-zostanie-zapisana-jako-czysty-szablon"), Messages.get("config-menu.open.i-bedzie-automatycznie-odswiezana-po-meczach"), "", Messages.get("config-menu.open.kliknij-a-potem-potwierdz-drugim-kliknieciem") } : new String[] { Messages.get("config-menu.open.najpierw-wykonaj-kroki-oznaczone-na-czerwono"), swmOk ? Messages.get("config-menu.open.slime-world-manager-dziala") : Messages.get("config-menu.open.brak-slime-world-managera"), mainLobbyOk ? Messages.get("config-menu.open.glowne-lobby-poprawne") : Messages.get("config-menu.open.glowne-lobby-musi-byc-na-innym") }))
                .build());
        inventory.setItem(53, new ItemBuilder(Material.COMPASS)
                .name(Messages.get("config-menu.open.teleport-na-spawn-swiata"))
                .lore(Messages.get("config-menu.open.przenosi-na-domyslny-spawn-edytowanej-mapy"), Messages.get("config-menu.open.kliknij-aby-teleportowac"))
                .appearance(this.plugin, "menus.icons.config-menu.open.teleport-na-spawn-swiata")
                .build());
        player.openInventory(inventory);
    }

    public void openPoints(final Player player) {
        final Arena arena = arena(player);
        final Inventory inventory = MenuHolder.create(player, "config-points", 45, Messages.get("menus.configuration.points.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, header(arena, Messages.get("config-menu.open-points.punkty-glowne"), Messages.get("config-menu.open-points.stan-w-odpowiednim-miejscu-i-kliknij")));
        inventory.setItem(20, status(Material.BEACON, Messages.get("config-menu.open-points.poczekalnia-mapy"), arena == null ? null : arena.getLobby(),
                Messages.get("config-menu.open-points.kliknij-ustaw-dokladnie-tutaj")));
        inventory.setItem(24, status(Material.ENDER_PEARL, Messages.get("config-menu.open-points.spawn-obserwatorow"), arena == null ? null : arena.getSpectator(),
                Messages.get("config-menu.open-points.kliknij-ustaw-dokladnie-tutaj")));
        inventory.setItem(31, new ItemBuilder(Material.COMPASS)
                .name(Messages.get("config-menu.open-points.teleport-na-spawn-swiata"))
                .lore(Messages.get("config-menu.open-points.kliknij"))
                .appearance(this.plugin, "menus.icons.config-menu.open-points.teleport-na-spawn-swiata")
                .build());
        inventory.setItem(40, back());
        player.openInventory(inventory);
    }

    public void openTeams(final Player player) {
        final Arena arena = arena(player);
        final Inventory inventory = MenuHolder.create(player, "config-teams", 54, Messages.get("menus.configuration.teams.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, header(arena, Messages.get("config-menu.open-teams.druzyny"), Messages.get("config-menu.open-teams.kliknij-druzyne-i-ustaw-wszystko-po"),
                Messages.get("config-menu.open-teams.kazda-baza-wymaga-5-elementow")));
        final TeamColor[] teams = TeamColor.values();
        final int[] slots = { 19, 21, 23, 25 };
        for (int x = 0; x < teams.length; x++) {
            final TeamColor team = teams[x];
            final int done = teamConfiguredCount(arena, team);
            inventory.setItem(slots[x], new ItemBuilder(Material.WOOL).data(team.dye().getWoolData())
                    .name(Messages.get("config-menu.open-teams.text-2", "VALUE1", team.chat(), "TEAM", team.display().toUpperCase()))
                    .lore(Messages.get("config-menu.open-teams.postep-5", "VALUE1", (done == 5 ? "&a" : "&e"), "DONE", done), configured(arena != null && arena.team(team).getSpawn() != null, Messages.get("config-menu.open-teams.spawn-gracza")), configured(arena != null && arena.team(team).getBed() != null, Messages.get("configuration.labels.bed")), configured(arena != null && arena.team(team).getBaseGenerator() != null, Messages.get("config-menu.open-teams.generator-bazy")), configured(arena != null && arena.team(team).getProtectedPos1() != null, Messages.get("config-menu.open-teams.strefa-punkt-1")), configured(arena != null && arena.team(team).getProtectedPos2() != null, Messages.get("config-menu.open-teams.strefa-punkt-2")), Messages.get("config-menu.open-teams.text"), Messages.get("config-menu.open-teams.kliknij-aby-skonfigurowac-te-baze"))
                    .appearance(this.plugin, "menus.icons.config-menu.open-teams.text-2")
                    .build());
        }
        inventory.setItem(31, new ItemBuilder(Material.BOOK)
                .name(Messages.get("config-menu.open-teams.co-to-jest-strefa-bez-budowania"))
                .lore(Messages.get("config-menu.open-teams.ustawiasz-dwa-przeciwlegle-rogi-prostokata-na"), Messages.get("config-menu.open-teams.w-meczu-nie-da-sie-nad"), Messages.get("config-menu.open-teams.ani-wylewac-wody-lawy-bridge-egg"), Messages.get("config-menu.open-teams.text"), Messages.get("config-menu.open-teams.obejmij-nia-np-generator-bazy-spawn"))
                .appearance(this.plugin, "menus.icons.config-menu.open-teams.co-to-jest-strefa-bez-budowania")
                .build());
        inventory.setItem(49, back());
        player.openInventory(inventory);
    }

    public void openTeam(final Player player, final TeamColor team) {
        final Arena arena = arena(player);
        if (arena == null || team == null) {
            return;
        }
        this.selectedTeam.put(player.getUniqueId(), team);
        final Inventory inventory = MenuHolder.create(player, "config-team", 54, Messages.get("menus.configuration.team.title"));
        fill(inventory, (short) 7);
        final TeamData data = arena.team(team);
        inventory.setItem(4, new ItemBuilder(Material.WOOL).data(team.dye().getWoolData())
                .name(Messages.get("config-menu.open-team.baza", "VALUE1", team.chat(), "TEAM", team.display().toUpperCase()))
                .lore(Messages.get("config-menu.open-team.ustaw-piec-pozycji-ponizej"), Messages.get("config-menu.open-team.zielony-opis-gotowe-czerwony-brak"))
                .appearance(this.plugin, "menus.icons.config-menu.open-team.baza")
                .build());
        inventory.setItem(10, status(Material.ENDER_PEARL, Messages.get("config-menu.open-team.1-spawn-gracza", "VALUE1", team.chat()), data.getSpawn(),
                Messages.get("config-menu.open-team.kliknij-ustaw-dokladnie-tam-gdzie-stoisz")));
        inventory.setItem(12, status(Material.BED, Messages.get("config-menu.open-team.2-lozko", "VALUE1", team.chat()), data.getBed(), Messages.get("config-menu.open-team.kliknij-a-potem-ppm-kilofem-w")));
        inventory.setItem(14, status(Material.IRON_INGOT, Messages.get("config-menu.open-team.3-generator-zelaza-zlota", "VALUE1", team.chat()),
                data.getBaseGenerator(), Messages.get("config-menu.open-team.kliknij-a-potem-ppm-kilofem-w-2")));
        inventory.setItem(20, status(Material.REDSTONE, Messages.get("config-menu.open-team.4-strefa-bez-budowania-punkt-1", "VALUE1", team.chat()),
                data.getProtectedPos1(), Messages.get("config-menu.open-team.kliknij-a-potem-ppm-kilofem-w-3")));
        inventory.setItem(24, status(Material.REDSTONE, Messages.get("config-menu.open-team.5-strefa-bez-budowania-punkt-2", "VALUE1", team.chat()),
                data.getProtectedPos2(), Messages.get("config-menu.open-team.kliknij-a-potem-ppm-kilofem-w-4")));
        final boolean zone = data.hasProtectedRegion();
        inventory.setItem(31, new ItemBuilder(zone ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .name(zone ? Messages.get("config-menu.open-team.strefa-bez-budowania-gotowa") : Messages.get("config-menu.open-team.strefa-niegotowa"))
                .lore(zone ? new String[] { Messages.get("config-menu.open-team.punkt-1", "VALUE1", coords(data.getProtectedPos1())), Messages.get("config-menu.open-team.punkt-2", "VALUE1", coords(data.getProtectedPos2())), "", Messages.get("config-menu.open-team.w-meczu-budowanie-w-tym-obszarze") } : new String[] { Messages.get("config-menu.open-team.ustaw-punkt-1-i-punkt-2"), Messages.get("config-menu.open-team.najlepiej-obejmij-nimi-spawn-generator-i") })
                .build());
        inventory.setItem(33, new ItemBuilder(Material.COMPASS)
                .name(Messages.get("config-menu.open-team.teleport-na-spawn-tej-druzyny"))
                .lore(data.getSpawn() == null ? Messages.get("config-menu.open-team.najpierw-ustaw-spawn") : Messages.get("config-menu.open-team.kliknij-aby-sie-teleportowac"))
                .appearance(this.plugin, "menus.icons.config-menu.open-team.teleport-na-spawn-tej-druzyny")
                .build());
        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name(Messages.get("config-menu.open-team.wroc-do-druzyn"))
                .appearance(this.plugin, "menus.icons.config-menu.open-team.wroc-do-druzyn")
                .build());
        player.openInventory(inventory);
    }

    public void openGenerators(final Player player) {
        final Arena arena = arena(player);
        final Inventory inventory = MenuHolder.create(player, "config-generators", 45, Messages.get("menus.configuration.generators.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, header(arena, Messages.get("config-menu.open-generators.generatory"), Messages.get("config-menu.open-generators.wybierz-typ-zamknie-sie-menu"),
                Messages.get("config-menu.open-generators.a-nastepnie-kliknij-ppm-kilofem-w")));
        inventory.setItem(19, new ItemBuilder(Material.DIAMOND)
                .name(Messages.get("config-menu.open-generators.dodaj-generator-diamentow"))
                .lore(Messages.get("config-menu.open-generators.aktualnie", "VALUE1", count(arena, GeneratorType.DIAMOND)), Messages.get("config-menu.open-generators.text"), Messages.get("config-menu.open-generators.kliknij-i-wskaz-blok-kilofem"))
                .appearance(this.plugin, "menus.icons.config-menu.open-generators.dodaj-generator-diamentow")
                .build());
        inventory.setItem(28, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name(Messages.get("config-menu.open-generators.usun-wszystkie-generatory-diamentow"))
                .lore(Messages.get("config-menu.open-generators.aktualnie", "VALUE1", count(arena, GeneratorType.DIAMOND)), Messages.get("config-menu.open-generators.shift-klik-usun-wszystkie"))
                .appearance(this.plugin, "menus.icons.config-menu.open-generators.usun-wszystkie-generatory-diamentow")
                .build());
        inventory.setItem(25, new ItemBuilder(Material.EMERALD)
                .name(Messages.get("config-menu.open-generators.dodaj-generator-szmaragdow"))
                .lore(Messages.get("config-menu.open-generators.aktualnie", "VALUE1", count(arena, GeneratorType.EMERALD)), Messages.get("config-menu.open-generators.text"), Messages.get("config-menu.open-generators.kliknij-i-wskaz-blok-kilofem"))
                .appearance(this.plugin, "menus.icons.config-menu.open-generators.dodaj-generator-szmaragdow")
                .build());
        inventory.setItem(34, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name(Messages.get("config-menu.open-generators.usun-wszystkie-generatory-szmaragdow"))
                .lore(Messages.get("config-menu.open-generators.aktualnie", "VALUE1", count(arena, GeneratorType.EMERALD)), Messages.get("config-menu.open-generators.shift-klik-usun-wszystkie"))
                .appearance(this.plugin, "menus.icons.config-menu.open-generators.usun-wszystkie-generatory-szmaragdow")
                .build());
        inventory.setItem(40, back());
        player.openInventory(inventory);
    }

    public void openShops(final Player player) {
        clearShopEditor(player);
        final Arena arena = arena(player);
        final Inventory inventory = MenuHolder.create(player, "config-shops", 45, Messages.get("menus.configuration.shops.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, header(arena, Messages.get("config-menu.open-shops.sklepy-npc"), Messages.get("config-menu.open-shops.wybierz-sklep-a-potem-ppm-kilofem"),
                Messages.get("config-menu.open-shops.w-blok-pod-miejscem-gdzie-ma")));
        inventory.setItem(19, new ItemBuilder(Material.CHEST)
                .name(Messages.get("config-menu.open-shops.dodaj-sklep-z-przedmiotami"))
                .lore(Messages.get("config-menu.open-shops.aktualnie", "VALUE1", (arena == null ? 0 : arena.getItemShops().size())), Messages.get("config-menu.open-shops.text"), Messages.get("config-menu.open-shops.kliknij-i-wskaz-blok-kilofem"))
                .appearance(this.plugin, "menus.icons.config-menu.open-shops.dodaj-sklep-z-przedmiotami")
                .build());
        inventory.setItem(28, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name(Messages.get("config-menu.open-shops.usun-wszystkie-sklepy-z-przedmiotami"))
                .lore(Messages.get("config-menu.open-shops.shift-klik-usun-wszystkie"))
                .appearance(this.plugin, "menus.icons.config-menu.open-shops.usun-wszystkie-sklepy-z-przedmiotami")
                .build());
        inventory.setItem(25, new ItemBuilder(Material.ANVIL)
                .name(Messages.get("config-menu.open-shops.dodaj-sklep-ulepszen-druzyny"))
                .lore(Messages.get("config-menu.open-shops.aktualnie", "VALUE1", (arena == null ? 0 : arena.getUpgradeShops().size())), Messages.get("config-menu.open-shops.text"), Messages.get("config-menu.open-shops.kliknij-i-wskaz-blok-kilofem"))
                .appearance(this.plugin, "menus.icons.config-menu.open-shops.dodaj-sklep-ulepszen-druzyny")
                .build());
        inventory.setItem(34, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name(Messages.get("config-menu.open-shops.usun-wszystkie-sklepy-ulepszen"))
                .lore(Messages.get("config-menu.open-shops.shift-klik-usun-wszystkie"))
                .appearance(this.plugin, "menus.icons.config-menu.open-shops.usun-wszystkie-sklepy-ulepszen")
                .build());
        inventory.setItem(31, new ItemBuilder(Material.CHEST)
                .name(Messages.get("config-menu.open-shops.konfiguruj-sklep-z-przedmiotami"))
                .lore(Messages.get("config-menu.open-shops.ceny-waluty-ilosci-i-wlaczanie-wylaczanie"), Messages.get("config-menu.open-shops.kazdego-przedmiotu-bez-grzebania-w-pliku"), Messages.get("config-menu.open-shops.text"), Messages.get("config-menu.open-shops.kliknij-aby-edytowac"))
                .appearance(this.plugin, "menus.icons.config-menu.open-shops.konfiguruj-sklep-z-przedmiotami")
                .build());
        inventory.setItem(33, new ItemBuilder(Material.DIAMOND)
                .name(Messages.get("config-menu.open-shops.konfiguruj-ulepszenia-druzyny"))
                .lore(Messages.get("config-menu.open-shops.ceny-sharpness-protection-haste-forge"), Messages.get("config-menu.open-shops.heal-pool-dragon-buff-oraz-pulapek"), Messages.get("config-menu.open-shops.text"), Messages.get("config-menu.open-shops.kliknij-aby-edytowac"))
                .appearance(this.plugin, "menus.icons.config-menu.open-shops.konfiguruj-ulepszenia-druzyny")
                .build());
        inventory.setItem(40, back());
        player.openInventory(inventory);
    }

    private void openShopItemEditor(final Player player) {
        this.shopEditorMode.put(player.getUniqueId(), "items");
        this.selectedShopItem.remove(player.getUniqueId());
        this.selectedToolTier.remove(player.getUniqueId());
        final Inventory inventory = MenuHolder.create(player, "config-shops", 54, Messages.get("menus.configuration.shops.title"));
        fill(inventory, (short) 7);
        final ShopItem[] values = ShopItem.values();
        for (int slot = 0; slot < values.length && slot < 45; slot++) {
            final ShopItem item = values[slot];
            final Material currency = this.plugin.settings().shopCurrency(item);
            inventory.setItem(slot, new ItemBuilder(item.icon(), Math.min(64, this.plugin.settings().shopAmount(item)))
                    .name((this.plugin.settings().shopEnabled(item) ? "&a" : "&c") + item.display())
                    .lore(Messages.get("config-menu.open-shop-item-editor.stan", "VALUE1", (this.plugin.settings().shopEnabled(item) ? Messages.get("config-menu.open-shop-item-editor.wlaczony") : Messages.get("config-menu.open-shop-item-editor.wylaczony"))), item == ShopItem.PICKAXE || item == ShopItem.AXE ? Messages.get("config-menu.open-shop-item-editor.cena-poziomowa-kliknij-aby-edytowac") : Messages.get("config-menu.open-shop-item-editor.cena", "VALUE1", this.plugin.settings().shopPrice(item), "VALUE2", currencyLabel(currency)), Messages.get("config-menu.open-shop-item-editor.ilosc", "VALUE1", this.plugin.settings().shopAmount(item)), Messages.get("config-menu.open-shop-item-editor.text"), Messages.get("config-menu.open-shop-item-editor.kliknij-aby-edytowac"))
                    .build());
        }
        inventory.setItem(47, new ItemBuilder(Material.BOOK)
                .name(Messages.get("config-menu.open-shop-item-editor.jak-to-dziala"))
                .lore(Messages.get("config-menu.open-shop-item-editor.kliknij-dowolny-przedmiot"), Messages.get("config-menu.open-shop-item-editor.w-kolejnym-oknie-zmienisz-cene-ilosc"), Messages.get("config-menu.open-shop-item-editor.walute-oraz-wlaczysz-wylaczysz-pozycje"), Messages.get("config-menu.open-shop-item-editor.text"), Messages.get("config-menu.open-shop-item-editor.kilof-i-siekiera-maja-osobne-ceny"))
                .appearance(this.plugin, "menus.icons.config-menu.open-shop-item-editor.jak-to-dziala")
                .build());
        inventory.setItem(49, back());
        player.openInventory(inventory);
    }

    private void openShopItemDetail(final Player player, final ShopItem item) {
        if (item == null) {
            openShopItemEditor(player);
            return;
        }
        this.shopEditorMode.put(player.getUniqueId(), "item-detail");
        this.selectedShopItem.put(player.getUniqueId(), item);
        this.selectedToolTier.remove(player.getUniqueId());
        final Inventory inventory = MenuHolder.create(player, "config-shops", 45, Messages.get("menus.configuration.shops.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, new ItemBuilder(item.icon(), Math.min(64, this.plugin.settings().shopAmount(item)))
                .name(Messages.get("config-menu.open-shop-item-detail.text", "ITEM", item.display().toUpperCase()))
                .lore(Messages.get("config-menu.open-shop-item-detail.stan", "VALUE1", (this.plugin.settings().shopEnabled(item) ? Messages.get("config-menu.open-shop-item-detail.wlaczony") : Messages.get("config-menu.open-shop-item-detail.wylaczony"))), Messages.get("config-menu.open-shop-item-detail.ilosc", "VALUE1", this.plugin.settings().shopAmount(item)), item == ShopItem.PICKAXE || item == ShopItem.AXE ? Messages.get("config-menu.open-shop-item-detail.cena-ustawiana-osobno-dla-kazdego-poziomu") : Messages.get("config-menu.open-shop-item-detail.cena", "VALUE1", this.plugin.settings().shopPrice(item), "VALUE2", currencyLabel(this.plugin.settings().shopCurrency(item))))
                .build());
        if (item == ShopItem.PICKAXE || item == ShopItem.AXE) {
            final int[] slots = { 19, 21, 23, 25 };
            for (int tier = 1; tier <= 4; tier++) {
                inventory.setItem(slots[tier - 1], new ItemBuilder(item.icon(), tier)
                        .name(Messages.get("config-menu.open-shop-item-detail.poziom", "TIER", tier))
                        .lore(Messages.get("config-menu.open-shop-item-detail.cena", "VALUE1", this.plugin.settings().toolPrice(item, tier), "VALUE2", currencyLabel(this.plugin.settings().toolCurrency(item, tier))), Messages.get("config-menu.open-shop-item-detail.text-2"), Messages.get("config-menu.open-shop-item-detail.kliknij-aby-edytowac-ten-poziom"))
                        .build());
            }
        } else {
            inventory.setItem(19, new ItemBuilder(Material.REDSTONE)
                    .name(Messages.get("config-menu.open-shop-item-detail.cena-1"))
                    .lore(Messages.get("config-menu.open-shop-item-detail.aktualnie", "VALUE1", this.plugin.settings().shopPrice(item)))
                    .appearance(this.plugin, "menus.icons.config-menu.open-shop-item-detail.cena-1")
                    .build());
            inventory.setItem(21, new ItemBuilder(Material.EMERALD)
                    .name(Messages.get("config-menu.open-shop-item-detail.cena-1-2"))
                    .lore(Messages.get("config-menu.open-shop-item-detail.aktualnie", "VALUE1", this.plugin.settings().shopPrice(item)))
                    .appearance(this.plugin, "menus.icons.config-menu.open-shop-item-detail.cena-1-2")
                    .build());
            inventory.setItem(23, new ItemBuilder(Material.REDSTONE)
                    .name(Messages.get("config-menu.open-shop-item-detail.ilosc-1"))
                    .lore(Messages.get("config-menu.open-shop-item-detail.aktualnie", "VALUE1", this.plugin.settings().shopAmount(item)))
                    .appearance(this.plugin, "menus.icons.config-menu.open-shop-item-detail.ilosc-1")
                    .build());
            inventory.setItem(25, new ItemBuilder(Material.EMERALD)
                    .name(Messages.get("config-menu.open-shop-item-detail.ilosc-1-2"))
                    .lore(Messages.get("config-menu.open-shop-item-detail.aktualnie", "VALUE1", this.plugin.settings().shopAmount(item)))
                    .appearance(this.plugin, "menus.icons.config-menu.open-shop-item-detail.ilosc-1-2")
                    .build());
            inventory.setItem(31, new ItemBuilder(this.plugin.settings().shopCurrency(item))
                    .name(Messages.get("config-menu.open-shop-item-detail.zmien-walute"))
                    .lore(Messages.get("config-menu.open-shop-item-detail.aktualnie", "VALUE1", currencyLabel(this.plugin.settings().shopCurrency(item))), Messages.get("config-menu.open-shop-item-detail.kliknij-nastepna-waluta"))
                    .build());
        }
        inventory.setItem(33, new ItemBuilder(this.plugin.settings().shopEnabled(item) ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .name(this.plugin.settings().shopEnabled(item) ? Messages.get("config-menu.open-shop-item-detail.przedmiot-wlaczony") : Messages.get("config-menu.open-shop-item-detail.przedmiot-wylaczony"))
                .lore(Messages.get("config-menu.open-shop-item-detail.kliknij-aby-przelaczyc"))
                .build());
        inventory.setItem(40, back());
        player.openInventory(inventory);
    }

    private void openToolTierDetail(final Player player, final ShopItem item, final int tier) {
        this.selectedShopItem.put(player.getUniqueId(), item);
        this.selectedToolTier.put(player.getUniqueId(), tier);
        this.shopEditorMode.put(player.getUniqueId(), "tool-tier");
        final Inventory inventory = MenuHolder.create(player, "config-shops", 45, Messages.get("menus.configuration.shops.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, new ItemBuilder(item.icon(), tier)
                .name(Messages.get("config-menu.open-tool-tier-detail.poziom", "ITEM", item.display().toUpperCase(), "TIER", tier))
                .lore(Messages.get("config-menu.open-tool-tier-detail.cena", "VALUE1", this.plugin.settings().toolPrice(item, tier), "VALUE2", currencyLabel(this.plugin.settings().toolCurrency(item, tier))))
                .build());
        inventory.setItem(19, new ItemBuilder(Material.REDSTONE)
                .name(Messages.get("config-menu.open-tool-tier-detail.cena-1"))
                .appearance(this.plugin, "menus.icons.config-menu.open-tool-tier-detail.cena-1")
                .build());
        inventory.setItem(21, new ItemBuilder(Material.EMERALD)
                .name(Messages.get("config-menu.open-tool-tier-detail.cena-1-2"))
                .appearance(this.plugin, "menus.icons.config-menu.open-tool-tier-detail.cena-1-2")
                .build());
        inventory.setItem(31, new ItemBuilder(this.plugin.settings().toolCurrency(item, tier))
                .name(Messages.get("config-menu.open-tool-tier-detail.zmien-walute"))
                .lore(Messages.get("config-menu.open-tool-tier-detail.aktualnie", "VALUE1", currencyLabel(this.plugin.settings().toolCurrency(item, tier))), Messages.get("config-menu.open-tool-tier-detail.kliknij-nastepna-waluta"))
                .build());
        inventory.setItem(40, back());
        player.openInventory(inventory);
    }

    private void openUpgradePriceEditor(final Player player) {
        this.shopEditorMode.put(player.getUniqueId(), "upgrades");
        this.selectedShopItem.remove(player.getUniqueId());
        this.selectedToolTier.remove(player.getUniqueId());
        final Inventory inventory = MenuHolder.create(player, "config-shops", 45, Messages.get("menus.configuration.shops.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, new ItemBuilder(Material.DIAMOND)
                .name(Messages.get("config-menu.open-upgrade-price-editor.ceny-ulepszen-druzyny"))
                .lore(Messages.get("config-menu.open-upgrade-price-editor.zwykly-klik-1-diament"), Messages.get("config-menu.open-upgrade-price-editor.shift-klik-1-diament"), Messages.get("config-menu.open-upgrade-price-editor.zmiany-zapisuja-sie-od-razu"))
                .appearance(this.plugin, "menus.icons.config-menu.open-upgrade-price-editor.ceny-ulepszen-druzyny")
                .build());
        final String[] keys = { "sharpness", "protection", "protection", "protection", "protection", "haste", "haste", "forge", "forge", "forge",
                "forge", "healPool", "dragonBuff", "traps", "traps", "traps" };
        final int[] levels = { 0, 1, 2, 3, 4, 1, 2, 1, 2, 3, 4, 0, 0, 1, 2, 3 };
        final int[] defaults = { 8, 5, 10, 20, 30, 4, 6, 4, 8, 12, 16, 3, 5, 1, 2, 4 };
        final String[] names = { Messages.get("config-menu.open-upgrade-price-editor.ostrzone-miecze"), Messages.get("config-menu.open-upgrade-price-editor.ochrona-i"),
                Messages.get("config-menu.open-upgrade-price-editor.ochrona-ii"), Messages.get("config-menu.open-upgrade-price-editor.ochrona-iii"), Messages.get("config-menu.open-upgrade-price-editor.ochrona-iv"),
                Messages.get("config-menu.open-upgrade-price-editor.pospiech-i"), Messages.get("config-menu.open-upgrade-price-editor.pospiech-ii"), Messages.get("config-menu.open-upgrade-price-editor.kuznia-i"),
                Messages.get("config-menu.open-upgrade-price-editor.kuznia-ii"), Messages.get("config-menu.open-upgrade-price-editor.kuznia-iii"), Messages.get("config-menu.open-upgrade-price-editor.kuznia-iv"),
                Messages.get("config-menu.open-upgrade-price-editor.strefa-leczenia"), Messages.get("config-menu.open-upgrade-price-editor.wzmocnienie-smoka"),
                Messages.get("config-menu.open-upgrade-price-editor.pulapka-1"), Messages.get("config-menu.open-upgrade-price-editor.pulapka-2"), Messages.get("config-menu.open-upgrade-price-editor.pulapka-3") };
        final int[] slots = { 9, 10, 11, 12, 13, 18, 19, 20, 21, 22, 23, 27, 28, 29, 30, 31 };
        for (int i = 0; i < slots.length; i++) {
            final int price = this.plugin.settings().upgradePrice(keys[i], levels[i], defaults[i]);
            inventory.setItem(slots[i], new ItemBuilder(Material.DIAMOND, Math.max(1, Math.min(64, price)))
                    .name(Messages.get("config-menu.open-upgrade-price-editor.text-2", "VALUE1", names[i]))
                    .lore(Messages.get("config-menu.open-upgrade-price-editor.cena-diamentow", "PRICE", price), Messages.get("config-menu.open-upgrade-price-editor.text"), Messages.get("config-menu.open-upgrade-price-editor.kliknij-1"), Messages.get("config-menu.open-upgrade-price-editor.shift-klik-1"))
                    .appearance(this.plugin, "menus.icons.config-menu.open-upgrade-price-editor.text-2")
                    .build());
        }
        inventory.setItem(40, back());
        player.openInventory(inventory);
    }

    public void openSettings(final Player player) {
        final Inventory inventory = MenuHolder.create(player, "config-settings", 54, Messages.get("menus.configuration.settings.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, header(arena(player), Messages.get("config-menu.open-settings.ustawienia-rozgrywki"), Messages.get("config-menu.open-settings.lpm-zwieksza-ppm-zmniejsza-wartosc")));
        inventory.setItem(10, new ItemBuilder(Material.EMERALD)
                .name(Messages.get("config-menu.open-settings.graczy-w-druzynie"))
                .lore(Messages.get("config-menu.open-settings.aktualnie", "VALUE1", this.plugin.settings().playersPerTeam()), Messages.get("config-menu.open-settings.lpm-1-ppm-1"))
                .appearance(this.plugin, "menus.icons.config-menu.open-settings.graczy-w-druzynie")
                .build());
        inventory.setItem(12, new ItemBuilder(Material.REDSTONE)
                .name(Messages.get("config-menu.open-settings.minimalna-liczba-graczy"))
                .lore(Messages.get("config-menu.open-settings.aktualnie", "VALUE1", this.plugin.settings().minPlayers()), Messages.get("config-menu.open-settings.lpm-1-ppm-1"))
                .appearance(this.plugin, "menus.icons.config-menu.open-settings.minimalna-liczba-graczy")
                .build());
        inventory.setItem(14, new ItemBuilder(Material.WATCH)
                .name(Messages.get("config-menu.open-settings.odliczanie-do-startu"))
                .lore(Messages.get("config-menu.open-settings.aktualnie-s", "VALUE1", this.plugin.settings().countdown()), Messages.get("config-menu.open-settings.lpm-5-s-ppm-5-s"))
                .appearance(this.plugin, "menus.icons.config-menu.open-settings.odliczanie-do-startu")
                .build());
        inventory.setItem(16, new ItemBuilder(Material.FEATHER)
                .name(Messages.get("config-menu.open-settings.czas-odrodzenia"))
                .lore(Messages.get("config-menu.open-settings.aktualnie-s", "VALUE1", this.plugin.settings().respawnSeconds()), Messages.get("config-menu.open-settings.lpm-1-s-ppm-1-s"))
                .appearance(this.plugin, "menus.icons.config-menu.open-settings.czas-odrodzenia")
                .build());
        inventory.setItem(28, new ItemBuilder(Material.GOLDEN_APPLE)
                .name(Messages.get("config-menu.open-settings.ochrona-po-odrodzeniu"))
                .lore(Messages.get("config-menu.open-settings.aktualnie-s", "VALUE1", this.plugin.settings().respawnProtectionSeconds()), Messages.get("config-menu.open-settings.lpm-1-s-ppm-1-s"))
                .appearance(this.plugin, "menus.icons.config-menu.open-settings.ochrona-po-odrodzeniu")
                .build());
        inventory.setItem(30, new ItemBuilder(Material.WATCH)
                .name(Messages.get("config-menu.open-settings.czas-na-ponowne-polaczenie"))
                .lore(Messages.get("config-menu.open-settings.aktualnie-s", "VALUE1", this.plugin.settings().reconnectSeconds()), Messages.get("config-menu.open-settings.lpm-30-s-ppm-30-s"))
                .appearance(this.plugin, "menus.icons.config-menu.open-settings.czas-na-ponowne-polaczenie")
                .build());
        inventory.setItem(32, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name(Messages.get("config-menu.open-settings.poziom-pustki"))
                .lore(Messages.get("config-menu.open-settings.aktualnie-y", "VALUE1", this.plugin.settings().voidY()), Messages.get("config-menu.open-settings.lpm-1-ppm-1"))
                .appearance(this.plugin, "menus.icons.config-menu.open-settings.poziom-pustki")
                .build());
        inventory.setItem(49, back());
        player.openInventory(inventory);
    }

    public void openTimers(final Player player) {
        final Inventory inventory = MenuHolder.create(player, "config-timers", 45, Messages.get("menus.configuration.timers.title"));
        fill(inventory, (short) 7);
        inventory.setItem(4, header(arena(player), Messages.get("config-menu.open-timers.czasy-faz"), Messages.get("config-menu.open-timers.lpm-ppm-30-s-shift-klik")));
        final String[] keys = { "diamond2", "emerald2", "diamond3", "emerald3", "bedsGone", "suddenDeath", "end" };
        final String[] names = { Messages.get("config-menu.open-timers.diamenty-ii"), Messages.get("config-menu.open-timers.szmaragdy-ii"),
                Messages.get("config-menu.open-timers.diamenty-iii"), Messages.get("config-menu.open-timers.szmaragdy-iii"), Messages.get("config-menu.open-timers.zniszczenie-lozek"),
                Messages.get("config-menu.open-timers.nagla-smierc-border"), Messages.get("config-menu.open-timers.zamkniecie-bordera") };
        final Material[] mats = { Material.DIAMOND, Material.EMERALD, Material.DIAMOND, Material.EMERALD_BLOCK, Material.BED, Material.DRAGON_EGG, Material.WATCH };
        final int[] slots = { 10, 12, 14, 16, 28, 30, 32 };
        for (int x = 0; x < keys.length; x++) {
            inventory.setItem(slots[x], new ItemBuilder(mats[x])
                    .name(Messages.get("config-menu.open-timers.text", "VALUE1", names[x]))
                    .lore(Messages.get("config-menu.open-timers.aktualnie", "VALUE1", fmt(this.plugin.settings().timer(keys[x]))), x == 5 ? Messages.get("config-menu.open-timers.od-tej-chwili-border-sie-kurczy") : x == 6 ? Messages.get("config-menu.open-timers.border-1x1-obrazenia-az-do-zwyciestwa") : Messages.get("config-menu.open-timers.czas-od-rozpoczecia-meczu"), Messages.get("config-menu.open-timers.lpm-30-s-ppm-30-s"), Messages.get("config-menu.open-timers.shift-lpm-60-s-shift-ppm"))
                    .build());
        }
        inventory.setItem(40, back());
        player.openInventory(inventory);
    }

    public void click(final Player player, final int slot, final boolean left, final boolean shift) {
        clickMain(player, slot, shift);
    }

    public void clickMain(final Player player, final int slot) {
        clickMain(player, slot, false);
    }

    public void clickMain(final Player player, final int slot, final boolean shift) {
        final Arena arena = arena(player);
        if (arena == null) {
            player.closeInventory();
            Messages.send(player, Messages.get("config-menu.click-main.brak-aktywnej-mapy-uzyj-konfiguruj-nazwa"));
            return;
        }
        if (slot == 10) {
            openPoints(player);
            return;
        }
        if (slot == 12) {
            openTeams(player);
            return;
        }
        if (slot == 14) {
            openGenerators(player);
            return;
        }
        if (slot == 16) {
            openShops(player);
            return;
        }
        if (slot == 28) {
            openSettings(player);
            return;
        }
        if (slot == 30) {
            openTimers(player);
            return;
        }
        if (slot == 32) {
            sendValidation(player, arena);
            return;
        }
        if (slot == 49) {
            openNextStep(player, arena);
            return;
        }
        if (slot == 53) {
            final World world = Bukkit.getWorld(arena.getWorld());
            if (world != null) {
                player.teleport(world.getSpawnLocation().clone().add(.5, 0, .5));
                player.closeInventory();
                Messages.send(player, Messages.get("config-menu.click-main.przeniesiono-na-spawn-swiata-ppm-kilofem"));
            }
            return;
        }
        if (slot == 51) {
            save(arena);
            final List<String> problems = ArenaValidator.validate(arena);
            final boolean mainLobbyOk = this.plugin.lobby() != null && this.plugin.lobby().getLobby() != null && this.plugin.lobby().getLobby().getWorld() != null && !this.plugin.lobby().getLobby().getWorld().getName().equalsIgnoreCase(arena.getWorld());
            if (!problems.isEmpty()) {
                this.publishConfirmUntil.remove(player.getUniqueId());
                sendValidation(player, arena);
                Messages.send(player, Messages.get("config-menu.click-main.nie-zatwierdze-niekompletnej-mapy-uzupelnij-czerwone"));
                open(player);
                return;
            }
            if (this.plugin.worlds() == null || !this.plugin.worlds().available()) {
                this.publishConfirmUntil.remove(player.getUniqueId());
                Messages.send(player, Messages.get("config-menu.click-main.brak-dzialajacego-slime-world-managera-administrator"));
                open(player);
                return;
            }
            if (!mainLobbyOk) {
                this.publishConfirmUntil.remove(player.getUniqueId());
                Messages.send(player, Messages.get("config-menu.click-main.najpierw-ustaw-glowne-lobby-na-innym"));
                Messages.send(player, Messages.get("config-menu.click-main.nie-moge-wyladowac-mapy-jezeli-lobby"));
                open(player);
                return;
            }
            final Long until = this.publishConfirmUntil.get(player.getUniqueId());
            if (until == null || until.longValue() < System.currentTimeMillis()) {
                this.publishConfirmUntil.put(player.getUniqueId(), System.currentTimeMillis() + 10000L);
                Messages.send(player, Messages.get("config-menu.click-main.potwierdzenie-kliknij-ponownie-zatwierdz-mape-w"));
                open(player);
                return;
            }
            this.publishConfirmUntil.remove(player.getUniqueId());
            this.selectedTeam.remove(player.getUniqueId());
            clearShopEditor(player);
            if (this.plugin.configHolograms() != null && arena != null) {
                this.plugin.configHolograms().clear(arena);
            }
            player.closeInventory();
            Messages.send(player, Messages.get("config-menu.click-main.zapisywanie-czystego-szablonu-mapy-niczego-teraz"));
            if (this.plugin.worlds().publishTemplate(player, arena)) {
                completePublished(player, arena);
            } else {
                if (this.plugin.configHolograms() != null) {
                    this.plugin.configHolograms().refresh(arena);
                }
                open(player);
            }
            return;
        }
        if (slot == 45) {
            if (isDirty(arena) && !shift) {
                Messages.send(player, Messages.get("config-menu.click-main.masz-nieopublikowane-zmiany-shift-klik-w"));
                open(player);
                return;
            }
            endSession(player);
        }
    }

    public void clickPoints(final Player player, final int slot) {
        final Arena arena = arena(player);
        if (arena == null) {
            return;
        }
        if (slot == 20) {
            arena.setLobby(player.getLocation().clone());
            save(arena);
            Messages.send(player, Messages.get("config-menu.click-points.ustawiono-poczekalnie-mapy-dokladnie-w-tym"));
            openPoints(player);
            return;
        }
        if (slot == 24) {
            arena.setSpectator(player.getLocation().clone());
            save(arena);
            Messages.send(player, Messages.get("config-menu.click-points.ustawiono-spawn-obserwatorow-dokladnie-w-tym"));
            openPoints(player);
            return;
        }
        if (slot == 31) {
            final World world = Bukkit.getWorld(arena.getWorld());
            if (world != null) {
                player.teleport(world.getSpawnLocation().clone().add(.5, 0, .5));
                player.closeInventory();
            }
            return;
        }
        if (slot == 40) {
            open(player);
        }
    }

    public void clickTeams(final Player player, final int slot) {
        final Arena arena = arena(player);
        if (arena == null) {
            return;
        }
        final TeamColor[] teams = TeamColor.values();
        final int[] slots = { 19, 21, 23, 25 };
        for (int x = 0; x < teams.length; x++) {
            if (slot == slots[x]) {
                openTeam(player, teams[x]);
                return;
            }
        }
        if (slot == 49) {
            open(player);
        }
    }

    public void clickTeam(final Player player, final int slot) {
        final Arena arena = arena(player);
        if (arena == null) {
            return;
        }
        final TeamColor team = this.selectedTeam.get(player.getUniqueId());
        if (team == null) {
            openTeams(player);
            return;
        }
        if (slot == 10) {
            arena.team(team).setSpawn(player.getLocation().clone());
            save(arena);
            Messages.send(player, Messages.get("config-menu.click-team.ustawiono-spawn", "VALUE1", team.chat(), "TEAM", team.display()));
            openTeam(player, team);
            return;
        }
        if (slot == 12) {
            beginSelection(player, "bed:" + team
                    .name(), Messages.get("config-menu.click-team.kliknij-ppm-kilofem-w-lozko-druzyny", "VALUE1", team.chat(), "TEAM", team.display()));
            return;
        }
        if (slot == 14) {
            beginSelection(player, "basegen:" + team
                    .name(), Messages.get("config-menu.click-team.kliknij-ppm-kilofem-w-blok-pod", "VALUE1", team.chat(), "TEAM", team.display()));
            return;
        }
        if (slot == 20) {
            beginSelection(player, "protect1:" + team
                    .name(), Messages.get("config-menu.click-team.kliknij-ppm-kilofem-w-pierwszy-rog", "VALUE1", team.chat(), "TEAM", team.display()));
            return;
        }
        if (slot == 24) {
            beginSelection(player, "protect2:" + team
                    .name(), Messages.get("config-menu.click-team.kliknij-ppm-kilofem-w-przeciwlegly-rog", "VALUE1", team.chat(), "TEAM", team.display()));
            return;
        }
        if (slot == 33) {
            final Location spawn = arena.team(team).getSpawn();
            if (spawn != null) {
                player.teleport(spawn);
                player.closeInventory();
                Messages.send(player, Messages.get("config-menu.click-team.przeniesiono-na-spawn-druzyny-ppm-kilofem"));
            }
            return;
        }
        if (slot == 49) {
            openTeams(player);
            return;
        }
    }

    public void clickGenerators(final Player player, final int slot, final boolean shift) {
        final Arena arena = arena(player);
        if (arena == null) {
            return;
        }
        if (slot == 19) {
            beginSelection(player, "gen:diamond", Messages.get("config-menu.click-generators.kliknij-ppm-kilofem-w-blok-pod"));
            return;
        }
        if (slot == 25) {
            beginSelection(player, "gen:emerald", Messages.get("config-menu.click-generators.kliknij-ppm-kilofem-w-blok-pod-2"));
            return;
        }
        if (slot == 28 && shift) {
            arena.getGenerators().get(GeneratorType.DIAMOND).clear();
            save(arena);
            Messages.send(player, Messages.get("config-menu.click-generators.usunieto-wszystkie-generatory-diamentow"));
            openGenerators(player);
            return;
        }
        if (slot == 34 && shift) {
            arena.getGenerators().get(GeneratorType.EMERALD).clear();
            save(arena);
            Messages.send(player, Messages.get("config-menu.click-generators.usunieto-wszystkie-generatory-szmaragdow"));
            openGenerators(player);
            return;
        }
        if (slot == 28 || slot == 34) {
            Messages.send(player, Messages.get("config-menu.click-generators.aby-usunac-wszystkie-generatory-uzyj-shift"));
            return;
        }
        if (slot == 40) {
            open(player);
        }
    }

    public void clickShops(final Player player, final int slot, final boolean shift) {
        final Arena arena = arena(player);
        if (arena == null) {
            return;
        }
        final UUID playerId = player.getUniqueId();
        final String mode = this.shopEditorMode.get(playerId);
        if ("items".equals(mode)) {
            if (slot == 49) {
                clearShopEditor(player);
                openShops(player);
                return;
            }
            final ShopItem[] values = ShopItem.values();
            if (slot >= 0 && slot < values.length) {
                openShopItemDetail(player, values[slot]);
                return;
            }
            return;
        }
        if ("item-detail".equals(mode)) {
            final ShopItem item = this.selectedShopItem.get(playerId);
            if (item == null) {
                openShopItemEditor(player);
                return;
            }
            if (slot == 40) {
                openShopItemEditor(player);
                return;
            }
            if (slot == 33) {
                this.plugin.settings().setShopEnabled(item, !this.plugin.settings().shopEnabled(item));
                openShopItemDetail(player, item);
                return;
            }
            if (item == ShopItem.PICKAXE || item == ShopItem.AXE) {
                final int[] slots = { 19, 21, 23, 25 };
                for (int tier = 1; tier <= 4; tier++) {
                    if (slot == slots[tier - 1]) {
                        openToolTierDetail(player, item, tier);
                        return;
                    }
                }
                return;
            }
            if (slot == 19) {
                this.plugin.settings().setShopPrice(item, this.plugin.settings().shopPrice(item) - 1);
                openShopItemDetail(player, item);
                return;
            }
            if (slot == 21) {
                this.plugin.settings().setShopPrice(item, this.plugin.settings().shopPrice(item) + 1);
                openShopItemDetail(player, item);
                return;
            }
            if (slot == 23) {
                this.plugin.settings().setShopAmount(item, this.plugin.settings().shopAmount(item) - 1);
                openShopItemDetail(player, item);
                return;
            }
            if (slot == 25) {
                this.plugin.settings().setShopAmount(item, this.plugin.settings().shopAmount(item) + 1);
                openShopItemDetail(player, item);
                return;
            }
            if (slot == 31) {
                this.plugin.settings().setShopCurrency(item, nextCurrency(this.plugin.settings().shopCurrency(item)));
                openShopItemDetail(player, item);
                return;
            }
            return;
        }
        if ("tool-tier".equals(mode)) {
            final ShopItem item = this.selectedShopItem.get(playerId);
            final Integer tier = this.selectedToolTier.get(playerId);
            if (item == null || tier == null) {
                openShopItemEditor(player);
                return;
            }
            if (slot == 40) {
                openShopItemDetail(player, item);
                return;
            }
            if (slot == 19) {
                this.plugin.settings().setToolPrice(item, tier, this.plugin.settings().toolPrice(item, tier) - 1);
                openToolTierDetail(player, item, tier);
                return;
            }
            if (slot == 21) {
                this.plugin.settings().setToolPrice(item, tier, this.plugin.settings().toolPrice(item, tier) + 1);
                openToolTierDetail(player, item, tier);
                return;
            }
            if (slot == 31) {
                this.plugin.settings().setToolCurrency(item, tier, nextCurrency(this.plugin.settings().toolCurrency(item, tier)));
                openToolTierDetail(player, item, tier);
                return;
            }
            return;
        }
        if ("upgrades".equals(mode)) {
            if (slot == 40) {
                clearShopEditor(player);
                openShops(player);
                return;
            }
            final String[] keys = { "sharpness", "protection", "protection", "protection", "protection", "haste", "haste", "forge", "forge",
                    "forge", "forge", "healPool", "dragonBuff", "traps", "traps", "traps" };
            final int[] levels = { 0, 1, 2, 3, 4, 1, 2, 1, 2, 3, 4, 0, 0, 1, 2, 3 };
            final int[] defaults = { 8, 5, 10, 20, 30, 4, 6, 4, 8, 12, 16, 3, 5, 1, 2, 4 };
            final int[] slots = { 9, 10, 11, 12, 13, 18, 19, 20, 21, 22, 23, 27, 28, 29, 30, 31 };
            for (int i = 0; i < slots.length; i++) {
                if (slot == slots[i]) {
                    final int current = this.plugin.settings().upgradePrice(keys[i], levels[i], defaults[i]);
                    this.plugin.settings().setUpgradePrice(keys[i], levels[i], Math.max(0, current + (shift ? -1 : 1)));
                    openUpgradePriceEditor(player);
                    return;
                }
            }
            return;
        }
        if (slot == 31) {
            openShopItemEditor(player);
            return;
        }
        if (slot == 33) {
            openUpgradePriceEditor(player);
            return;
        }
        if (slot == 19) {
            beginSelection(player, "shop:item", Messages.get("config-menu.click-shops.kliknij-ppm-kilofem-w-blok-na"));
            return;
        }
        if (slot == 25) {
            beginSelection(player, "shop:upgrade", Messages.get("config-menu.click-shops.kliknij-ppm-kilofem-w-blok-na-2"));
            return;
        }
        if (slot == 28 && shift) {
            arena.getItemShops().clear();
            save(arena);
            this.plugin.game().setActive(arena);
            this.plugin.npcs().respawn();
            Messages.send(player, Messages.get("config-menu.click-shops.usunieto-wszystkie-npc-sklepow-z-przedmiotami"));
            openShops(player);
            return;
        }
        if (slot == 34 && shift) {
            arena.getUpgradeShops().clear();
            save(arena);
            this.plugin.game().setActive(arena);
            this.plugin.npcs().respawn();
            Messages.send(player, Messages.get("config-menu.click-shops.usunieto-wszystkie-npc-sklepow-ulepszen"));
            openShops(player);
            return;
        }
        if (slot == 28 || slot == 34) {
            Messages.send(player, Messages.get("config-menu.click-shops.aby-usunac-wszystkie-npc-tego-typu"));
            return;
        }
        if (slot == 40) {
            clearShopEditor(player);
            open(player);
        }
    }

    public void clickSettings(final Player player, final int slot, final boolean left) {
        if (slot == 49) {
            open(player);
            return;
        }
        if (slot == 10) {
            final int v = this.plugin.settings().playersPerTeam() + (left ? 1 : -1);
            this.plugin.settings().setInt("game.playersPerTeam", Math.max(1, Math.min(16, v)));
        } else if (slot == 12) {
            final int v = this.plugin.settings().minPlayers() + (left ? 1 : -1);
            this.plugin.settings().setInt("game.minPlayers", Math.max(2, Math.min(this.plugin.settings().playersPerTeam() * 4, v)));
        } else if (slot == 14) {
            final int v = this.plugin.settings().countdown() + (left ? 5 : -5);
            this.plugin.settings().setInt("game.countdownSeconds", Math.max(5, Math.min(120, v)));
        } else if (slot == 16) {
            final int v = this.plugin.settings().respawnSeconds() + (left ? 1 : -1);
            this.plugin.settings().setInt("game.respawnSeconds", Math.max(1, Math.min(15, v)));
        } else if (slot == 28) {
            final int v = this.plugin.settings().respawnProtectionSeconds() + (left ? 1 : -1);
            this.plugin.settings().setInt("game.respawnProtectionSeconds", Math.max(0, Math.min(10, v)));
        } else if (slot == 30) {
            final int v = this.plugin.settings().reconnectSeconds() + (left ? 30 : -30);
            this.plugin.settings().setInt("game.reconnectSeconds", Math.max(30, Math.min(600, v)));
        } else if (slot == 32) {
            this.plugin.settings().setDouble("game.voidY", this.plugin.settings().voidY() + (left ? 1 : -1));
        } else {
            return;
        }
        openSettings(player);
    }

    public void clickTimer(final Player player, final int slot, final boolean left, final boolean shift) {
        final int[] slots = { 10, 12, 14, 16, 28, 30, 32 };
        final String[] keys = { "diamond2", "emerald2", "diamond3", "emerald3", "bedsGone", "suddenDeath", "end" };
        if (slot == 40) {
            open(player);
            return;
        }
        for (int x = 0; x < slots.length; x++) {
            if (slot == slots[x]) {
                int delta = shift ? 60 : 30;
                if (!left) {
                    delta = -delta;
                }
                final int value = Math.max(30, this.plugin.settings().timer(keys[x]) + delta);
                this.plugin.settings().setInt("game.timers." + keys[x], value);
                normalizeTimers();
                openTimers(player);
                return;
            }
        }
    }

    private Material nextCurrency(final Material current) {
        if (current == Material.IRON_INGOT) {
            return Material.GOLD_INGOT;
        }
        if (current == Material.GOLD_INGOT) {
            return Material.DIAMOND;
        }
        if (current == Material.DIAMOND) {
            return Material.EMERALD;
        }
        return Material.IRON_INGOT;
    }

    private String currencyLabel(final Material material) {
        if (material == Material.IRON_INGOT) {
            return Messages.get("currency.editor-iron");
        }
        if (material == Material.GOLD_INGOT) {
            return Messages.get("currency.editor-gold");
        }
        if (material == Material.DIAMOND) {
            return Messages.get("currency.editor-diamond");
        }
        if (material == Material.EMERALD) {
            return Messages.get("currency.editor-emerald");
        }
        return material.name();
    }

    private void clearShopEditor(final Player player) {
        if (player == null) {
            return;
        }
        final UUID playerId = player.getUniqueId();
        this.shopEditorMode.remove(playerId);
        this.selectedShopItem.remove(playerId);
        this.selectedToolTier.remove(playerId);
    }

    private boolean teamsComplete(final Arena arena) {
        if (arena == null) {
            return false;
        }
        for (final TeamColor color : TeamColor.values()) {
            if (teamConfiguredCount(arena, color) < 5) {
                return false;
            }
        }
        return true;
    }

    private ItemStack stepItem(final Material material, final int number, final String name, final boolean complete, final String... lore) {
        final List<String> lines = new ArrayList<>();
        lines.add(complete ? Messages.get("config-menu.step-item.ten-krok-jest-gotowy") : Messages.get("config-menu.step-item.ten-krok-wymaga-ustawienia"));
        lines.add("");
        for (final String line : lore) {
            lines.add(line);
        }
        return new ItemBuilder(material, Math.max(1, number))
                .name(Messages.get("config-menu.step-item.text", "NUMBER", number, "NAME", name))
                .lore(lines.toArray(new String[lines.size()]))
                .build();
    }

    private ItemStack statusPane(final boolean ok, final String okText, final String badText) {
        return new ItemBuilder(Material.STAINED_GLASS_PANE).data((short) (ok ? 5 : 14))
                .name(ok ? Messages.get("config-menu.status-pane.gotowe") : Messages.get("config-menu.status-pane.do-zrobienia"))
                .lore(ok ? "&7" + okText : "&7" + badText)
                .build();
    }

    private ItemStack progressPane(final boolean ok, final String step) {
        return new ItemBuilder(Material.STAINED_GLASS_PANE).data((short) (ok ? 5 : 14))
                .name(Messages.get("config-menu.progress-pane.text-3", "VALUE1", (ok ? Messages.get("config-menu.progress-pane.text") : Messages.get("config-menu.progress-pane.text-2")), "STEP", step))
                .lore(ok ? Messages.get("config-menu.progress-pane.ten-etap-jest-ukonczony") : Messages.get("config-menu.progress-pane.ten-etap-wymaga-uwagi"))
                .appearance(this.plugin, "menus.icons.config-menu.progress-pane.text-3")
                .build();
    }

    private String[] nextStepLore(final Arena arena, final boolean pointsOk, final boolean teamsOk, final boolean generatorsOk, final boolean shopsOk,
            final boolean ready) {
        if (!pointsOk) {
            return new String[] { Messages.get("config-menu.next-step-lore.najpierw-ustaw-punkty-glowne"), Messages.get("config-menu.next-step-lore.kliknij-a-otworze-wlasciwe-menu") };
        }
        if (!teamsOk) {
            return new String[] { Messages.get("config-menu.next-step-lore.nastepnie-ustaw-druzyny"), Messages.get("config-menu.next-step-lore.kliknij-a-otworze-wlasciwe-menu") };
        }
        if (!generatorsOk) {
            return new String[] { Messages.get("config-menu.next-step-lore.nastepnie-dodaj-generatory"), Messages.get("config-menu.next-step-lore.kliknij-a-otworze-wlasciwe-menu") };
        }
        if (!shopsOk) {
            return new String[] { Messages.get("config-menu.next-step-lore.nastepnie-ustaw-sklepy-npc"), Messages.get("config-menu.next-step-lore.kliknij-a-otworze-wlasciwe-menu") };
        }
        if (!ready) {
            return new String[] { Messages.get("config-menu.next-step-lore.zostaly-inne-braki-konfiguracji"), Messages.get("config-menu.next-step-lore.kliknij-aby-dostac-pelny-raport") };
        }
        return new String[] { Messages.get("config-menu.next-step-lore.wszystkie-obowiazkowe-kroki-sa-gotowe"), Messages.get("config-menu.next-step-lore.kliknij-aby-przejsc-do-przycisku-publikacji") };
    }

    private void openNextStep(final Player player, final Arena arena) {
        final boolean pointsOk = arena != null && arena.getLobby() != null && arena.getSpectator() != null;
        final boolean teamsOk = teamsComplete(arena);
        final boolean generatorsOk = arena != null && !arena.getGenerators().get(GeneratorType.DIAMOND).isEmpty() && !arena.getGenerators().get(GeneratorType.EMERALD).isEmpty();
        final boolean shopsOk = arena != null && arena.getItemShops().size() >= 4 && arena.getUpgradeShops().size() >= 4;
        if (!pointsOk) {
            openPoints(player);
            return;
        }
        if (!teamsOk) {
            openTeams(player);
            return;
        }
        if (!generatorsOk) {
            openGenerators(player);
            return;
        }
        if (!shopsOk) {
            openShops(player);
            return;
        }
        final List<String> problems = ArenaValidator.validate(arena);
        if (!problems.isEmpty()) {
            sendValidation(player, arena);
            return;
        }
        Messages.send(player, Messages.get("config-menu.open-next-step.mapa-jest-kompletna-kliknij-zielony-przycisk"));
        open(player);
    }

    private void beginSelection(final Player player, final String mode, final String message) {
        this.pendingSelection.put(player.getUniqueId(), mode);
        player.closeInventory();
        if (!isConfiguratorTool(player.getItemInHand())) {
            giveTool(player);
        }
        Messages.send(player, Messages.get("config-menu.begin-selection.text"));
        Messages.send(player, Messages.get("config-menu.begin-selection.tryb-wskazywania"));
        Messages.send(player, message);
        Messages.send(player, Messages.get("config-menu.begin-selection.nie-musisz-wpisywac-zadnej-komendy"));
        Messages.send(player, Messages.get("config-menu.begin-selection.text"));
    }

    private Location centerAbove(final Block block, final Player player) {
        return new Location(block.getWorld(), block.getX() + 0.5D, block.getY() + 1.0D, block.getZ() + 0.5D, player.getLocation().getYaw(), 0.0F);
    }

    private void sendValidation(final Player player, final Arena arena) {
        final List<String> problems = ArenaValidator.validate(arena);
        Messages.send(player, Messages.get("config-menu.send-validation.text"));
        Messages.send(player, Messages.get("config-menu.send-validation.sprawdzanie-mapy", "MAP", arena.getName()));
        if (problems.isEmpty()) {
            Messages.send(player, Messages.get("config-menu.send-validation.wszystko-jest-ustawione-mapa-jest-gotowa"));
        } else {
            Messages.send(player, Messages.get("config-menu.send-validation.mapa-nie-jest-kompletna-brakuje"));
            for (final String problem : problems) {
                Messages.send(player, Messages.get("config-menu.send-validation.text-2", "PROBLEM", problem));
            }
        }
        Messages.send(player, Messages.get("config-menu.send-validation.text"));
    }

    private void save(final Arena arena) {
        if (arena == null) {
            return;
        }
        this.plugin.arenaStorage().saveArena(arena);
        this.dirtyArenas.add(arena.getName().toLowerCase());
        if (this.plugin.configHolograms() != null) {
            this.plugin.configHolograms().refresh(arena);
        }
    }

    private boolean containsBlock(final List<Location> locations, final Location location) {
        if (location == null) {
            return false;
        }
        for (final Location l : locations) {
            if (l == null) {
                continue;
            }
            if (l.getBlockX() == location.getBlockX() && l.getBlockY() == location.getBlockY() && l.getBlockZ() == location.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    private TeamColor parseTeam(final String value) {
        try {
            return TeamColor.valueOf(value.toUpperCase());
        } catch (final Exception ignored) {
            return null;
        }
    }

    private int count(final Arena arena, final GeneratorType type) {
        return arena == null ? 0 : arena.getGenerators().get(type).size();
    }

    private String fmt(final int seconds) {
        return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
    }

    private String configured(final boolean ok, final String text) {
        return ok ? Messages.get("config-menu.configured.text", "TEXT", text) : Messages.get("config-menu.configured.text-2", "TEXT", text);
    }

    private String teamProgress(final Arena arena) {
        int done = 0, total = 20;
        if (arena != null) {
            for (final TeamColor color : TeamColor.values()) {
                done += teamConfiguredCount(arena, color);
            }
        }
        return Messages.get("config-menu.team-progress.elementow-baz-ustawionych", "VALUE1", (done == total ? Messages.get("config-menu.team-progress.text") : "&e") + done,
                "TOTAL", total);
    }

    private int teamConfiguredCount(final Arena arena, final TeamColor team) {
        if (arena == null || team == null) {
            return 0;
        }
        final TeamData data = arena.team(team);
        int done = 0;
        if (data.getSpawn() != null) {
            done++;
        }
        if (data.getBed() != null) {
            done++;
        }
        if (data.getBaseGenerator() != null) {
            done++;
        }
        if (data.getProtectedPos1() != null) {
            done++;
        }
        if (data.getProtectedPos2() != null) {
            done++;
        }
        return done;
    }

    private String[] teamState(final Arena arena, final TeamColor team) {
        final TeamData data = arena == null ? null : arena.team(team);
        return new String[] { configured(data != null && data.getSpawn() != null, Messages.get("configuration.labels.spawn")), configured(data != null && data.getBed() != null,
                Messages.get("configuration.labels.bed")), configured(data != null && data.getBaseGenerator() != null, Messages.get("config-menu.team-state.generator-bazy")),
                configured(data != null && data.hasProtectedRegion(), Messages.get("config-menu.team-state.strefa-bez-budowania")) };
    }

    private String coords(final Location location) {
        if (location == null) {
            return Messages.get("config-menu.coords.brak");
        }
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private ItemStack category(final Material material, final String name, final String... lore) {
        return new ItemBuilder(material).name(name).lore(lore).build();
    }

    private ItemStack header(final Arena arena, final String name, final String... extra) {
        final List<String> lore = new ArrayList<>();
        lore.add(Messages.get("config-menu.header.mapa", "MAP", (arena == null ? Messages.get("config-menu.header.brak") : arena.getName())));
        lore.addAll(Arrays.asList(extra));
        return new ItemBuilder(Material.NETHER_STAR).name(name).lore(lore.toArray(new String[lore.size()])).build();
    }

    private ItemStack status(final Material material, final String name, final Location location, final String hint) {
        final String position = location == null ? Messages.get("config-menu.status.nie-ustawiono") : Messages.get("config-menu.status.text",
                "VALUE1", location.getBlockX(), "VALUE2", location.getBlockY(), "VALUE3", location.getBlockZ());
        return new ItemBuilder(material).name(name).lore(position, Messages.get("config-menu.status.text-2"), hint).build();
    }

    private ItemStack back() {
        return new ItemBuilder(Material.ARROW)
                .name(Messages.get("config-menu.back.wroc"))
                .lore(Messages.get("config-menu.back.powrot-do-poprzedniego-menu"))
                .appearance(this.plugin, "menus.icons.config-menu.back.wroc")
                .build();
    }

    private void fill(final Inventory inventory, final short data) {
        final ItemStack filler = new ItemBuilder(Material.STAINED_GLASS_PANE).data(data)
                .name(Messages.get("config-menu.fill.text"))
                .appearance(this.plugin, "menus.icons.config-menu.fill.text")
                .build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private void normalizeTimers() {
        final String[] keys = { "diamond2", "emerald2", "diamond3", "emerald3", "bedsGone", "suddenDeath", "end" };
        int previous = 0;
        for (final String key : keys) {
            int value = this.plugin.settings().timer(key);
            if (value <= previous) {
                value = previous + 30;
                this.plugin.settings().setInt("game.timers." + key, value);
            }
            previous = value;
        }
    }
}
