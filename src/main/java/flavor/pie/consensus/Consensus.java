package flavor.pie.consensus;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Game;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

@Plugin(id = "consensus", name = "Consensus", version = "1.1.2", authors = "pie_flavor", description = "Allows players to vote for things to happen.")
public class Consensus {

    @Inject
    Game game;
    @Inject @DefaultConfig(sharedRoot = true)
    Path path;
    @Inject @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;
    @Inject
    Commands commands;

    Config config;
    Map<UUID, Instant> mutes = new HashMap<>();

    @Listener
    public void preInit(GamePreInitializationEvent e) throws IOException, ObjectMappingException {
        loader.getDefaultOptions().getSerializers().registerType(TypeToken.of(Duration.class), new DurationSerializer());
        loadConfig();
    }

    @Listener
    public void init(GameInitializationEvent e) {
        commands.registerCommands();
    }

    @Listener
    public void reload(GameReloadEvent e) throws IOException, ObjectMappingException {
        loadConfig();
        commands.registerCommands();
    }

    public void loadConfig() throws IOException, ObjectMappingException {
        Asset cfg = game.getAssetManager().getAsset(this, "default.conf").get();
        try {
            if (!Files.exists(path)) {
                cfg.copyToFile(path);
            }
            config = loader.load().getValue(Config.type);
        } catch (IOException | ObjectMappingException ex) {
            if (config == null) {
                config = HoconConfigurationLoader.builder().setURL(cfg.getUrl()).build().load(loader.getDefaultOptions()).getValue(Config.type);
            }
            throw ex;
        }
    }

    @Listener
    public void chat(MessageChannelEvent.Chat e, @First Player p) {
        Instant mute = mutes.get(p.getUniqueId());
        Instant now = Instant.now();
        if (mute != null && mute.isAfter(now)) {
            p.playSound(SoundTypes.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, p.getLocation().getPosition(), 10.0);
            p.sendMessage(ChatTypes.ACTION_BAR, Text.of("You are muted for another ", durationToString(Duration.between(now, mute))));
            e.setCancelled(true);
        }
    }

    public void startBooleanVote(CommandSource creator, Text action, IntPredicate consumer, Duration duration) {
        Set<UUID> set = new HashSet<>();
        Text msg = Text.of(TextColors.GREEN, creator.getName(), " has begun a vote to ", action, "! Click ", Text.builder("here").color(TextColors.GOLD)
                .onHover(
                        TextActions.showText(Text.of(TextColors.GREEN, "Click me!"))).onClick(TextActions.executeCallback(src -> {
                    if (src instanceof Player) {
                        if (set.add(((Player) src).getUniqueId())) {
                            src.sendMessage(Text.of("Voted YES to ", action));
                        }
                    }
                })).build(), " to vote yes!");
        game.getServer().getBroadcastChannel().send(msg);
        Task.builder()
                .delayTicks(duration.getSeconds() * 20)
                .execute(() -> {
                    Text desc = Text.of(TextColors.GREEN, "The vote to ", action, " has ");
                    if (consumer.test(set.size())) {
                        game.getServer().getBroadcastChannel().send(desc.concat(Text.of(TextColors.GREEN, "passed.")));
                    } else {
                        game.getServer().getBroadcastChannel().send(desc.concat(Text.of(TextColors.RED, "failed.")));
                    }
                })
                .submit(this);
    }

    private final static Pattern DURATION_TO_STRING_PATTERN = Pattern.compile("[PT]");

    String durationToString(Duration duration) {
        return DURATION_TO_STRING_PATTERN.matcher(duration.toString()).replaceAll("");
    }
}
