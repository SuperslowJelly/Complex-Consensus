package flavor.pie.consensus;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import flavor.pie.util.arguments.MoreArguments;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
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
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;

@Plugin(id = "consensus", name = "Consensus", version = "1.0.0")
public class Consensus {
    @Inject
    Game game;
    @Inject
    Logger logger;
    @Inject @DefaultConfig(sharedRoot = true)
    Path path;
    @Inject @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;
    Config config;
    Map<UUID, Instant> mutes = new HashMap<>();
    @Listener
    public void preInit(GamePreInitializationEvent e) throws IOException, ObjectMappingException {
        loader.getDefaultOptions().getSerializers().registerType(TypeToken.of(Duration.class), new DurationSerializer());
        loadConfig();
    }

    @Listener
    public void init(GameInitializationEvent e) {
        CommandSpec ban = CommandSpec.builder()
                .executor(this::ban)
                .arguments(
                        GenericArguments.player(Text.of("player")),
                        GenericArguments.optional(MoreArguments.duration(Text.of("duration"))),
                        MoreArguments.text(Text.of("reason"), false, true)
                )
                .build();
        CommandSpec mute = CommandSpec.builder()
                .executor(this::mute)
                .arguments(
                        GenericArguments.player(Text.of("player")),
                        GenericArguments.optional(MoreArguments.duration(Text.of("duration"))),
                        MoreArguments.text(Text.of("reason"), false, true)
                )
                .build();
        CommandSpec kick = CommandSpec.builder()
                .executor(this::kick)
                .arguments(
                        GenericArguments.player(Text.of("player")),
                        MoreArguments.text(Text.of("reason"), false, true)
                )
                .build();
        CommandSpec time = CommandSpec.builder()
                .executor(this::time)
                .arguments(
                        GenericArguments.integer(Text.of("time")),
                        GenericArguments.optional(GenericArguments.world(Text.of("world")))
                )
                .build();
        CommandSpec dummy = CommandSpec.builder()
                .executor(this::dummy)
                .arguments(
                        MoreArguments.text(Text.of("text"), false, false),
                        GenericArguments.optionalWeak(GenericArguments.doubleNum(Text.of("majority")), 0.5),
                        GenericArguments.optional(MoreArguments.duration(Text.of("duration")))
                ).build();
        CommandSpec poll = CommandSpec.builder()
                .child(ban, "ban")
                .child(mute, "mute")
                .child(kick, "kick")
                .child(time, "time")
                .child(dummy, "dummy")
                .build();
        game.getCommandManager().register(this, poll, "poll");
    }

    @Listener
    public void reload(GameReloadEvent e) throws IOException, ObjectMappingException {
        loadConfig();
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

    public CommandResult ban(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        Text reason = args.<Text>getOne("reason").get();
        Duration duration = args.<Duration>getOne("duration").orElse(config.ban.maxDuration);
        if (config.ban.exempt != null && p.hasPermission(config.ban.exempt) && (config.ban.override == null || !src.hasPermission(config.ban.override))) {
            throw new CommandException(Text.of("This person cannot be banned!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (config.ban.minPlayers != 0 && config.ban.minPlayers < size) {
            throw new CommandException(Text.of("Cannot voteban; not enough players online (required: ", config.ban.minPlayers, ")!"));
        }
        startVote(src, Text.of(TextColors.RED, "ban ", p.getName(), " for ", reason, " for ", duration.toString().replaceAll("[PT]", "")), i -> {
            if (config.ban.majority * (double) size <= i) {
                game.getServiceManager().provideUnchecked(BanService.class).addBan(Ban.builder()
                        .profile(p.getProfile())
                        .expirationDate(Instant.now().plus(duration))
                        .reason(reason)
                        .source(Text.of("Majority vote"))
                        .type(BanTypes.PROFILE)
                        .startDate(Instant.now())
                        .build());
                return true;
            } else {
                return false;
            }
        }, config.time.duration);
        return CommandResult.success();
    }

    public CommandResult mute(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        Text reason = args.<Text>getOne("reason").get();
        Duration duration = args.<Duration>getOne("duration").orElse(config.mute.maxDuration);
        if (config.mute.exempt != null && p.hasPermission(config.mute.exempt) && (config.mute.override == null || !src.hasPermission(config.mute.override))) {
            throw new CommandException(Text.of("This person cannot be banned!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (config.mute.minPlayers != 0 && config.mute.minPlayers < size) {
            throw new CommandException(Text.of("Cannot votemute; not enough players online (required: ", config.mute.minPlayers, ")!"));
        }
        startVote(src, Text.of(TextColors.YELLOW, "mute ", p.getName(), " for ", reason, " for ", duration.toString().replaceAll("[PT]", "")), i -> {
            if (config.mute.majority * (double) size <= i) {
                mutes.put(p.getUniqueId(), Instant.now().plus(duration));
                return true;
            } else {
                return false;
            }
        }, config.time.duration);
        return CommandResult.success();
    }

    public CommandResult kick(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        Text reason = args.<Text>getOne("reason").get();
        if (config.kick.exempt != null && p.hasPermission(config.kick.exempt) && (config.kick.override == null || !src.hasPermission(config.kick.override))) {
            throw new CommandException(Text.of("This person cannot be kicked!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (config.kick.minPlayers != 0 && config.kick.minPlayers < size) {
            throw new CommandException(Text.of("Cannot votekick; not enough players online (required: ", config.kick.minPlayers, ")!"));
        }
        startVote(src, Text.of(TextColors.GOLD, "kick ", p.getName(), " for ", reason), i -> {
            if (config.mute.majority * (double) size <= i) {
                p.kick(reason);
                return true;
            } else {
                return false;
            }
        }, config.time.duration);
        return CommandResult.success();
    }

    public CommandResult time(CommandSource src, CommandContext args) throws CommandException {
        int time = args.<Integer>getOne("time").get();
        if (time < 0 || time > 24_000) {
            throw new CommandException(Text.of(time, " is not a valid time of day!"));
        }
        Optional<WorldProperties> world_ =  args.getOne("world");
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
        if (config.time.minPlayers != 0 && config.time.minPlayers < size) {
            throw new CommandException(Text.of("Cannot vote to change the time; not enough players online (required: ", config.time.minPlayers, ")!"));
        }
        startVote(src, Text.of(TextColors.AQUA, "change the time to ", time, " in world ", world.getWorldName()), i -> {
            if (config.time.majority * (double) size <= i) {
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
        }, config.time.duration);
        return CommandResult.success();
    }

    public CommandResult dummy(CommandSource src, CommandContext args) throws CommandException {
        Text text = args.<Text>getOne("text").get();
        Duration duration = args.<Duration>getOne("duration").orElse(Duration.of(1, ChronoUnit.MINUTES));
        double majority = args.<Double>getOne("majority").get();
        int size = game.getServer().getOnlinePlayers().size();
        startVote(src, text, i -> majority * (double) size <= i, duration);
        return CommandResult.success();
    }

    public void startVote(CommandSource creator, Text action, IntPredicate consumer, Duration duration) {
        Set<UUID> set = new HashSet<>();
        Text msg = Text.of(TextColors.GREEN, creator.getName(), " has begun a vote to ", action, "! Click ", Text.builder("here").color(TextColors.GOLD)
                .onHover(TextActions.showText(Text.of(TextColors.GREEN, "Click me!"))).onClick(TextActions.executeCallback(src -> {
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

    @Listener
    public void chat(MessageChannelEvent.Chat e, @First Player p) {
        Instant mute = mutes.get(p.getUniqueId());
        Instant now = Instant.now();
        if (mute != null && mute.isAfter(now)) {
            p.playSound(SoundTypes.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, p.getLocation().getPosition(), 10.0);
            p.sendMessage(ChatTypes.ACTION_BAR, Text.of("You are muted for another ", Duration.between(now, mute).toString().replaceAll("[PT]", "")));
            e.setCancelled(true);
        }
    }
}
