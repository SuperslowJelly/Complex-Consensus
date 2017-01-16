package flavor.pie.consensus;

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
        if (!s.startsWith("P") && !s.startsWith("p")) {
            s = "P"+s;
        }
        try {
            return Duration.parse(s);
        } catch (DateTimeParseException ex) {
            throw new ObjectMappingException(ex);
        }
    }

    @Override
    public void serialize(TypeToken<?> type, Duration obj, ConfigurationNode value) throws ObjectMappingException {
        value.setValue(obj.toString());
    }
}
