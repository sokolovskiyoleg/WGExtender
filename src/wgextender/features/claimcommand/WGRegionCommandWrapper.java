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

package wgextender.features.claimcommand;

import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wgextender.Config;
import wgextender.utils.CommandUtils;
import wgextender.utils.WEUtils;
import wgextender.utils.WGRegionUtils;

public class WGRegionCommandWrapper extends Command {

    private final WGClaimCommand wgClaimCommand;

    public static void inject(Config config) {
        WGRegionCommandWrapper wrapper = new WGRegionCommandWrapper(config, CommandUtils.getCommands().get("region"));
        CommandUtils.replaceCommand(wrapper.originalCmd, wrapper);
    }

    public static void uninject() {
        WGRegionCommandWrapper wrapper = (WGRegionCommandWrapper) CommandUtils.getCommands().get("region");
        CommandUtils.replaceCommand(wrapper, wrapper.originalCmd);
    }

    protected final Config config;
    protected final Command originalCmd;

    protected WGRegionCommandWrapper(Config config, Command originalCmd) {
        super(originalCmd.getName(), originalCmd.getDescription(), originalCmd.getUsage(), originalCmd.getAliases());
        this.config = config;
        this.originalCmd = originalCmd;
        this.wgClaimCommand = new WGClaimCommand(config);
    }

    private final BlockLimits blockLimits = new BlockLimits();

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if ((sender instanceof Player player) && (args.length >= 2) && args[0].equalsIgnoreCase("claim")) {
            String regionName = args[1];
            if (config.claimExpandSelectionVertical) {
                boolean result = WEUtils.expandVert((Player) sender);
                if (result) {
                    player.sendMessage(config.getMessages().regionExpandedVertically);
                }
            }
            if (!process(player)) {
                return true;
            }
            boolean hasRegion = AutoFlags.hasRegion(player.getWorld(), regionName);
            try {
                wgClaimCommand.claim(regionName, sender);
                if (!hasRegion && config.claimAutoFlagsEnabled) {
                    AutoFlags.setFlagsForRegion(WGRegionUtils.wrapAsPrivileged(player, config.showAutoFlagMessages), player.getWorld(), config, regionName);
                }
            } catch (CommandException ex) {
                sender.sendMessage(ex.getMessage());
            }
            return true;
        } else {
            return originalCmd.execute(sender, label, args);
        }
    }

    private boolean process(Player player) {
        BlockLimits.ProcessedClaimInfo info = blockLimits.processClaimInfo(config, player);
        return switch (info.result()) {
            case ALLOW -> true;
            case DENY_MAX_VOLUME -> {
                sendClaimDenied(player, config.getMessages().claimTooLarge, config.getMessages().claimYourLimit, info);
                yield false;
            }
            case DENY_MIN_VOLUME -> {
                sendClaimDenied(player, config.getMessages().claimTooSmall, config.getMessages().claimMinVolume, info);
                yield false;
            }
            case DENY_HORIZONTAL -> {
                sendClaimDenied(player, config.getMessages().claimTooNarrow, config.getMessages().claimMinWidth, info);
                yield false;
            }
            case DENY_VERTICAL -> {
                sendClaimDenied(player, config.getMessages().claimTooLow, config.getMessages().claimMinHeight, info);
                yield false;
            }
        };
    }

    private static void sendClaimDenied(
            Player player,
            String titleMessage,
            String detailsMessage,
            BlockLimits.ProcessedClaimInfo info
    ) {
        String limit = info.assignedLimit().toString();
        String size = info.assignedSize().toString();
        player.sendMessage(titleMessage);
        player.sendMessage(detailsMessage
                .replace("%limit%", limit)
                .replace("%size%", size));
    }
}
