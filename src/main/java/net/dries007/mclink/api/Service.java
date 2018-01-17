/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import static net.dries007.mclink.api.Constants.TYPE_MAP_STRING_STRING;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Service
{
    @NotNull
    public final String website;
    @NotNull
    public final ImmutableMap<String, String> info;
    @NotNull
    public final ImmutableMap<String, String> requiredArgs;
    @NotNull
    public final ImmutableMap<String, String> optionalArgs;
    @NotNull
    public final ImmutableMap<String, String> extra;

    private Service(@NotNull String website, @NotNull Map<String, String> info, @NotNull Map<String, String> ra, @NotNull Map<String, String> oa, @NotNull Map<String, String> extra)
    {
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
        return Objects.equals(website, service.website) &&
                Objects.equals(requiredArgs, service.requiredArgs) &&
                Objects.equals(optionalArgs, service.optionalArgs) &&
                Objects.equals(extra, service.extra);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(website, requiredArgs, optionalArgs, extra);
    }

    @Override
    public String toString()
    {
        return "Service{" +
                "website='" + website + '\'' +
                ", requiredArgs=" + requiredArgs +
                ", optionalArgs=" + optionalArgs +
                ", extra=" + extra +
                '}';
    }

    public String getConfigCommentString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Website: ").append(website).append('\n');
        sb.append("Arguments must be in this exact order. Use an empty string (\"\") to skip an argument.\n");
        sb.append("Only put in values, not the names of the arguments.\n");
        sb.append("Required arguments:").append('\n');
        sb.append(" - TOKEN: Your api token.").append('\n');
        for (Map.Entry<String, String> e : requiredArgs.entrySet()) sb.append(" - ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        sb.append("Optional arguments:").append('\n');
        for (Map.Entry<String, String> e : optionalArgs.entrySet()) sb.append(" - ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        return sb.toString();
    }

    static class Gson implements JsonDeserializer<Service>
    {
        @Override
        public Service deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject root = json.getAsJsonObject();
            String website = context.deserialize(root.remove("website"), String.class);
            Map<String, String> info = context.deserialize(root.remove("info"), TYPE_MAP_STRING_STRING);
            Map<String, String> required_args = context.deserialize(root.remove("required_args"), TYPE_MAP_STRING_STRING);
            Map<String, String> optional_args = context.deserialize(root.remove("optional_args"), TYPE_MAP_STRING_STRING);
            Map<String, String> extra = context.deserialize(root, TYPE_MAP_STRING_STRING);
            return new Service(website, info, required_args, optional_args, extra);
        }
    }
}
