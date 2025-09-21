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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wgextender.Config;
import wgextender.utils.WEUtils;
import wgextender.utils.WGRegionUtils;

public class WGClaimCommand {

    private final Config config;

    public WGClaimCommand(Config config) {
        this.config = config;
    }

    protected void claim(String id, CommandSender sender) throws CommandException {
        if (!(sender instanceof Player player)) {
            throw new CommandException(config.getMessages().playerOnlyCommand);
        }
        if (id.equalsIgnoreCase("__global__")) {
            throw new CommandException(config.getMessages().globalRegionNameForbidden);
        }
        if (!ProtectedRegion.isValidId(id) || id.startsWith("-")) {
            throw new CommandException(config.getMessages().invalidRegionName.replace("%id%", id));
        }

        BukkitWorldConfiguration wcfg = WGRegionUtils.getWorldConfig(player);

        if (wcfg.maxClaimVolume == Integer.MAX_VALUE) {
            throw new CommandException(config.getMessages().maxClaimVolumeError);
        }

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionPermissionModel permModel = new RegionPermissionModel(localPlayer);

        if (!permModel.mayClaim()) {
            throw new CommandPermissionsException();
        }

        RegionManager manager = WGRegionUtils.getRegionManager(player.getWorld());

        if (manager.hasRegion(id)) {
            throw new CommandException(config.getMessages().regionAlreadyExists);
        }

        ProtectedRegion region = createProtectedRegionFromSelection(player, id);

        if (!permModel.mayClaimRegionsUnbounded()) {
            int maxRegionCount = wcfg.getMaxRegionCount(localPlayer);
            if ((maxRegionCount >= 0) && (manager.getRegionCountOfPlayer(localPlayer) >= maxRegionCount)) {
                throw new CommandException(config.getMessages().tooManyRegions);
            }
            if (region.volume() > wcfg.maxClaimVolume) {
                throw new CommandException(config.getMessages().regionTooLarge
                        .replace("%max%", String.valueOf(wcfg.maxClaimVolume))
                        .replace("%current%", String.valueOf(region.volume())));
            }
        }

        ApplicableRegionSet regions = manager.getApplicableRegions(region);

        if (regions.size() > 0) {
            if (!regions.isOwnerOfAll(localPlayer)) {
                throw new CommandException(config.getMessages().overlappingOthersRegion);
            }
        } else if (wcfg.claimOnlyInsideExistingRegions) {
            throw new CommandException(config.getMessages().claimOnlyInsideExisting);
        }

        RegionAdder task = new RegionAdder(manager, region);
        task.setLocatorPolicy(UserLocatorPolicy.UUID_ONLY);
        task.setOwnersInput(new String[] { player.getName() });
        try {
            task.call();
            sender.sendMessage(config.getMessages().regionCreated.replace("%id%", id));
        } catch (Exception e) {
            sender.sendMessage(config.getMessages().regionCreationError.replace("%id%", id));
            e.printStackTrace();
        }
    }

    private ProtectedRegion createProtectedRegionFromSelection(Player player, String id) throws CommandException {
        try {
            Region selection = WEUtils.getSelection(player);
            if (selection instanceof CuboidRegion) {
                return new ProtectedCuboidRegion(id, selection.getMinimumPoint(), selection.getMaximumPoint());
            } else {
                throw new CommandException(config.getMessages().onlyCuboidSelectionAllowed);
            }
        } catch (IncompleteRegionException e) {
            throw new CommandException(config.getMessages().noSelectionMade);
        }
    }
}