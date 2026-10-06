package space.nyatix.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import space.nyatix.bedwars.message.Messages;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TitleSender {

    private final Plugin plugin;

    private Constructor<?> text, packet, times;

    private Method getHandle, sendPacket;

    private Field connection;

    private Object titleAction, subtitleAction, resetAction;

    private boolean unavailable, warned;

    public TitleSender(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void send(final Player player, final String title, final String subtitle, final int fadeIn, final int stay, final int fadeOut) {
        if (player == null || !player.isOnline() || this.unavailable) {
            return;
        }
        try {
            initialize(player);
            final Object channel = this.connection.get(this.getHandle.invoke(player));
            this.sendPacket.invoke(channel, this.times.newInstance(fadeIn, stay, fadeOut));
            sendText(channel, title, subtitle);
        } catch (final ReflectiveOperationException | LinkageError ex) {
            fail(ex);
        }
    }

    public void update(final Player player, final String title, final String subtitle) {
        if (player == null || !player.isOnline() || this.unavailable) {
            return;
        }
        try {
            initialize(player);
            sendText(this.connection.get(this.getHandle.invoke(player)), title, subtitle);
        } catch (final ReflectiveOperationException | LinkageError ex) {
            fail(ex);
        }
    }

    private void sendText(final Object channel, final String title, final String subtitle) throws ReflectiveOperationException {
        this.sendPacket.invoke(channel, this.packet.newInstance(this.subtitleAction, component(subtitle)));
        this.sendPacket.invoke(channel, this.packet.newInstance(this.titleAction, component(title)));
    }

    public void clear(final Player player) {
        if (player == null || !player.isOnline() || this.unavailable) {
            return;
        }
        try {
            initialize(player);
            this.sendPacket.invoke(this.connection.get(this.getHandle.invoke(player)), this.packet.newInstance(this.resetAction, null));
        } catch (final ReflectiveOperationException | LinkageError ex) {
            fail(ex);
        }
    }

    private Object component(final String value) throws ReflectiveOperationException {
        return this.text.newInstance(ChatColor.translateAlternateColorCodes('&', value == null ? "" : value));
    }

    private void initialize(final Player player) throws ReflectiveOperationException {
        if (this.sendPacket != null) {
            return;
        }
        final String craft = Bukkit.getServer().getClass().getPackage().getName();
        final String nms = "net.minecraft.server." + craft.substring(craft.lastIndexOf('.') + 1) + ".";
        final Class<?> titlePacket = Class.forName(nms + "PacketPlayOutTitle");
        final Class<?> action = Class.forName(nms + "PacketPlayOutTitle$EnumTitleAction");
        final Class<?> component = Class.forName(nms + "IChatBaseComponent");
        this.text = Class.forName(nms + "ChatComponentText").getConstructor(String.class);
        this.packet = titlePacket.getConstructor(action, component);
        this.times = titlePacket.getConstructor(int.class, int.class, int.class);
        this.titleAction = action.getField("TITLE").get(null);
        this.subtitleAction = action.getField("SUBTITLE").get(null);
        this.resetAction = action.getField("RESET").get(null);
        this.getHandle = player.getClass().getMethod("getHandle");
        final Object handle = this.getHandle.invoke(player);
        this.connection = handle.getClass().getField("playerConnection");
        this.sendPacket = this.connection.get(handle).getClass().getMethod("sendPacket", Class.forName(nms + "Packet"));
    }

    private void fail(final Throwable ex) {
        this.unavailable = true;
        if (!this.warned) {
            this.warned = true;
            this.plugin.getLogger().warning(Messages.get("title-sender.fail.nie-mozna-wysylac-title-wymagany-spigot", "VALUE1", ex.getClass().getSimpleName()));
        }
    }
}
