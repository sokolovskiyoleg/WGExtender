/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package wgextender.commands;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import wgextender.Config;
import wgextender.features.claimcommand.AutoFlags;
import wgextender.utils.Transform;
import wgextender.utils.WEUtils;
import wgextender.utils.WGRegionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.bukkit.util.StringUtil.copyPartialMatches;

//TODO: refactor
public class Commands implements CommandExecutor, TabCompleter {

	protected final Config config;

	public Commands(Config config) {
		this.config = config;
	}

	private static List<String> getRegionsInPlayerSelection(Player player) throws IncompleteRegionException {
		Region psel = WEUtils.getSelection(player);
		ProtectedRegion fakerg = new ProtectedCuboidRegion("wgexfakerg", psel.getMaximumPoint(), psel.getMinimumPoint());
		ApplicableRegionSet ars = WGRegionUtils.getRegionManager(player.getWorld()).getApplicableRegions(fakerg);
		return StreamSupport.stream(ars.spliterator(), false)
			.map(ProtectedRegion::getId)
			.collect(Collectors.toList());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command arg1, String label, String[] args) {
		if (!sender.hasPermission("wgextender.admin")) {
			sender.sendMessage(config.getMessages().adminNoPermission);
			return true;
		}
		if (args.length >= 1) {
			switch (args[0].toLowerCase()) {
				case "help" -> {
					sender.sendMessage(config.getMessages().adminHelpReload);
					sender.sendMessage(config.getMessages().adminHelpSearch);
					sender.sendMessage(config.getMessages().adminHelpSetFlag);
					sender.sendMessage(config.getMessages().adminHelpRemoveOwner);
					sender.sendMessage(config.getMessages().adminHelpRemoveMember);
					return true;
				}
				case "reload" -> {
					config.loadConfig();
					sender.sendMessage(config.getMessages().adminConfigReloaded);
					return true;
				}
				case "search" -> {
					if (sender instanceof Player player) {
						try {
							List<String> regions = getRegionsInPlayerSelection(player);
							if (regions.isEmpty()) {
								sender.sendMessage(config.getMessages().adminSearchNone);
							} else {
								sender.sendMessage(config.getMessages().adminSearchFound.replace("%regions%", regions.toString()));
							}
						} catch (IncompleteRegionException e) {
							sender.sendMessage(config.getMessages().adminSearchNoSelection);
						}
						return true;
					}
					return false;
				}
				case "setflag" -> {
					if (args.length < 4) {
						return false;
					}
					World world = Bukkit.getWorld(args[1]);
					if (world == null) {
						sender.sendMessage(config.getMessages().adminWorldNotFound);
						return true;
					}
					Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), args[2]);
					if (flag == null) {
						sender.sendMessage(config.getMessages().adminFlagNotFound);
						return true;
					}
					try {
						String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
						for (ProtectedRegion region : WGRegionUtils.getRegionManager(world).getRegions().values()) {
							if (region instanceof GlobalProtectedRegion) {
								continue;
							}
							AutoFlags.setFlag(WGRegionUtils.wrapAsPrivileged(sender, false), world, region, flag, value);
						}
						sender.sendMessage(config.getMessages().adminFlagsSet);
					} catch (CommandException e) {
						sender.sendMessage(config.getMessages().adminFlagInvalidFormat
								.replace("%flag%", flag.getName())
								.replace("%error%", e.getMessage()));
					}
					return true;
				}
				case "removeowner", "removemember" -> {
					if (args.length != 2) {
						return false;
					}
					boolean owner = args[0].equals("removeowner");
					OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(args[1]);
					String name = (offPlayer.getName() == null ? args[1] : offPlayer.getName()).toLowerCase();
					UUID uuid = offPlayer.getUniqueId();
					for (RegionManager manager : WGRegionUtils.getRegionContainer().getLoaded()) {
						for (ProtectedRegion region : manager.getRegions().values()) {
							DefaultDomain members = owner ? region.getOwners() : region.getMembers();
							members.removePlayer(uuid);
							members.removePlayer(name);
							region.setMembers(members);
						}
					}
					String group = owner
							? config.getMessages().adminRegionGroupOwners
							: config.getMessages().adminRegionGroupMembers;
					sender.sendMessage(config.getMessages().adminPlayerRemovedFromAllRegions.replace("%group%", group));
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0 || !sender.hasPermission("wgextender.admin")) {
			return Collections.emptyList();
		}
		if (args.length == 1) {
			return copyPartialMatches(
					args[0],
					sender instanceof Player
							? List.of("help", "reload", "search", "setflag", "removeowner", "removemember")
							: List.of("help", "reload", "setflag", "removeowner", "removemember"),
					new ArrayList<>()
			);
		}
		if (!"setflag".equalsIgnoreCase(args[0])) return Collections.emptyList();
		switch (args.length) {
			case 2 -> {
				return copyPartialMatches(args[1], Transform.toList(Bukkit.getWorlds(), World::getName), new ArrayList<>());
			}
			case 3 -> {
				return copyPartialMatches(args[2], Transform.toList(WorldGuard.getInstance().getFlagRegistry(), Flag::getName), new ArrayList<>());
			}
			case 4 -> {
				Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), args[2]);
				if (flag instanceof StateFlag) {
					return copyPartialMatches(args[3], Transform.toList(State.values(), State::toString), new ArrayList<>());
				}
				if (flag instanceof BooleanFlag) {
					return copyPartialMatches(args[3], List.of("true", "false"), new ArrayList<>());
				}
				if (flag instanceof EnumFlag<?> enumFlag) {
					try {
						return copyPartialMatches(args[3], Transform.toList(enumFlag.getEnumClass().getEnumConstants(), Enum::toString), new ArrayList<>());
					} catch (Exception ignored) { }
				}
			}
		}
		return Collections.emptyList();
	}

}
