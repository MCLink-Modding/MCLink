/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.collect.ImmutableCollection;
import com.mojang.authlib.GameProfile;
import net.dries007.mclink.api.APIException;
import net.dries007.mclink.api.Authentication;
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
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Dries007
 */
@SuppressWarnings("Duplicates")
@Mod(modid = Constants.MODID, name = Constants.MODNAME, useMetadata = true, acceptableRemoteVersions = "*", dependencies = "before:*")
public class MCLink extends MCLinkCommon
{
    private MinecraftServer server;

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) throws IConfig.ConfigException, IOException, APIException
    {
        super.setModVersion(event.getModMetadata().version);
        super.setMcVersion(MinecraftForge.MC_VERSION);
        super.setBranding(FMLCommonHandler.instance().getModName());
        super.setLogger(new Log4jLogger(event.getModLog()));
        super.setConfig(new ForgeConfig(event.getSuggestedConfigurationFile()));
        super.setSide(event.getSide().isClient() ? Side.CLIENT : Side.SERVER);
        super.init();
        if (event.getSide().isServer()) MinecraftForge.EVENT_BUS.register(this);
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
        super.deInit();
    }

    private Player getPlayerFromEntity(EntityPlayer player)
    {
        return new Player(new SenderWrapper(player), player.getDisplayNameString(), player.getPersistentID());
    }

    @SubscribeEvent
    public void connectEvent(FMLNetworkEvent.ServerConnectionFromClientEvent event)
    {
        if (event.isLocal()) return;
        EntityPlayerMP p = ((NetHandlerPlayServer) event.getHandler()).player;
        GameProfile gp = p.getGameProfile();
        PlayerList pl = server.getPlayerList();
        boolean op = pl.getOppedPlayers().bypassesPlayerLimit(gp) || pl.getOppedPlayers().getPermissionLevel(gp) > 0;
        boolean wl = pl.getWhitelistedPlayers().isWhitelisted(gp);
        super.checkAuthStatusAsync(getPlayerFromEntity(p), op, wl, new ThreadStartConsumer("checker-" + gp.getId()));
    }

    @SubscribeEvent
    public void loginEvent(PlayerLoggedInEvent event)
    {
        super.login(getPlayerFromEntity(event.player), event.player.canUseCommand(3, Constants.MODID));
    }

    @Override
    protected void authCompleteAsync(IPlayer player, ImmutableCollection<Authentication> authentications, Marker result)
    {
        server.addScheduledTask(() -> {
            // Don't kick players unless result was one of the DENIED_* values
            if (result != Marker.ALLOWED)
            {
                EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(player.getUuid());
                //noinspection ConstantConditions
                if (p != null) // The player may have disconnected before this could happen.
                    p.connection.disconnect(new TextComponentString(getConfig().getMessage(result)));
            }
            // Fire the event in all cases
            MinecraftForge.EVENT_BUS.post(new MCLinkAuthEvent(player.getUuid(), authentications, result));
        });
    }

    @Nullable
    @Override
    protected String nameFromUUID(UUID uuid)
    {
        GameProfile gp = server.getPlayerProfileCache().getProfileByUUID(uuid);
        //noinspection ConstantConditions
        if (gp == null) return null;
        return gp.getName();
    }

    @Override
    public void sendMessage(String message)
    {
        ITextComponent m = new TextComponentTranslation("chat.type.admin", Constants.MODNAME, message);
        m.setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true));
        if (server.shouldBroadcastConsoleToOps())
        {
            //noinspection unchecked
            for (EntityPlayer p : server.getPlayerList().getPlayers())
            {
                if (server.getPlayerList().canSendCommands(p.getGameProfile())) p.sendMessage(m);
            }
        }
        server.sendMessage(m);
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        sendMessage(message);
    }
}
