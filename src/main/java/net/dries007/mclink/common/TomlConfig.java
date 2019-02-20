/*
 * Copyright (c) 2017 - 2019 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.collect.Table;
import net.dries007.mclink.api.API;
import net.dries007.mclink.api.APIException;
import net.dries007.mclink.api.Constants;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author Dries007
 */
public class TomlConfig
{
    private final FileConfig cfg;

    public TomlConfig(Path folder)
    {
        cfg = FileConfig.builder(folder.resolve(Constants.MODID + ".toml"))
                .autosave()
                .sync()
                .concurrent()
                .defaultResource("/mclink-default.toml")
                .build();
    }

    public TomlConfig(File folder)
    {
        this(folder.toPath());
    }

    public @Nullable String reload() throws IOException, APIException
    {
        cfg.load();

        // todo

        API.setTimeout(cfg.getInt("mclink.timeout") * 1000);

        throw new RuntimeException(this.toString());

        //return null;
    }

    public String getKickMessage()
    {
        return cfg.get("mclink.messages.kick");
    }

    public String getErrorMessage()
    {
        return cfg.get("mclink.messages.error");
    }

    public String getClosedMessage()
    {
        return cfg.get("mclink.messages.closed");
    }

    public boolean isClosed()
    {
        return cfg.get("mclink.closed");
    }

    public boolean setClosed(boolean closed)
    {
        return cfg.<Boolean>set("mclink.closed", closed) != closed;
    }

    public boolean isFreeToJoin()
    {
        return cfg.get("mclink.freeToJoin");
    }

    public boolean isShowStatus()
    {
        return cfg.get("mclink.showStatus");
    }

    public Table<String, String, Map<String, ?>> getServices()
    {
        // todo re-add @NotNull
        // todo implement
        // todo maybe it's best to make a custom data structure? Recreating this entire thing every request seems wasteful
        return null;
    }

    public String getMessage(MCLinkCommon.Marker marker)
    {
        switch (marker)
        {
            default:
                return null;
            case DENIED_NO_AUTH:
                return getKickMessage();
            case DENIED_ERROR:
                return getErrorMessage();
            case DENIED_CLOSED:
                return getClosedMessage();
        }
    }

    @Override
    public String toString()
    {
        return "TomlConfig{" +
//                "cfg=" + cfg +
                ", file='" + cfg.getFile() + '\'' +
                ", kickMessage='" + getKickMessage() + '\'' +
                ", errorMessage='" + getErrorMessage() + '\'' +
                ", closedMessage='" + getClosedMessage() + '\'' +
                ", closed=" + isClosed() +
                ", freeToJoin=" + isFreeToJoin() +
                ", showStatus=" + isShowStatus() +
                ", services=" + getServices() +
                '}';
    }
}
