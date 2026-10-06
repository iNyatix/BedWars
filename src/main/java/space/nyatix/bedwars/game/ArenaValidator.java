package space.nyatix.bedwars.game;

import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GeneratorType;
import space.nyatix.bedwars.model.TeamColor;

import java.util.ArrayList;
import java.util.List;

public final class ArenaValidator {

    private ArenaValidator() {
    }

    public static List<String> validate(final Arena arena) {
        final List<String> out = new ArrayList<>();
        if (arena == null) {
            out.add(Messages.get("arena-validator.validate.brak-wybranej-mapy"));
            return out;
        }
        if (arena.getWorld() == null) {
            out.add(Messages.get("arena-validator.validate.brak-swiata-mapy"));
        }
        if (arena.getLobby() == null) {
            out.add(Messages.get("arena-validator.validate.brak-lobby"));
        }
        if (arena.getSpectator() == null) {
            out.add(Messages.get("arena-validator.validate.brak-spawnu-obserwatorow"));
        }
        if (!WorldBorderManager.isConfiguredStatic(arena)) {
            out.add(Messages.get("arena-validator.validate.brak-skonfigurowanego-worldbordera-srodek-promien"));
        }
        for (final TeamColor teamColor : TeamColor.values()) {
            if (arena.team(teamColor).getSpawn() == null) {
                out.add(Messages.get("arena-validator.validate.brak-spawnu-druzyny", "TEAM", teamColor.display()));
            }
            if (arena.team(teamColor).getBed() == null) {
                out.add(Messages.get("arena-validator.validate.brak-lozka-druzyny", "TEAM", teamColor.display()));
            }
            if (arena.team(teamColor).getBaseGenerator() == null) {
                out.add(Messages.get("arena-validator.validate.brak-generatora-bazy-druzyny", "TEAM", teamColor.display()));
            }
            if (arena.team(teamColor).getProtectedPos1() == null || arena.team(teamColor).getProtectedPos2() == null) {
                out.add(Messages.get("arena-validator.validate.brak-strefy-bez-budowania-druzyny-ustaw", "TEAM", teamColor.display()));
            }
        }
        if (arena.getGenerators().get(GeneratorType.DIAMOND).isEmpty()) {
            out.add(Messages.get("arena-validator.validate.brak-generatorow-diamentow"));
        }
        if (arena.getGenerators().get(GeneratorType.EMERALD).isEmpty()) {
            out.add(Messages.get("arena-validator.validate.brak-generatorow-szmaragdow"));
        }
        if (arena.getItemShops().size() < 4) {
            out.add(Messages.get("arena-validator.validate.sklepy-z-przedmiotami-4-ustaw-po", "VALUE1", arena.getItemShops().size()));
        }
        if (arena.getUpgradeShops().size() < 4) {
            out.add(Messages.get("arena-validator.validate.sklepy-ulepszen-4-ustaw-po-jednym", "VALUE1", arena.getUpgradeShops().size()));
        }
        return out;
    }
}
