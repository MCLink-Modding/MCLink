/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.api;

/**
 * Used when an API request returns an error in an expected place place.
 *
 * @author Dries007
 */
public class APIException extends Exception
{
    public final int status;

    APIException(int status, String description)
    {
        super(description);
        this.status = status;
    }
}
