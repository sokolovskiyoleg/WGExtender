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

package wgextender;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import wgextender.commands.Commands;
import wgextender.features.claimcommand.SelectionLimitListener;
import wgextender.features.claimcommand.WGRegionCommandWrapper;
import wgextender.features.extendedwand.WEWandCommandWrapper;
import wgextender.features.extendedwand.WEWandListener;
import wgextender.features.flags.ChorusFruitFlagHandler;
import wgextender.features.flags.OldPVPFlagsHandler;
import wgextender.features.flags.WGExtenderFlags;
import wgextender.features.regionprotect.ownormembased.PvPHandlingListener;
import wgextender.features.regionprotect.ownormembased.RestrictCommands;
import wgextender.features.regionprotect.regionbased.BlockBurn;
import wgextender.features.regionprotect.regionbased.Explode;
import wgextender.features.regionprotect.regionbased.FireSpread;
import wgextender.features.regionprotect.regionbased.LiquidFlow;

import java.util.Objects;
import java.util.logging.Level;

public class WGExtender extends JavaPlugin {

	private static WGExtender instance;
	public static WGExtender getInstance() {
		return instance;
	}

	public WGExtender() {
		instance = this;
	}

	private PvPHandlingListener pvplistener;
	private OldPVPFlagsHandler oldpvphandler;
	private Config pluginConfig;

	@Override
	public void onLoad() {
		WGExtenderFlags.registerFlags(getLogger());
	}

	@Override
	public void onEnable() {
		VaultIntegration.getInstance().initialize(this);
		pluginConfig = new Config(this);
		pluginConfig.loadConfig();
		Objects.requireNonNull(getCommand("wgex")).setExecutor(new Commands(pluginConfig));
		PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(new RestrictCommands(pluginConfig), this);
		pluginManager.registerEvents(new LiquidFlow(pluginConfig), this);
		pluginManager.registerEvents(new FireSpread(pluginConfig), this);
		pluginManager.registerEvents(new BlockBurn(pluginConfig), this);
		pluginManager.registerEvents(new Explode(pluginConfig), this);
		pluginManager.registerEvents(new WEWandListener(), this);
		pluginManager.registerEvents(new ChorusFruitFlagHandler(pluginConfig), this);
		pluginManager.registerEvents(new SelectionLimitListener(pluginConfig), this);
		try {
			WGRegionCommandWrapper.inject(pluginConfig);
			WEWandCommandWrapper.inject(pluginConfig);
			pvplistener = new PvPHandlingListener(pluginConfig);
			pvplistener.inject(this);
			oldpvphandler = new OldPVPFlagsHandler();
			if (pluginConfig.miscOldPvpFlags) {
				getLogger().warning(
						"Enabling the old-PvP flags. Do note that they're not supported, " +
						"as they're very out of scope of extending WG capabilities. " +
						"Consider turning them off by setting 'misc.old-pvp-flags' to 'false'"
				);
				oldpvphandler.start(this);
			}
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Unable to inject, shutting down", t);
			t.printStackTrace();
			Bukkit.shutdown();
		}
	}

	@Override
	public void onDisable() {
		try {
			WEWandCommandWrapper.uninject();
			WGRegionCommandWrapper.uninject();
			pvplistener.uninject();
			oldpvphandler.stop(this);
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Unable to uninject, shutting down", t);
			Bukkit.shutdown();
		}
	}

	public Config getPluginConfig() {
		return pluginConfig;
	}
}
