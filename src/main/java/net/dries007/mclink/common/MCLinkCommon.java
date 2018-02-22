/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import net.dries007.mclink.api.*;
import net.dries007.mclink.binding.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final Cache<UUID, ImmutableCollection<Authentication>> CACHE = CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();
    private final ConcurrentHashMap<UUID, Marker> UUID_STATUS_MAP = new ConcurrentHashMap<>();

    private ILogger logger = null;
    private IConfig config = null;

    private String modVersion = "";
    private String mcVersion = "";
    private Side side = Side.UNKNOWN;

    private Status latestStatus;
    private String branding;

    protected abstract void authCompleteAsync(IPlayer player, ImmutableCollection<Authentication> authentications, Marker result);

    @Nullable
    protected abstract String nameFromUUID(UUID uuid);

    public void init() throws IConfig.ConfigException, IOException, APIException
    {
        API.setMetaData(getBranding(), getModVersion(), getMcVersion());
        String warnings = config.reload();
        if (!Strings.isNullOrEmpty(warnings)) logger.warn(warnings);
    }

    public void deInit()
    {
        invalidateCache();
        UUID_STATUS_MAP.clear();
    }

    private void invalidateCache()
    {
        CACHE.invalidateAll();
        CACHE.cleanUp();
    }

    public void registerCommands(Consumer<ICommand> register)
    {
        register.accept(new MCLinkCommand());
    }

    public void login(IPlayer player, boolean sendStatus)
    {
        // If the map has it set to DENIED already, the lookup finished before we got here and we can kick the player.
        // If the marker was something else, we remove the marker to the async thread knows it needs to kick the player itself.
        Marker m = UUID_STATUS_MAP.remove(player.getUuid());
        switch (m)
        {
            case ALLOWED:
                break;
            case IN_PROGRESS:
                break;
            default:
                authCompleteAsync(player, ImmutableList.of(), m);
                break;
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
                invalidateCache();
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
        boolean b = config.setClosed(true);
        if (b) this.sendMessage("[" + Constants.MODNAME + "] The server is now closed!");
        return b;
    }

    @Override
    public void reloadAPIStatusAsync(@NotNull ISender sender, Consumer<Runnable> runner)
    {
        runner.accept(() -> {
            try
            {
                latestStatus = API.getStatus();

                if (Constants.API_VERSION == latestStatus.apiVersion)
                {
                    sender.sendMessage("API version up to date.", FormatCode.GREEN);
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
        });
    }

    @Override
    public void checkAuthStatusAsync(@NotNull IPlayer player, boolean oped, boolean whitelisted, Consumer<Runnable> runner)
    {
        if (oped) // not cached, bypassed server closed latestStatus
        {
            logger.info("Player {0} was authorized because they are on the OP list.", player);
            UUID_STATUS_MAP.put(player.getUuid(), Marker.ALLOWED);
            // Want to check status anyway, so we can maybe fire off an Event
            logger.info("Player {0} authorization is being checked anyway...", player);
            // Set free to true to disable kicking
            runner.accept(() -> check(player, true));
            return;
        }

        if (config.isClosed())
        {
            logger.info("Player {0} denied access because server is closed.", player);
            UUID_STATUS_MAP.put(player.getUuid(), Marker.DENIED_CLOSED);
            return;
        }

        if (config.isFreeToJoin()) {
            logger.info("Player {0} was authorized because the server is allowing all players", player);
            UUID_STATUS_MAP.put(player.getUuid(), Marker.ALLOWED);
            // Want to check status anyway, so we can maybe fire off an Event
            logger.info("Player {0} authorization is being checked anyway...", player);
            runner.accept(() -> check(player, true));
            return;
        }

        if (whitelisted) // not cached
        {
            logger.info("Player {0} was authorized because they are on the whitelist.", player);
            UUID_STATUS_MAP.put(player.getUuid(), Marker.ALLOWED);
            // Want to check status anyway, so we can maybe fire off an Event
            logger.info("Player {0} authorization is being checked anyway...", player);
            // Set free to true to disable kicking
            runner.accept(() -> check(player, true));
            return;
        }

        if (CACHE.getIfPresent(player) != null)
        {
            logger.info("Player {0} was authorized cached auth entries.", player);
            UUID_STATUS_MAP.put(player.getUuid(), Marker.ALLOWED);
            return;
        }

        UUID_STATUS_MAP.put(player.getUuid(), Marker.IN_PROGRESS);
        logger.info("Player {0} authorization is being checked...", player);

        // At this point, we're a non-free server awaiting auth. Free is false.
        runner.accept(() -> check(player, false));
    }

    private void check(IPlayer player,boolean free)
    {
        try
        {
            ImmutableMultimap<UUID, Authentication> map = API.getAuthorization(config.getTokenConfig(), player.getUuid());
            ImmutableCollection<Authentication> auth = map.get(player.getUuid());
            if (auth.isEmpty() & !free)
            {
                logger.info("Player {0} authorization was denied by MCLink.", player);
                if (UUID_STATUS_MAP.put(player.getUuid(), Marker.DENIED_NO_AUTH) == null) // was already removed by login
                {
                    UUID_STATUS_MAP.remove(player.getUuid()); // login event already past, so we don't need this anymore.
                    authCompleteAsync(player, ImmutableList.of(), Marker.DENIED_NO_AUTH);
                }
            }
            else
            {
                CACHE.put(player.getUuid(), auth);
                List<String> auths = new ArrayList<>();
                for (Authentication a : auth)
                {
                    String name = nameFromUUID(a.token);
                    auths.add(a.name + " from " + (name == null ? a.token : name + "[" + a.token + "]") + " with " + a.extra);
                }
                logger.info("Player {0} was authorized by: {1}", player, auths);
                // send the authentications to the mod in question
                authCompleteAsync(player, auth, Marker.ALLOWED);
                if (UUID_STATUS_MAP.put(player.getUuid(), Marker.ALLOWED) == null) // was already removed by login
                {
                    UUID_STATUS_MAP.remove(player.getUuid()); // login event already passed, so we don't need this anymore.
                }
            }
        }
        catch (Exception e)
        {
            logger.info("Player {0} was denied due to an exception.", player);
            logger.catching(e);

            if (UUID_STATUS_MAP.put(player.getUuid(), Marker.DENIED_ERROR) == null) // was already removed by login
            {
                UUID_STATUS_MAP.remove(player.getUuid()); // login event already past, so we don't need this anymore.
                authCompleteAsync(player, ImmutableList.of(), Marker.DENIED_ERROR);
            }
        }
    }

    @Nullable
    @Override
    public final String getModVersion()
    {
        return modVersion;
    }

    public void setModVersion(String modVersion)
    {
        this.modVersion = modVersion;
    }

    @Nullable
    public final String getMcVersion()
    {
        return mcVersion;
    }

    public void setMcVersion(String mcVersion)
    {
        this.mcVersion = mcVersion;
    }

    @Nullable
    @Override
    public String getBranding()
    {
        return branding;
    }

    public void setBranding(String branding)
    {
        this.branding = branding;
    }

    @Override
    public final ILogger getLogger()
    {
        return logger;
    }

    public void setLogger(ILogger logger)
    {
        this.logger = logger;
    }

    @Override
    public final IConfig getConfig()
    {
        return config;
    }

    public void setConfig(IConfig config)
    {
        this.config = config;
    }

    public final Side getSide()
    {
        return side;
    }

    public void setSide(Side side)
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
