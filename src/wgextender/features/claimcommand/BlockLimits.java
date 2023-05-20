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

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;
import wgextender.Config;
import wgextender.VaultIntegration;
import wgextender.utils.WEUtils;

import java.math.BigInteger;

public class BlockLimits {
	private static final BigInteger MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
	public static final BigInteger RESTRICTED = BigInteger.valueOf(-1);

	public ProcessedClaimInfo processClaimInfo(Config config, Player player) {
		ProcessedClaimInfo info = new ProcessedClaimInfo();
		Region psel;
		try {
			psel = WEUtils.getSelection(player);
		} catch (IncompleteRegionException e) {
			return info;
		}
		BlockVector3 min = psel.getMinimumPoint();
		BlockVector3 max = psel.getMaximumPoint();
		BigInteger size = BigInteger.ONE;
		size = size.multiply(distance(min.getBlockX(), max.getBlockX()));
		size = size.multiply(distance(min.getBlockY(), max.getBlockY()));
		size = size.multiply(distance(min.getBlockZ(), max.getBlockZ()));
		if (size.compareTo(MAX_VALUE) > 0) {
			info.disallow();
			info.setInfo(size, RESTRICTED);
			return info;
		}
		if (config.claimBlockLimitsEnabled) {
			if (player.hasPermission("worldguard.region.unlimited")) {
				return info;
			}
			String[] pgroups = VaultIntegration.getInstance().getPermissions().getPlayerGroups(player);
			if (pgroups.length == 0) {
				return info;
			}
			int maxBlocks = 0;
			for (String pgroup : pgroups) {
				pgroup = pgroup.toLowerCase();
				if (config.claimBlockLimits.containsKey(pgroup)) {
					maxBlocks = Math.max(maxBlocks, config.claimBlockLimits.get(pgroup));
				}
			}
			BigInteger maxBlocksi = BigInteger.valueOf(maxBlocks);
			if (size.compareTo(maxBlocksi) > 0) {
				info.disallow();
				info.setInfo(size, maxBlocksi);
				return info;
			}
		}
		return info;
	}

	protected static class ProcessedClaimInfo {

		private boolean claimAllowed = true;
		private BigInteger size;
		private BigInteger maxSize;

		public void disallow() {
			claimAllowed = false;
		}

		public boolean isClaimAllowed() {
			return claimAllowed;
		}

		public void setInfo(BigInteger claimed, BigInteger max) {
			size = claimed;
			maxSize = max;
		}

		public BigInteger getClaimedSize() {
			return size;
		}

		public BigInteger getMaxSize() {
			return maxSize;
		}

	}

	private static BigInteger distance(long min, long max) {
		return BigInteger.valueOf(max - min + 1L);
	}
}
