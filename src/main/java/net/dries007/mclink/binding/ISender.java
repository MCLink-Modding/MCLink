/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.binding;

import org.jetbrains.annotations.NotNull;

/**
 * @author Dries007
 */
public interface ISender
{
    @NotNull String getName();

    void sendMessage(String message);

    void sendMessage(String message, FormatCode formatCode);
}
