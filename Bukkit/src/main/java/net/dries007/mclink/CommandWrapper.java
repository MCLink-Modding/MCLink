/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ICommand;
import net.dries007.mclink.binding.IMinecraft;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * @author Dries007
 */
public class CommandWrapper implements TabExecutor
{
    private final Plugin plugin;
    private final IMinecraft mc;
    private final ICommand c;

    CommandWrapper(Plugin plugin, IMinecraft mc, ICommand c)
    {
        this.plugin = plugin;
        this.mc = mc;
        this.c = c;
    }

    /**
     * Executes the given command, returning its success
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!c.getName().equalsIgnoreCase(command.getName()) || !sender.isOp()) return false;
        try
        {
            c.run(mc, new SenderWrapper(sender), args);
            return true;
        }
        catch (ICommand.CommandException error)
        {
            sender.sendMessage(FormatCode.RED + "Caught error:");
            Throwable e = error;
            do sender.sendMessage(FormatCode.RED + e.getClass().getSimpleName() + ": " + e.getMessage());
            while ((e = error.getCause()) != null);
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (!c.getName().equalsIgnoreCase(command.getName()) || !sender.isOp()) return null;
        return c.getTabOptions(new SenderWrapper(sender), args);
    }
}
