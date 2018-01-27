/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.api.APIException;
import net.dries007.mclink.api.Constants;
import net.dries007.mclink.common.CommonConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * @author Dries007
 */
public class BukkitConfig extends CommonConfig
{
    private static final String GENERAL = "general";
    private static final String SERVICES = "services";

    private final Plugin plugin;
    private StringBuilder header;
    private char sep;
    private ConfigurationSection general;
    private ConfigurationSection services;

    BukkitConfig(Plugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    protected String getString(String key, String def, String comment)
    {
        header.append(GENERAL).append(sep).append(key).append(": ").append(comment).append('\n');
        general.addDefault(key, def);
        return general.getString(key, def);
    }

    @Override
    protected boolean getBoolean(String key, boolean def, String comment)
    {
        header.append(GENERAL).append(sep).append(key).append(": ").append(comment).append('\n');
        general.addDefault(key, def);
        return general.getBoolean(key, def);
    }

    @Override
    public boolean setClosed(boolean closed)
    {
        general.set("closed", closed);
        plugin.saveConfig();
        return super.setClosed(closed);
    }

    @Override
    protected int getInt(String key, int def, int min, int max, String comment)
    {
        header.append(GENERAL).append(sep).append(key).append(": ").append(comment).append('\n');
        general.addDefault(key, def);
        int i = general.getInt(key, def);
        if (i < min || i > max)
        {
            i = Math.max(Math.min(i, max), min);
            general.set(key, i);
        }
        return i;
    }

    @Override
    protected void addService(String name, String comment)
    {
        header.append(SERVICES).append(sep).append(name).append(": ").append(comment).append('\n');
        services.set(name, new ArrayList<String>());
    }

    @Override
    protected void setServiceComment(String name, String comment)
    {
        header.append('\n').append(SERVICES).append(sep).append(name).append("\n~~~~~~~~~~\n").append(comment).append('\n');
    }

    @Override
    protected void setGlobalCommentServices(String comment)
    {
        header.append("\nSERVICES\n----------\n").append(comment).append('\n');
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String>[] getServiceEntries(String name)
    {
        services.getStringList(name).stream().map(CommonConfig::splitArgumentString).forEach(System.out::println);
        return services.getStringList(name).stream()
                .map(CommonConfig::splitArgumentString)
                .toArray((IntFunction<List<String>[]>) List[]::new);
    }

    @Override
    protected Set<String> getAllDefinedServices()
    {
        return services.getKeys(false);
    }

    @Override
    @Nullable
    public String reload() throws ConfigException, IOException, APIException
    {
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        FileConfigurationOptions options = config.options();
        sep = options.pathSeparator();

        if (!config.isConfigurationSection(GENERAL)) config.createSection(GENERAL);
        if (!config.isConfigurationSection(SERVICES)) config.createSection(SERVICES);

        general = config.getConfigurationSection(GENERAL);
        services = config.getConfigurationSection(SERVICES);

        header = new StringBuilder(Constants.MODNAME + "\n==========\n");

        String msg = super.reload();
        options.copyDefaults(true).copyHeader(true).header(header.toString());

        header = null;

        plugin.saveConfig();
        return msg;
    }
}
