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
import wgextender.color.ColorizerProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Config {
    private static final Map<String, String> MESSAGE_KEY_OVERRIDES = Map.of(
            "adminHelpSetFlag", "admin-help-setflag",
            "adminHelpRemoveOwner", "admin-help-removeowner",
            "adminHelpRemoveMember", "admin-help-removemember"
    );


    private final Plugin plugin;
    protected final File configFile;
    public Config(WGExtender plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "config.yml");
        loadDefaultMessages();
    }

    public boolean claimExpandSelectionVertical = false;

    public boolean claimBlockLimitsEnabled = false;
    public boolean claimResetOversizedSelection = false;
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
        public String playerOnlyCommand;
        public String globalRegionNameForbidden;
        public String invalidRegionName;
        public String maxClaimVolumeError;
        public String regionAlreadyExists;
        public String tooManyRegions;
        public String regionTooLarge;
        public String overlappingOthersRegion;
        public String claimOnlyInsideExisting;
        public String regionCreated;
        public String regionCreationError;
        public String onlyCuboidSelectionAllowed;
        public String noSelectionMade;
        public String regionExpandedVertically;
        public String claimTooLarge;
        public String claimYourLimit;
        public String claimTooSmall;
        public String claimMinVolume;
        public String claimTooNarrow;
        public String claimMinWidth;
        public String claimTooLow;
        public String claimMinHeight;
        public String adminNoPermission;
        public String adminHelpReload;
        public String adminHelpSearch;
        public String adminHelpSetFlag;
        public String adminHelpRemoveOwner;
        public String adminHelpRemoveMember;
        public String adminConfigReloaded;
        public String adminSearchNone;
        public String adminSearchFound;
        public String adminSearchNoSelection;
        public String adminWorldNotFound;
        public String adminFlagNotFound;
        public String adminFlagsSet;
        public String adminFlagInvalidFormat;
        public String adminPlayerRemovedFromAllRegions;
        public String adminRegionGroupOwners;
        public String adminRegionGroupMembers;
        public String chorusFruitDenied;
        public String restrictedCommandDenied;
        public String wandGiven;
        public String wandDisplayName;
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
        claimResetOversizedSelection = config.getBoolean("claim.blocklimits.reset-oversized-selection", claimResetOversizedSelection);
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
        loadMessages(config.getConfigurationSection("messages"), false);
    }

    private void loadDefaultMessages() {
        loadMessages(getDefaultMessagesSection(), true);
    }

    private void loadMessages(ConfigurationSection messagesSection, boolean initializeDefaults) {
        ColorizerProvider.init(messagesSection);
        for (Field field : Messages.class.getFields()) {
            if (!field.getType().equals(String.class)) {
                continue;
            }
            try {
                String configKey = toMessageConfigKey(field.getName());
                String defaultValue = initializeDefaults ? "" : (String) field.get(messages);
                field.set(messages, colorize(messagesSection, configKey, defaultValue));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Не удалось загрузить сообщение: " + field.getName(), e);
            }
        }
    }

    private ConfigurationSection getDefaultMessagesSection() {
        try (InputStream inputStream = plugin.getResource("config.yml")) {
            if (inputStream == null) {
                throw new IllegalStateException("Файл config.yml не найден");
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                ConfigurationSection messagesSection = defaultConfig.getConfigurationSection("messages");
                if (messagesSection == null) {
                    throw new IllegalStateException("В config.yml отсутствует секция messages");
                }
                return messagesSection;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать config.yml", e);
        }
    }

    private static String toMessageConfigKey(String fieldName) {
        String override = MESSAGE_KEY_OVERRIDES.get(fieldName);
        if (override != null) {
            return override;
        }

        StringBuilder key = new StringBuilder(fieldName.length() + 8);
        for (int i = 0; i < fieldName.length(); i++) {
            char current = fieldName.charAt(i);
            if (Character.isUpperCase(current)) {
                key.append('-').append(Character.toLowerCase(current));
            } else {
                key.append(current);
            }
        }
        return key.toString();
    }

    private static String colorize(ConfigurationSection section, String key, String defaultValue) {
        String rawText = section == null ? defaultValue : section.getString(key, defaultValue);
        if (rawText == null) {
            return null;
        }
        return ColorizerProvider.get().colorize(rawText);
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