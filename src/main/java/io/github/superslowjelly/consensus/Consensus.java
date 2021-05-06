package io.github.superslowjelly.consensus;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
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
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;

import javax.annotation.Nullable;

@Plugin(id = "consensus", name = "Consensus", version = "1.2.2-SNAPSHOT", authors = "pie_flavor, SuperslowJelly", description = "Allows players to vote for things to happen.")
public class Consensus {

    @Inject
    Game game;
    @Inject @DefaultConfig(sharedRoot = true)
    Path path;
    @Inject @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;
    @Inject
    Commands commands;
    @Inject
    Logger logger;

    Task task;
    Config config;

    @Listener
    public void preInit(GamePreInitializationEvent e) throws IOException {
        loader.getDefaultOptions().getSerializers().registerType(TypeToken.of(Duration.class), new DurationSerializer());
        loadConfig();
    }

    @Listener
    public void init(GameInitializationEvent e) {
        register();
    }

    @Listener
    public void reload(GameReloadEvent e) throws IOException {
        loadConfig();
        register();
    }

    private void register() {
        commands.registerCommands();
        if (task != null) {
            task.cancel();
        }
    }

    public void loadConfig() throws IOException {
        Asset cfg = game.getAssetManager().getAsset(this, "default.conf").get();
        if (!Files.exists(path)) {
            cfg.copyToFile(path);
        }
    }

    public void startBooleanVote(@Nullable CommandSource creator, String action, IntPredicate consumer, Duration duration) {
        Set<UUID> set = new HashSet<>();
        Text click = Text.builder("here")
                .onHover(TextActions.showText(TextBuilder.create("&aClick here to vote YES!").build()))
                .onClick(TextActions.executeCallback(src -> {
                    if (src instanceof Player) {
                        if (set.add(((Player) src).getUniqueId())) {
                            src.sendMessage(TextBuilder.create(TextBuilder.PREFIX + " &aSuccessfuly voted YES to " + action).build());
                        }
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
                        game.getServer().getBroadcastChannel().send(textBuilder.append("&apassed!").build());
                    } else {
                        game.getServer().getBroadcastChannel().send(textBuilder.append("&cfailed.").build());
                    }
                })
                .submit(this);
    }

}
