/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ISender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
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
        sender.sendMessage(new TextComponent(message));
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        sender.sendMessage(new ComponentBuilder(message).color(ChatColor.getByChar(formatCode.c)).create());
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
