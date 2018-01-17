/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import net.dries007.mclink.api.*;
import net.dries007.mclink.binding.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Dries007
 */
public abstract class MCLinkCommon implements IMinecraft
{
    private final Cache<IPlayer, ImmutableCollection<Authentication>> CACHE = CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();
    private final ConcurrentHashMap<IPlayer, Marker> UUID_STATUS_MAP = new ConcurrentHashMap<>();

    private ILogger logger = null;
    private IConfig config = null;

    private String modVersion = "";
    private String mcVersion = "";
    private Side side = Side.UNKNOWN;

    private Status latestStatus;

    protected abstract void kickAsync(IPlayer IPlayer, String msg);

    protected abstract IPlayer resolveUUID(UUID uuid);

    protected void init() throws IConfig.ConfigException, IOException, APIException
    {
        API.setMetaData(getModVersion(), getMcVersion());
        String warnings = config.reload();
        if (!Strings.isNullOrEmpty(warnings)) logger.warn(warnings);
    }

    protected void deInit()
    {
        invalidateCache();
        UUID_STATUS_MAP.clear();
    }

    protected void invalidateCache()
    {
        CACHE.invalidateAll();
        CACHE.cleanUp();
    }

    protected void registerCommands(Consumer<ICommand> register)
    {
        register.accept(new MCLinkCommand());
    }

    protected void login(IPlayer player, boolean sendStatus)
    {
        // If the map has it set to DENIED already, the lookup finished before we got here and we can kick the player.
        // If the marker was something else, we remove the marker to the async thread knows it needs to kick the player itself.
        switch (UUID_STATUS_MAP.remove(player))
        {
            case ALLOWED:
                break;
            case IN_PROGRESS:
                break;
            case DENIED_NO_AUTH:
                kickAsync(player, config.getKickMessage());
                return;
            case DENIED_ERROR:
                kickAsync(player, config.getErrorMessage());
                return;
            case DENIED_CLOSED:
                kickAsync(player, config.getClosedMessage());
                return;
        }
        if (sendStatus && latestStatus != null && config.isShowStatus())
        {
            if (Constants.API_VERSION < latestStatus.apiVersion)
                player.sendMessage("[MCLink] API version outdated. Please update ASAP");
            if (latestStatus.message != null)
                player.sendMessage("[MCLink] API status message: " + latestStatus.message);
        }
    }

    @Override
    public void reloadConfigAsync(@NotNull ISender sender)
    {
        new Thread(() -> {
            try
            {
                String msg = config.reload();
                if (Strings.isNullOrEmpty(msg))
                {
                    logger.info("Config reloaded by {0}. All OK.", sender.getName());
                    sender.sendMessage("Reloaded!", FormatCode.GREEN);
                }
                else
                {
                    logger.warn("Config reloaded by {0}, with warnings: {1}", sender, msg);
                    sender.sendMessage("Reloaded with warning:", FormatCode.YELLOW);
                    sender.sendMessage(msg, FormatCode.YELLOW);
                }
            }
            catch (Throwable e)
            {
                logger.error("Config reloaded by {0}, with errors: {1}", sender, e.getMessage());
                logger.catching(e);
                sender.sendMessage("Error while reloading! Exceptions:", FormatCode.RED);
                do
                    sender.sendMessage(MessageFormat.format("{0}: {1}", e.getClass().getSimpleName(), e.getMessage()), FormatCode.RED);
                while ((e = e.getCause()) != null);
            }
        }, Constants.MODNAME + "-reloadConfigAsync").start();
    }

    @Override
    public boolean open()
    {
        boolean b = config.setClosed(false);
        if (b) this.sendMessage("[" + Constants.MODNAME + "] The server is now open!");
        return b;
    }

    @Override
    public boolean close()
    {
        boolean b = config.setClosed(false);
        if (b) this.sendMessage("[" + Constants.MODNAME + "] The server is now closed!");
        return b;
    }

    @Override
    public void reloadAPIStatusAsync(@NotNull ISender sender)
    {
        new Thread(() -> {
            try
            {
                latestStatus = API.getStatus();

                if (latestStatus.message != null) logger.warn(latestStatus.message);

                if (Constants.API_VERSION == latestStatus.apiVersion)
                {
                    sender.sendMessage("API version outdated. Please update ASAP", FormatCode.GREEN);
                }
                else
                {
                    logger.warn("API version outdated. Please update ASAP");
                    sender.sendMessage("API version outdated. Please update ASAP", FormatCode.YELLOW);
                }

                if (latestStatus.message == null)
                {
                    sender.sendMessage("No API status message.", FormatCode.GREEN);
                }
                else
                {
                    logger.warn("[API STATUS] {}", latestStatus.message);
                    sender.sendMessage("API status message:", FormatCode.YELLOW);
                    sender.sendMessage(latestStatus.message, FormatCode.YELLOW);
                }
            }
            catch (Throwable e)
            {
                logger.error("Config reloaded by {0}, with errors: {1}", sender, e.getMessage());
                logger.catching(e);
                sender.sendMessage("Error while reloading! Exceptions:", FormatCode.RED);
                do
                    sender.sendMessage(MessageFormat.format("{0}: {1}", e.getClass().getSimpleName(), e.getMessage()), FormatCode.RED);
                while ((e = e.getCause()) != null);
            }
        }, Constants.MODNAME + "-reloadAPIStatusAsync").start();
    }

    @Override
    public void checkAuthStatusAsync(@NotNull IPlayer IPlayer, boolean oped, boolean whitelisted)
    {
        if (oped) // not cached, bypassed server closed latestStatus
        {
            logger.info("IPlayer {0} was authorized because they are on the OP list.", IPlayer);
            UUID_STATUS_MAP.put(IPlayer, Marker.ALLOWED);
            return;
        }

        if (config.isClosed())
        {
            logger.info("IPlayer {0} denied access because server is closed.", IPlayer);
            UUID_STATUS_MAP.put(IPlayer, Marker.DENIED_CLOSED);
            return;
        }

        if (whitelisted) // not cached
        {
            logger.info("IPlayer {0} was authorized because they are on the whitelist.", IPlayer);
            UUID_STATUS_MAP.put(IPlayer, Marker.ALLOWED);
            return;
        }

        if (CACHE.getIfPresent(IPlayer) != null)
        {
            logger.info("IPlayer {0} was authorized cached auth entries.", IPlayer);
            UUID_STATUS_MAP.put(IPlayer, Marker.ALLOWED);
            return;
        }

        UUID_STATUS_MAP.put(IPlayer, Marker.IN_PROGRESS);
        logger.info("IPlayer {0} [{1}] authorization is being checked...", IPlayer);

        new Thread(() -> check(IPlayer), "MCLink" + IPlayer).start();
    }

    private void check(IPlayer IPlayer)
    {
        try
        {
            ImmutableMultimap<UUID, Authentication> map = API.getAuthorization(config.getTokenConfig(), IPlayer.getUuid());
            ImmutableCollection<Authentication> auth = map.get(IPlayer.getUuid());
            if (auth.isEmpty())
            {
                logger.info("IPlayer {0} authorization was denied by MCLink.", IPlayer);
                if (UUID_STATUS_MAP.put(IPlayer, Marker.DENIED_NO_AUTH) == null) // was already removed by login
                {
                    UUID_STATUS_MAP.remove(IPlayer); // login event already past, so we don't need this anymore.
                    kickAsync(IPlayer, config.getKickMessage());
                }
            }
            else
            {
                CACHE.put(IPlayer, auth);
                List<String> auths = new ArrayList<>();
                for (Authentication a : auth)
                {
                    IPlayer p = resolveUUID(a.token);
                    auths.add(a.name + " from " + (p == null ? a.token : p) + " with " + a.extra);
                }
                logger.info("IPlayer {0} was authorized by: {1}", IPlayer, auths);
                if (UUID_STATUS_MAP.put(IPlayer, Marker.ALLOWED) == null) // was already removed by login
                {
                    UUID_STATUS_MAP.remove(IPlayer); // login event already passed, so we don't need this anymore.
                }
            }
        }
        catch (Exception e)
        {
            logger.info("IPlayer {0} was denied due to an exception.", IPlayer);
            logger.catching(e);

            if (UUID_STATUS_MAP.put(IPlayer, Marker.DENIED_ERROR) == null) // was already removed by login
            {
                UUID_STATUS_MAP.remove(IPlayer); // login event already past, so we don't need this anymore.
                kickAsync(IPlayer, config.getErrorMessage());
            }
        }
    }

    @NotNull
    @Override
    public final String getModVersion()
    {
        return modVersion;
    }

    protected void setModVersion(String modVersion)
    {
        this.modVersion = modVersion;
    }

    @NotNull
    public final String getMcVersion()
    {
        return mcVersion;
    }

    protected void setMcVersion(String mcVersion)
    {
        this.mcVersion = mcVersion;
    }

    @Override
    public final ILogger getLogger()
    {
        return logger;
    }

    protected void setLogger(ILogger logger)
    {
        this.logger = logger;
    }

    @Override
    public final IConfig getConfig()
    {
        return config;
    }

    protected void setConfig(IConfig config)
    {
        this.config = config;
    }

    public final Side getSide()
    {
        return side;
    }

    protected void setSide(Side side)
    {
        this.side = side;
    }

    @NotNull
    @Override
    public String getName()
    {
        return "SERVER";
    }

    @Override
    public String toString()
    {
        return "MCLinkCommon{" +
                "API_VERSION=" + Constants.API_VERSION +
                ", logger=" + logger +
                ", config=" + config +
                ", modVersion='" + modVersion + '\'' +
                ", mcVersion='" + mcVersion + '\'' +
                ", side=" + side +
                ", latestStatus=" + latestStatus +
                '}';
    }

    public enum Marker
    {
        ALLOWED, IN_PROGRESS, DENIED_NO_AUTH, DENIED_ERROR, DENIED_CLOSED
    }

    public enum Side
    {
        UNKNOWN, SERVER, CLIENT
    }
}
