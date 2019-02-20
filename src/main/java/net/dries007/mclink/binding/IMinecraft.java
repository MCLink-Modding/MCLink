/*
 * Copyright (c) 2017 - 2019 Dries007. All rights reserved
 */

package net.dries007.mclink.binding;

import net.dries007.mclink.common.TomlConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The minecraft (server) representation.
 * Any message send to this should be broadcast to all ops (or equivalent)
 *
 * @author Dries007
 */
public interface IMinecraft extends ISender
{
    @Nullable String getModVersion();

    @Nullable String getMcVersion();

    @Nullable String getBranding();

    ILogger getLogger();

    TomlConfig getConfig();

    boolean open();

    boolean close();

    void reloadConfigAsync(@NotNull ISender sender);

    void checkAuthStatusAsync(@NotNull IPlayer player, boolean oped, boolean whitelisted, Consumer<Runnable> runner);

    void reloadAPIStatusAsync(@NotNull ISender sender, Consumer<Runnable> runner);
}
