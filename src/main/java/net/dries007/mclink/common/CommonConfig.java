/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink.common;

import com.google.common.collect.*;
import net.dries007.mclink.api.API;
import net.dries007.mclink.api.APIException;
import net.dries007.mclink.api.Constants;
import net.dries007.mclink.api.Service;
import net.dries007.mclink.binding.IConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dries007
 */
@SuppressWarnings("SameParameterValue")
public abstract class CommonConfig implements IConfig
{
    /**
     * Thanks https://stackoverflow.com/a/366239
     * group 1 = quote character or null
     * group 2 = unquoted text or null
     * group 3 = plain text or quoted text
     */
    private static final Pattern SPLIT_PATTERN = Pattern.compile("(?:(['\"])(.*?)(?<!\\\\)(?>\\\\\\\\)*\\1|([^\\s]+))");

    private final Table<String, String, List<String>> tokenConfig = HashBasedTable.create();

    @NotNull
    private String kickMessage = "";
    @NotNull
    private String errorMessage = "";
    @NotNull
    private String closedMessage = "";

    private boolean showStatus;
    private boolean closed;

    public static ImmutableList<String> splitArgumentString(String line)
    {
        line = line.trim();
        if (line.isEmpty()) return ImmutableList.of();
        Matcher m = SPLIT_PATTERN.matcher(line);
        ImmutableList.Builder<String> b = ImmutableList.builder();
        while (m.find())
        {
            if (m.group(1) == null) b.add(m.group(0));
            else b.add(m.group(2).replaceAll("\\\\[" + m.group(1) + "]", ""));
        }
        return b.build();
    }

    @NotNull
    @Override
    public Table<String, String, List<String>> getTokenConfig()
    {
        return tokenConfig;
    }

    @NotNull
    @Override
    public String getKickMessage()
    {
        return kickMessage;
    }

    @NotNull
    @Override
    public String getErrorMessage()
    {
        return errorMessage;
    }

    @NotNull
    @Override
    public String getClosedMessage()
    {
        return closedMessage;
    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public boolean setClosed(boolean closed)
    {
        boolean prev = this.closed;
        this.closed = closed;
        return prev != closed;
    }

    @Override
    public boolean isShowStatus()
    {
        return showStatus;
    }

    @Nullable
    @Override
    public String reload() throws ConfigException, IOException, APIException
    {
        String warnings = "";
        ImmutableMap<String, Service> services = API.getServices();
        Table<String, String, List<String>> tokenConfig = HashBasedTable.create();

        String kickMessage = getString("kickMessage", "This is an MCLink protected server. Link your accounts via " + Constants.BASE_URL + " and make sure you are subscribed to the right people.", "The message used to kickAsync players. Make sure to include instructions on how to get on!");
        String errorMessage = getString("errorMessage", "MCLink could not verify your status. Please contact a server admin.", "The message people get when an error happens while MCLink checks their ID.");
        String closedMessage = getString("closedMessage", "The server is currently closed for the public.", "The message people get when the server is closed.");
        boolean showStatus = getBoolean("showStatus", true, "Show important status messages to level 2+ OP players when they log in.");
        boolean closed = getBoolean("closed", false, "Use the ingame command /mclink to update this. Keeps track of if the server is closed.");
        int timeout = getInt("timeout", 30, 0, 300, "Timeout for the API requests in seconds. Keep this high enough to avoid players being kicked while actually being authorized. 0 = infinite timeout") * 1000;

        setGlobalCommentServices("All service options are put here.\n" +
                "Blank ones will be added if new services are added, old ones are not removed.\n" +
                "Check the MCLink website for a config file example.");

        Set<String> definedServices = getAllDefinedServices();
        for (String serviceName : definedServices)
        {
            Service s = services.get(serviceName);
            if (s == null)
            {
                setServiceComment(serviceName, "THIS SERVICE IS NOT AVAILABLE.");
                continue;
            }
            setServiceComment(serviceName, s.getConfigCommentString());
            for (List<String> originalLine : getServiceEntries(serviceName))
            {
                ArrayList<String> line = new ArrayList<>(originalLine);
                String token = line.remove(0);
                if (tokenConfig.contains(token, serviceName))
                    die("Your MCLink config contains duplicate API tokens per service. This is not allowed. {0} {1}", serviceName, token);
                if (line.size() < s.requiredArgs.size())
                    die("Your MCLink config for {0} {1} does not contain enough arguments. See the comment for the required arguments.", serviceName, token);
                if (line.size() > s.requiredArgs.size() + s.optionalArgs.size())
                    die("Your MCLink config for {0} {1} contains too many arguments. See the comment for the allowed arguments.", serviceName, token);
                tokenConfig.put(token, serviceName, ImmutableList.copyOf(line));
            }
        }
        for (String newService : Sets.difference(services.keySet(), definedServices))
        {
            addService(newService, services.get(newService).getConfigCommentString());
        }

        if (tokenConfig.isEmpty())
            warnings += "Your MCLink config is empty, this will result in no-one being allowed on the server!\n";
        Sets.SetView<String> diff = Sets.difference(tokenConfig.columnKeySet(), services.keySet());
        if (!diff.isEmpty())
            warnings += MessageFormat.format("Your tokenConfig for MCLink contains some services that are not available: {0}\n", diff);
        ImmutableMap<String, UUID> tokenUUIDMap = API.getUUIDsFromTokens(tokenConfig.rowKeySet());
        diff = Sets.difference(tokenConfig.rowKeySet(), tokenUUIDMap.keySet());
        if (!diff.isEmpty())
            warnings += MessageFormat.format("Your tokenConfig for MCLink contains some API tokens that are invalid: {0}\n", diff);

        this.tokenConfig.clear();
        this.tokenConfig.putAll(tokenConfig);

        this.kickMessage = kickMessage;
        this.errorMessage = errorMessage;
        this.closedMessage = closedMessage;
        this.showStatus = showStatus;

        if (this.closed != closed)
        {
            this.closed = closed;
            warnings += MessageFormat.format("The server is now {0}!", closed ? "CLOSED" : "OPENED");
        }
        API.setTimeout(timeout);

        warnings = warnings.trim();
        return warnings.isEmpty() ? null : warnings;
    }

    private void die(String message, Object... objects)
    {
        throw new RuntimeException(MessageFormat.format(message, objects));
    }

    protected abstract String getString(String key, String def, String comment);

    protected abstract boolean getBoolean(String key, boolean def, String comment);

    protected abstract int getInt(String key, int def, int min, int max, String comment);

    protected abstract void addService(String name, String comment);

    protected abstract void setServiceComment(String name, String comment);

    protected abstract void setGlobalCommentServices(String comment);

    protected abstract List<String>[] getServiceEntries(String name);

    protected abstract Set<String> getAllDefinedServices();
}
