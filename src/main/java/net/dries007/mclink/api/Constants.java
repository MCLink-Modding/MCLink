package net.dries007.mclink.api;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Some constants used across versions
 *
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Constants
{
    public static final Charset UTF8 = StandardCharsets.UTF_8;

    public static final int TIMEOUT = 5000;

    public static final String MODID = "mclink";
    public static final String MODNAME = "MCLink";
    public static final int API_VERSION = 1;

    public static final String BASE_URL = "https://mclink.dries007.net";
    public static final String API_URL = BASE_URL + "/api/" + API_VERSION + "/";
}
