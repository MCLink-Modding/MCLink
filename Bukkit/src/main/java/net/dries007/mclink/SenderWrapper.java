/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ISender;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dries007
 */
public class SenderWrapper implements ISender
{
    private final CommandSender sender;

    SenderWrapper(CommandSender sender)
    {
        this.sender = sender;
    }

    @NotNull
    @Override
    public String getName()
    {
        return sender.getName();
    }

    @Override
    public void sendMessage(String message)
    {
        sender.sendMessage(message);
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        sender.sendMessage(FormatCode.FORMAT_CHAR + formatCode.c + message);
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
