package wgextender.color;

import org.bukkit.ChatColor;

public class LegacyColorizer implements Colorizer {

    @Override
    public String colorize(String message) {
        if (message == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}