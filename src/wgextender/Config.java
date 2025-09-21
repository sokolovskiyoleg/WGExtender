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

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.math.BigInteger;
import java.util.*;

public class Config {

    private final Plugin plugin;
    protected final File configFile;
    public Config(WGExtender plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public boolean claimExpandSelectionVertical = false;

    public boolean claimBlockLimitsEnabled = false;
    public Map<String, BigInteger> claimBlockLimits = new LinkedHashMap<>();
    public BigInteger claimBlockLimitDefault = BigInteger.ZERO;
    public BigInteger claimBlockMinimalVolume = BigInteger.ZERO;
    public BigInteger claimBlockMinimalHorizontal = BigInteger.ZERO;
    public BigInteger claimBlockMinimalVertical = BigInteger.ZERO;

    public boolean checkLavaFlow = false;
    public boolean checkWaterFlow = false;
    public boolean checkOtherLiquidFlow = false;
    public boolean checkFireSpreadToRegion = false;
    public boolean disableFireSpreadInRegion = false;
    public boolean disableBlockBurnInRegion = false;
    public boolean checkExplosionBlockDamage = false;
    public boolean checkExplosionEntityDamage = false;

    public boolean claimAutoFlagsEnabled = false;
    public boolean showAutoFlagMessages = false;
    public Map<Flag<?>, String> claimAutoFlags = new HashMap<>();

    public boolean restrictCommandsInRegionEnabled = false;
    public List<String> restrictedCommandsInRegion = new ArrayList<>();

    public boolean extendedWorldEditWandEnabled = false;

    public Boolean miscDefaultPvPFlagOperationMode = null;

    public boolean miscOldPvpFlags = true;

    public Messages messages = new Messages();


    public class Messages {
        public String playerOnlyCommand = "Эта команда только для игроков";
        public String globalRegionNameForbidden = "§8[§c!§8] §7Нельзя заприватить регион, с названием __global__.";
        public String invalidRegionName = "§8[§c!§8] §7Имя региона §c%id%§7 содержит запрещённые символы.";
        public String maxClaimVolumeError = "The maximum claim volume get in the configuration is higher than is supported. Currently, it must be 2147483647 or smaller. Please contact a server administrator.";
        public String regionAlreadyExists = "§8[§c§l!§8] §7Регион с таким именем уже существует.";
        public String tooManyRegions = "§8[§c§l!§8] §7У вас слишком много регионов, удалите один из них перед тем как заприватить новый.";
        public String regionTooLarge = "Размер региона слишком большой. Максимальный размер: %max%, ваш размер: %current%";
        public String overlappingOthersRegion = "§8[§c§l!§8] §7Регион не создан. Он перекрывает чужой регион.";
        public String claimOnlyInsideExisting = "Вы можете приватить только внутри своих регионов.";
        public String regionCreated = "§8[§a§l!§8] §7Регион §a%id%§7 создан.";
        public String regionCreationError = "Произошла ошибка при привате региона %id%";
        public String onlyCuboidSelectionAllowed = "Вы можете использовать только кубическкую территорию.";
        public String noSelectionMade = "Сначала выделите территорию. Используйте WorldEdit для выделения (wiki: http://wiki.sk89q.com/wiki/WorldEdit).";
        //
        public String regionExpandedVertically = "Регион автоматически расширен по вертикали";
        public String claimTooLarge = "§8[§c§l!§8] §7Вы не можете заприватить такой большой регион";
        public String claimYourLimit = "§8[§c§l!§8] §7Ваш лимит: §8[§a%limit%§8]. §7Размер выделения: §8[§c%size%§8].§r";
        public String claimTooSmall = "Вы не можете заприватить такой маленький регион";
        public String claimMinVolume = "Минимальный объем: %limit%, вы попытались заприватить: %size%";
        public String claimTooNarrow = "Вы не можете заприватить такой узкий регион";
        public String claimMinWidth = "Минимальная ширина: %limit%, вы попытались заприватить: %size%";
        public String claimTooLow = "Вы не можете заприватить такой низкий регион";
        public String claimMinHeight = "Минимальная высота: %limit%, вы попытались заприватить: %size%";
    }

    protected static final String miscPvPFlagOperationModeAllow = "allow";
    protected static final String miscPvPFlagOperationModeDeny = "deny";
    protected static final String miscPvPFlagOperationModeDefault = "default";

    public void loadConfig() {
        plugin.saveDefaultConfig();
        loadAll();
    }

    private void loadAll() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        claimExpandSelectionVertical = config.getBoolean("claim.vertexpand", claimExpandSelectionVertical);

        claimBlockLimitsEnabled = config.getBoolean("claim.blocklimits.enabled", claimBlockLimitsEnabled);
        claimBlockLimits.clear();
        ConfigurationSection limitsSection = config.getConfigurationSection("claim.blocklimits.limits");
        if (limitsSection != null) {
            claimBlockLimitDefault = asBig(limitsSection, "default");
            for (String group : limitsSection.getKeys(false)) {
                claimBlockLimits.put(
                        group.toLowerCase(),
                        asBig(limitsSection, group)
                );
            }
        } else {
            claimBlockLimitDefault = BigInteger.ZERO;
        }
        ConfigurationSection minLimitsSection = config.getConfigurationSection("claim.blocklimits.minimal");
        if (minLimitsSection != null) {
            claimBlockMinimalVolume = asBig(minLimitsSection, "volume");
            claimBlockMinimalHorizontal = asBig(minLimitsSection, "horizontal");
            claimBlockMinimalVertical = asBig(minLimitsSection, "vertical");
        } else {
            claimBlockMinimalVolume = BigInteger.ZERO;
            claimBlockMinimalHorizontal = BigInteger.ZERO;
            claimBlockMinimalVertical = BigInteger.ZERO;
        }

        checkLavaFlow = config.getBoolean("regionprotect.flow.lava", checkLavaFlow);
        checkWaterFlow = config.getBoolean("regionprotect.flow.water", checkWaterFlow);
        checkOtherLiquidFlow = config.getBoolean("regionprotect.flow.other", checkOtherLiquidFlow);
        checkFireSpreadToRegion = config.getBoolean("regionprotect.fire.spread.toregion", checkFireSpreadToRegion);
        disableFireSpreadInRegion = config.getBoolean("regionprotect.fire.spread.inregion", disableFireSpreadInRegion);
        disableBlockBurnInRegion = config.getBoolean("regionprotect.fire.burn", disableBlockBurnInRegion);
        checkExplosionBlockDamage = config.getBoolean("regionprotect.explosion.block", checkExplosionBlockDamage);
        checkExplosionEntityDamage = config.getBoolean("regionprotect.explosion.entity", checkExplosionEntityDamage);

        claimAutoFlagsEnabled = config.getBoolean("autoflags.enabled", claimAutoFlagsEnabled);
        showAutoFlagMessages = config.getBoolean("autoflags.show-messages", showAutoFlagMessages);
        claimAutoFlags.clear();
        ConfigurationSection autoflagsSection = config.getConfigurationSection("autoflags.flags");
        if (autoflagsSection != null) {
            for (String flagStr : autoflagsSection.getKeys(false)) {
                Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagStr);
                if (flag != null) {
                    claimAutoFlags.put(flag, autoflagsSection.getString(flagStr));
                }
            }
        }

        restrictCommandsInRegionEnabled = config.getBoolean("restrictcommands.enabled", restrictCommandsInRegionEnabled);
        restrictedCommandsInRegion = new ArrayList<>(config.getStringList("restrictcommands.commands"));

        extendedWorldEditWandEnabled = config.getBoolean("extendedwewand", extendedWorldEditWandEnabled);

        String miscPvpModeStr = config.getString("misc.pvpmode", miscPvPFlagOperationModeDefault);
        if (miscPvpModeStr.equalsIgnoreCase(miscPvPFlagOperationModeAllow)) {
            miscDefaultPvPFlagOperationMode = Boolean.TRUE;
        } else if (miscPvpModeStr.equalsIgnoreCase(miscPvPFlagOperationModeDeny)) {
            miscDefaultPvPFlagOperationMode = Boolean.FALSE;
        } else {
            miscDefaultPvPFlagOperationMode = null;
        }
        miscOldPvpFlags = config.getBoolean("misc.old-pvp-flags");

        loadMessages(config);
    }

    private void loadMessages(FileConfiguration config) {
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            messages.playerOnlyCommand = messagesSection.getString("player-only-command", messages.playerOnlyCommand);
            messages.globalRegionNameForbidden = messagesSection.getString("global-region-name-forbidden", messages.globalRegionNameForbidden);
            messages.invalidRegionName = messagesSection.getString("invalid-region-name", messages.invalidRegionName);
            messages.maxClaimVolumeError = messagesSection.getString("max-claim-volume-error", messages.maxClaimVolumeError);
            messages.regionAlreadyExists = messagesSection.getString("region-already-exists", messages.regionAlreadyExists);
            messages.tooManyRegions = messagesSection.getString("too-many-regions", messages.tooManyRegions);
            messages.regionTooLarge = messagesSection.getString("region-too-large", messages.regionTooLarge);
            messages.overlappingOthersRegion = messagesSection.getString("overlapping-others-region", messages.overlappingOthersRegion);
            messages.claimOnlyInsideExisting = messagesSection.getString("claim-only-inside-existing", messages.claimOnlyInsideExisting);
            messages.regionCreated = messagesSection.getString("region-created", messages.regionCreated);
            messages.regionCreationError = messagesSection.getString("region-creation-error", messages.regionCreationError);
            messages.onlyCuboidSelectionAllowed = messagesSection.getString("only-cuboid-selection-allowed", messages.onlyCuboidSelectionAllowed);
            messages.noSelectionMade = messagesSection.getString("no-selection-made", messages.noSelectionMade);
            //
            messages.regionExpandedVertically = messagesSection.getString("region-expanded-vertically", messages.regionExpandedVertically);
            messages.claimTooLarge = messagesSection.getString("claim-too-large", messages.claimTooLarge);
            messages.claimYourLimit = messagesSection.getString("claim-your-limit", messages.claimYourLimit);
            messages.claimTooSmall = messagesSection.getString("claim-too-small", messages.claimTooSmall);
            messages.claimMinVolume = messagesSection.getString("claim-min-volume", messages.claimMinVolume);
            messages.claimTooNarrow = messagesSection.getString("claim-too-narrow", messages.claimTooNarrow);
            messages.claimMinWidth = messagesSection.getString("claim-min-width", messages.claimMinWidth);
            messages.claimTooLow = messagesSection.getString("claim-too-low", messages.claimTooLow);
            messages.claimMinHeight = messagesSection.getString("claim-min-height", messages.claimMinHeight);
        }
    }

    private static BigInteger asBig(ConfigurationSection section, String key) {
        if (section.isInt(key)) {
            return BigInteger.valueOf(section.getInt(key));
        } else {
            String value = section.getString(key, "0");
            if (value.equals("0")) return BigInteger.ZERO;
            return new BigInteger(value);
        }
    }

    public Messages getMessages() {
        return messages;
    }
}