package io.github.superslowjelly.consensus;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.time.Duration;
import java.time.format.DateTimeParseException;

public class DurationSerializer implements TypeSerializer<Duration> {

    @Override
    public Duration deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        String s = value.getString();
        if (!s.contains("T")) {
            if (s.contains("D")) {
                if (s.contains("H") || s.contains("M") || s.contains("S")) {
                    s = s.replace("D", "DT");
                }
            } else {
                if (s.startsWith("P")) {
                    s = "PT" + s.substring(1);
                } else {
                    s = "T" + s;
                }
            }
        }
        if (!s.startsWith("P")) {
            s = "P" + s;
        }
        try {
            return Duration.parse(s);
        } catch (DateTimeParseException ex) {
            throw new ObjectMappingException("Invalid date!", ex);
        }
    }

    @Override
    public void serialize(TypeToken<?> type, Duration obj, ConfigurationNode value) throws ObjectMappingException {
        value.setValue(obj.toString());
    }
}
