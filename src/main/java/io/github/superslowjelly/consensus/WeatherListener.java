package io.github.superslowjelly.consensus;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.weather.Weathers;

public class WeatherListener {

    private Consensus plugin;

    public WeatherListener(Consensus plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onWeatherChange(ChangeWorldWeatherEvent e) {
        if (!e.getWeather().equals(Weathers.CLEAR) && plugin.config.triggers.weather.worlds.contains(e.getTargetWorld().getName()) ^
                plugin.config.triggers.weather.type == Config.ListType.BLACKLIST) {
            int size = Sponge.getServer().getOnlinePlayers().size();
            System.out.println(plugin.config.weather.minPlayers);
            if (plugin.config.weather.minPlayers == 0 || plugin.config.weather.minPlayers <= size) {
                plugin.startBooleanVote(null,
                        Text.of(TextColors.DARK_AQUA, "change the weather to clear in world ",
                                e.getTargetWorld().getName()), i -> {
                            if (plugin.config.weather.majority * (double) size <= i) {
                                e.getTargetWorld().setWeather(Weathers.CLEAR);
                                return true;
                            } else {
                                return false;
                            }
                        }, plugin.config.weather.duration);
            }
        }
    }
}
