/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import net.dries007.mclink.api.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class MCLink extends JavaPlugin implements Listener
{
    private final Cache<UUID, ImmutableCollection<Authentication>> cache = CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

    private String kickMessage;
    private String errorMessage;
    private String closedMessage;
    private boolean closed;

    private Status status;

    private ImmutableMap<String, Service> services;
    private Table<String, String, List<String>> tokenConfig;
    private ImmutableMap<String, UUID> tokenUUIDMap;

    @Override
    public void onEnable()
    {
        try
        {
            reloadAsync();
        }
        catch (IOException | APIException ignored)
        {

        }
        saveConfig();
    }

    private void reloadAsync() throws IOException, APIException
    {
        API.setMetaData(getDescription().getVersion(), Bukkit.getVersion());
        FileConfiguration cfg = getConfig();
        cfg.options().copyDefaults(true);

        kickMessage = cfg.getString("kickMessage", "This is an MCLink protected server. Link your accounts via " + Constants.BASE_URL + " and make sure you are subscribed to the right people.");
        errorMessage = cfg.getString("errorMessage", "MCLink could not verify your status. Please contact a server admin.");
        closedMessage = cfg.getString("closedMessage", "The server is currently closed for the public.");
        closed = cfg.getBoolean("closed", false);
        API.setTimeout(cfg.getInt("timeout", 30) * 1000);

        status = API.getStatus();
        services = API.getServices();
        tokenConfig = HashBasedTable.create();

        ConfigurationSection rootCfg = cfg.getConfigurationSection("tokens");
        if (rootCfg == null) rootCfg = cfg.createSection("tokens");

        for (String token : rootCfg.getKeys(false))
        {
            ConfigurationSection tokenCfg = rootCfg.getConfigurationSection(token);
            for (String serviceName : tokenCfg.getKeys(false))
            {
                if (!services.containsKey(serviceName))
                {
                    getLogger().warning("Service " + serviceName + " (token " + token + ") is not available. Ignoring.");
                    continue;
                }
                List<String> args = tokenCfg.getStringList(serviceName);
                Service s = services.get(serviceName);
                if (tokenConfig.contains(token, serviceName)) die("Your MCLink config contains duplicate API tokens per service. This is not allowed. {0} {1}", serviceName, token);
                if (args.size() < s.requiredArgs.size()) die("Your MCLink config for {0} {1} does not contain enough arguments. See the comment for the required arguments.", serviceName, token);
                if (args.size() > s.requiredArgs.size() + s.optionalArgs.size()) die("Your MCLink config for {0} {1} contains too many arguments. See the comment for the allowed arguments.", serviceName, token);

                tokenConfig.put(token, serviceName, ImmutableList.copyOf(args));
            }
        }

        if (tokenConfig.isEmpty()) die("Your MCLink config is empty, this will result in no-one being allowed on the server!");
        Sets.SetView<String> diff = Sets.difference(tokenConfig.columnKeySet(), services.keySet());
        if (!diff.isEmpty()) die("Your tokenConfig for MCLink contains some services that are not available: {0}", diff);
        tokenUUIDMap = API.getUUIDsFromTokens(tokenConfig.rowKeySet());
        diff = Sets.difference(tokenConfig.rowKeySet(), tokenUUIDMap.keySet());
        if (!diff.isEmpty()) die("Your tokenConfig for MCLink contains some API tokens that are invalid: {0}", diff);

        if (Constants.API_VERSION < status.apiVersion) getLogger().warning("API version outdated. Please update ASAP");
        if (status.message != null) getLogger().warning(status.message);
    }

    private void die(String message, Object... objects)
    {
        getLogger().severe("-=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=-");
        getLogger().log(Level.SEVERE, message, objects);
        getLogger().severe("-=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=-");
        RuntimeException e = new RuntimeException("[MCLink] Fatal error. See log above for more info.");
        e.setStackTrace(new StackTraceElement[0]);
        throw e;
    }

    @Override
    public void onDisable()
    {
        cache.invalidateAll();
        cache.cleanUp();
    }

    @EventHandler
    public void onPlayerLoginEvent(PlayerLoginEvent event)
    {
        if (Constants.API_VERSION < status.apiVersion) getLogger().warning("API version outdated. Please update ASAP");
        if (status.message != null) getLogger().warning(status.message);

        getLogger().info("Doing the checking magic.");
    }
}
