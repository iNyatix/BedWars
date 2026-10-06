package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.model.GameState;
import space.nyatix.bedwars.model.TeamColor;

import java.util.ArrayList;
import java.util.List;

public class BedBreakEffectsListener implements Listener {

    private final BedWarsPlugin plugin;

    public BedBreakEffectsListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void breakBed(final BlockBreakEvent event) {
        if (this.plugin.game() == null || this.plugin.game().state() != GameState.RUNNING) {
            return;
        }
        final Block block = event.getBlock();
        final TeamColor victim = this.plugin.game().bedTeam(block.getLocation());
        if (victim == null) {
            return;
        }
        final TeamColor own = this.plugin.game().teamOf(event.getPlayer().getUniqueId());
        if (victim == own || event.isCancelled()) {
            return;
        }
        event.setCancelled(true);
        final Location effect = block.getLocation().clone().add(0.5D, 0.5D, 0.5D);
        removeBed(block);
        try {
            effect.getWorld().strikeLightningEffect(effect);
        } catch (final Throwable ignored) {
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            removeDrops(effect);
        }, 1L);
    }

    private void removeBed(final Block block) {
        final List<Block> parts = new ArrayList<>();
        parts.add(block);
        for (final BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST }) {
            final Block other = block.getRelative(face);
            if (other.getType() == Material.BED_BLOCK) {
                parts.add(other);
            }
        }
        for (final Block part : parts) {
            try {
                part.setType(Material.AIR);
            } catch (final Throwable ignored) {
            }
        }
    }

    private void removeDrops(final Location where) {
        try {
            for (final Entity entity : where.getWorld().getEntities()) {
                if (!(entity instanceof Item) || entity.getLocation().distanceSquared(where) > 9.0D) {
                    continue;
                }
                final ItemStack stack = ((Item) entity).getItemStack();
                if (stack != null && (stack.getType() == Material.BED || stack.getType() == Material.BED_BLOCK)) {
                    entity.remove();
                }
            }
        } catch (final Throwable ignored) {
        }
    }
}
