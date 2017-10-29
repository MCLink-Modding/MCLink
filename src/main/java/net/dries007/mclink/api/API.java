package net.dries007.mclink.api;

import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

import static net.dries007.mclink.api.Constants.TYPE_MAP_STRING_STRING;

/**
 * All API interactions should be done via this class.
 *
 * You should call {see setMetaData} before you call any API function.
 * @see #setMetaData(String, String)
 *
 * @author Dries007
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class API
{
    public static final URL URL_STATUS = getURL("status");
    public static final URL URL_SERVICES = getURL("services");
    public static final URL URL_UUID = getURL("uuid");
    public static final URL URL_INFO = getURL("info");
    public static final URL URL_AUTHENTICATE = getURL("authenticate");

    /**
     * This functions should be called before any api requests are made.
     * If any parameter is unknown, it can be set to null.
     *
     * @param mod The version of the mod/plugin
     * @param mc The version of Minecraft
     */
    public static void setMetaData(String mod, String mc)
    {
        StringBuilder sb = new StringBuilder(Constants.MODNAME);
        if (mod != null) sb.append('/').append(mod.replaceAll("[;()\n\r]", ""));
        sb.append(" (APIv").append(Constants.API_VERSION).append("; ");
        if (mc != null) sb.append("MCv").append(mc.replaceAll("[;()\n\r]", "")).append("; ");
        String os = System.getProperty("os.name") + ' ' + System.getProperty("os.arch") + ' ' + System.getProperty("os.version");
        sb.append(os.replaceAll("[;()\n\r]", ""));
        userAgent = sb.append(')').toString();
    }

    /**
     * Set the timeouts for this API.
     * Make sure they are log enough because the backend needs to have time to make requests to all services you require.
     * @param timeout
     */
    public static void setTimeout(int timeout)
    {
        if (timeout < 0) throw new IllegalArgumentException("No negative timeouts! 0 = infinite.");
        API.timeout = timeout;
    }

    /**
     * Request the current server status.
     * @return Status
     */
    public static Status getStatus() throws IOException, APIException
    {
        return GSON.fromJson(doGetRequest(URL_STATUS), Status.class);
    }

    /**
     * Get a list of services currently fit for use.
     * @return Service[]
     */
    public static ImmutableMap<String, Service> getServices() throws IOException, APIException
    {
        return ImmutableMap.copyOf(GSON.<Map<String, Service>>fromJson(doGetRequest(URL_SERVICES), new TypeToken<Map<String, Service>>(){}.getType()));
    }

    /**
     * @see #getUUIDsFromTokens(Iterable)
     */
    public static ImmutableMap<String, UUID> getUUIDsFromTokens(String... tokens) throws IOException, APIException
    {
        return getUUIDsFromTokens(Arrays.asList(tokens));
    }

    /**
     * Get a the UUIDs corresponding to one or more tokens
     * Not all requested tokens may be present. If not, the token was invalid.
     * @return Map{String, UUID}
     */
    public static ImmutableMap<String, UUID> getUUIDsFromTokens(Iterable<String> tokens) throws IOException, APIException
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
    public static ImmutableTable<UUID, String, ImmutableMap<String, String>> getInfo(UUID... uuids) throws IOException, APIException
    {
        return getInfo(Arrays.asList(uuids));
    }

    /**
     * Get all information known about a series of uuids.
     *
     * Returns a table with:
     *   The rows are the uuids requested (Not all requested uuids may be present)
     *   The columns are the names of the services available. (Not all services may be available for all users)
     *   The cell values are maps of key:value pairs, as listed in the info received from getServices.
     *
     * @see #getServices()
     *
     * @return Table{UUID, String, Map{String, String}
     */
    public static ImmutableTable<UUID, String, ImmutableMap<String, String>> getInfo(Iterable<UUID> uuids) throws IOException, APIException
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
    public static ImmutableMultimap<UUID, Authentication> getAuthorization(Table<String, String, List<String>> tokenconfig, UUID... uuids) throws IOException, APIException
    {
        return getAuthorization(tokenconfig, Arrays.asList(uuids));
    }

    /**
     * Get authorization info for a series of users.
     * If a user is not present in the output map, you must assume they are not authorized.
     * The tokenTable is organizes as follows:
     *   The rows are API tokens
     *   The columns are Service's names
     *   The cell values are a list of Service parameters. If no parameters are to be used, use an empty list.
     */
    public static ImmutableMultimap<UUID, Authentication> getAuthorization(Table<String, String, List<String>> tokenTable, Iterable<UUID> uuids) throws IOException, APIException
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

    public static JsonElement doGetRequest(URL url) throws IOException, APIException
    {
        return doGetRequest(url, null);
    }

    public static JsonElement doGetRequest(URL url, Multimap<String, String> params) throws IOException, APIException
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

    private static JsonElement parseConnectionOutput(HttpURLConnection con) throws IOException, APIException
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

    public static JsonElement doPostRequest(URL url, JsonElement body) throws IOException, APIException
    {
        return doPostRequest(url, body, null);
    }

    public static JsonElement doPostRequest(URL url, JsonElement body, Multimap<String, String> params) throws IOException, APIException
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

    // -- PRIVATES --

    private static String userAgent;
    private static int timeout;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls()
            .registerTypeAdapter(Service.class, new Service.Gson())
            .registerTypeAdapter(Status.class, new Status.Gson())
            .registerTypeAdapter(Authentication.class, new Authentication.Gson())
            .setPrettyPrinting()
            .create();

    static
    {
        setMetaData(null, null);
        timeout = 30000;
    }

    private API() { throw new AssertionError("No API instances for you!"); }

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

    private static String urlEncode(Multimap<String, String> params) throws UnsupportedEncodingException
    {
        if (params == null || params.isEmpty()) return "";
        StringBuilder query = new StringBuilder("?");
        Iterator<Entry<String, String>> i = params.entries().iterator();
        urlEncode(query, i.next());
        while (i.hasNext()) {
             query.append('&');
             urlEncode(query, i.next());
        }
        return query.toString();
    }

    private static void urlEncode(StringBuilder query, Entry<String, String> e) throws UnsupportedEncodingException
    {
        query.append(URLEncoder.encode(e.getKey(), Constants.UTF8.name()));
        if (!Strings.isNullOrEmpty(e.getValue()))
        {
            query.append('=').append(URLEncoder.encode(e.getValue(), Constants.UTF8.name()));
        }
    }

    private static Map<String, String> parseExtra(Set<Entry<String, JsonElement>> entries)
    {
        HashMap<String, String> out = new HashMap<>();
        for (Entry<String, JsonElement> entry : entries)
        {
            out.put(entry.getKey(), GSON.fromJson(entry.getValue(), String.class));
        }
        return out;
    }
}
