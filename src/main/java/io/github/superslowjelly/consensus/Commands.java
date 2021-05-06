package io.github.superslowjelly.consensus;

import com.google.inject.Inject;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.weather.Weather;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class Commands {

    @Inject
    private Consensus plugin;
    @Inject
    private Game game;

    void registerCommands() {
        game.getCommandManager().getOwnedBy(plugin).forEach(game.getCommandManager()::removeMapping);
        CommandSpec.Builder poll = CommandSpec.builder();

        if (plugin.config.time.enabled) {
            CommandSpec time = CommandSpec.builder()
                    .description(Text.of("Used to start a poll to change the time for the optionally specified world."))
                    .permission(Permissions.COMMAND_TIME_USE)
                    .executor(this::time)
                    .arguments(
                            GenericArguments.integer(Text.of("time")),
                            GenericArguments.optional(GenericArguments.world(Text.of("world")))
                    )
                    .build();
            poll.child(time, "time");
        }

        if (plugin.config.weather.enabled) {
            CommandSpec weather = CommandSpec.builder()
                    .description(Text.of("Used to start a poll to change the weather for the optionally specified world."))
                    .permission(Permissions.COMMAND_WEATHER_USE)
                    .executor(this::weather)
                    .arguments(
                            GenericArguments.catalogedElement(Text.of("weather"), CatalogTypes.WEATHER),
                            GenericArguments.optional(GenericArguments.world(Text.of("world")))
                    )
                    .build();
            poll.child(weather, "weather");
        }

        CommandSpec dummy = CommandSpec.builder()
                .description(Text.of("Used to start a poll for any given subject."))
                .permission(Permissions.COMMAND_DUMMY_USE)
                .executor(this::dummy)
                .arguments(
                        GenericArguments.text(Text.of("text"), TextSerializers.FORMATTING_CODE, true),
                        GenericArguments.optionalWeak(GenericArguments.doubleNum(Text.of("majority")), 0.5),
                        GenericArguments.optional(GenericArguments.duration(Text.of("duration")))
                ).build();

        poll.child(dummy, "dummy");
        game.getCommandManager().register(plugin, poll.build(), "poll");
    }

    public CommandResult time(CommandSource src, CommandContext args) throws CommandException {
        int time = args.<Integer>getOne("time").get();
        if (time < 0 || time > 24_000) {
            throw new CommandException(Text.of(time, " is not a valid time of day!"));
        }
        Optional<WorldProperties> world_ = args.getOne("world");
        WorldProperties world;
        if (world_.isPresent()) {
            world = world_.get();
        } else {
            if (src instanceof Player) {
                world = ((Player) src).getWorld().getProperties();
            } else {
                throw new CommandException(Text.of("You must specify a world!"));
            }
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (plugin.config.time.minPlayers != 0 && size < plugin.config.time.minPlayers) {
            throw new CommandException(Text.of("Cannot vote to change the time; not enough players online (required: ", plugin.config.time.minPlayers, ")!"));
        }
        plugin.startBooleanVote(src, Text.of(TextColors.AQUA, "change the time to ", time, " in world ", world.getWorldName()), i -> {
            if (plugin.config.time.majority * (double) size <= i) {
                long worldTime = world.getWorldTime();
                int currentTime = (int) (worldTime % 24_000);
                int timeToAdd;
                if (currentTime < time) {
                    timeToAdd = time - currentTime;
                } else {
                    timeToAdd = 24_000 - (currentTime - time);
                }
                world.setWorldTime(worldTime + timeToAdd);
                return true;
            } else {
                return false;
            }
        }, plugin.config.time.duration);
        return CommandResult.success();
    }

    public CommandResult weather(CommandSource src, CommandContext args) throws CommandException {
        Weather weather = args.<Weather>getOne("weather").get();
        Optional<WorldProperties> props_ = args.getOne("world");
        WorldProperties props;
        if (props_.isPresent()) {
            props = props_.get();
        } else {
            if (src instanceof Player) {
                props = ((Player) src).getWorld().getProperties();
            } else {
                throw new CommandException(Text.of("You must specify a world!"));
            }
        }
        Optional<World> world_ = game.getServer().getWorld(props.getUniqueId());
        World world;
        if (world_.isPresent()) {
            world = world_.get();
        } else {
            throw new CommandException(Text.of("This world is not loaded!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (plugin.config.weather.minPlayers != 0 && size < plugin.config.time.minPlayers) {
            throw new CommandException(Text.of("Cannot vote to change the weather; not enough players online (required: ", plugin.config.weather.minPlayers, ")!"));
        }
        plugin.startBooleanVote(src, Text.of(TextColors.DARK_AQUA, "change the weather to ", weather.getName(), " in world ", world.getName()), i -> {
            if (plugin.config.weather.majority * (double) size <= i) {
                world.setWeather(weather);
                return true;
            } else {
                return false;
            }
        }, plugin.config.weather.duration);
        return CommandResult.success();
    }

    public CommandResult dummy(CommandSource src, CommandContext args) throws CommandException {
        Text text = args.<Text>getOne("text").get();
        Duration duration = args.<Duration>getOne("duration").orElse(Duration.of(1, ChronoUnit.MINUTES));
        double majority = args.<Double>getOne("majority").get();
        int size = game.getServer().getOnlinePlayers().size();
        plugin.startBooleanVote(src, text, i -> majority * (double) size <= i, duration);
        return CommandResult.success();
    }

}
