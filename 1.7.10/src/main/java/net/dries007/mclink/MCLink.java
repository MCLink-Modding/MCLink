/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.base.Throwables;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.dries007.mclink.api.APIException;
import net.dries007.mclink.api.Constants;
import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.IConfig;
import net.dries007.mclink.binding.IPlayer;
import net.dries007.mclink.common.Log4jLogger;
import net.dries007.mclink.common.MCLinkCommon;
import net.dries007.mclink.common.Player;
import net.dries007.mclink.common.ThreadStartConsumer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.server.management.UserList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Dries007
 */
@SuppressWarnings("Duplicates")
@Mod(modid = Constants.MODID, name = Constants.MODNAME, useMetadata = true, acceptableRemoteVersions = "*", dependencies = "before:*")
public class MCLink extends MCLinkCommon
{
    private static final ConcurrentHashMap<String, String> TO_KICK = new ConcurrentHashMap<>();
    private static final Method METHOD_USERLIST_CONTAINS = ReflectionHelper.findMethod(UserList.class, null, new String[]{"func_152692_d"}, Object.class);
    private MinecraftServer server;

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

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) throws IConfig.ConfigException, IOException, APIException
    {
        super.setModVersion(event.getModMetadata().version);
        super.setMcVersion(MinecraftForge.MC_VERSION);
        super.setBranding(FMLCommonHandler.instance().getModName());
        super.setLogger(new Log4jLogger(event.getModLog()));
        super.setConfig(new ForgeConfig(event.getSuggestedConfigurationFile()));
        super.init();

        if (event.getSide().isClient())
        {
            super.setSide(Side.CLIENT);
            return;
        }

        super.setSide(Side.SERVER);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        server = event.getServer();
        super.registerCommands(e -> event.registerServerCommand(new CommandWrapper(this, e)));
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event)
    {
        server = null;
        TO_KICK.clear();
        super.deInit();
    }

    private Player getPlayerFromEntity(EntityPlayer player)
    {
        return new Player(new SenderWrapper(player), player.getDisplayName(), player.getPersistentID());
    }

    @SubscribeEvent
    public void connectEvent(FMLNetworkEvent.ServerConnectionFromClientEvent event)
    {
        if (event.isLocal) return;
        EntityPlayerMP pl = ((NetHandlerPlayServer) event.handler).playerEntity;
        GameProfile gp = pl.getGameProfile();
        ServerConfigurationManager scm = server.getConfigurationManager();
        super.checkAuthStatusAsync(getPlayerFromEntity(pl), userListContains(scm.func_152603_m(), gp), userListContains(scm.func_152599_k(), gp), new ThreadStartConsumer("checker-" + gp.getId()));
    }

    @SubscribeEvent
    public void loginEvent(PlayerEvent.PlayerLoggedInEvent event)
    {
        super.login(getPlayerFromEntity(event.player), event.player.canCommandSenderUseCommand(3, Constants.MODID));
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || TO_KICK.isEmpty()) return; // short-circuit optimization

        TO_KICK.forEach((p, msg) -> {
            EntityPlayerMP e = server.getConfigurationManager().func_152612_a(p);
            //noinspection ConstantConditions
            if (e != null) e.playerNetServerHandler.kickPlayerFromServer(msg);
            TO_KICK.remove(p);
        });
    }

    @Override
    protected void authCompleteAsync(IPlayer player, String msg)
    {
        // 1.7.10 doesn't have threading, so use server tick even to sync.
        TO_KICK.put(player.getName(), msg);
    }

    @Nullable
    @Override
    protected String nameFromUUID(UUID uuid)
    {
        GameProfile gp = server.func_152358_ax().func_152652_a(uuid);
        //noinspection ConstantConditions
        if (gp == null) return null;
        return gp.getName();
    }

    @Override
    public void sendMessage(String message)
    {
        ChatComponentTranslation m = new ChatComponentTranslation("chat.type.admin", Constants.MODNAME, message);
        m.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(true));
        if (server.func_152363_m())
        {
            //noinspection unchecked
            for (EntityPlayer p : (List<EntityPlayer>) server.getConfigurationManager().playerEntityList)
            {
                if (server.getConfigurationManager().func_152596_g(p.getGameProfile())) p.addChatComponentMessage(m);
            }
        }
        server.addChatMessage(m);
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        sendMessage(message);
    }
}
