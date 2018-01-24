/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ILogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Dries007
 */
public class JavaLogger implements ILogger
{
    private final Logger logger;

    public JavaLogger(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void debug(String msg)
    {
        logger.log(Level.FINE, msg);
    }

    @Override
    public void info(String msg)
    {
        logger.log(Level.INFO, msg);
    }

    @Override
    public void warn(String msg)
    {
        logger.log(Level.WARNING, msg);
    }

    @Override
    public void error(String msg)
    {
        logger.log(Level.SEVERE, msg);
    }

    @Override
    public void catching(final Throwable error)
    {
        StringBuilder sb = new StringBuilder("Caught error:\n");
        Throwable e = error;
        do sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');
        while ((e = error.getCause()) != null);
        logger.log(Level.SEVERE, sb.toString(), error);
    }

    @NotNull
    @Override
    public String getName()
    {
        return "LOGGER";
    }

    @Override
    public void sendMessage(String message)
    {
        info(message);
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        info(message);
    }
}
