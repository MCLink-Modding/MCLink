/*
 * Copyright (c) 2017 - 2019 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ILogger;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dries007
 */
public class Log4jLogger implements ILogger
{
    private final Logger logger;

    public Log4jLogger(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void debug(String msg)
    {
        logger.debug(msg);
    }

    @Override
    public void info(String msg)
    {
        logger.info(msg);
    }

    @Override
    public void warn(String msg)
    {
        logger.warn(msg);
    }

    @Override
    public void error(String msg)
    {
        logger.error(msg);
    }

    @Override
    public void catching(Throwable error)
    {
        logger.catching(error);
    }

    @Override
    public boolean logDebug()
    {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean logInfo()
    {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean logWarm()
    {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean logError()
    {
        return logger.isErrorEnabled();
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

    @Override
    public void sendMessageAsync(String message)
    {
        sendMessage(message);
    }

    @Override
    public void sendMessageAsync(String message, FormatCode formatCode)
    {
        sendMessage(message, formatCode);
    }
}
