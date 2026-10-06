package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.GameState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InvisibilityManager {

    private final BedWarsPlugin plugin;

    private final Set<UUID> hidden = new HashSet<>();

    private final int taskId;

    private boolean reflectionReady;

    private boolean reflectionFailed;

    private Method craftPlayerGetHandle;

    private Field playerConnectionField;

    private Method sendPacketMethod;

    private Constructor<?> equipmentPacketConstructor;

    private Method asNmsCopy;

    public InvisibilityManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 5L, 5L);
    }

    public void tick() {
        if (this.plugin.game() == null) {
            return;
        }
        if (this.plugin.game().state() != GameState.RUNNING) {
            if (!this.hidden.isEmpty()) {
                clearAll();
            }
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.game().teamOf(player.getUniqueId()) == null || this.plugin.game().isSpectator(player.getUniqueId())) {
                if (this.hidden.remove(player.getUniqueId())) {
                    showEquipment(player);
                }
                continue;
            }
            final boolean invisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
            if (invisible) {
                this.hidden.add(player.getUniqueId());
                hideEquipment(player);
            } else if (this.hidden.remove(player.getUniqueId())) {
                showEquipment(player);
            }
        }
    }

    public void reveal(final Player player) {
        if (player == null) {
            return;
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        this.hidden.remove(player.getUniqueId());
        showEquipment(player);
    }

    public void refreshIfInvisible(final Player player) {
        if (player == null || !player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (player.isOnline() && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                hideEquipment(player);
            }
        });
    }

    public void clear(final Player player) {
        if (player == null) {
            return;
        }
        if (this.hidden.remove(player.getUniqueId())) {
            showEquipment(player);
        }
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTask(this.taskId);
        clearAll();
    }

    public void clearAll() {
        for (final UUID playerId : new HashSet<>(this.hidden)) {
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                showEquipment(player);
            }
        }
        this.hidden.clear();
    }

    private void hideEquipment(final Player subject) {
        if (subject == null || !subject.isOnline()) {
            return;
        }
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(subject) || !viewer.getWorld().equals(subject.getWorld())) {
                continue;
            }
            sendEquipment(viewer, subject, 0, null);
            sendEquipment(viewer, subject, 1, null);
            sendEquipment(viewer, subject, 2, null);
            sendEquipment(viewer, subject, 3, null);
            sendEquipment(viewer, subject, 4, null);
        }
    }

    private void showEquipment(final Player subject) {
        if (subject == null || !subject.isOnline()) {
            return;
        }
        final ItemStack hand = subject.getItemInHand();
        final ItemStack boots = subject.getInventory().getBoots();
        final ItemStack legs = subject.getInventory().getLeggings();
        final ItemStack chest = subject.getInventory().getChestplate();
        final ItemStack helmet = subject.getInventory().getHelmet();
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(subject) || !viewer.getWorld().equals(subject.getWorld())) {
                continue;
            }
            sendEquipment(viewer, subject, 0, hand);
            sendEquipment(viewer, subject, 1, boots);
            sendEquipment(viewer, subject, 2, legs);
            sendEquipment(viewer, subject, 3, chest);
            sendEquipment(viewer, subject, 4, helmet);
        }
    }

    private void sendEquipment(final Player viewer, final Player subject, final int slot, final ItemStack item) {
        try {
            if (!prepareReflection(viewer)) {
                return;
            }
            final Object viewerHandle = this.craftPlayerGetHandle.invoke(viewer);
            final Object connection = this.playerConnectionField.get(viewerHandle);
            Object nmsItem = null;
            if (item != null && item.getType() != Material.AIR) {
                nmsItem = this.asNmsCopy.invoke(null, item);
            }
            final Object packet = this.equipmentPacketConstructor.newInstance(subject.getEntityId(), slot, nmsItem);
            this.sendPacketMethod.invoke(connection, packet);
        } catch (final Throwable ex) {
            if (!this.reflectionFailed) {
                this.reflectionFailed = true;
                this.plugin.getLogger().warning(Messages.get("invisibility.send-equipment.nie-udalo-sie-ukrywac-zbroi-podczas", "VALUE1",
                        ex.getClass().getSimpleName(), "ERROR", ex.getMessage()));
            }
        }
    }

    private boolean prepareReflection(final Player sample) {
        if (this.reflectionReady) {
            return true;
        }
        if (this.reflectionFailed) {
            return false;
        }
        try {
            final Object server = Bukkit.getServer();
            final String pkg = server.getClass().getPackage().getName();
            final String version = pkg.substring(pkg.lastIndexOf('.') + 1);
            final Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            final Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            final Class<?> nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
            final Class<?> packet = Class.forName("net.minecraft.server." + version + ".Packet");
            final Class<?> equipmentPacket = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityEquipment");
            this.craftPlayerGetHandle = craftPlayer.getMethod("getHandle");
            final Object handle = this.craftPlayerGetHandle.invoke(sample);
            this.playerConnectionField = handle.getClass().getField("playerConnection");
            final Object connection = this.playerConnectionField.get(handle);
            this.sendPacketMethod = connection.getClass().getMethod("sendPacket", packet);
            this.equipmentPacketConstructor = equipmentPacket.getConstructor(int.class, int.class, nmsItemStack);
            this.asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            this.reflectionReady = true;
            return true;
        } catch (final Throwable ex) {
            this.reflectionFailed = true;
            this.plugin.getLogger().warning(Messages.get("invisibility.prepare-reflection.brak-zgodnego-packetu-wyposazenia-dla-niewidzialnosci",
                    "VALUE1", ex.getClass().getSimpleName(), "ERROR", ex.getMessage()));
            return false;
        }
    }
}
