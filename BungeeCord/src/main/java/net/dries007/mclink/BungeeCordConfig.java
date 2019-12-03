/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.api.APIException;
import net.dries007.mclink.api.Constants;
import net.dries007.mclink.common.CommonConfig;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * @author Dries007
 */
public class BungeeCordConfig extends CommonConfig
{
    private static final String GENERAL = "general";
    private static final String SERVICES = "services";

    private final Plugin plugin;
    private Configuration general;
    private Configuration services;
    private static final char sep = '.';
    private StringBuilder header;

    BungeeCordConfig(Plugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    protected String getString(String key, String def, String comment)
    {
        header.append(GENERAL).append(sep).append(key).append(": ").append(comment).append('\n');
        return general.getString(key, def);
    }

    @Override
    protected boolean getBoolean(String key, boolean def, String comment)
    {
        header.append(GENERAL).append(sep).append(key).append(": ").append(comment).append('\n');
        return general.getBoolean(key, def);
    }

    @Override
    public boolean setClosed(boolean closed)
    {
        general.set("closed", closed);
        return super.setClosed(closed);
    }

    @Override
    protected int getInt(String key, int def, int min, int max, String comment)
    {
        header.append(GENERAL).append(sep).append(key).append(": ").append(comment).append('\n');
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
        return new HashSet<>(services.getKeys());
    }

    @Override
    @Nullable
    public String reload() throws ConfigException, IOException, APIException
    {
        final File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists())
        {
            plugin.getLogger().info("Create " + dataFolder.getPath() + " : " + dataFolder.mkdir());
        }
        final File configFile = new File(dataFolder, "mclink.yml");
        if (!configFile.exists())
        {
            try (InputStream in = plugin.getResourceAsStream("mclink.yml"))
            {
                Files.copy(in, configFile.toPath());
            }
        }

        final Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        general = configuration.getSection(GENERAL);
        services = configuration.getSection(SERVICES);

        header = new StringBuilder(Constants.MODNAME + "\n==========\n");
        String msg = super.reload();
        header = null;

        return msg;
    }
}
