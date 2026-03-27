package wgextender.features.claimcommand;

import org.bukkit.entity.Player;
import wgextender.Config;

public class SelectionLimitValidator {

    private final Config config;
    private final BlockLimits blockLimits = new BlockLimits();

    public SelectionLimitValidator(Config config) {
        this.config = config;
    }

    public boolean validateClaim(Player player) {
        BlockLimits.ProcessedClaimInfo info = getSelectionInfo(player);
        if (info.result() == BlockLimits.Result.ALLOW) {
            return true;
        }
        sendLimitDenied(player, info);
        return false;
    }

    public BlockLimits.ProcessedClaimInfo getSelectionInfo(Player player) {
        return blockLimits.processClaimInfo(config, player);
    }

    public void sendLimitDenied(Player player, BlockLimits.ProcessedClaimInfo info) {
        String titleMessage;
        String detailsMessage;
        switch (info.result()) {
            case DENY_MAX_VOLUME -> {
                titleMessage = config.getMessages().claimTooLarge;
                detailsMessage = config.getMessages().claimYourLimit;
            }
            case DENY_MIN_VOLUME -> {
                titleMessage = config.getMessages().claimTooSmall;
                detailsMessage = config.getMessages().claimMinVolume;
            }
            case DENY_HORIZONTAL -> {
                titleMessage = config.getMessages().claimTooNarrow;
                detailsMessage = config.getMessages().claimMinWidth;
            }
            case DENY_VERTICAL -> {
                titleMessage = config.getMessages().claimTooLow;
                detailsMessage = config.getMessages().claimMinHeight;
            }
            default -> {
                return;
            }
        }

        player.sendMessage(titleMessage);
        player.sendMessage(detailsMessage
                .replace("%limit%", info.assignedLimit().toString())
                .replace("%size%", info.assignedSize().toString()));
    }
}
