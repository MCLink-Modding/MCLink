/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.api.APIException;
import net.dries007.mclink.common.CommonConfig;
import net.minecraftforge.common.config.Configuration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

/**
 * @author Dries007
 */
@SuppressWarnings("Duplicates")
public class ForgeConfig extends CommonConfig
{
    private static final String CAT = "Services";
    private final Configuration cfg;

    ForgeConfig(File file)
    {
        this.cfg = new Configuration(file, true);
    }

    @Nullable
    @Override
    public String reload() throws ConfigException, IOException, APIException
    {
        cfg.load();
        String msg = super.reload();
        cfg.save();
        return msg;
    }

    @Override
    public boolean setClosed(boolean close)
    {
        cfg.get(CATEGORY_GENERAL, "closed", false).set(close);
        cfg.save();
        return super.setClosed(close);
    }

    @Override
    protected String getString(String key, String def, String comment)
    {
        return cfg.getString(key, CATEGORY_GENERAL, def, comment);
    }

    @Override
    protected boolean getBoolean(String key, boolean def, String comment)
    {
        return cfg.getBoolean(key, CATEGORY_GENERAL, def, comment);
    }

    @Override
    protected int getInt(String key, int def, int min, int max, String comment)
    {
        return cfg.getInt(key, CATEGORY_GENERAL, def, min, max, comment);
    }

    @Override
    protected void addService(String name, String comment)
    {
        cfg.get(CAT, name, new String[0], comment);
    }

    @Override
    protected Set<String> getAllDefinedServices()
    {
        return cfg.getCategory(CAT).keySet();
    }

    @Override
    protected void setGlobalCommentServices(String comment)
    {
        cfg.setCategoryComment(CAT, comment);
    }

    @Override
    protected List<String>[] getServiceEntries(String name)
    {
        return Arrays.stream(cfg.get(CAT, name, new String[0]).getStringList())
                .map(CommonConfig::splitArgumentString)
                .toArray((IntFunction<List<String>[]>) List[]::new);
    }

    @Override
    protected void setServiceComment(String name, String comment)
    {
        cfg.getCategory(name).setComment(comment);
    }
}
