package net.dries007.mclink;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.dries007.mclink.api.*;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserList;
import net.minecraft.server.management.UserListOps;
import net.minecraft.server.management.UserListWhitelist;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

/**
 * @author Dries007
 */
@Mod(modid = Constants.MODID, name = Constants.MODNAME, useMetadata = true, acceptableRemoteVersions = "*", dependencies = "before:*")
public class MCLink
{
    /**
     * Thanks https://stackoverflow.com/a/366239
     * group 1 = quote character or null
     * group 2 = unquoted text or null
     * group 3 = plain text or quoted text
     */
    private static final Pattern SPLIT_PATTERN = Pattern.compile("(?:(['\"])(.*?)(?<!\\\\)(?>\\\\\\\\)*\\1|([^\\s]+))");
    private static final Cache<UUID, ImmutableCollection<Authentication>> CACHE = CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();
    private static final ConcurrentHashMap<UUID, Marker> UUID_STATUS_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> TO_KICK = new ConcurrentHashMap<>();
    private static final Method METHOD_USERLIST_CONTAINS = ReflectionHelper.findMethod(UserList.class, null, new String[]{"func_152692_d"}, Object.class);

    private Logger logger;
    private Configuration forgeConfig;

    private Status status;
    private ImmutableMap<String, Service> services;
    private Table<String, String, List<String>> tokenConfig;
    private ImmutableMap<String, UUID> tokenUUIDMap;
    private String kickMessage;
    private String errorMessage;
    private String closedMessage;
    private boolean showStatus;
    private boolean closed;

    private enum Marker
    {
        ALLOWED, IN_PROGRESS, DENIED_NO_AUTH, DENIED_ERROR, DENIED_CLOSED
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws IOException, APIException
    {
        if (event.getSide().isClient()) return;
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
        API.setMetaData(event.getModMetadata().version, MinecraftForge.MC_VERSION);

        forgeConfig = new Configuration(event.getSuggestedConfigurationFile(), true);

        reload();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandBase()
        {
            @Override
            public String getCommandName()
            {
                return "mclink";
            }

            @Override
            public String getCommandUsage(ICommandSender sender)
            {
                return "/mclink [close|open|reload|status]";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) throws CommandException
            {
                if (args.length == 0)
                {
                    sender.addChatMessage(new ChatComponentText("Subcommands:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA)));
                    sender.addChatMessage(new ChatComponentText("- close: Do not let anyone join via MCLink. Ops and manually whitelisted players can still join."));
                    sender.addChatMessage(new ChatComponentText("- open: Let people join via MCLink again."));
                    sender.addChatMessage(new ChatComponentText("- reload: Reload all configs & API status. May take a few moments."));
                    sender.addChatMessage(new ChatComponentText("- status: Get current open/closed status & any API messages."));
                    return;
                }

                switch (args[0].toLowerCase())
                {
                    case "close":
                        changeClosed(sender, true);
                        break;
                    case "open":
                        changeClosed(sender, false);
                        break;
                    case "reload":
                        try
                        {
                            reload();
                            sender.addChatMessage(new ChatComponentText("Reloaded. Check log for possible warnings.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));
                        }
                        catch (Exception e)
                        {
                            logger.error("Error while reloading!", e);
                            sender.addChatMessage(new ChatComponentText("ERROR reloading config. See log for more info.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                            throw new CommandException(e.getMessage());
                        }
                        //fallthrough
                    case "status":
                        sender.addChatMessage(new ChatComponentText("The server is currently " + (closed ? "CLOSED" : "OPENED")));
                        if (Constants.API_VERSION < status.apiVersion) sender.addChatMessage(new ChatComponentText("[MCLink] API version outdated. Please update ASAP"));
                        if (status.message != null) sender.addChatMessage(new ChatComponentText("[MCLink] ").appendText(status.message));
                        break;
                    default:
                        throw new CommandNotFoundException("Subcommand not found.");
                }
            }

            @Override
            public List addTabCompletionOptions(ICommandSender sender, String[] args)
            {
                if (args.length == 1) return getListOfStringsMatchingLastWord(args, "close", "open", "reload", "status");
                return super.addTabCompletionOptions(sender, args);
            }

            private void changeClosed(ICommandSender sender, boolean closed)
            {
                MCLink.this.closed = closed;
                forgeConfig.get(CATEGORY_GENERAL, "closed", false).set(closed);
                forgeConfig.save();
                logger.info("The server is now {0}!", closed ? "CLOSED" : "OPENED");
                sender.addChatMessage(new ChatComponentText("The server is currently " + (closed ? "CLOSED" : "OPENED")));
            }
        });
    }

    private static boolean userListContains(UserList userList, GameProfile gameProfile)
    {
        try
        {
            return (boolean) METHOD_USERLIST_CONTAINS.invoke(userList, gameProfile);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e); // This cannot happen, the method is set accessible by the ReflectionHelper
        }
        catch (InvocationTargetException e)
        {
            Throwables.propagate(e.getTargetException());
            return false;
        }
    }

    @SuppressWarnings("ConstantConditions")
    @SubscribeEvent
    public void connectEvent(final FMLNetworkEvent.ServerConnectionFromClientEvent event)
    {
        GameProfile gp = ((NetHandlerPlayServer) event.handler).playerEntity.getGameProfile();
        final String name = gp.getName();
        final UUID uuid = gp.getId();
        UserListOps ops = server().getConfigurationManager().func_152603_m();
        UserListWhitelist whitelist = server().getConfigurationManager().func_152599_k();

        if (event.isLocal)
        {
            logger.info("Player {0} [{1}] was authorized because SSP", name, uuid);
            UUID_STATUS_MAP.put(uuid, Marker.ALLOWED);
        }
        else if (userListContains(ops, gp))
        {
            logger.info("Player {0} [{1}] was authorized because they are on the OP list.", name, uuid);
            UUID_STATUS_MAP.put(uuid, Marker.ALLOWED);
        }
        else if (userListContains(whitelist, gp))
        {
            logger.info("Player {0} [{1}] was authorized because they are on the whitelist.", name, uuid);
            UUID_STATUS_MAP.put(uuid, Marker.ALLOWED);
        }
        else if (closed)
        {
            logger.info("Player {0} [{1}] denied access because server is closed.", name, uuid);
            UUID_STATUS_MAP.put(uuid, Marker.DENIED_CLOSED);
        }
        else if (CACHE.getIfPresent(uuid) != null)
        {
            logger.info("Player {0} [{1}] was authorized cached auth entries.", name, uuid);
            UUID_STATUS_MAP.put(uuid, Marker.ALLOWED);
        }
        else
        {
            UUID_STATUS_MAP.put(uuid, Marker.IN_PROGRESS);
            logger.info("Player {0} [{1}] authorization is being checked...", name, uuid);
            new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        check(name, uuid);
                    }
                }, "MCLink " + name).start();
        }
    }

    @SubscribeEvent
    public void loginEvent(PlayerEvent.PlayerLoggedInEvent event)
    {
        // If the map has it set to DENIED already, the lookup finished before we got here and we can kick the player.
        // If the marker was something else, we remove the marker to the async thread knows it needs to kick the player itself.
        switch (UUID_STATUS_MAP.remove(event.player.getGameProfile().getId()))
        {
            case DENIED_NO_AUTH:
                ((EntityPlayerMP) event.player).playerNetServerHandler.kickPlayerFromServer(kickMessage);
                return;
            case DENIED_ERROR:
                ((EntityPlayerMP) event.player).playerNetServerHandler.kickPlayerFromServer(errorMessage);
                return;
            case DENIED_CLOSED:
                ((EntityPlayerMP) event.player).playerNetServerHandler.kickPlayerFromServer(closedMessage);
                return;
        }
        if (showStatus && event.player.canCommandSenderUseCommand(3, "mclink"))
        {
            if (Constants.API_VERSION < status.apiVersion) event.player.addChatMessage(new ChatComponentText("[MCLink] API version outdated. Please update ASAP"));
            if (status.message != null) event.player.addChatMessage(new ChatComponentText("[MCLink] ").appendText(status.message));
        }
    }

    private MinecraftServer server()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    @SuppressWarnings("ConstantConditions")
    private void check(String name, UUID uuid)
    {
        try
        {
            ImmutableMultimap<UUID, Authentication> map = API.getAuthorization(tokenConfig, uuid);
            ImmutableCollection<Authentication> auth = map.get(uuid);
            if (auth.isEmpty())
            {
                logger.info("Player {0} [{1}] was denied.", name, uuid);
                if (UUID_STATUS_MAP.put(uuid, Marker.DENIED_NO_AUTH) == null) // was already removed by login
                {
                    kickAsync(uuid, kickMessage);
                }
            }
            else
            {
                CACHE.put(uuid, auth);
                List<String> auths = new ArrayList<>();
                for (Authentication a : auth)
                {
                    GameProfile p = server().func_152358_ax().func_152652_a(a.token);
                    auths.add(a.name + " from " + (p == null ? "?" : p.getName()) + " [" + a.token + "] with " + a.extra);
                }
                logger.info("Player {0} [{1}] was authorized by: {2}", name, uuid, auths);
                if (UUID_STATUS_MAP.put(uuid, Marker.ALLOWED) == null) // was already removed by login
                {
                    UUID_STATUS_MAP.remove(uuid); // login event already passed, so we don't need this anymore.
                }
            }
        }
        catch (Exception e)
        {
            logger.info("Player {0} [{1}] was denied due to an exception.", name, uuid);
            logger.catching(e);
            if (UUID_STATUS_MAP.put(uuid, Marker.DENIED_ERROR) == null) // was already removed by login
            {
                kickAsync(uuid, errorMessage);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void tickEvent(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || TO_KICK.isEmpty()) return; // short-circuit optimization

        HashMap<UUID, EntityPlayerMP> map = new HashMap<>();
        for (EntityPlayerMP o : (List<EntityPlayerMP>)server().getConfigurationManager().playerEntityList)
        {
            if (TO_KICK.containsKey(o.getPersistentID())) map.put(o.getPersistentID(), o);
        }
        for (Map.Entry<UUID, EntityPlayerMP> e : map.entrySet())
        {
            String message = TO_KICK.remove(e.getKey());
            if (message == null) continue; // should not happen, but better be safe than sorry with Minecraft...
            e.getValue().playerNetServerHandler.kickPlayerFromServer(message);
        }
    }

    private void kickAsync(final UUID uuid, final String msg)
    {
        UUID_STATUS_MAP.remove(uuid); // login event already past, so we don't need this anymore.
        TO_KICK.put(uuid, msg); // 1.7.10 doesn't have threading, so use server tick even to sync.
    }

    private void reload() throws IOException, APIException
    {
        Status status = API.getStatus();
        ImmutableMap<String, Service> services = API.getServices();
        Table<String, String, List<String>> tokenConfig = HashBasedTable.create();

        forgeConfig.load();

        final String serviceCat = "Service";
        forgeConfig.setCategoryComment(serviceCat, "All service options are put in this subcategory.\n" +
                "Blank ones will be added if new services are added, old ones are not removed.\n" +
                "The formatting for the config options MUST follow these rules:\n" +
                "       TOKEN argument argument \"argument with spaces special characters like these: # ? : < > ! & \"\n" +
                "Any argument with spaces (or special characters) in them must be surrounded with double quotes.\n" +
                "If you want to use a double quote in a string, escape it with a backslash: \\\"");
        ConfigCategory serviceCategory = forgeConfig.getCategory(serviceCat);

        for (Map.Entry<String, Property> e : serviceCategory.getValues().entrySet())
        {
            Service s = services.get(e.getKey());
            Property p = e.getValue();
            if (s == null)
            {
                p.comment = "THIS SERVICE IS NOT AVAILABLE.";
                continue;
            }
            p.comment = s.getConfigCommentString();
            for (String line : p.getStringList())
            {
                List<String> split = new ArrayList<>();
                Matcher m = SPLIT_PATTERN.matcher(line);
                while (m.find())
                {
                    if (m.group(1) == null) split.add(m.group(0));
                    else split.add(m.group(2).replaceAll("\\\\[" + m.group(1) + "]", ""));
                }
                if (split.isEmpty()) continue; // ignore empty lines
                String token = split.remove(0);
                if (tokenConfig.contains(token, e.getKey())) die("Your MCLink config contains duplicate API tokens per service. This is not allowed. {0} {1}", e.getKey(), token);
                if (split.size() < s.requiredArgs.size()) die("Your MCLink config for {0} {1} does not contain enough arguments. See the comment for the required arguments.", e.getKey(), token);
                if (split.size() > s.requiredArgs.size() + s.optionalArgs.size()) die("Your MCLink config for {0} {1} contains too many arguments. See the comment for the allowed arguments.", e.getKey(), token);

                tokenConfig.put(token, e.getKey(), ImmutableList.copyOf(split));
            }
        }
        for (String newService : Sets.difference(services.keySet(), serviceCategory.keySet()))
        {
            forgeConfig.get(serviceCat, newService, new String[0], services.get(newService).getConfigCommentString());
        }

        String kickMessage = forgeConfig.getString("kickMessage", CATEGORY_GENERAL, "This is an MCLink protected server. Link your accounts via " + Constants.BASE_URL + " and make sure you are subscribed to the right people.", "The message used to kickAsync players. Make sure to include instructions on how to get on!");
        String errorMessage = forgeConfig.getString("errorMessage", CATEGORY_GENERAL, "MCLink could not verify your status. Please contact a server admin.", "The message people get when an error happens while MCLink checks their ID.");
        String closedMessage = forgeConfig.getString("closedMessage", CATEGORY_GENERAL, "The server is currently closed for the public.", "The message people get when the server is closed.");
        boolean showStatus = forgeConfig.getBoolean("showStatus", CATEGORY_GENERAL, true, "Show important status messages to level 2+ OP players when they log in.");
        boolean closed = forgeConfig.getBoolean("closed", CATEGORY_GENERAL, false, "Use the ingame command /mclink to update this.\nKeeps track of if the server is closed.");
        int timeout = forgeConfig.getInt("timeout", CATEGORY_GENERAL, 30, 0, 300, "Timeout for the API requests in seconds. Keep this high enough to avoid players being kicked while actually being authorized. 0 = infinite timeout") * 1000;

        // We can't count on hasChanged, because we change the comments, which is not tracked.
        forgeConfig.save();

        if (tokenConfig.isEmpty()) warn("Your MCLink config is empty, this will result in no-one being allowed on the server!");
        Sets.SetView<String> diff = Sets.difference(tokenConfig.columnKeySet(), services.keySet());
        if (!diff.isEmpty()) warn("Your tokenConfig for MCLink contains some services that are not available: {0}", diff);
        ImmutableMap<String, UUID> tokenUUIDMap = API.getUUIDsFromTokens(tokenConfig.rowKeySet());
        diff = Sets.difference(tokenConfig.rowKeySet(), tokenUUIDMap.keySet());
        if (!diff.isEmpty()) warn("Your tokenConfig for MCLink contains some API tokens that are invalid: {0}", diff);

        this.status = status;
        this.services = services;
        this.tokenConfig = tokenConfig;
        this.tokenUUIDMap = tokenUUIDMap;
        this.kickMessage = kickMessage;
        this.errorMessage = errorMessage;
        this.closedMessage = closedMessage;
        this.showStatus = showStatus;
        if (this.closed != closed)
        {
            this.closed = closed;
            logger.info("The server is now {0}!", closed ? "CLOSED" : "OPENED");
        }
        API.setTimeout(timeout);

        CACHE.invalidateAll();
        CACHE.cleanUp();

        if (Constants.API_VERSION < status.apiVersion) logger.warn("API version outdated. Please update ASAP");
        if (status.message != null) logger.warn(status.message);
    }

    private void die(String message, Object... objects)
    {
        throw new RuntimeException("[MCLink] Fatal error: " + MessageFormat.format(message, objects));
    }

    private void warn(String message, Object... objects)
    {
        logger.warn("-=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=-");
        logger.warn(MessageFormat.format(message, objects));
        logger.warn("-=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=--=##=-");
    }
}
