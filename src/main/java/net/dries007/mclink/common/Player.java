/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.IPlayer;
import net.dries007.mclink.binding.ISender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author Dries007
 */
public class Player implements IPlayer
{
    @Nullable
    private final ISender sender;
    @NotNull
    private final String name;
    @NotNull
    private final UUID uuid;

    public Player(@Nullable ISender sender, @NotNull String name, @NotNull UUID uuid)
    {
        this.sender = sender;
        this.name = name;
        this.uuid = uuid;
    }

    @NotNull
    @Override
    public UUID getUuid()
    {
        return uuid;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "Player{" +
                "name='" + name + '\'' +
                ", uuid=" + uuid +
                '}';
    }

    @Override
    public void sendMessage(String message)
    {
        if (sender != null) sender.sendMessage(message);
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        if (sender != null) sender.sendMessage(message, formatCode);
    }
}




