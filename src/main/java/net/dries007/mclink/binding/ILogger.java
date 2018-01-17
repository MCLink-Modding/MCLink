/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.binding;

import java.text.MessageFormat;

/**
 * @author Dries007
 */
public interface ILogger extends ISender
{
    void debug(String msg);

    void info(String msg);

    void warn(String msg);

    void error(String msg);

    void catching(Throwable error);

    default boolean logDebug() { return true; }

    default boolean logInfo() { return true; }

    default boolean logWarm() { return true; }

    default boolean logError() { return true; }

    default void debug(String msg, Object... objects)
    {
        if (logDebug())
        {
            debug(MessageFormat.format(msg, objects));
        }
    }

    default void info(String msg, Object... objects)
    {
        if (logInfo())
        {
            info(MessageFormat.format(msg, objects));
        }
    }

    default void warn(String msg, Object... objects)
    {
        if (logWarm())
        {
            warn(MessageFormat.format(msg, objects));
        }
    }

    default void error(String msg, Object... objects)
    {
        if (logError())
        {
            error(MessageFormat.format(msg, objects));
        }
    }
}
