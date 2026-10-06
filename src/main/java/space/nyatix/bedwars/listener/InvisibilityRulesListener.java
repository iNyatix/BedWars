package space.nyatix.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import space.nyatix.bedwars.BedWarsPlugin;
import space.nyatix.bedwars.model.GameState;

import java.lang.reflect.Method;
import java.util.Locale;

public class InvisibilityRulesListener implements Listener {

    private final BedWarsPlugin plugin;

    public InvisibilityRulesListener(final BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void revealInvisibleVictimOnPlayerDamage(final EntityDamageByEntityEvent event) {
        if (this.plugin.game() == null || this.plugin.game().state() != GameState.RUNNING) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        final Player victim = (Player) event.getEntity();
        if (this.plugin.game().teamOf(victim.getUniqueId()) == null || this.plugin.game().isSpectator(victim.getUniqueId())) {
            return;
        }
        if (!victim.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return;
        }
        if (isPlayerDamage(event.getDamager())) {
            this.plugin.invisibility().reveal(victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void consume(final PlayerItemConsumeEvent event) {
        if (!isInvisibilityPotion(event.getItem())) {
            return;
        }
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            removeOneGlassBottle(player);
        });
    }

    private boolean isPlayerDamage(final Entity damager) {
        if (damager instanceof Player) {
            return true;
        }
        if (damager == null) {
            return false;
        }
        try {
            final Method getShooter = damager.getClass().getMethod("getShooter");
            final Object shooter = getShooter.invoke(damager);
            return shooter instanceof Player;
        } catch (final Throwable ignored) {
            return false;
        }
    }

    private boolean isInvisibilityPotion(final ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        final ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        final String name = meta.getDisplayName();
        if (name == null) {
            return false;
        }
        final String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("niewidzialnosci") || lower.contains("invisibility");
    }

    private void removeOneGlassBottle(final Player player) {
        if (player == null) {
            return;
        }
        final PlayerInventory inventory = player.getInventory();
        final ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            return;
        }
        for (int i = 0; i < contents.length; i++) {
            final ItemStack stack = contents[i];
            if (stack != null && stack.getType() != null && "GLASS_BOTTLE".equals(stack.getType().name())) {
                inventory.setItem(i, null);
                player.updateInventory();
                return;
            }
        }
    }
}
