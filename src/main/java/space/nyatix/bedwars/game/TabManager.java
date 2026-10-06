package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.MatchViewText;
import space.nyatix.bedwars.message.Messages;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TabManager {

    private final BedWarsPlugin plugin;

    private boolean warned;

    public TabManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void update(final Player viewer) {
        if (viewer == null) {
            return;
        }
        if (!Options.enabled(this.plugin, "tab.enabled", true)) {
            viewer.setPlayerListName(viewer.getName());
            this.sendHeaderFooter(viewer, "", "");
            return;
        }
        final GameManager game = this.plugin.game();
        final Object[] variables = new MatchViewText(this.plugin).variables(viewer, viewer);
        final String role = game.canIntervene(viewer) ? "admin" : game.isSpectator(viewer.getUniqueId()) ? "spectator" : "player";
        viewer.setPlayerListName(MatchViewText.limit(Messages.get("tab.name-" + role, variables), 32));
        final boolean lobby = game.teamOf(viewer.getUniqueId()) == null && !game.isSpectator(viewer.getUniqueId());
        this.sendHeaderFooter(viewer, Messages.get("tab.header", variables), Messages.get(lobby ? "tab.footer-lobby" : "tab.footer-match", variables));
    }

    private String trim(final String text, final int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        final String out = text.substring(0, max);
        return out.endsWith("§") ? out.substring(0, out.length() - 1) : out;
    }

    private void sendHeaderFooter(final Player player, final String header, final String footer) {
        try {
            final String craft = Bukkit.getServer().getClass().getPackage().getName();
            final String version = craft.substring(craft.lastIndexOf('.') + 1);
            final String nms = "net.minecraft.server." + version + ".";
            final Class<?> component = Class.forName(nms + "IChatBaseComponent");
            final Class<?> text = Class.forName(nms + "ChatComponentText");
            final Object h = text.getConstructor(String.class).newInstance(header);
            final Object f = text.getConstructor(String.class).newInstance(footer);
            final Class<?> packetClass = Class.forName(nms + "PacketPlayOutPlayerListHeaderFooter");
            final Object packet = packetClass.newInstance();
            final Field field = componentField(packetClass, "a", component, 0), b = componentField(packetClass, "b", component, 1);
            field.setAccessible(true);
            b.setAccessible(true);
            field.set(packet, h);
            b.set(packet, f);
            final Object handle = player.getClass().getMethod("getHandle").invoke(player);
            final Field connection = findField(handle.getClass());
            connection.setAccessible(true);
            final Object pc = connection.get(handle);
            Method send = null;
            for (final Method method : pc.getClass().getMethods()) {
                if ("sendPacket".equals(method.getName()) && method.getParameterTypes().length == 1) {
                    send = method;
                    break;
                }
            }
            if (send == null) {
                throw new NoSuchMethodException("sendPacket");
            }
            send.invoke(pc, packet);
        } catch (final Throwable ex) {
            if (!this.warned) {
                this.warned = true;
                this.plugin.getLogger().warning(Messages.get("tab.send-header-footer.nie-udalo-sie-ustawic-naglowka-tab", "VALUE1", ex.getClass().getSimpleName(),
                        "ERROR", ex.getMessage()));
            }
        }
    }

    private Field componentField(final Class<?> type, final String preferred, final Class<?> component, final int index) throws Exception {
        try {
            return type.getDeclaredField(preferred);
        } catch (final NoSuchFieldException ignored) {
        }
        int found = 0;
        for (final Field field : type.getDeclaredFields()) {
            if (component.isAssignableFrom(field.getType())) {
                if (found++ == index) {
                    return field;
                }
            }
        }
        throw new NoSuchFieldException(preferred);
    }

    private Field findField(final Class<?> type) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField("playerConnection");
            } catch (final NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException("playerConnection");
    }
}
