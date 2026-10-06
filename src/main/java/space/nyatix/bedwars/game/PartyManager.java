package space.nyatix.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.config.Options;
import space.nyatix.bedwars.message.Messages;
import space.nyatix.bedwars.model.Arena;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.TeamColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PartyManager {

    private final BedWarsPlugin plugin;

    private final Map<UUID, Party> byMember = new HashMap<>();

    private final Map<UUID, Invite> invites = new HashMap<>();

    public PartyManager(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public Party partyOf(final UUID player) {
        return player == null ? null : this.byMember.get(player);
    }

    public boolean inParty(final UUID player) {
        return partyOf(player) != null;
    }

    public Party ensureParty(final Player leader) {
        final Party existing = partyOf(leader.getUniqueId());
        if (existing != null) {
            return existing;
        }
        final Party party = new Party(leader.getUniqueId());
        party.members.add(leader.getUniqueId());
        this.byMember.put(leader.getUniqueId(), party);
        return party;
    }

    public String invite(final Player leader, final Player target) {
        if (leader == null || target == null) {
            return Messages.get("party.invite.nie-znaleziono-gracza");
        }
        if (leader.equals(target)) {
            return Messages.get("party.invite.nie-mozesz-zaprosic-samego-siebie");
        }
        final Party party = ensureParty(leader);
        if (!party.leader.equals(leader.getUniqueId())) {
            return Messages.get("party.invite.tylko-lider-party-moze-zapraszac");
        }
        if (party.members.contains(target.getUniqueId())) {
            return Messages.get("party.invite.ten-gracz-jest-juz-w-twoim");
        }
        if (partyOf(target.getUniqueId()) != null) {
            return Messages.get("party.invite.ten-gracz-jest-juz-w-innym");
        }
        if (party.members.size() >= this.plugin.settings().playersPerTeam()) {
            return Messages.get("party.invite.party-jest-pelne-dla-aktualnego-rozmiaru");
        }
        this.invites.put(target.getUniqueId(), new Invite(leader.getUniqueId(), System.currentTimeMillis() + 1000L * Options.integer(this.plugin,
                "party.inviteSeconds", 60, 5, 3600)));
        Messages.send(target, Messages.get("party.invite.party-zaprosil-cie-do-party", "PLAYER", leader.getName()));
        Messages.send(target, Messages.get("party.invite.wpisz-party-akceptuj-w-ciagu-60", "SECONDS", Options.integer(this.plugin, "party.inviteSeconds", 60, 5, 3600)));
        return Messages.get("party.invite.zaproszono-gracza-do-party", "PLAYER", target.getName());
    }

    public String accept(final Player player) {
        final Invite invite = this.invites.remove(player.getUniqueId());
        if (invite == null || invite.expiresAt < System.currentTimeMillis()) {
            return Messages.get("party.accept.nie-masz-aktywnego-zaproszenia-do-party");
        }
        final Player leader = Bukkit.getPlayer(invite.leader);
        if (leader == null) {
            return Messages.get("party.accept.lider-party-jest-offline");
        }
        Party party = partyOf(invite.leader);
        if (party == null) {
            party = ensureParty(leader);
        }
        if (party.members.size() >= this.plugin.settings().playersPerTeam()) {
            return Messages.get("party.accept.party-jest-juz-pelne");
        }
        final Party old = partyOf(player.getUniqueId());
        if (old != null) {
            return Messages.get("party.accept.najpierw-opusc-obecne-party");
        }
        party.members.add(player.getUniqueId());
        this.byMember.put(player.getUniqueId(), party);
        broadcast(party, Messages.get("party.accept.party-dolaczyl-do-party", "PLAYER", player.getName()));
        return null;
    }

    public String leave(final Player player) {
        final Party party = partyOf(player.getUniqueId());
        if (party == null) {
            return Messages.get("party.leave.nie-jestes-w-party");
        }
        party.members.remove(player.getUniqueId());
        this.byMember.remove(player.getUniqueId());
        this.invites.remove(player.getUniqueId());
        if (party.members.isEmpty()) {
            return Messages.get("party.leave.opusciles-party");
        }
        if (party.leader.equals(player.getUniqueId())) {
            party.leader = party.members.iterator().next();
            final Player next = Bukkit.getPlayer(party.leader);
            broadcast(party, Messages.get("party.leave.party-opuscil-party-nowy-lider", "PLAYER", player.getName(), "PLAYER2", (next == null ? name(party.leader) : next.getName())));
        } else {
            broadcast(party, Messages.get("party.leave.party-opuscil-party", "PLAYER", player.getName()));
        }
        return Messages.get("party.leave.opusciles-party");
    }

    public String kick(final Player leader, final Player target) {
        final Party party = partyOf(leader.getUniqueId());
        if (party == null) {
            return Messages.get("party.kick.nie-jestes-w-party");
        }
        if (!party.leader.equals(leader.getUniqueId())) {
            return Messages.get("party.kick.tylko-lider-party-moze-wyrzucac-graczy");
        }
        if (target == null || !party.members.contains(target.getUniqueId())) {
            return Messages.get("party.kick.ten-gracz-nie-jest-w-twoim");
        }
        if (target.equals(leader)) {
            return Messages.get("party.kick.uzyj-party-opusc-albo-party-rozwiaz");
        }
        party.members.remove(target.getUniqueId());
        this.byMember.remove(target.getUniqueId());
        Messages.send(target, Messages.get("party.kick.party-zostales-wyrzucony-z-party"));
        broadcast(party, Messages.get("party.kick.party-zostal-wyrzucony-z-party", "PLAYER", target.getName()));
        return null;
    }

    public String disband(final Player leader) {
        final Party party = partyOf(leader.getUniqueId());
        if (party == null) {
            return Messages.get("party.disband.nie-jestes-w-party");
        }
        if (!party.leader.equals(leader.getUniqueId())) {
            return Messages.get("party.disband.tylko-lider-party-moze-rozwiazac-party");
        }
        broadcast(party, Messages.get("party.disband.party-party-zostalo-rozwiazane"));
        for (final UUID playerId : new ArrayList<>(party.members)) {
            this.byMember.remove(playerId);
        }
        this.invites.entrySet().removeIf(e -> party.members.contains(e.getValue().leader));
        party.members.clear();
        return null;
    }

    public void sendInfo(final Player player) {
        final Party party = partyOf(player.getUniqueId());
        if (party == null) {
            Messages.send(player, Messages.get("party.send-info.party-nie-jestes-w-party"));
            return;
        }
        Messages.send(player, Messages.get("party.send-info.text"));
        Messages.send(player, Messages.get("party.send-info.party", "VALUE1", party.members.size(), "VALUE2", this.plugin.settings().playersPerTeam()));
        Messages.send(player, Messages.get("party.send-info.lider", "VALUE1", name(party.leader)));
        final StringBuilder members = new StringBuilder();
        for (final UUID playerId : party.members) {
            if (members.length() > 0) {
                members.append(Messages.get("party.send-info.text-2"));
            }
            final Player online = Bukkit.getPlayer(playerId);
            members.append(online != null ? "§a" : "§8").append(name(playerId));
        }
        Messages.send(player, Messages.get("party.send-info.gracze", "VALUE1", members.toString()));
        Messages.send(player, Messages.get("party.send-info.kolor-party", "TEAM", (party.preferredTeam == null ? Messages.get("party.send-info.automatyczny") : party.preferredTeam.chat() + party.preferredTeam.display())));
        Messages.send(player, Messages.get("party.send-info.lider-wybiera-kolor-przez-party-kolor"));
        Messages.send(player, Messages.get("party.send-info.klikniecie-kompasu-przez-dowolna-osobe-z"));
        Messages.send(player, Messages.get("party.send-info.probuje-wrzucic-wszystkich-online-do-tej"));
        Messages.send(player, Messages.get("party.send-info.text"));
    }

    public List<Player> onlineMembers(final Player player) {
        final Party party = partyOf(player.getUniqueId());
        final List<Player> out = new ArrayList<>();
        if (party == null) {
            out.add(player);
            return out;
        }
        for (final UUID playerId : party.members) {
            final Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                out.add(p);
            }
        }
        if (!out.contains(player)) {
            out.add(player);
        }
        return out;
    }

    public int size(final UUID player) {
        final Party p = partyOf(player);
        return p == null ? 1 : p.members.size();
    }

    public TeamColor preferredTeam(final UUID player) {
        final Party party = partyOf(player);
        return party == null ? null : party.preferredTeam;
    }

    public String selectColor(final Player leader, final TeamColor color) {
        final Party party = partyOf(leader.getUniqueId());
        if (party == null) {
            return Messages.get("party.select-color.nie-jestes-w-party");
        }
        if (!party.leader.equals(leader.getUniqueId())) {
            return Messages.get("party.select-color.tylko-lider-party-moze-wybierac-kolor");
        }
        final GameState state = this.plugin.game().state();
        if (state != GameState.WAITING && state != GameState.COUNTDOWN) {
            return Messages.get("party.select-color.kolor-party-mozna-zmienic-tylko-przed");
        }
        final List<Player> waiting = new ArrayList<>();
        for (final Player member : onlineMembers(leader)) {
            if (this.plugin.game().teamOf(member.getUniqueId()) != null) {
                waiting.add(member);
            }
        }
        if (color != null && !waiting.isEmpty() && !this.plugin.game().assignGroup(waiting, color)) {
            return Messages.get("party.select-color.w-wybranym-kolorze-nie-ma-miejsca");
        }
        party.preferredTeam = color;
        broadcast(party, Messages.get("party.select-color.party-kolor-party", "TEAM", (color == null ? Messages.get("party.select-color.automatyczny") : color.chat() + color.display())));
        if (color != null) {
            broadcast(party, Messages.get("party.select-color.ten-kolor-bedzie-wybierany-w-kazdym"));
        }
        return null;
    }

    public String assignForQueue(final Player clicker, final List<Player> group) {
        final Arena arena = this.plugin.game().active();
        if (arena == null) {
            return Messages.get("party.assign-for-queue.brak-aktywnej-mapy");
        }
        final int limit = this.plugin.settings().playersPerTeam();
        if (size(clicker.getUniqueId()) > limit) {
            return Messages.get("party.assign-for-queue.party-ma-za-duzo-osob-druzyna", "LIMIT", limit);
        }
        final TeamColor requested = preferredTeam(clicker.getUniqueId());
        final TeamColor target = requested == null ? findTeam(arena, group, limit) : requested;
        if (target == null) {
            return Messages.get("party.assign-for-queue.brak-druzyny-z-miejscem-dla-calego");
        }
        if (!this.plugin.game().assignGroup(group, target)) {
            return requested == null ? Messages.get("party.assign-for-queue.nie-udalo-sie-przypisac-calego-party") : Messages.get("party.assign-for-queue.brak-miejsca-dla-calego-party-w",
                    
                    "VALUE1", requested.chat(), "TEAM", requested.display());
        }
        for (final Player member : group) {
            Messages.send(member, Messages.get("party.assign-for-queue.party-party-zostalo-przydzielone-do", "VALUE1", target.chat(), "TEAM", target.display()));
        }
        return null;
    }

    private TeamColor findTeam(final Arena arena, final List<Player> group, final int limit) {
        final Set<UUID> ids = new HashSet<>();
        TeamColor current = null;
        for (final Player player : group) {
            ids.add(player.getUniqueId());
            final TeamColor team = this.plugin.game().teamOf(player.getUniqueId());
            if (current == null && team != null) {
                current = team;
            }
        }
        if (current != null && countExcluding(arena, current, ids) + ids.size() <= limit) {
            return current;
        }
        TeamColor best = null;
        int min = Integer.MAX_VALUE;
        for (final TeamColor team : TeamColor.values()) {
            final int count = countExcluding(arena, team, ids);
            if (count + ids.size() <= limit && count < min) {
                best = team;
                min = count;
            }
        }
        return best;
    }

    private int countExcluding(final Arena arena, final TeamColor team, final Set<UUID> group) {
        int count = 0;
        for (final UUID playerId : arena.team(team).getPlayers()) {
            if (!group.contains(playerId)) {
                count++;
            }
        }
        return count;
    }

    private void broadcast(final Party party, final String message) {
        for (final UUID playerId : party.members) {
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                Messages.send(player, message);
            }
        }
    }

    private String name(final UUID playerId) {
        final String n = Bukkit.getOfflinePlayer(playerId).getName();
        return n == null ? playerId.toString() : n;
    }

    public static class Party {

        private UUID leader;

        private TeamColor preferredTeam;

        private final LinkedHashSet<UUID> members = new LinkedHashSet<>();

        private Party(final UUID leader) {
            this.leader = leader;
        }

        public UUID leader() {
            return this.leader;
        }

        public Set<UUID> members() {
            return Collections.unmodifiableSet(this.members);
        }
    }

    private static class Invite {

        private final UUID leader;

        private final long expiresAt;

        private Invite(final UUID leader, final long expiresAt) {
            this.leader = leader;
            this.expiresAt = expiresAt;
        }
    }
}
