package space.nyatix.bedwars.model;

import space.nyatix.bedwars.message.Messages;

import java.util.Locale;

public enum TrapType {

    ITS_A_TRAP,
    COUNTER_OFFENSIVE,
    ALARM,
    MINER_FATIGUE;

    public String display() {
        return Messages.get("traps." + name().toLowerCase(Locale.ROOT).replace('_', '-') + ".name");
    }
}
