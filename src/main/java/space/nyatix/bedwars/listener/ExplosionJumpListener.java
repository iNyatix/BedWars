package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.model.GameState;

public class ExplosionJumpListener implements Listener {

    private final BedWarsPlugin plugin;

    public ExplosionJumpListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void explode(final EntityExplodeEvent event) {
        if (this.plugin.game() == null || this.plugin.game().state() != GameState.RUNNING) {
            return;
        }
        final Entity entity = event.getEntity();
        final boolean tnt = entity instanceof TNTPrimed;
        final boolean fireball = entity instanceof Fireball;
        if (!tnt && !fireball) {
            return;
        }
        if (tnt && !this.plugin.settings().tntJumpEnabled()) {
            return;
        }
        if (fireball && !this.plugin.settings().fireballJumpEnabled()) {
            return;
        }
        final Location center = event.getLocation().clone();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            apply(center, tnt);
        });
    }

    private void apply(final Location center, final boolean tnt) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        final double radius = tnt ? this.plugin.settings().tntJumpRadius() : this.plugin.settings().fireballJumpRadius();
        final double horizontal = tnt ? this.plugin.settings().tntJumpHorizontal() : this.plugin.settings().fireballJumpHorizontal();
        final double vertical = tnt ? this.plugin.settings().tntJumpVertical() : this.plugin.settings().fireballJumpVertical();
        final double maxHorizontal = tnt ? this.plugin.settings().tntJumpMaxHorizontal() : this.plugin.settings().fireballJumpMaxHorizontal();
        if (radius <= 0) {
            return;
        }
        final World world = center.getWorld();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != world) {
                continue;
            }
            if (this.plugin.game().teamOf(player.getUniqueId()) == null || this.plugin.game().isSpectator(player.getUniqueId()) || this.plugin.game().isFinalDead(player.getUniqueId())) {
                continue;
            }
            final Location feet = player.getLocation();
            final double distance = feet.distance(center);
            if (distance > radius) {
                continue;
            }
            final double strength = Math.max(0.0D, 1.0D - (distance / radius));
            Vector away = feet.toVector().subtract(center.toVector());
            away.setY(0.0D);
            if (away.lengthSquared() < 0.0001D) {
                away = player.getLocation().getDirection().multiply(-1.0D).setY(0.0D);
            }
            if (away.lengthSquared() > 0.0001D) {
                away.normalize();
            }
            final double h = Math.min(maxHorizontal, horizontal * (0.30D + 0.70D * strength));
            away.multiply(h);
            final Vector current = player.getVelocity();
            final double y = Math.max(current.getY(), vertical * (0.55D + 0.45D * strength));
            final Vector result = new Vector(current.getX() + away.getX(), y, current.getZ() + away.getZ());
            final double horiz = Math.sqrt(result.getX() * result.getX() + result.getZ() * result.getZ());
            final double cap = Math.max(maxHorizontal, 0.1D);
            if (horiz > cap) {
                final double scale = cap / horiz;
                result.setX(result.getX() * scale);
                result.setZ(result.getZ() * scale);
            }
            player.setVelocity(result);
        }
    }
}
