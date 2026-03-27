package wgextender.features.claimcommand;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.RegionSelector;
import org.bukkit.entity.Player;
import wgextender.Config;
import wgextender.WGExtender;
import wgextender.utils.WEUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class SelectionLimitTracker {

    private final Config config;
    private final SelectionLimitValidator selectionLimitValidator;
    private final Map<SelectionKey, SelectionCheckpoint> checkpoints = new HashMap<>();
    private final Set<UUID> scheduledPlayers = new HashSet<>();

    public SelectionLimitTracker(Config config) {
        this.config = config;
        this.selectionLimitValidator = new SelectionLimitValidator(config);
    }

    public void scheduleValidation(Player player) {
        if (!isEnabled() || !scheduledPlayers.add(player.getUniqueId())) {
            return;
        }

        org.bukkit.Bukkit.getScheduler().runTask(WGExtender.getInstance(), () -> {
            try {
                validateSelection(player);
            } finally {
                scheduledPlayers.remove(player.getUniqueId());
            }
        });
    }

    public void clear(Player player) {
        UUID playerId = player.getUniqueId();
        scheduledPlayers.remove(playerId);
        checkpoints.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private void validateSelection(Player player) {
        if (!isEnabled() || !player.isOnline()) {
            return;
        }

        LocalSession session = WEUtils.getWorldEditPlugin().getSession(player);
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(player.getWorld());
        RegionSelector selector = session.getRegionSelector(world);
        SelectionKey key = new SelectionKey(player.getUniqueId(), player.getWorld().getUID());

        if (!selector.isDefined()) {
            checkpoints.remove(key);
            return;
        }

        Optional<SelectionCheckpoint> currentCheckpoint = SelectionCheckpoint.capture(world, selector);
        BlockLimits.ProcessedClaimInfo info = selectionLimitValidator.getSelectionInfo(player);
        if (info.result() == BlockLimits.Result.ALLOW) {
            currentCheckpoint.ifPresentOrElse(
                    checkpoint -> checkpoints.put(key, checkpoint),
                    () -> checkpoints.remove(key)
            );
            return;
        }

        selectionLimitValidator.sendLimitDenied(player, info);

        if (currentCheckpoint.isEmpty()) {
            checkpoints.remove(key);
            return;
        }

        SelectionCheckpoint previousCheckpoint = checkpoints.get(key);
        if (previousCheckpoint == null) {
            return;
        }

        if (!previousCheckpoint.restore(session)) {
            WGExtender.getInstance().getLogger().log(
                    Level.WARNING,
                    "Unable to restore oversized WorldEdit selection for player {0}",
                    player.getName()
            );
            return;
        }

        session.dispatchCUISelection(BukkitAdapter.adapt(player));
    }

    private boolean isEnabled() {
        return config.claimBlockLimitsEnabled && config.claimResetOversizedSelection;
    }

    private record SelectionKey(UUID playerId, UUID worldId) {
    }
}
