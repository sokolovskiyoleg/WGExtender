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

package wgextender.utils;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.Optional;

public class WEUtils {

	public static WorldEditPlugin getWorldEditPlugin() {
		return JavaPlugin.getPlugin(WorldEditPlugin.class);
	}

    public static Region getSelection(Player player) throws IncompleteRegionException {
		return getWorldEditPlugin().getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
	}

	public static boolean expandVert(Player player) {
		LocalSession session = getWorldEditPlugin().getSession(player);
		com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(player.getWorld());
        try {
			Region region = session.getSelection(weWorld);
			region.expand(
					BlockVector3.at(0, (weWorld.getMaxY() + 1), 0),
					BlockVector3.at(0, -(weWorld.getMaxY() + 1), 0)
			);
            session.getRegionSelector(weWorld).learnChanges();
            return true;
		} catch (Throwable ignored) { }
        return false;
	}

    public static boolean clearSelection(Player player) {
        LocalSession session = getWorldEditPlugin().getSession(player);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(player.getWorld());
        try {
            session.getRegionSelector(weWorld).clear();
            session.getRegionSelector(weWorld).learnChanges();
            return true;
        } catch (Throwable ignored) { }
        return false;
    }

    public static Material getWandMaterial() {
        String wandItem = getWorldEditPlugin().getLocalConfiguration().wandItem;
        String materialName = wandItem.toUpperCase();
        String[] parts = materialName.split(":", 2);
        return Material.getMaterial(parts.length == 2 ? parts[1] : parts[0]);
    }

    public static Optional<RegionSelector> copyRegionSelector(RegionSelector selector) {
        try {
            return Optional.of(createSelectorCopy(selector));
        } catch (ReflectiveOperationException ex) {
            return Optional.empty();
        }
    }

    private static RegionSelector createSelectorCopy(RegionSelector selector) throws ReflectiveOperationException {
        Class<? extends RegionSelector> selectorClass = selector.getClass();

        try {
            Constructor<? extends RegionSelector> copyConstructor = selectorClass.getConstructor(RegionSelector.class);
            return copyConstructor.newInstance(selector);
        } catch (NoSuchMethodException ignored) { }

        if (selector instanceof CuboidRegionSelector) {
            return new CuboidRegionSelector(selector);
        }

        throw new NoSuchMethodException(selectorClass.getName());
    }

}
