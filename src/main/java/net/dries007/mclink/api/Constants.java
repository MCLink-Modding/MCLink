/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.api;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Some constants used across versions
 *
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Constants
{
    public static final Charset UTF8 = StandardCharsets.UTF_8;

    public static final String MODID = "mclink";
    public static final String MODNAME = "MCLink";
    public static final int API_VERSION = 1;

    public static final boolean STAGING = Boolean.parseBoolean(System.getProperty("mclink.staging", Boolean.FALSE.toString()));
    public static final String BASE_URL = STAGING ? "https://staging.mclink.dries007.net" : "https://mclink.dries007.net";
    public static final String API_URL = BASE_URL + "/api/" + API_VERSION + "/";

    public static final Type TYPE_MAP_STRING_STRING = new TypeToken<Map<String, String>>() {}.getType();
}
