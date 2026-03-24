package wgextender.features.claimcommand;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import wgextender.Config;
import wgextender.utils.WEUtils;

import java.util.Locale;
import java.util.Set;

public class SelectionLimitListener implements Listener {

    private static final Set<String> SELECTION_COMMANDS = Set.of(
            "pos1",
            "pos2",
            "hpos1",
            "hpos2",
            "chunk",
            "expand",
            "contract",
            "shift",
            "outset",
            "inset",
            "sel"
    );

    private final Config config;

    public SelectionLimitListener(Config config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSelectionWandUse(PlayerInteractEvent event) {
        if (!isEnabled()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isSelectionWand(event.getPlayer())) {
            return;
        }
        WEUtils.ensureSelectionLimitSelector(event.getPlayer(), config);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSelectionCommand(PlayerCommandPreprocessEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (!isSelectionCommand(event.getMessage())) {
            return;
        }
        Player player = event.getPlayer();
        WEUtils.ensureSelectionLimitSelector(player, config);
        if (isSelectorReplacingCommand(event.getMessage())) {
            org.bukkit.Bukkit.getScheduler().runTask(
                    wgextender.WGExtender.getInstance(),
                    () -> WEUtils.ensureSelectionLimitSelector(player, config)
            );
        }
    }

    private boolean isEnabled() {
        return config.claimBlockLimitsEnabled && config.claimResetOversizedSelection;
    }

    private static boolean isSelectionWand(Player player) {
        return player.getInventory().getItemInMainHand().getType() == WEUtils.getWandMaterial();
    }

    private static boolean isSelectionCommand(String rawMessage) {
        return SELECTION_COMMANDS.contains(resolveCommand(rawMessage));
    }

    private static boolean isSelectorReplacingCommand(String rawMessage) {
        return resolveCommand(rawMessage).equals("sel");
    }

    private static String resolveCommand(String rawMessage) {
        if (rawMessage == null || rawMessage.length() <= 1) {
            return "";
        }
        String normalized = rawMessage.substring(1).trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return "";
        }
        int spaceIndex = normalized.indexOf(' ');
        String command = spaceIndex >= 0 ? normalized.substring(0, spaceIndex) : normalized;
        int namespaceIndex = command.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex < command.length() - 1) {
            command = command.substring(namespaceIndex + 1);
        }
        return command;
    }
}
