package io.github.superslowjelly.consensus;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigSerializable
public class Config {

    @Setting public TimeModule time = new TimeModule();
    @Setting public WeatherModule weather = new WeatherModule();

    @ConfigSerializable
    public static class TimeModule extends PollModule { }

    @ConfigSerializable
    public static class WeatherModule extends PollModule { }

    public static abstract class PollModule extends Module {
        @Setting public double majority = 0.5;
        @Setting("min-players") public int minPlayers = 0;
        @Setting public Duration duration = Duration.of(1, ChronoUnit.MINUTES);
    }

    public static abstract class Module {
        @Setting public boolean enabled = false;
    }

}
