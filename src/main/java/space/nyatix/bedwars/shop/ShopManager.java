package space.nyatix.bedwars.shop;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.config.QuickBuyStorage;
import space.nyatix.bedwars.gui.MenuHolder;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.PlayerLoadout;
import space.nyatix.bedwars.model.TeamColor;
import space.nyatix.bedwars.model.TeamData;
import space.nyatix.bedwars.model.TrapType;
import space.nyatix.bedwars.util.ConfiguredSound;
import space.nyatix.bedwars.util.ItemBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ShopManager {

    private final BedWarsPlugin plugin;

    private final QuickBuyStorage quickStorage;

    private final Map<UUID, ShopCategory> openCategory = new HashMap<>();

    private final Map<UUID, ShopItem[]> quickBuy = new HashMap<>();

    private static final int[] SHOP_SLOTS = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43 };

    public ShopManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.quickStorage = new QuickBuyStorage(plugin);
    }

    public void resetMatch() {
    }

    public void open(final Player player) {
        open(player, ShopCategory.QUICK_BUY);
    }

    public void open(final Player player, final ShopCategory category) {
        this.openCategory.put(player.getUniqueId(), category);
        final Inventory inventory = MenuHolder.create(player, "shop-items", 54, Messages.get("menus.shop.title", "CATEGORY", pretty(category)));
        top(inventory, category);
        final List<ShopItem> items = new ArrayList<>();
        if (category == ShopCategory.QUICK_BUY) {
            final ShopItem[] qb = quick(player);
            for (final ShopItem x : qb) {
                if (x != null && this.plugin.settings().shopEnabled(x)) {
                    items.add(x);
                }
            }
        } else {
            for (final ShopItem x : ShopItem.values()) {
                if (x.category() == category && this.plugin.settings().shopEnabled(x)) {
                    items.add(x);
                }
            }
        }
        int n = 0;
        for (final ShopItem x : items) {
            if (n >= SHOP_SLOTS.length) {
                break;
            }
            MenuHolder.get(inventory).setAction(SHOP_SLOTS[n], x.name());
            inventory.setItem(SHOP_SLOTS[n++], render(player, x));
        }
        if (category == ShopCategory.QUICK_BUY) {
            for (; n < SHOP_SLOTS.length; n++) {
                inventory.setItem(SHOP_SLOTS[n], new ItemBuilder(Material.STAINED_GLASS_PANE).data((short) 14)
                        .name(Messages.get("shop.open.puste-miejsce-szybkiego-zakupu"))
                        .lore(Messages.get("shop.open.uzyj-shift-klik-na-przedmiocie-w"), Messages.get("shop.open.kategorii-aby-dodac-go-tutaj"))
                        .appearance(this.plugin, "menus.icons.shop.open.puste-miejsce-szybkiego-zakupu")
                        .build());
            }
        }
        player.openInventory(inventory);
    }

    private String pretty(final ShopCategory category) {
        if (category == ShopCategory.QUICK_BUY) {
            return Messages.get("shop.pretty.szybki-zakup");
        }
        if (category == ShopCategory.BLOCKS) {
            return Messages.get("shop.pretty.bloki");
        }
        if (category == ShopCategory.MELEE) {
            return Messages.get("shop.pretty.walka");
        }
        if (category == ShopCategory.ARMOR) {
            return Messages.get("shop.pretty.zbroja");
        }
        if (category == ShopCategory.TOOLS) {
            return Messages.get("shop.pretty.narzedzia");
        }
        if (category == ShopCategory.RANGED) {
            return Messages.get("shop.pretty.dystans");
        }
        if (category == ShopCategory.POTIONS) {
            return Messages.get("shop.pretty.mikstury");
        }
        return Messages.get("shop.pretty.uzytkowe");
    }

    private void top(final Inventory inventory, final ShopCategory active) {
        final ShopCategory[] cats = ShopCategory.values();
        final Material[] icons = { Material.NETHER_STAR, Material.STAINED_CLAY, Material.GOLD_SWORD, Material.CHAINMAIL_BOOTS, Material.STONE_PICKAXE,
                Material.BOW, Material.BREWING_STAND_ITEM, Material.TNT };
        for (int x = 0; x < cats.length; x++) {
            final ItemBuilder builder = new ItemBuilder(icons[x]).name((cats[x] == active ? "&a" : "&f") + pretty(cats[x]));
            if (cats[x] == active) {
                builder.enchant(Enchantment.DURABILITY, 1);
            }
            inventory.setItem(x, builder.build());
        }
        for (int x = 9; x < 18; x++) {
            inventory.setItem(x, new ItemBuilder(Material.STAINED_GLASS_PANE).data((short) 7)
                    .name(Messages.get("shop.top.text"))
                    .appearance(this.plugin, "menus.icons.shop.top.text")
                    .build());
        }
    }

    private ItemStack render(final Player player, final ShopItem shopItem) {
        final Material currency = effectiveCurrency(player, shopItem);
        final String cur = currencyName(currency);
        final boolean enough = count(player, currency) >= effectivePrice(player, shopItem);
        final String price = (enough ? "&a" : "&c") + effectivePrice(player, shopItem) + " " + cur;
        final List<String> lore = new ArrayList<>();
        lore.addAll(Messages.lines("shop.items." + shopItem
                .name().toLowerCase(Locale.ROOT).replace('_', '-') + ".lore", "PRICE", effectivePrice(player, shopItem), "CURRENCY", cur));
        lore.add(Messages.get("shop.render.koszt", "PRICE", price));
        lore.add("");
        if (shopItem == ShopItem.PICKAXE) {
            lore.add(Messages.get("shop.render.stale-narzedzie-z-ulepszeniami-po-smierci"));
        }
        if (shopItem == ShopItem.PICKAXE) {
            lore.add(Messages.get("shop.render.traci-jeden-poziom"));
        }
        if (shopItem == ShopItem.AXE) {
            lore.add(Messages.get("shop.render.stale-narzedzie-z-ulepszeniami-po-smierci"));
        }
        if (shopItem == ShopItem.AXE) {
            lore.add(Messages.get("shop.render.traci-jeden-poziom"));
        }
        lore.add(enough ? Messages.get("shop.render.kliknij-aby-kupic") : Messages.get("shop.render.nie-masz-wystarczajaco", "CUR", cur));
        return new ItemBuilder(shopItem.icon(), this.plugin.settings().shopAmount(shopItem))
                .name(Messages.get(enough ? "shop.item-affordable" : "shop.item-unaffordable", "ITEM", shopItem.display()))
                .lore(lore.toArray(new String[lore.size()]))
                .build();
    }

    public void clickShop(final Player player, final int rawSlot, final boolean shift, final ItemStack clicked) {
        if (rawSlot >= 0 && rawSlot <= 7) {
            open(player, ShopCategory.values()[rawSlot]);
            return;
        }
        final MenuHolder holder = MenuHolder.get(player.getOpenInventory().getTopInventory());
        if (holder == null || !holder.belongsTo(player)) {
            return;
        }
        final String action = holder.getAction(rawSlot);
        final ShopItem item = action == null ? null : ShopItem.valueOf(action);
        if (item == null) {
            return;
        }
        if (shift) {
            if (this.openCategory.get(player.getUniqueId()) != ShopCategory.QUICK_BUY) {
                addQuick(player, item);
                Messages.send(player, Messages.get("shop.click-shop.dodano-do-szybkiego-zakupu", "ITEM", item.display()));
            } else {
                removeQuick(player, item);
                Messages.send(player, Messages.get("shop.click-shop.usunieto-z-szybkiego-zakupu", "ITEM", item.display()));
            }
            open(player, this.openCategory.get(player.getUniqueId()));
            return;
        }
        purchase(player, item);
        open(player, this.openCategory.getOrDefault(player.getUniqueId(), ShopCategory.QUICK_BUY));
    }

    private void addQuick(final Player player, final ShopItem item) {
        final ShopItem[] a = quick(player);
        for (final ShopItem shopItem : a) {
            if (shopItem == item) {
                return;
            }
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                a[i] = item;
                this.quickStorage.save(player.getUniqueId(), a);
                return;
            }
        }
        a[a.length - 1] = item;
        this.quickStorage.save(player.getUniqueId(), a);
    }

    private void removeQuick(final Player player, final ShopItem item) {
        final ShopItem[] a = quick(player);
        for (int i = 0; i < a.length; i++) {
            if (a[i] == item) {
                a[i] = null;
            }
        }
        this.quickStorage.save(player.getUniqueId(), a);
    }

    private ShopItem[] quick(final Player player) {
        ShopItem[] a = this.quickBuy.get(player.getUniqueId());
        if (a == null) {
            final ShopItem[] defaults = new ShopItem[] { ShopItem.WOOL, ShopItem.STONE_SWORD, ShopItem.TNT, ShopItem.GOLDEN_APPLE, ShopItem.FIREBALL,
                    ShopItem.LADDER, ShopItem.PICKAXE, ShopItem.AXE, ShopItem.PEARL, null, null, null, null, null, null, null, null, null, null, null, null };
            a = this.quickStorage.load(player.getUniqueId(), defaults);
            this.quickBuy.put(player.getUniqueId(), a);
        }
        return a;
    }

    private int effectivePrice(final Player player, final ShopItem shopItem) {
        final PlayerLoadout loadout = this.plugin.game().loadout(player.getUniqueId());
        if (shopItem == ShopItem.PICKAXE) {
            return this.plugin.settings().toolPrice(shopItem, Math.min(4, loadout.pickaxeTier() + 1));
        }
        if (shopItem == ShopItem.AXE) {
            return this.plugin.settings().toolPrice(shopItem, Math.min(4, loadout.axeTier() + 1));
        }
        return this.plugin.settings().shopPrice(shopItem);
    }

    private Material effectiveCurrency(final Player player, final ShopItem shopItem) {
        if (shopItem == ShopItem.PICKAXE || shopItem == ShopItem.AXE) {
            final int current = shopItem == ShopItem.PICKAXE ? this.plugin.game().loadout(player.getUniqueId()).pickaxeTier() : this.plugin.game().loadout(player.getUniqueId()).axeTier();
            return this.plugin.settings().toolCurrency(shopItem, Math.min(4, current + 1));
        }
        return this.plugin.settings().shopCurrency(shopItem);
    }

    public boolean purchase(final Player player, final ShopItem shopItem) {
        if (!this.plugin.settings().shopEnabled(shopItem)) {
            Messages.send(player, Messages.get("shop.purchase.ten-przedmiot-jest-wylaczony-w-konfiguracji"));
            return false;
        }
        final int price = effectivePrice(player, shopItem);
        final Material currency = effectiveCurrency(player, shopItem);
        if (!canPurchasePermanent(player, shopItem)) {
            Messages.send(player, Messages.get("shop.purchase.masz-juz-to-stale-ulepszenie-lub"));
            return false;
        }
        if (!take(player, currency, price)) {
            Messages.send(player, Messages.get("shop.purchase.nie-masz-wystarczajaco", "VALUE1", currencyName(currency)));
            ConfiguredSound.play(this.plugin, player, "purchase-failed", Sound.ENDERMAN_TELEPORT, 0.5F, 0.5F);
            return false;
        }
        final PlayerLoadout loadout = this.plugin.game().loadout(player.getUniqueId());
        if (shopItem == ShopItem.CHAIN_ARMOR) {
            loadout.armorTier(1);
            equipArmor(player);
        } else if (shopItem == ShopItem.IRON_ARMOR) {
            loadout.armorTier(2);
            equipArmor(player);
        } else if (shopItem == ShopItem.DIAMOND_ARMOR) {
            loadout.armorTier(3);
            equipArmor(player);
        } else if (shopItem == ShopItem.SHEARS) {
            loadout.shears(true);
            giveTools(player);
        } else if (shopItem == ShopItem.PICKAXE) {
            loadout.pickaxeTier(Math.min(4, loadout.pickaxeTier() + 1));
            giveTools(player);
        } else if (shopItem == ShopItem.AXE) {
            loadout.axeTier(Math.min(4, loadout.axeTier() + 1));
            giveTools(player);
        } else if (shopItem == ShopItem.SPEED) {
            givePotion(player, Messages.get("shop.purchase.mikstura-szybkosci-ii-45-s", "SECONDS", Options.integer(this.plugin, "items.potions.speed.seconds",
                    45, 1, 3600), "LEVEL", 1 + Options.integer(this.plugin, "items.potions.speed.amplifier", 1, 0, 10)), PotionEffectType.SPEED, 20 * Options.integer(this.plugin,
                    "items.potions.speed.seconds", 45, 1, 3600), Options.integer(this.plugin, "items.potions.speed.amplifier", 1, 0, 10), this.plugin.settings().shopAmount(shopItem));
        } else if (shopItem == ShopItem.JUMP) {
            givePotion(player, Messages.get("shop.purchase.mikstura-skoku-v-45-s", "SECONDS", Options.integer(this.plugin, "items.potions.jump.seconds",
                    45, 1, 3600), "LEVEL", 1 + Options.integer(this.plugin, "items.potions.jump.amplifier", 4, 0, 10)), PotionEffectType.JUMP, 20 * Options.integer(this.plugin,
                    "items.potions.jump.seconds", 45, 1, 3600), Options.integer(this.plugin, "items.potions.jump.amplifier", 4, 0, 10), this.plugin.settings().shopAmount(shopItem));
        } else if (shopItem == ShopItem.INVIS) {
            givePotion(player, Messages.get("shop.purchase.mikstura-niewidzialnosci-30-s", "SECONDS", Options.integer(this.plugin, "items.potions.invis.seconds",
                    30, 1, 3600), "LEVEL", 1 + Options.integer(this.plugin, "items.potions.invis.amplifier", 0, 0, 10)), PotionEffectType.INVISIBILITY, 20 * Options.integer(this.plugin,
                    "items.potions.invis.seconds", 30, 1, 3600), Options.integer(this.plugin, "items.potions.invis.amplifier", 0, 0, 10), this.plugin.settings().shopAmount(shopItem));
        } else if (shopItem == ShopItem.STONE_SWORD || shopItem == ShopItem.IRON_SWORD || shopItem == ShopItem.DIAMOND_SWORD) {
            removeWeakerSwords(player, shopItem.icon());
            player.getInventory().addItem(new ItemStack(shopItem.icon(), this.plugin.settings().shopAmount(shopItem)));
        } else if (shopItem == ShopItem.KNOCKBACK_STICK) {
            final ItemStack s = new ItemBuilder(Material.STICK, this.plugin.settings().shopAmount(shopItem))
                    .name(Messages.get("shop.purchase.kij-odrzutu"))
                    .enchant(Enchantment.KNOCKBACK, 1)
                    .build();
            player.getInventory().addItem(s);
        } else if (shopItem == ShopItem.BOW_POWER) {
            final ItemStack s = new ItemStack(Material.BOW, this.plugin.settings().shopAmount(shopItem));
            s.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
            player.getInventory().addItem(s);
        } else if (shopItem == ShopItem.BOW_PUNCH) {
            final ItemStack s = new ItemStack(Material.BOW, this.plugin.settings().shopAmount(shopItem));
            s.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
            s.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 1);
            player.getInventory().addItem(s);
        } else if (shopItem == ShopItem.MAGIC_MILK) {
            loadout.magicMilk(true);
            player.getInventory().addItem(new ItemBuilder(Material.MILK_BUCKET, this.plugin.settings().shopAmount(shopItem))
                    .tag("magic-milk")
                    .name(Messages.get("shop.purchase.magiczne-mleko"))
                    .lore(Messages.get("shop.purchase.odpornosc-na-pulapki-przez-30-sekund", "SECONDS", Options.integer(this.plugin, "items.magicMilk.seconds", 30, 1, 3600)))
                    .build());
        } else {
            final ItemStack give = new ItemStack(shopItem.icon(), this.plugin.settings().shopAmount(shopItem));
            if (shopItem == ShopItem.WOOL) {
                final TeamColor teamColor = this.plugin.game().teamOf(player.getUniqueId());
                if (teamColor != null) {
                    give.setDurability(teamColor.dye().getWoolData());
                }
            }
            player.getInventory().addItem(give);
        }
        this.plugin.game().applyTeamEnchantments(player);
        ConfiguredSound.play(this.plugin, player, "purchase", Sound.NOTE_PLING, 1F, 1.6F);
        return true;
    }

    private boolean canPurchasePermanent(final Player player, final ShopItem shopItem) {
        final PlayerLoadout loadout = this.plugin.game().loadout(player.getUniqueId());
        if (shopItem == ShopItem.CHAIN_ARMOR) {
            return loadout.armorTier() < 1;
        }
        if (shopItem == ShopItem.IRON_ARMOR) {
            return loadout.armorTier() < 2;
        }
        if (shopItem == ShopItem.DIAMOND_ARMOR) {
            return loadout.armorTier() < 3;
        }
        if (shopItem == ShopItem.SHEARS) {
            return !loadout.shears();
        }
        if (shopItem == ShopItem.PICKAXE) {
            return loadout.pickaxeTier() < 4;
        }
        if (shopItem == ShopItem.AXE) {
            return loadout.axeTier() < 4;
        }
        return true;
    }

    private void removeWeakerSwords(final Player player, final Material bought) {
        final int boughtTier = swordTier(bought);
        if (boughtTier <= 0) {
            return;
        }
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            final ItemStack current = player.getInventory().getItem(i);
            if (current == null) {
                continue;
            }
            final int currentTier = swordTier(current.getType());
            if (currentTier > 0 && currentTier < boughtTier) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private int swordTier(final Material material) {
        if (material == Material.WOOD_SWORD) {
            return 1;
        }
        if (material == Material.STONE_SWORD) {
            return 2;
        }
        if (material == Material.IRON_SWORD) {
            return 3;
        }
        if (material == Material.DIAMOND_SWORD) {
            return 4;
        }
        return 0;
    }

    private void givePotion(final Player player, final String name, final PotionEffectType type, final int ticks, final int amp, final int amount) {
        final ItemStack item = new ItemStack(Material.POTION, Math.max(1, Math.min(64, amount)), (short) 0);
        final PotionMeta m = (PotionMeta) item.getItemMeta();
        m.setDisplayName(name);
        m.addCustomEffect(new PotionEffect(type, ticks, amp), true);
        item.setItemMeta(m);
        player.getInventory().addItem(item);
    }

    public void restorePermanent(final Player player) {
        equipArmor(player);
        giveTools(player);
        this.plugin.game().applyTeamEnchantments(player);
    }

    public void equipArmor(final Player player) {
        final int t = this.plugin.game().loadout(player.getUniqueId()).armorTier();
        final Material boots = t == 0 ? Material.LEATHER_BOOTS : t == 1 ? Material.CHAINMAIL_BOOTS : t == 2 ? Material.IRON_BOOTS : Material.DIAMOND_BOOTS;
        final Material legs = t == 0 ? Material.LEATHER_LEGGINGS : t == 1 ? Material.CHAINMAIL_LEGGINGS : t == 2 ? Material.IRON_LEGGINGS : Material.DIAMOND_LEGGINGS;
        final ItemStack helmet = coloredLeather(Material.LEATHER_HELMET, player), chest = coloredLeather(Material.LEATHER_CHESTPLATE, player);
        final ItemStack legItem = t == 0 ? coloredLeather(Material.LEATHER_LEGGINGS, player) : new ItemStack(legs), bootItem = t == 0 ? coloredLeather(Material.LEATHER_BOOTS,
                player) : new ItemStack(boots);
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chest);
        player.getInventory().setLeggings(legItem);
        player.getInventory().setBoots(bootItem);
    }

    private ItemStack coloredLeather(final Material material, final Player player) {
        final ItemStack item = new ItemStack(material);
        try {
            final LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            final TeamColor teamColor = this.plugin.game().teamOf(player.getUniqueId());
            final Color color = teamColor == TeamColor.RED ? Color.RED : teamColor == TeamColor.GREEN ? Color.LIME : teamColor == TeamColor.BLUE ? Color.BLUE : Color.WHITE;
            meta.setColor(color);
            item.setItemMeta(meta);
        } catch (Exception ignored) {
        }
        return item;
    }

    public void giveTools(final Player player) {
        final PlayerLoadout loadout = this.plugin.game().loadout(player.getUniqueId());
        removeToolTypes(player);
        if (loadout.shears()) {
            player.getInventory().addItem(new ItemStack(Material.SHEARS));
        }
        if (loadout.pickaxeTier() > 0) {
            player.getInventory().addItem(toolPick(loadout.pickaxeTier()));
        }
        if (loadout.axeTier() > 0) {
            player.getInventory().addItem(toolAxe(loadout.axeTier()));
        }
    }

    private void removeToolTypes(final Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            final ItemStack item = player.getInventory().getItem(i);
            if (item == null) {
                continue;
            }
            final String n = item.getType().name();
            if (n.endsWith("_PICKAXE") || n.endsWith("_AXE") || item.getType() == Material.SHEARS) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private ItemStack toolPick(final int t) {
        final Material material = t == 1 ? Material.WOOD_PICKAXE : t == 2 ? Material.IRON_PICKAXE : t == 3 ? Material.GOLD_PICKAXE : Material.DIAMOND_PICKAXE;
        final ItemStack item = new ItemStack(material);
        if (t >= 2) {
            item.addUnsafeEnchantment(Enchantment.DIG_SPEED, t == 2 ? 1 : t == 3 ? 2 : 3);
        }
        return item;
    }

    private ItemStack toolAxe(final int t) {
        final Material material = t == 1 ? Material.WOOD_AXE : t == 2 ? Material.STONE_AXE : t == 3 ? Material.IRON_AXE : Material.DIAMOND_AXE;
        final ItemStack item = new ItemStack(material);
        if (t >= 2) {
            item.addUnsafeEnchantment(Enchantment.DIG_SPEED, t == 2 ? 1 : t == 3 ? 2 : 3);
        }
        return item;
    }

    private int count(final Player player, final Material material) {
        int n = 0;
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                n += item.getAmount();
            }
        }
        return n;
    }

    public boolean take(final Player player, final Material material, final int amount) {
        if (count(player, material) < amount) {
            return false;
        }
        int left = amount;
        for (int i = 0; i < player.getInventory().getSize() && left > 0; i++) {
            final ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != material) {
                continue;
            }
            final int use = Math.min(left, item.getAmount());
            final int remain = item.getAmount() - use;
            if (remain <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(remain);
            }
            left -= use;
        }
        return true;
    }

    private String currencyName(final Material material) {
        return material == Material.IRON_INGOT ? Messages.get("currency.iron") : material == Material.GOLD_INGOT ? Messages.get("currency.gold") : material == Material.DIAMOND ? Messages.get("currency.diamond") : Messages.get("currency.emerald");
    }

    public void openUpgrades(final Player player) {
        final TeamColor teamColor = this.plugin.game().teamOf(player.getUniqueId());
        if (teamColor == null) {
            return;
        }
        final TeamData teamData = this.plugin.game().active().team(teamColor);
        final Inventory inventory = MenuHolder.create(player, "shop-upgrades", 45, Messages.get("menus.upgrades.title"));
        inventory.setItem(10, new ItemBuilder(Material.IRON_SWORD)
                .name(Messages.get("shop.open-upgrades.ostrzone-miecze"))
                .lore(Messages.get("shop.open-upgrades.ostrosc-i-na-wszystkich-mieczach"), Messages.get("shop.open-upgrades.koszt-diamentow", "VALUE1", this.plugin.settings().upgradePrice("sharpness", 0, 8)), Messages.get("shop.open-upgrades.kupione", "VALUE1", (teamData.getSharpnessLevel() > 0 ? Messages.get("shop.open-upgrades.tak") : Messages.get("shop.open-upgrades.nie"))))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.ostrzone-miecze")
                .build());
        inventory.setItem(12, new ItemBuilder(Material.IRON_CHESTPLATE)
                .name(Messages.get("shop.open-upgrades.wzmocniona-zbroja"))
                .lore(Messages.get("shop.open-upgrades.ochrona-na-calej-zbroi"), Messages.get("shop.open-upgrades.koszty-diamentow", "VALUE1", upgradeCosts("protection", new int[] { 5, 10, 20, 30 })), Messages.get("shop.open-upgrades.poziom-4", "VALUE1", teamData.getProtectionLevel()))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.wzmocniona-zbroja")
                .build());
        inventory.setItem(14, new ItemBuilder(Material.GOLD_PICKAXE)
                .name(Messages.get("shop.open-upgrades.szalony-gornik"))
                .lore(Messages.get("shop.open-upgrades.staly-efekt-pospiechu"), Messages.get("shop.open-upgrades.koszty-diamentow", "VALUE1", upgradeCosts("haste", new int[] { 4, 6 })), Messages.get("shop.open-upgrades.poziom-2", "VALUE1", teamData.getHasteLevel()))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.szalony-gornik")
                .build());
        inventory.setItem(16, new ItemBuilder(Material.FURNACE)
                .name(Messages.get("shop.open-upgrades.kuznia"))
                .lore(Messages.get("shop.open-upgrades.ulepsza-generowanie-surowcow-w-bazie"), Messages.get("shop.open-upgrades.koszty-diamentow", "VALUE1", upgradeCosts("forge", new int[] { 4, 8, 12, 16 })), Messages.get("shop.open-upgrades.poziom-4", "VALUE1", teamData.getForgeLevel()))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.kuznia")
                .build());
        inventory.setItem(28, new ItemBuilder(Material.BEACON)
                .name(Messages.get("shop.open-upgrades.strefa-leczenia"))
                .lore(Messages.get("shop.open-upgrades.regeneracja-w-poblizu-bazy"), Messages.get("shop.open-upgrades.koszt-diamenty", "VALUE1", this.plugin.settings().upgradePrice("healPool", 0, 3)), Messages.get("shop.open-upgrades.kupione", "VALUE1", (teamData.hasHealPool() ? Messages.get("shop.open-upgrades.tak") : Messages.get("shop.open-upgrades.nie"))))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.strefa-leczenia")
                .build());
        inventory.setItem(30, new ItemBuilder(Material.DRAGON_EGG)
                .name(Messages.get("shop.open-upgrades.wzmocnienie-smoka"))
                .lore(Messages.get("shop.open-upgrades.twoja-druzyna-otrzyma-dodatkowego-smoka"), Messages.get("shop.open-upgrades.podczas-naglej-smierci"), Messages.get("shop.open-upgrades.koszt-diamentow", "VALUE1", this.plugin.settings().upgradePrice("dragonBuff", 0, 5)), Messages.get("shop.open-upgrades.kupione", "VALUE1", (teamData.hasDragonBuff() ? Messages.get("shop.open-upgrades.tak") : Messages.get("shop.open-upgrades.nie"))))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.wzmocnienie-smoka")
                .build());
        inventory.setItem(32, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name(Messages.get("shop.open-upgrades.to-pulapka"))
                .lore(Messages.get("shop.open-upgrades.slepota-i-spowolnienie-dla-intruza"), Messages.get("shop.open-upgrades.koszt-pulapki-diamentow", "VALUE1", trapCost(teamData)))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.to-pulapka")
                .build());
        inventory.setItem(33, new ItemBuilder(Material.FEATHER)
                .name(Messages.get("shop.open-upgrades.pulapka-kontratakujaca"))
                .lore(Messages.get("shop.open-upgrades.szybkosc-i-skok-dla-obroncow"), Messages.get("shop.open-upgrades.koszt-pulapki-diamentow", "VALUE1", trapCost(teamData)))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.pulapka-kontratakujaca")
                .build());
        inventory.setItem(34, new ItemBuilder(Material.REDSTONE_TORCH_ON)
                .name(Messages.get("shop.open-upgrades.pulapka-alarmowa"))
                .lore(Messages.get("shop.open-upgrades.ujawnia-niewidzialnych-intruzow"), Messages.get("shop.open-upgrades.koszt-pulapki-diamentow", "VALUE1", trapCost(teamData)))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.pulapka-alarmowa")
                .build());
        inventory.setItem(35, new ItemBuilder(Material.IRON_PICKAXE)
                .name(Messages.get("shop.open-upgrades.pulapka-zmeczenia-gornika"))
                .lore(Messages.get("shop.open-upgrades.zmeczenie-gornika-dla-intruza"), Messages.get("shop.open-upgrades.koszt-pulapki-diamentow", "VALUE1", trapCost(teamData)))
                .appearance(this.plugin, "menus.icons.shop.open-upgrades.pulapka-zmeczenia-gornika")
                .build());
        player.openInventory(inventory);
    }

    private int trapCost(final TeamData teamData) {
        final int n = Math.min(2, teamData.getTraps().size());
        return this.plugin.settings().upgradePrice("traps", n + 1, n == 0 ? 1 : n == 1 ? 2 : 4);
    }

    public void clickUpgrade(final Player player, final int slot) {
        final TeamColor teamColor = this.plugin.game().teamOf(player.getUniqueId());
        if (teamColor == null) {
            return;
        }
        final TeamData teamData = this.plugin.game().active().team(teamColor);
        if (slot == 10 && teamData.getSharpnessLevel() == 0 && payD(player, this.plugin.settings().upgradePrice("sharpness", 0, 8))) {
            teamData.setSharpnessLevel(1);
            this.plugin.game().applyTeamEnchantments(teamColor);
        } else if (slot == 12) {
            final int lv = teamData.getProtectionLevel();
            if (lv < 4 && payD(player, this.plugin.settings().upgradePrice("protection", lv + 1, new int[] { 5, 10, 20, 30 }[lv]))) {
                teamData.setProtectionLevel(lv + 1);
                this.plugin.game().applyTeamEnchantments(teamColor);
            }
        } else if (slot == 14) {
            final int lv = teamData.getHasteLevel();
            if (lv < 2 && payD(player, this.plugin.settings().upgradePrice("haste", lv + 1, new int[] { 4, 6 }[lv]))) {
                teamData.setHasteLevel(lv + 1);
            }
        } else if (slot == 16) {
            final int lv = teamData.getForgeLevel();
            if (lv < 4 && payD(player, this.plugin.settings().upgradePrice("forge", lv + 1, new int[] { 4, 8, 12, 16 }[lv]))) {
                teamData.setForgeLevel(lv + 1);
            }
        } else if (slot == 28 && !teamData.hasHealPool() && payD(player, this.plugin.settings().upgradePrice("healPool", 0, 3))) {
            teamData.setHealPool(true);
        } else if (slot == 30 && !teamData.hasDragonBuff() && payD(player, this.plugin.settings().upgradePrice("dragonBuff", 0, 5))) {
            teamData.setDragonBuff(true);
        } else if (slot >= 32 && slot <= 35) {
            final TrapType[] types = { TrapType.ITS_A_TRAP, TrapType.COUNTER_OFFENSIVE, TrapType.ALARM, TrapType.MINER_FATIGUE };
            if (teamData.getTraps().size() >= 3) {
                Messages.send(player, Messages.get("shop.click-upgrade.kolejka-pulapek-jest-pelna"));
            } else {
                final int cost = trapCost(teamData);
                if (payD(player, cost)) {
                    teamData.getTraps().add(types[slot - 32]);
                }
            }
        }
        openUpgrades(player);
    }

    private String upgradeCosts(final String key, final int[] defaults) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < defaults.length; i++) {
            if (i > 0) {
                out.append("/");
            }
            out.append(this.plugin.settings().upgradePrice(key, i + 1, defaults[i]));
        }
        return out.toString();
    }

    private boolean payD(final Player player, final int n) {
        if (!take(player, Material.DIAMOND, n)) {
            Messages.send(player, Messages.get("shop.pay-d.potrzebujesz-diamentow", "VALUE1", n));
            return false;
        }
        ConfiguredSound.play(this.plugin, player, "upgrade", Sound.LEVEL_UP, 1F, 1.2F);
        return true;
    }
}
