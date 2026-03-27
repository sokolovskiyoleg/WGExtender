package wgextender.features.claimcommand;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.world.World;
import wgextender.utils.WEUtils;

import java.util.Objects;
import java.util.Optional;

public final class SelectionCheckpoint {

    private final World world;
    private final RegionSelector selector;

    private SelectionCheckpoint(World world, RegionSelector selector) {
        this.world = world;
        this.selector = selector;
    }

    public static Optional<SelectionCheckpoint> capture(World world, RegionSelector selector) {
        if (world == null || selector == null || !selector.isDefined()) {
            return Optional.empty();
        }

        return WEUtils.copyRegionSelector(selector)
                .map(selectorCopy -> new SelectionCheckpoint(world, selectorCopy));
    }

    public boolean restore(LocalSession session) {
        Objects.requireNonNull(session, "session");

        Optional<RegionSelector> selectorCopy = WEUtils.copyRegionSelector(selector);
        if (selectorCopy.isEmpty()) {
            return false;
        }

        session.setRegionSelector(world, selectorCopy.get());
        session.getRegionSelector(world).learnChanges();
        return true;
    }

    public World world() {
        return world;
    }
}
