package wgextender.features.claimcommand;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import org.bukkit.entity.Player;
import wgextender.Config;
import wgextender.utils.WEUtils;

import java.util.List;

public class SelectionLimitRegionSelector implements RegionSelector {

    private final Config config;
    private final SelectionLimitValidator selectionLimitValidator;
    private final RegionSelector delegate;

    public SelectionLimitRegionSelector(Config config, RegionSelector delegate) {
        this.config = config;
        this.selectionLimitValidator = new SelectionLimitValidator(config);
        this.delegate = delegate;
    }

    public RegionSelector unwrap() {
        return delegate;
    }

    @Override
    public com.sk89q.worldedit.world.World getWorld() {
        return delegate.getWorld();
    }

    @Override
    public void setWorld(com.sk89q.worldedit.world.World world) {
        delegate.setWorld(world);
    }

    @Override
    public boolean selectPrimary(BlockVector3 position, SelectorLimits limits) {
        return delegate.selectPrimary(position, limits);
    }

    @Override
    public boolean selectSecondary(BlockVector3 position, SelectorLimits limits) {
        return delegate.selectSecondary(position, limits);
    }

    @Override
    public void explainPrimarySelection(Actor actor, LocalSession session, BlockVector3 position) {
        if (handleOversizedSelection(actor, session)) {
            return;
        }
        delegate.explainPrimarySelection(actor, session, position);
    }

    @Override
    public void explainSecondarySelection(Actor actor, LocalSession session, BlockVector3 position) {
        if (handleOversizedSelection(actor, session)) {
            return;
        }
        delegate.explainSecondarySelection(actor, session, position);
    }

    @Override
    public void explainRegionAdjust(Actor actor, LocalSession session) {
        if (handleOversizedSelection(actor, session)) {
            return;
        }
        delegate.explainRegionAdjust(actor, session);
    }

    @Override
    public BlockVector3 getPrimaryPosition() throws IncompleteRegionException {
        return delegate.getPrimaryPosition();
    }

    @Override
    public Region getRegion() throws IncompleteRegionException {
        return delegate.getRegion();
    }

    @Override
    public Region getIncompleteRegion() {
        return delegate.getIncompleteRegion();
    }

    @Override
    public boolean isDefined() {
        return delegate.isDefined();
    }

    @Override
    public void learnChanges() {
        delegate.learnChanges();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public String getTypeName() {
        return delegate.getTypeName();
    }

    @Override
    public List<String> getInformationLines() {
        return delegate.getInformationLines();
    }

    @Override
    public List<com.sk89q.worldedit.util.formatting.text.Component> getSelectionInfoLines() {
        return delegate.getSelectionInfoLines();
    }

    private boolean handleOversizedSelection(Actor actor, LocalSession session) {
        if (!config.claimBlockLimitsEnabled || !config.claimResetOversizedSelection) {
            return false;
        }
        if (!(actor instanceof com.sk89q.worldedit.entity.Player worldEditPlayer)) {
            return false;
        }
        Player player = BukkitAdapter.adapt(worldEditPlayer);
        if (player == null) {
            return false;
        }
        return selectionLimitValidator.validateAndResetSelection(player, () -> {
            boolean reset = WEUtils.resetSelectionLimitSelector(session, getWorld(), config);
            if (reset) {
                session.dispatchCUISelection(actor);
            }
            return reset;
        });
    }
}
