package space.nyatix.bedwars.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MenuHolder implements InventoryHolder {

    private final UUID owner;

    private final String type;

    private final Map<Integer, String> actions = new HashMap<>();

    private Inventory inventory;

    private MenuHolder(final UUID owner, final String type) {
        this.owner = owner;
        this.type = type;
    }

    public static Inventory create(final Player player, final String type, final int size, final String title) {
        final MenuHolder holder = new MenuHolder(player.getUniqueId(), type);
        String display = title;
        if (display.length() > 32) {
            display = display.substring(0, 32);
            if (display.endsWith("§")) {
                display = display.substring(0, display.length() - 1);
            }
        }
        holder.inventory = Bukkit.createInventory(holder, size, display);
        return holder.inventory;
    }

    public static MenuHolder get(final Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MenuHolder ? (MenuHolder) inventory.getHolder() : null;
    }

    public boolean belongsTo(final Player player) {
        return this.owner.equals(player.getUniqueId());
    }

    public String getType() {
        return this.type;
    }

    public void setAction(final int slot, final String action) {
        this.actions.put(slot, action);
    }

    public String getAction(final int slot) {
        return this.actions.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
