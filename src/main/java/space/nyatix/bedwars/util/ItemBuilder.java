package space.nyatix.bedwars.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;

    private String tag;

    public ItemBuilder(final Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder(final Material material, final int a) {
        this.item = new ItemStack(material, a);
    }

    public ItemBuilder data(final short d) {
        this.item.setDurability(d);
        return this;
    }

    public ItemBuilder name(final String n) {
        final ItemMeta meta = this.item.getItemMeta();
        meta.setDisplayName(ColorUtil.c(n));
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(final String... ls) {
        final ItemMeta meta = this.item.getItemMeta();
        final List<String> l = new ArrayList<>();
        for (final String s : ls) {
            if (s == null) {
                continue;
            }
            for (final String line : s.split("\\n", -1)) {
                l.add(ColorUtil.c(line));
            }
        }
        meta.setLore(l);
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder enchant(final Enchantment e, final int l) {
        this.item.addUnsafeEnchantment(e, l);
        return this;
    }

    public ItemBuilder amount(final int a) {
        this.item.setAmount(a);
        return this;
    }

    public ItemBuilder tag(final String tag) {
        this.tag = tag;
        return this;
    }

    public ItemBuilder appearance(final BedWarsPlugin plugin, final String path) {
        this.item.setType(Options.material(plugin, path + ".material", this.item.getType()));
        if (plugin.getConfig().isSet(path + ".data")) {
            this.item.setDurability((short) Options.integer(plugin, path + ".data", 0, 0, 32767));
        }
        return this;
    }

    public ItemStack build() {
        return this.tag == null ? this.item : ItemTag.set(this.item, this.tag);
    }
}
