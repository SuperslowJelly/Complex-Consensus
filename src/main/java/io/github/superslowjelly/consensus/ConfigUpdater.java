package io.github.superslowjelly.consensus;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.util.TypeTokens;

import java.util.stream.Stream;

public class ConfigUpdater {
    private ConfigUpdater() { }

    public static void t2(ConfigurationNode node) {
        try {
            ConfigurationNode enabledModes = node.getNode("enabled-modes");
            for (String string : enabledModes.getList(TypeTokens.STRING_TOKEN)) {
                node.getNode(string, "enabled").setValue(true);
            }
            enabledModes.setValue(null);
            Stream.of("ban", "kick", "mute").forEach(s -> node.getNode(s, "exempt").setValue(null));
            Stream.of("ban", "kick", "mute", "command").forEach(s -> node.getNode(s, "override").setValue(null));
            node.getNode("version").setValue(2);
        } catch (ObjectMappingException ex) {
            rethrow(ex);
        }
    }

    private static <T extends Throwable> void rethrow(Throwable t) throws T { throw (T) t; }
}
