/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.api;

import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Status
{
    public final int apiVersion;
    @Nullable
    public final String message;
    @Nullable
    public final String serverAddress;
    @Nullable
    public final String serverVersion;

    private Status(int apiVersion, @Nullable String message, @Nullable String serverAddress, @Nullable String serverVersion)
    {
        this.apiVersion = apiVersion;
        this.message = message;
        this.serverAddress = serverAddress;
        this.serverVersion = serverVersion;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Status status = (Status) o;
        return apiVersion == status.apiVersion &&
                Objects.equals(message, status.message) &&
                Objects.equals(serverAddress, status.serverAddress) &&
                Objects.equals(serverVersion, status.serverVersion);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(apiVersion, message, serverAddress, serverVersion);
    }

    @Override
    public String toString()
    {
        return "Status{" +
                "apiVersion=" + apiVersion +
                ", message='" + message + '\'' +
                ", serverAddress='" + serverAddress + '\'' +
                ", serverVersion='" + serverVersion + '\'' +
                '}';
    }

    static class Gson implements JsonDeserializer<Status>
    {
        @Override
        public Status deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject root = json.getAsJsonObject();
            int apiVersion = context.deserialize(root.get("api_version"), int.class);
            String message = context.deserialize(root.get("message"), String.class);
            String serverAddress = null;
            String serverVersion = null;
            JsonElement server = root.get("server");
            if (!server.isJsonNull())
            {
                JsonObject serverO = server.getAsJsonObject();
                serverAddress = context.deserialize(serverO.get("address"), String.class);
                serverVersion = context.deserialize(serverO.get("version"), String.class);
            }
            return new Status(apiVersion, message, serverAddress, serverVersion);
        }
    }
}
