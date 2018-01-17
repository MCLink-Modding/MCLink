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
import java.util.UUID;

import static net.dries007.mclink.api.Constants.TYPE_MAP_STRING_STRING;

/**
 * @author Dries007
 */
public class Authentication
{
    @NotNull
    public final String name;
    @NotNull
    public final UUID token;
    @NotNull
    public final ImmutableMap<String, String> extra;

    Authentication(@NotNull String name, @NotNull UUID token, @NotNull Map<String, String> extra)
    {
        this.name = name;
        this.token = token;
        this.extra = ImmutableMap.copyOf(extra);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Authentication that = (Authentication) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(token, that.token) &&
                Objects.equals(extra, that.extra);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, token, extra);
    }

    @Override
    public String toString()
    {
        return "Authentication{" +
                "name='" + name + '\'' +
                ", token=" + token +
                ", extra=" + extra +
                '}';
    }

    static class Gson implements JsonDeserializer<Authentication>
    {
        @Override
        public Authentication deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject root = json.getAsJsonObject();
            String name = context.deserialize(root.remove("name"), String.class);
            UUID token = context.deserialize(root.remove("token"), UUID.class);
            Map<String, String> extra = context.deserialize(root, TYPE_MAP_STRING_STRING);
            return new Authentication(name, token, extra);
        }
    }
}
