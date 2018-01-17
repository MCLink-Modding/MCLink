/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.binding;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Dries007
 */
public interface IPlayer extends ISender
{
    @NotNull UUID getUuid();

    @NotNull String getName();
}
