/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.binding;

import org.jetbrains.annotations.NotNull;

/**
 * The minecraft (server) representation.
 * Any message send to this should be broadcast to all ops (or equivalent)
 *
 * @author Dries007
 */
public interface IMinecraft extends ISender
{
    @NotNull
    String getModVersion();

    @NotNull
    String getMcVersion();

    ILogger getLogger();

    IConfig getConfig();

    boolean open();

    boolean close();

    void reloadConfigAsync(@NotNull ISender sender);

    void checkAuthStatusAsync(@NotNull IPlayer player, boolean oped, boolean whitelisted);

    void reloadAPIStatusAsync(@NotNull ISender sender);
}
