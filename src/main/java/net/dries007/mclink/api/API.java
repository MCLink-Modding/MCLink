/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.api;

import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

import static net.dries007.mclink.api.Constants.STAGING;
import static net.dries007.mclink.api.Constants.TYPE_MAP_STRING_STRING;

/**
 * All API interactions should be done via this class.
 * <p>
 * You should call {see setMetaData} before you call any API function.
 *
 * @author Dries007
 * @see #setMetaData(String, String, String)
 */
@SuppressWarnings("WeakerAccess")
public final class API
{
    public static final URL URL_STATUS = getURL("status");
    public static final URL URL_SERVICES = getURL("services");
    public static final URL URL_UUID = getURL("uuid");
    public static final URL URL_INFO = getURL("info");
    public static final URL URL_AUTHENTICATE = getURL("authenticate");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls()
            .registerTypeAdapter(Service.class, new Service.Gson())
            .registerTypeAdapter(Status.class, new Status.Gson())
            .registerTypeAdapter(Authentication.class, new Authentication.Gson())
            .setPrettyPrinting()
            .create();
    private static String userAgent;
    private static int timeout;

    static
    {
        setMetaData(null, null, null);
        timeout = 30000;
    }

    private API() { throw new AssertionError("No API instances for you!"); }

    /**
     * This functions should be called before any api requests are made.
     * If any parameter is unknown, it can be set to null.
     *
     * @param branding The branding of the environment (Minecraft, Forge, Bukkit, ...)
     * @param mod      The version of the mod/plugin
     * @param mc       The version of Minecraft
     */
    public static void setMetaData(@Nullable String branding, @Nullable String mod, @Nullable String mc)
    {
        StringBuilder sb = new StringBuilder(Constants.MODNAME);
        if (mod != null) sb.append('/').append(mod.replaceAll("[;()\n\r]", ""));
        sb.append(" (APIv").append(Constants.API_VERSION).append("; ");
        if (mc != null) sb.append("MCv").append(mc.replaceAll("[;()\n\r]", "")).append("; ");
        if (branding != null) sb.append(branding.replaceAll("[;()\n\r]", "")).append("; ");
        String os = System.getProperty("os.name") + ' ' + System.getProperty("os.arch") + ' ' + System.getProperty("os.version");
        sb.append(os.replaceAll("[;()\n\r]", ""));
        if (STAGING) sb.append("STAGING");
        userAgent = sb.append(')').toString();
    }

    /**
     * Set the timeouts for this API.
     * Make sure they are log enough because the backend needs to have time to make requests to all services you require.
     *
     * @param timeout in seconds
     */
    public static void setTimeout(int timeout)
    {
        if (timeout < 0) throw new IllegalArgumentException("No negative timeouts! 0 = infinite.");
        API.timeout = timeout;
    }

    /**
     * Request the current server status.
     *
     * @return Status
     */
    @NotNull
    public static Status getStatus() throws IOException, APIException
    {
        return GSON.fromJson(doGetRequest(URL_STATUS), Status.class);
    }

    /**
     * Get a list of services currently fit for use.
     *
     * @return Service[]
     */
    @NotNull
    public static ImmutableMap<String, Service> getServices() throws IOException, APIException
    {
        return ImmutableMap.copyOf(GSON.<Map<String, Service>>fromJson(doGetRequest(URL_SERVICES), new TypeToken<Map<String, Service>>() {}.getType()));
    }

    /**
     * @see #getUUIDsFromTokens(Iterable)
     */
    @NotNull
    public static ImmutableMap<String, UUID> getUUIDsFromTokens(@NotNull String... tokens) throws IOException, APIException
    {
        return getUUIDsFromTokens(Arrays.asList(tokens));
    }

    /**
     * Get a the UUIDs corresponding to one or more tokens
     * Not all requested tokens may be present. If not, the token was invalid.
     *
     * @return Map{String, UUID}
     */
    @NotNull
    public static ImmutableMap<String, UUID> getUUIDsFromTokens(@NotNull Iterable<String> tokens) throws IOException, APIException
    {
        JsonArray in = new JsonArray();
        for (String token : tokens)
        {
            in.add(new JsonPrimitive(token));
        }
        JsonObject root = doPostRequest(URL_UUID, in).getAsJsonObject();
        ImmutableMap.Builder<String, UUID> b = ImmutableMap.builder();
        for (Entry<String, JsonElement> e : root.entrySet())
        {
            if (!e.getValue().isJsonNull())
            {
                b.put(e.getKey(), UUID.fromString(e.getValue().getAsString()));
            }
        }
        return b.build();
    }

    /**
     * @see #getInfo(Iterable)
     */
    @NotNull
    public static ImmutableTable<UUID, String, ImmutableMap<String, String>> getInfo(@NotNull UUID... uuids) throws IOException, APIException
    {
        return getInfo(Arrays.asList(uuids));
    }

    /**
     * Get all information known about a series of uuids.
     * <p>
     * Returns a table with:
     * The rows are the uuids requested (Not all requested uuids may be present)
     * The columns are the names of the services available. (Not all services may be available for all users)
     * The cell values are maps of key:value pairs, as listed in the info received from getServices.
     *
     * @return Table{UUID, String, Map{String, String}
     * @see #getServices()
     */
    @NotNull
    public static ImmutableTable<UUID, String, ImmutableMap<String, String>> getInfo(@NotNull Iterable<UUID> uuids) throws IOException, APIException
    {
        JsonArray in = new JsonArray();
        for (UUID token : uuids)
        {
            in.add(new JsonPrimitive(token.toString()));
        }
        JsonObject root = doPostRequest(URL_INFO, in).getAsJsonObject();
        ImmutableTable.Builder<UUID, String, ImmutableMap<String, String>> b = ImmutableTable.builder();
        for (Entry<String, JsonElement> player : root.entrySet())
        {
            UUID uuid = UUID.fromString(player.getKey());
            if (player.getValue().isJsonNull()) continue;
            for (Entry<String, JsonElement> service : player.getValue().getAsJsonObject().entrySet())
            {
                if (service.getValue().isJsonNull()) continue;
                JsonObject data = service.getValue().getAsJsonObject();
                b.put(uuid, service.getKey(), ImmutableMap.copyOf(GSON.<Map<String, String>>fromJson(data, TYPE_MAP_STRING_STRING)));
            }
        }
        return b.build();
    }

    /**
     * @see #getAuthorization(Table, Iterable)
     */
    @NotNull
    public static ImmutableMultimap<UUID, Authentication> getAuthorization(@NotNull Table<String, String, List<String>> tokenconfig, @NotNull UUID... uuids) throws IOException, APIException
    {
        return getAuthorization(tokenconfig, Arrays.asList(uuids));
    }

    /**
     * Get authorization info for a series of users.
     * If a user is not present in the output map, you must assume they are not authorized.
     * The tokenTable is organizes as follows:
     * The rows are API tokens
     * The columns are Service's names
     * The cell values are a list of Service parameters. If no parameters are to be used, use an empty list.
     */
    @NotNull
    public static ImmutableMultimap<UUID, Authentication> getAuthorization(@NotNull Table<String, String, List<String>> tokenTable, @NotNull Iterable<UUID> uuids) throws IOException, APIException
    {
        JsonObject root = new JsonObject();

        JsonArray uuidArray = new JsonArray();
        for (UUID token : uuids)
        {
            uuidArray.add(new JsonPrimitive(token.toString()));
        }
        root.add("uuids", uuidArray);

        JsonObject tokensObject = new JsonObject();
        for (Entry<String, Map<String, List<String>>> row : tokenTable.rowMap().entrySet())
        {
            JsonObject rowObject = new JsonObject();
            for (Entry<String, List<String>> service : row.getValue().entrySet())
            {
                JsonArray parameterArray = new JsonArray();
                for (String parameter : service.getValue())
                {
                    parameterArray.add(new JsonPrimitive(parameter));
                }
                rowObject.add(service.getKey(), parameterArray);
            }
            tokensObject.add(row.getKey(), rowObject);
        }
        root.add("tokens", tokensObject);

        root = doPostRequest(URL_AUTHENTICATE, root).getAsJsonObject();

        ImmutableMultimap.Builder<UUID, Authentication> b = ImmutableMultimap.builder();
        for (Entry<String, JsonElement> e : root.entrySet())
        {
            if (e.getValue().isJsonNull()) continue;
            JsonArray authArray = e.getValue().getAsJsonArray();
            if (authArray.size() == 0) continue;
            b.putAll(UUID.fromString(e.getKey()), GSON.fromJson(authArray, Authentication[].class));
        }
        return b.build();
    }

    // -- PRIVATES --

    @NotNull
    public static JsonElement doGetRequest(@NotNull URL url) throws IOException, APIException
    {
        return doGetRequest(url, null);
    }

    @NotNull
    public static JsonElement doGetRequest(@NotNull URL url, @Nullable Multimap<String, String> params) throws IOException, APIException
    {
        String query = urlEncode(params);
        String location = url.toString();
        HttpURLConnection con;
        do
        {
            url = new URL(url, location + query);
            con = ((HttpURLConnection) url.openConnection());
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(true);
            con.setAllowUserInteraction(false);
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
            con.setRequestProperty("User-Agent", userAgent);
            con.setRequestProperty("Accept-Charset", Constants.UTF8.name());
            con.setRequestProperty("Accept", "application/json");
            location = con.getHeaderField("Location");
        }
        while (location != null && con.getResponseCode() / 100 == 3);
        return parseConnectionOutput(con);
    }

    @NotNull
    private static JsonElement parseConnectionOutput(@NotNull HttpURLConnection con) throws IOException, APIException
    {
        try
        {
            InputStream is = con.getErrorStream();
            if (is == null) is = con.getInputStream();
            JsonElement e = new JsonParser().parse(new InputStreamReader(is));
            if (e.isJsonObject())
            {
                JsonObject root = e.getAsJsonObject();
                if (root.has("error") && root.get("error").getAsBoolean())
                {
                    throw new APIException(root.get("status").getAsInt(), root.get("description").getAsString());
                }
            }
            return e;
        }
        catch (JsonParseException e)
        {
            throw new IOException("Error parsing JSON. The backend is likely offline or is having a serious error.", e);
        }
    }

    @NotNull
    public static JsonElement doPostRequest(@NotNull URL url, @NotNull JsonElement body) throws IOException, APIException
    {
        return doPostRequest(url, body, null);
    }

    @NotNull
    public static JsonElement doPostRequest(@NotNull URL url, @NotNull JsonElement body, @Nullable Multimap<String, String> params) throws IOException, APIException
    {
        String data = body.toString();
        String query = urlEncode(params);
        String location = url.toString();
        HttpURLConnection con;
        do
        {
            url = new URL(url, location + query);
            con = ((HttpURLConnection) url.openConnection());
            con.setRequestMethod("POST");
            con.setInstanceFollowRedirects(true);
            con.setAllowUserInteraction(false);
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
            con.setDoOutput(true);
            con.setRequestProperty("User-Agent", userAgent);
            con.setRequestProperty("Accept-Charset", Constants.UTF8.name());
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Type", "application/json; charset=" + Constants.UTF8.name());
            DataOutputStream os = new DataOutputStream(con.getOutputStream());
            os.writeBytes(data);
            os.flush();
            location = con.getHeaderField("Location");
        }
        while (location != null && con.getResponseCode() / 100 == 3);
        return parseConnectionOutput(con);
    }

    private static URL getURL(String endpoint)
    {
        try
        {
            return new URL(Constants.API_URL + endpoint);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException("Impossible exception has occurred.", e);
        }
    }

    @NotNull
    private static String urlEncode(@Nullable Multimap<String, @Nullable String> params) throws UnsupportedEncodingException
    {
        if (params == null || params.isEmpty()) return "";
        StringBuilder query = new StringBuilder("?");
        Iterator<Entry<String, String>> i = params.entries().iterator();
        urlEncode(query, i.next());
        while (i.hasNext())
        {
            query.append('&');
            urlEncode(query, i.next());
        }
        return query.toString();
    }

    private static void urlEncode(@NotNull StringBuilder query, @NotNull Entry<String, @Nullable String> e) throws UnsupportedEncodingException
    {
        query.append(URLEncoder.encode(e.getKey(), Constants.UTF8.name()));
        if (!Strings.isNullOrEmpty(e.getValue()))
        {
            query.append('=').append(URLEncoder.encode(e.getValue(), Constants.UTF8.name()));
        }
    }
}
