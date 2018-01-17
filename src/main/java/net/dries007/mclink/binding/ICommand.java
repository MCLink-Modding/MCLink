/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.binding;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Command wrapper
 *
 * @author Dries007
 */
public interface ICommand
{
    @NotNull
    String getName();

    @NotNull
    String getUsage(ISender sender);

    void run(@NotNull IMinecraft mc, @NotNull ISender sender, @NotNull String[] args) throws CommandException;

    @NotNull
    List<String> getTabOptions(@NotNull ISender sender, @NotNull String[] args);

    class CommandException extends Exception
    {
        public CommandException(String message)
        {
            super(message);
        }

        public CommandException(String message, Throwable cause)
        {
            super(message + " " + cause.getMessage());
        }

        public CommandException(Throwable cause)
        {
            super(cause.getMessage());
        }
    }
}
