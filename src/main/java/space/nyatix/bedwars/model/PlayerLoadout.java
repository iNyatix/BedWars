package space.nyatix.bedwars.model;

public class PlayerLoadout {

    private int armorTier = 0, pickaxeTier = 0, axeTier = 0;

    private boolean shears = false, magicMilk = false;

    public int armorTier() {
        return this.armorTier;
    }

    public void armorTier(final int v) {
        this.armorTier = Math.max(this.armorTier, v);
    }

    public int pickaxeTier() {
        return this.pickaxeTier;
    }

    public void pickaxeTier(final int v) {
        this.pickaxeTier = Math.max(0, Math.min(4, v));
    }

    public int axeTier() {
        return this.axeTier;
    }

    public void axeTier(final int v) {
        this.axeTier = Math.max(0, Math.min(4, v));
    }

    public boolean shears() {
        return this.shears;
    }

    public boolean magicMilk() {
        return this.magicMilk;
    }

    public void magicMilk(final boolean v) {
        this.magicMilk = v;
    }

    public void shears(final boolean v) {
        this.shears = v;
    }

    public void downgradeTools() {
        if (this.pickaxeTier > 1) {
            this.pickaxeTier--;
        }
        if (this.axeTier > 1) {
            this.axeTier--;
        }
    }

    public void reset() {
        this.armorTier = this.pickaxeTier = this.axeTier = 0;
        this.shears = false;
        this.magicMilk = false;
    }
}
