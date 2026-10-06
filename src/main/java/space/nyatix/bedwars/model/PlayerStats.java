package space.nyatix.bedwars.model;

public class PlayerStats {

    private int kills, finalKills, bedsBroken, deaths, wins, resourcesCollected;

    public int getKills() {
        return this.kills;
    }

    public int getFinalKills() {
        return this.finalKills;
    }

    public int getBedsBroken() {
        return this.bedsBroken;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public int getWins() {
        return this.wins;
    }

    public int getResourcesCollected() {
        return this.resourcesCollected;
    }

    public void kill() {
        this.kills++;
    }

    public void finalKill() {
        this.finalKills++;
    }

    public void bed() {
        this.bedsBroken++;
    }

    public void death() {
        this.deaths++;
    }

    public void win() {
        this.wins++;
    }

    public void resource(final int amount) {
        this.resourcesCollected += Math.max(0, amount);
    }
}
