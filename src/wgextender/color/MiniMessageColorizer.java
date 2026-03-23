package wgextender.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.internal.parser.ParsingExceptionImpl;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MiniMessageColorizer implements Colorizer {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyColorizer fallback = new LegacyColorizer();

    @Override
    public String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        try {
            Component component = miniMessage.deserialize(message);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (ParsingExceptionImpl ex) {
            return fallback.colorize(message);
        }
    }
}