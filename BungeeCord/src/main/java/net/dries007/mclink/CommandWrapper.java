/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.ICommand;
import net.dries007.mclink.binding.IMinecraft;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

/**
 * @author Dries007
 */
public class CommandWrapper extends Command
{
    private final ICommand iCommand;
    private final IMinecraft mc;

    public CommandWrapper(ICommand iCommand, IMinecraft mc)
    {
        super("mclink");
        this.iCommand = iCommand;
        this.mc = mc;
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if (!iCommand.getName().equalsIgnoreCase(sender.getName()) || !sender.hasPermission("bungeecord.command.alert"))
        {
            return;
        }

        try
        {
            iCommand.run(mc, new SenderWrapper(sender), args);
        }
        catch (ICommand.CommandException error)
        {
            sender.sendMessage(new ComponentBuilder("Caught error:").color(ChatColor.RED).create());
            Throwable e = error;
            do
            {
                sender.sendMessage(new ComponentBuilder(e.getClass().getSimpleName() + ": " + e.getMessage()).color(ChatColor.RED).create());
            }
            while ((e = error.getCause()) != null);
        }
    }
}
