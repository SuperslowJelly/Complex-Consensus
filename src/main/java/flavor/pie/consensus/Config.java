package flavor.pie.consensus;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class Config {
    public static final TypeToken<Config> type = TypeToken.of(Config.class);
    @Setting("enabled-modes") public List<Mode> enabledModes = Collections.emptyList();
    public enum Mode {
        BAN, MUTE, KICK, TIME, COMMAND,
    }
    @Setting public BanModule ban = new BanModule();
    @Setting public KickModule kick = new KickModule();
    @Setting public MuteModule mute = new MuteModule();
    @Setting public TimeModule time = new TimeModule();
    @Setting public CommandModule command = new CommandModule();
    @ConfigSerializable
    public static class BanModule extends Module {
        @Setting("max-duration") public Duration maxDuration = Duration.of(1, ChronoUnit.DAYS);
        @Setting public String exempt;
        @Setting public String override;
    }
    @ConfigSerializable
    public static class KickModule extends Module {
        @Setting public String exempt;
        @Setting public String override;
    }
    @ConfigSerializable
    public static class MuteModule extends Module {
        @Setting public String exempt;
        @Setting public String override;
        @Setting("max-duration") public Duration maxDuration = Duration.of(1, ChronoUnit.HOURS);
    }
    @ConfigSerializable
    public static class TimeModule extends Module {

    }
    @ConfigSerializable
    public static class CommandModule extends Module {
        @Setting("allowed-commands") public List<String> allowedCommands = Collections.emptyList();
        @Setting public String override;
    }
    public static abstract class Module {
        @Setting public double majority = 0.5;
        @Setting("min-players") public int minPlayers = 10;
        @Setting public Duration duration = Duration.of(1, ChronoUnit.MINUTES);
    }

}
