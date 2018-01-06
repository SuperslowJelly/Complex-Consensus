package flavor.pie.consensus;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimeTask implements Runnable {

    private Consensus plugin;
    private Map<UUID, Long> worldTimes = new HashMap<>();

    public TimeTask(Consensus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int size = Sponge.getServer().getOnlinePlayers().size();
        if (plugin.config.time.minPlayers != 0 && plugin.config.time.minPlayers <= size) {
            Sponge.getServer().getWorlds().stream().filter(
                    w -> plugin.config.triggers.time.worlds.contains(w.getName()) ^
                            plugin.config.triggers.time.type == Config.ListType.BLACKLIST)
                    .map(World::getProperties).forEach(w -> {
                Long l = worldTimes.get(w.getUniqueId());
                if (l == null) {
                    worldTimes.put(w.getUniqueId(), w.getTotalTime());
                } else {
                    long time = w.getTotalTime();
                    long rem;
                    if (l % 24_000 < 12_000 && (rem = time % 24_000) > 12_000) {
                        plugin.startBooleanVote(null,
                                Text.of(TextColors.AQUA, "change the time to day in world ", w.getWorldName()), i -> {
                                    if (plugin.config.time.majority * (double) size <= i) {
                                        w.setWorldTime(time + (24_000 - rem));
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }, plugin.config.time.duration);
                    }
                }
            });
        }
    }
}
