/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.ICommand;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Dries007
 */
public class CommandWrapper extends CommandBase
{
    private final MCLink mcLink;
    private final ICommand cmd;

    CommandWrapper(MCLink mcLink, ICommand cmd)
    {
        this.mcLink = mcLink;
        this.cmd = cmd;
    }

    @Override
    @NotNull
    public String getCommandName()
    {
        return cmd.getName();
    }

    @Override
    @NotNull
    public String getCommandUsage(@NotNull ICommandSender sender)
    {
        return cmd.getUsage(new SenderWrapper(sender));
    }

    @Override
    public void processCommand(@NotNull ICommandSender sender, @NotNull String[] args)
    {
        try
        {
            cmd.run(mcLink, new SenderWrapper(sender), args);
        }
        catch (ICommand.CommandException e)
        {
            throw new CommandException(e.getMessage());
        }
    }

    @Override
    @NotNull
    public List addTabCompletionOptions(@NotNull ICommandSender sender, String[] args)
    {
        return getListOfStringsFromIterableMatchingLastWord(args, cmd.getTabOptions(new SenderWrapper(sender), args));
    }

    @Override
    public String toString()
    {
        return "CommandWrapper{" + cmd.getName() + "}";
    }
}
