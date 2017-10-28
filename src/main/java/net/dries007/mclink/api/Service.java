package net.dries007.mclink.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import static net.dries007.mclink.api.API.TYPE_MAP_STRING_STRING;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Service
{
    public final String name;
    public final String website;
    public final ImmutableMap<String, String> info;
    public final ImmutableMap<String, String> requiredArgs;
    public final ImmutableMap<String, String> optionalArgs;
    public final ImmutableMap<String, String> extra;

    Service(String name, String website, Map<String, String> info, Map<String, String> ra, Map<String, String> oa, Map<String, String> extra)
    {
        this.name = name;
        this.website = website;
        this.info = ImmutableMap.copyOf(info);
        this.requiredArgs = ImmutableMap.copyOf(ra);
        this.optionalArgs = ImmutableMap.copyOf(oa);
        this.extra = ImmutableMap.copyOf(extra);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(name, service.name) &&
                Objects.equals(website, service.website) &&
                Objects.equals(requiredArgs, service.requiredArgs) &&
                Objects.equals(optionalArgs, service.optionalArgs) &&
                Objects.equals(extra, service.extra);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, website, requiredArgs, optionalArgs, extra);
    }

    @Override
    public String toString()
    {
        return "Service{" +
                "name='" + name + '\'' +
                ", website='" + website + '\'' +
                ", requiredArgs=" + requiredArgs +
                ", optionalArgs=" + optionalArgs +
                ", extra=" + extra +
                '}';
    }

    static class Gson implements JsonDeserializer<Service[]>
    {
        @Override
        public Service[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject root = json.getAsJsonObject();
            Service[] out = new Service[root.entrySet().size()];
            int i = 0;
            for (Map.Entry<String, JsonElement> entry : root.entrySet())
            {
                JsonObject service = entry.getValue().getAsJsonObject();
                String website = context.deserialize(service.remove("website"), String.class);
                Map<String, String> info = context.deserialize(service.remove("info"), TYPE_MAP_STRING_STRING);
                Map<String, String> required_args = context.deserialize(service.remove("required_args"), TYPE_MAP_STRING_STRING);
                Map<String, String> optional_args = context.deserialize(service.remove("optional_args"), TYPE_MAP_STRING_STRING);
                Map<String, String> extra = context.deserialize(service, TYPE_MAP_STRING_STRING);
                out[i++] = new Service(entry.getKey(), website, info, required_args, optional_args, extra);
            }
            return out;
        }
    }
}
