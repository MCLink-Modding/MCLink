/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.binding;

import com.google.common.collect.Table;
import net.dries007.mclink.api.APIException;
import net.dries007.mclink.common.MCLinkCommon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author Dries007
 */
public interface IConfig
{
    /**
     * @return null if everything was OK, string on warning, exception on error
     */
    @Nullable
    String reload() throws ConfigException, IOException, APIException;

    @NotNull
    String getKickMessage();

    @NotNull
    String getErrorMessage();

    @NotNull
    String getClosedMessage();

    boolean isClosed();

    /**
     * @return true IF changed ELSE false
     */
    boolean setClosed(boolean closed);

    boolean isFreeToJoin();

    boolean isShowStatus();

    @NotNull
    Table<String, String, List<String>> getTokenConfig();

    String getMessage(MCLinkCommon.Marker marker);

    class ConfigException extends Exception {}
}
