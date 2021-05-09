package io.github.superslowjelly.consensus;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageChannel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;

@Plugin(id = "consensus", name = "Consensus", version = "1.2.2-SNAPSHOT", authors = "pie_flavor, SuperslowJelly", description = "Allows players to vote for things to happen.")
public class Consensus {

    @Inject
    private Game game;

    @Inject @DefaultConfig(sharedRoot = true)
    private Path path;

    @Inject @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    @Inject
    private Commands commands;

    @Inject
    private Logger logger;

    public static Consensus instance;

    private Task task;

    public Config config;

    @Listener
    public void preInit(GamePreInitializationEvent e) throws IOException, ObjectMappingException {
        loader.getDefaultOptions().getSerializers().registerType(TypeToken.of(Duration.class), new DurationSerializer());
        loadConfig();
    }

    @Listener
    public void init(GameInitializationEvent e) {
        register();
    }

    @Listener
    public void reload(GameReloadEvent e) throws IOException, ObjectMappingException {
        loadConfig();
        register();
    }

    private void register() {
        instance = this;
        commands.registerCommands();
        if (task != null) {
            task.cancel();
        }
    }

    public void loadConfig() throws IOException, ObjectMappingException {
        Asset cfg = game.getAssetManager().getAsset(this, "default.conf").get();
        if (!Files.exists(path)) {
            cfg.copyToFile(path);
        }
        ConfigurationNode node = loader.load();
        config = node.getValue(Config.type);
    }

    public void startBooleanVote(@Nullable CommandSource creator, String action, IntPredicate consumer, Duration duration) {
        Set<UUID> set = new HashSet<>();
        Text click = Text.builder().append(TextBuilder.create("&ehere").build())
                .onHover(TextActions.showText(TextBuilder.create("&aClick here to vote YES!").build()))
                .onClick(TextActions.executeCallback(src -> {
                    if (src instanceof Player) {
                        Text response;
                        if (src.hasPermission(Permissions.POLL_VOTE)) {
                            if (set.add(((Player) src).getUniqueId())) {
                                response = TextBuilder.create(TextBuilder.PREFIX + " &aSuccessfuly voted YES to " + action).build();
                            } else response = TextBuilder.create(TextBuilder.PREFIX + " &cError: Something went wrong!").build();
                        } else response = TextBuilder.create(TextBuilder.PREFIX + " &cError: You do not have permission to vote in polls!").build();
                        src.sendMessage(response);
                    }
                })).build();
        Text msg = TextBuilder.create()
                .append(TextBuilder.PREFIX + " &e" + (creator != null ? creator.getName() : "The Server") + " &fhas begun a vote to &e" + action + "&f! Click ")
                .append(click)
                .append(" &fto vote &aYES&f!")
                .build();
        game.getServer().getBroadcastChannel().send(msg);
        Task.builder()
                .delayTicks(duration.getSeconds() * 20)
                .execute(() -> {
                    TextBuilder textBuilder = TextBuilder.create(TextBuilder.PREFIX + " &fThe vote to &e" + action + " &fhas ");
                    if (consumer.test(set.size())) {
                        textBuilder.append("&apassed&f, enough people voted!");
                    } else {
                        textBuilder.append("&cfailed&f, not enough people voted!.");
                    }
                    MessageChannel.permission(Permissions.POLL_SEE).send(textBuilder.build());
                })
                .submit(this);
    }

}
