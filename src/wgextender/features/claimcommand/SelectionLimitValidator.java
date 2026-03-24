package wgextender.features.claimcommand;

import org.bukkit.entity.Player;
import wgextender.Config;
import wgextender.WGExtender;
import wgextender.utils.WEUtils;

import java.util.function.BooleanSupplier;
import java.util.logging.Level;

public class SelectionLimitValidator {

    private final Config config;
    private final BlockLimits blockLimits = new BlockLimits();

    public SelectionLimitValidator(Config config) {
        this.config = config;
    }

    public boolean validateClaim(Player player) {
        BlockLimits.ProcessedClaimInfo info = blockLimits.processClaimInfo(config, player);
        if (info.result() == BlockLimits.Result.ALLOW) {
            return true;
        }
        sendLimitDenied(player, info);
        return false;
    }

    public void validateAndResetSelection(Player player) {
        validateAndResetSelection(player, () -> WEUtils.clearSelection(player));
    }

    public boolean validateAndResetSelection(Player player, BooleanSupplier resetAction) {
        if (!isEnabled()) {
            return false;
        }
        BlockLimits.ProcessedClaimInfo info = blockLimits.processClaimInfo(config, player);
        if (info.result() == BlockLimits.Result.ALLOW) {
            return false;
        }
        sendLimitDenied(player, info);
        if (!resetAction.getAsBoolean()) {
            WGExtender.getInstance().getLogger().log(
                    Level.WARNING,
                    "Unable to clear oversized WorldEdit selection for player {0}",
                    player.getName()
            );
        }
        return true;
    }

    private boolean isEnabled() {
        return config.claimBlockLimitsEnabled && config.claimResetOversizedSelection;
    }

    private void sendLimitDenied(Player player, BlockLimits.ProcessedClaimInfo info) {
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
