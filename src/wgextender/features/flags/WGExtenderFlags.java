package wgextender.features.flags;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.flags.registry.UnknownFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import wgextender.utils.ReflectionUtils;
import wgextender.utils.WGRegionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WGExtenderFlags {
    private WGExtenderFlags() {}

    private static final List<Flag<?>> FLAGS = new ArrayList<>();

    public static final StateFlag CHORUS_FRUIT_USE_FLAG = cache(new StateFlag("chorus-fruit-use", true));
    public static final BooleanFlag OLDPVP_ATTACKSPEED = cache(new BooleanFlag("oldpvp-attackspeed"));
    public static final BooleanFlag OLDPVP_NOBOW = cache(new BooleanFlag("oldpvp-nobow"));
    public static final BooleanFlag OLDPVP_NOSHIELDBLOCK = cache(new BooleanFlag("oldpvp-noshieldblock"));

    private static <T, F extends Flag<T>> F cache(F flag) {
        FLAGS.add(flag);
        return flag;
    }

    @SuppressWarnings("unchecked")
    public static void registerFlags() throws IllegalAccessException {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        Map<String, Flag<?>> flagMap = (Map<String, Flag<?>>) ReflectionUtils.getField(registry.getClass(), "flags").get(registry);
        for (var flag : FLAGS) registerFlag(flag, flagMap);
    }

    @SuppressWarnings("unchecked")
    private static void registerFlag(Flag<?> flag, Map<String, Flag<?>> flagMap) {
        //manually insert flag into the registry
        Flag<?> prevFlag = flagMap.put(flag.getName().toLowerCase(), flag);
        if (prevFlag == null) return;
        //change flag instance in every loaded region if had old one
        for (RegionManager rm : WGRegionUtils.getRegionContainer().getLoaded()) {
            for (ProtectedRegion region : rm.getRegions().values()) {
                Map<Flag<?>, Object> regionFlags = region.getFlags();
                Object prevValue = regionFlags.remove(prevFlag);
                if (prevValue == null) continue;

                //unknown flag will store marshaled value as value directly, so we can try to unmarshal it
                if (prevFlag instanceof UnknownFlag) {
                    try {
                        Object unmarshalled = flag.unmarshal(prevValue);
                        if (unmarshalled != null) {
                            regionFlags.put(flag, unmarshalled);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                //before reload instance probably, try to marshal value first to see if it is compatible
                else {
                    try {
                        ((Flag<Object>) flag).marshal(prevValue);
                        regionFlags.put(flag, prevValue);
                    } catch (Throwable ignored) {
                    }
                }
                region.setDirty(true);
            }
        }
    }
}
