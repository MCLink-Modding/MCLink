/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import com.google.common.collect.ImmutableList;
import net.dries007.mclink.api.Constants;
import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ICommand;
import net.dries007.mclink.binding.IMinecraft;
import net.dries007.mclink.binding.ISender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Dries007
 */
public class MCLinkCommand implements ICommand
{
    @NotNull
    @Override
    public String getName()
    {
        return Constants.MODID;
    }

    @NotNull
    @Override
    public String getUsage(ISender sender)
    {
        return Constants.MODID + " [close|open|reloadConfigAsync|status]";
    }

    @Override
    public void run(@NotNull IMinecraft mc, @NotNull ISender sender, @NotNull String[] args) throws CommandException
    {
        if (args.length == 0)
        {
            sender.sendMessage("Subcommands:", FormatCode.AQUA);
            sender.sendMessage("- close: Do not let anyone join via MCLink. Ops and manually whitelisted players can still join.");
            sender.sendMessage("- open: Let people join via MCLink again.");
            sender.sendMessage("- reloadConfigAsync: Reload all configs & API status. May take a few moments.");
            sender.sendMessage("- status: Get current open/closed status & any API messages.");
            return;
        }

        switch (args[0].toLowerCase())
        {
            case "close":
                if (!mc.close()) sender.sendMessage("Server already closed.", FormatCode.YELLOW);
                break;
            case "open":
                if (!mc.open()) sender.sendMessage("Server already open.", FormatCode.YELLOW);
                break;
            case "reloadConfigAsync":
                mc.reloadAPIStatusAsync(sender, new ThreadStartConsumer("reloadAPIStatusAsync"));
                mc.reloadConfigAsync(sender);
                break;
            case "status":
                mc.reloadAPIStatusAsync(sender, new ThreadStartConsumer("reloadAPIStatusAsync"));
                sender.sendMessage("The server is currently " + (mc.getConfig().isClosed() ? "CLOSED" : "OPENED"));
                break;
            default:
                throw new CommandException("Subcommand not found.");
        }
    }

    @NotNull
    @Override
    public List<String> getTabOptions(@NotNull ISender sender, @NotNull String[] args)
    {
        if (args.length == 1) return ImmutableList.of("close", "open", "reloadConfigAsync", "status");
        return ImmutableList.of();
    }
}
