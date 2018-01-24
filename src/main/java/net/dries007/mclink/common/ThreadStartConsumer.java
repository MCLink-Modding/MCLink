/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import java.util.function.Consumer;

import static net.dries007.mclink.api.Constants.MODNAME;

/**
 * @author Dries007
 */
public class ThreadStartConsumer implements Consumer<Runnable>
{
    public final String name;

    public ThreadStartConsumer(String name)
    {
        this.name = name;
    }

    @Override
    public void accept(Runnable runnable)
    {
        new Thread(runnable, MODNAME + "-" + name).start();
    }
}
