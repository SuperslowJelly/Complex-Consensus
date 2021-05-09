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
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.weather.Weather;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class Commands {

    @Inject
    private Game game;

    void registerCommands() {
        game.getCommandManager().getOwnedBy(Consensus.instance).forEach(game.getCommandManager()::removeMapping);
        CommandSpec.Builder poll = CommandSpec.builder()
                .description(Text.of("Used to start a poll."))
                .permission(Permissions.COMMAND_POLL_USE);

        if (Consensus.instance.config.time.enabled) {
            CommandSpec time = CommandSpec.builder()
                    .description(Text.of("Used to start a poll to change the time for the optionally specified world."))
                    .permission(Permissions.COMMAND_TIME_USE)
                    .executor(this::time)
                    .arguments(
                            GenericArguments.string(Text.of("time")),
                            GenericArguments.optional(GenericArguments.world(Text.of("world")))
                    )
                    .build();
            poll.child(time, "time");
        }

        if (Consensus.instance.config.weather.enabled) {
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

        if (Consensus.instance.config.dummy.enabled) {
            CommandSpec dummy = CommandSpec.builder()
                    .description(Text.of("Used to start a poll for any given subject."))
                    .permission(Permissions.COMMAND_DUMMY_USE)
                    .executor(this::dummy)
                    .arguments(
                            GenericArguments.string(Text.of("text")),
                            GenericArguments.optionalWeak(GenericArguments.doubleNum(Text.of("majority")), 0.5),
                            GenericArguments.optional(GenericArguments.duration(Text.of("duration")))
                    ).build();
            poll.child(dummy, "dummy");
        }

        game.getCommandManager().register(Consensus.instance, poll.build(), "poll");
    }

    public CommandResult time(CommandSource src, CommandContext args) throws CommandException {
        String input = args.<String>getOne("time").get();
        int time;
        switch (input) {
            case "day":
                time = 1000;
                break;
            case "noon":
                time = 6000;
                break;
            case "sunset":
                time = 12000;
                break;
            case "night":
                time = 13000;
                break;
            case "midnight":
                time = 18000;
                break;
            case "sunrise":
                time = 23000;
                break;
            default:
                throw new CommandException(Text.of(input + " is not a valid time of day! Valid times: <day|noon|sunset|night|midnight|sunrise>!"));
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
        if (Consensus.instance.config.time.minPlayers != 0 && size < Consensus.instance.config.time.minPlayers) {
            throw new CommandException(Text.of("Cannot vote to change the time; not enough players online (required: ", Consensus.instance.config.time.minPlayers, ")!"));
        }
        Consensus.instance.startBooleanVote(src, "change the time to " + input + " in the world '" + world.getWorldName() + "'", i -> {
            if (Consensus.instance.config.time.majority * (double) size <= i) {
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
        }, Consensus.instance.config.time.duration);
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
        if (Consensus.instance.config.weather.minPlayers != 0 && size < Consensus.instance.config.time.minPlayers) {
            throw new CommandException(Text.of("Cannot vote to change the weather; not enough players online (required: ", Consensus.instance.config.weather.minPlayers, ")!"));
        }
        Consensus.instance.startBooleanVote(src, "change the weather to " + weather.getName().replace('_', ' ') + " in world '" + world.getName() + "'", i -> {
            if (Consensus.instance.config.weather.majority * (double) size <= i) {
                world.setWeather(weather);
                return true;
            } else {
                return false;
            }
        }, Consensus.instance.config.weather.duration);
        return CommandResult.success();
    }

    public CommandResult dummy(CommandSource src, CommandContext args) throws CommandException {
        String text = args.<String>getOne("text").get();
        Duration duration = args.<Duration>getOne("duration").orElse(Duration.of(1, ChronoUnit.MINUTES));
        double majority = args.<Double>getOne("majority").get();
        int size = game.getServer().getOnlinePlayers().size();
        Consensus.instance.startBooleanVote(src, text, i -> majority * (double) size <= i, duration);
        return CommandResult.success();
    }

}
