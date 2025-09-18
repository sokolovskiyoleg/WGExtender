package wgextender.features.claimcommand;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.task.RegionAdder;
import com.sk89q.worldguard.internal.permission.RegionPermissionModel;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.DomainInputResolver.UserLocatorPolicy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wgextender.utils.WEUtils;
import wgextender.utils.WGRegionUtils;

public class WGClaimCommand {

	protected static void claim(String id, CommandSender sender) throws CommandException {
		if (!(sender instanceof Player player)) {
			throw new CommandException("Эта команда только для игроков");
		}
		if (id.equalsIgnoreCase("__global__")) {
			throw new CommandException("§8[§c!§8] §7Нельзя заприватить регион, с названием __global__.");
		}
		if (!ProtectedRegion.isValidId(id) || id.startsWith("-")) {
			throw new CommandException("§8[§c!§8] §7Имя региона §c" + id + "§7 содержит запрещённые символы.");
		}

        BukkitWorldConfiguration wcfg = WGRegionUtils.getWorldConfig(player);

		if (wcfg.maxClaimVolume == Integer.MAX_VALUE) {
			throw new CommandException("The maximum claim volume get in the configuration is higher than is supported. " +
					"Currently, it must be " + Integer.MAX_VALUE + " or smaller. Please contact a server administrator.");
		}

		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		RegionPermissionModel permModel = new RegionPermissionModel(localPlayer);

		if (!permModel.mayClaim()) {
			throw new CommandPermissionsException();
		}

		RegionManager manager = WGRegionUtils.getRegionManager(player.getWorld());

		if (manager.hasRegion(id)) {
			throw new CommandException("§8[§c§l!§8] §7Регион с таким именем уже существует.");
		}

		ProtectedRegion region = createProtectedRegionFromSelection(player, id);

		if (!permModel.mayClaimRegionsUnbounded()) {
			int maxRegionCount = wcfg.getMaxRegionCount(localPlayer);
			if ((maxRegionCount >= 0) && (manager.getRegionCountOfPlayer(localPlayer) >= maxRegionCount)) {
				throw new CommandException("§8[§c§l!§8] §7У вас слишком много регионов, удалите один из них перед тем как заприватить новый.");
			}
			if (region.volume() > wcfg.maxClaimVolume) {
				throw new CommandException("Размер региона слишком большой. Максимальный размер: " + wcfg.maxClaimVolume + ", ваш размер: " + region.volume());
			}
		}

		ApplicableRegionSet regions = manager.getApplicableRegions(region);

		if (regions.size() > 0) {
			if (!regions.isOwnerOfAll(localPlayer)) {
				throw new CommandException("§8[§c§l!§8] §7Регион не создан. Он перекрывает чужой регион.");
			}
		} else if (wcfg.claimOnlyInsideExistingRegions) {
			throw new CommandException("Вы можете приватить только внутри своих регионов.");
		}

		RegionAdder task = new RegionAdder(manager, region);
		task.setLocatorPolicy(UserLocatorPolicy.UUID_ONLY);
		task.setOwnersInput(new String[] { player.getName() });
		try {
			task.call();
			sender.sendMessage(ChatColor.YELLOW + "§8[§a§l!§8] §7Регион §a"+id + "§7 создан.");
		} catch (Exception e) {
			sender.sendMessage(ChatColor.YELLOW + "Произошла ошибка при привате региона "+id);
			e.printStackTrace();
		}
	}

	private static ProtectedRegion createProtectedRegionFromSelection(Player player, String id) throws CommandException {
		try {
			Region selection = WEUtils.getSelection(player);
			if (selection instanceof CuboidRegion) {
				return new ProtectedCuboidRegion(id, selection.getMinimumPoint(), selection.getMaximumPoint());
			} else {
				throw new CommandException("Вы можете использовать только кубическкую территорию.");
			}
		} catch (IncompleteRegionException e) {
			throw new CommandException("Сначала выделите территорию. " + "Используйте WorldEdit для выделения " + "(wiki: http://wiki.sk89q.com/wiki/WorldEdit).");
		}

	}

}
