package space.nyatix.bedwars.model;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import space.nyatix.bedwars.message.Messages;

import java.util.Locale;

public enum TeamColor {

    RED(ChatColor.RED, DyeColor.RED),
    GREEN(ChatColor.GREEN, DyeColor.LIME),
    BLUE(ChatColor.BLUE, DyeColor.BLUE),
    WHITE(ChatColor.WHITE, DyeColor.WHITE);

    private final ChatColor chat;

    private final DyeColor dye;

    TeamColor(final ChatColor c, final DyeColor d) {
        this.chat = c;
        this.dye = d;
    }

    public ChatColor chat() {
        return this.chat;
    }

    public DyeColor dye() {
        return this.dye;
    }

    public String shortName() {
        return Messages.get("teams." + name().toLowerCase(Locale.ROOT) + ".short");
    }

    public String display() {
        return Messages.get("teams." + name().toLowerCase(Locale.ROOT).replace('_', '-') + ".name");
    }
}
