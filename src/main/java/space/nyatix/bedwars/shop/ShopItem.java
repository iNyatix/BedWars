package space.nyatix.bedwars.shop;

import org.bukkit.Material;
import space.nyatix.bedwars.message.Messages;

import java.util.Locale;

public enum ShopItem {

    WOOL(ShopCategory.BLOCKS, Material.WOOL, 16, Material.IRON_INGOT, 4),
    CLAY(ShopCategory.BLOCKS, Material.STAINED_CLAY, 16, Material.IRON_INGOT, 12),
    GLASS(ShopCategory.BLOCKS, Material.GLASS, 4, Material.IRON_INGOT, 12),
    ENDSTONE(ShopCategory.BLOCKS, Material.ENDER_STONE, 12, Material.IRON_INGOT, 24),
    LADDER(ShopCategory.BLOCKS, Material.LADDER, 8, Material.IRON_INGOT, 4),
    WOOD(ShopCategory.BLOCKS, Material.WOOD, 16, Material.GOLD_INGOT, 4),
    OBSIDIAN(ShopCategory.BLOCKS, Material.OBSIDIAN, 4, Material.EMERALD, 4),
    STONE_SWORD(ShopCategory.MELEE, Material.STONE_SWORD, 1, Material.IRON_INGOT, 10),
    IRON_SWORD(ShopCategory.MELEE, Material.IRON_SWORD, 1, Material.GOLD_INGOT, 7),
    DIAMOND_SWORD(ShopCategory.MELEE, Material.DIAMOND_SWORD, 1, Material.EMERALD, 4),
    KNOCKBACK_STICK(ShopCategory.MELEE, Material.STICK, 1, Material.GOLD_INGOT, 5),
    CHAIN_ARMOR(ShopCategory.ARMOR, Material.CHAINMAIL_BOOTS, 1, Material.IRON_INGOT, 40),
    IRON_ARMOR(ShopCategory.ARMOR, Material.IRON_BOOTS, 1, Material.GOLD_INGOT, 12),
    DIAMOND_ARMOR(ShopCategory.ARMOR, Material.DIAMOND_BOOTS, 1, Material.EMERALD, 6),
    SHEARS(ShopCategory.TOOLS, Material.SHEARS, 1, Material.IRON_INGOT, 20),
    PICKAXE(ShopCategory.TOOLS, Material.WOOD_PICKAXE, 1, Material.IRON_INGOT, 10),
    AXE(ShopCategory.TOOLS, Material.WOOD_AXE, 1, Material.IRON_INGOT, 10),
    ARROWS(ShopCategory.RANGED, Material.ARROW, 8, Material.GOLD_INGOT, 2),
    BOW(ShopCategory.RANGED, Material.BOW, 1, Material.GOLD_INGOT, 12),
    BOW_POWER(ShopCategory.RANGED, Material.BOW, 1, Material.GOLD_INGOT, 20),
    BOW_PUNCH(ShopCategory.RANGED, Material.BOW, 1, Material.EMERALD, 6),
    SPEED(ShopCategory.POTIONS, Material.POTION, 1, Material.EMERALD, 1),
    JUMP(ShopCategory.POTIONS, Material.POTION, 1, Material.EMERALD, 1),
    INVIS(ShopCategory.POTIONS, Material.POTION, 1, Material.EMERALD, 2),
    GOLDEN_APPLE(ShopCategory.UTILITY, Material.GOLDEN_APPLE, 1, Material.GOLD_INGOT, 3),
    BEDBUG(ShopCategory.UTILITY, Material.SNOW_BALL, 1, Material.IRON_INGOT, 24),
    DREAM_DEFENDER(ShopCategory.UTILITY, Material.MONSTER_EGG, 1, Material.IRON_INGOT, 120),
    FIREBALL(ShopCategory.UTILITY, Material.FIREBALL, 1, Material.IRON_INGOT, 40),
    TNT(ShopCategory.UTILITY, Material.TNT, 1, Material.GOLD_INGOT, 4),
    PEARL(ShopCategory.UTILITY, Material.ENDER_PEARL, 1, Material.EMERALD, 4),
    WATER(ShopCategory.UTILITY, Material.WATER_BUCKET, 1, Material.GOLD_INGOT, 3),
    BRIDGE_EGG(ShopCategory.UTILITY, Material.EGG, 1, Material.EMERALD, 1),
    MAGIC_MILK(ShopCategory.UTILITY, Material.MILK_BUCKET, 1, Material.GOLD_INGOT, 4),
    SPONGE(ShopCategory.UTILITY, Material.SPONGE, 4, Material.GOLD_INGOT, 3);

    private final ShopCategory cat;

    private final Material icon, currency;

    private final int amount, price;

    ShopItem(final ShopCategory category, final Material material, final int a, final Material cur, final int price) {
        this.cat = category;
        this.icon = material;
        this.amount = a;
        this.currency = cur;
        this.price = price;
    }

    public ShopCategory category() {
        return this.cat;
    }

    public Material icon() {
        return this.icon;
    }

    public int amount() {
        return this.amount;
    }

    public String display() {
        return Messages.get("shop.items." + name().toLowerCase(Locale.ROOT).replace('_', '-') + ".name");
    }

    public Material currency() {
        return this.currency;
    }

    public int price() {
        return this.price;
    }
}
