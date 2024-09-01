package wgextender.features.flags;

import com.google.common.base.Function;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import wgextender.WGExtender;
import wgextender.utils.WGRegionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public class OldPVPFlagsHandler implements Listener {
	@SuppressWarnings("deprecation")
	private static final Set<EntityDamageEvent.DamageModifier> PVP_MODIFIERS = EnumSet.of(
			DamageModifier.ARMOR, DamageModifier.RESISTANCE, DamageModifier.MAGIC, DamageModifier.ABSORPTION
	);
	private final Map<UUID, Double> oldValues = new HashMap<>();
	private Field functionsField;

	public void start(Plugin plugin) {
        try {
            functionsField = EntityDamageEvent.class.getDeclaredField("modifierFunctions");
			functionsField.setAccessible(true);
        } catch (Exception ex) {
            plugin.getLogger().log(
					Level.SEVERE,
					"Couldn't get access to 'modifierFunctions' field. Old PvP flags will not be enabled",
					ex
			);
			return;
        }

		Server server = plugin.getServer();
		server.getPluginManager().registerEvents(this, plugin);
		server.getScheduler().runTaskTimer(WGExtender.getInstance(), () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (WGRegionUtils.isFlagTrue(player.getLocation(), WGExtenderFlags.OLDPVP_ATTACKSPEED)) {
					if (!oldValues.containsKey(player.getUniqueId())) {
						AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
						oldValues.put(player.getUniqueId(), attribute.getBaseValue());
						attribute.setBaseValue(16.0);
					}
				} else {
					reset(player);
				}
			}
		}, 0, 1);
	}

	public void stop() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			reset(player);
		}
	}

	private void reset(Player player) {
		Double oldValue = oldValues.remove(player.getUniqueId());
		if (oldValue != null) {
			player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(oldValue);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent event) {
		reset(event.getPlayer());
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player player) ||
				!player.isBlocking() ||
				!WGRegionUtils.isFlagTrue(entity.getLocation(), WGExtenderFlags.OLDPVP_NOSHIELDBLOCK)) {
			return;
		}
		Map<DamageModifier, Function<Double, Double>> func;
		try {
			func = (Map<DamageModifier, Function<Double, Double>>) functionsField.get(event);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			WGExtender.getInstance().getLogger().log(Level.SEVERE, "Unable to recalculate blocking damage", e);
			return;
		}
		double totalDamage = event.getDamage() + event.getDamage(DamageModifier.HARD_HAT);
		//reset blocking modifier
		event.setDamage(DamageModifier.BLOCKING, 0);
		//recalculate other modifiers
		for (var modifier : PVP_MODIFIERS) {
			double damage = func.get(modifier).apply(totalDamage);
			event.setDamage(modifier, damage);
			totalDamage += damage;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent event) {
		if (isRightClick(event.getAction()) &&
				event.getHand() == EquipmentSlot.OFF_HAND &&
				event.getItem() != null &&
				event.getItem().getType() == Material.BOW &&
				WGRegionUtils.isFlagTrue(event.getPlayer().getLocation(), WGExtenderFlags.OLDPVP_NOBOW)) {
			event.setCancelled(true);
		}
	}

	private static boolean isRightClick(Action action) {
		return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
	}
}
