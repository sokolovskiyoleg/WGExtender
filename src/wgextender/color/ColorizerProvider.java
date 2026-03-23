package wgextender.color;

import org.bukkit.configuration.ConfigurationSection;

public final class ColorizerProvider {

    private static Colorizer colorizer = new LegacyColorizer();

    private ColorizerProvider() {
    }

    public static void init(ConfigurationSection configSection) {
        String serializerType = configSection == null
                ? "LEGACY"
                : configSection.getString("serializer", "LEGACY");
        if ("MINIMESSAGE".equalsIgnoreCase(serializerType)) {
            colorizer = new MiniMessageColorizer();
        } else {
            colorizer = new LegacyColorizer();
        }
    }

    public static Colorizer get() {
        return colorizer;
    }
}