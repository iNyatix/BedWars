package space.nyatix.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ItemTag {

    private static final String KEY = "BedWarsItem";

    private static final Map<ItemStack, String> fallback = Collections.synchronizedMap(new IdentityHashMap<>());

    private ItemTag() {
    }

    public static ItemStack set(final ItemStack item, final String value) {
        try {
            final String craft = Bukkit.getServer().getClass().getPackage().getName();
            final Class<?> itemClass = Class.forName(craft + ".inventory.CraftItemStack");
            final Object handle = itemClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
            final String nms = handle.getClass().getPackage().getName();
            final Class<?> compoundClass = Class.forName(nms + ".NBTTagCompound");
            Object compound = handle.getClass().getMethod("getTag").invoke(handle);
            if (compound == null) {
                compound = compoundClass.newInstance();
            }
            compoundClass.getMethod("setString", String.class, String.class).invoke(compound, KEY, value);
            handle.getClass().getMethod("setTag", compoundClass).invoke(handle, compound);
            return (ItemStack) itemClass.getMethod("asCraftMirror", handle.getClass()).invoke(null, handle);
        } catch (final ReflectiveOperationException | LinkageError exception) {
            if (fallback.size() >= 1024) {
                fallback.clear();
            }
            fallback.put(item, value);
            return item;
        }
    }

    public static boolean is(final ItemStack item, final String value) {
        if (item == null) {
            return false;
        }
        if (value.equals(fallback.get(item))) {
            return true;
        }
        try {
            final String craft = Bukkit.getServer().getClass().getPackage().getName();
            final Class<?> itemClass = Class.forName(craft + ".inventory.CraftItemStack");
            final Object handle = itemClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
            if (handle == null) {
                return false;
            }
            final Object compound = handle.getClass().getMethod("getTag").invoke(handle);
            return compound != null && value.equals(compound.getClass().getMethod("getString", String.class).invoke(compound, KEY));
        } catch (final ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }
}
