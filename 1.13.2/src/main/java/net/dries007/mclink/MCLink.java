/*
 * Copyright (c) 2017 - 2019 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.collect.ImmutableCollection;
import com.mojang.authlib.GameProfile;
import net.dries007.mclink.api.APIException;
import net.dries007.mclink.api.Authentication;
import net.dries007.mclink.api.Constants;
import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.IPlayer;
import net.dries007.mclink.common.Log4jLogger;
import net.dries007.mclink.common.MCLinkCommon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.BrandingControl;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.versions.mcp.MCPVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

@Mod(Constants.MODID)
public class MCLink extends MCLinkCommon
{
    public static final Logger LOGGER = LogManager.getLogger();

    private MinecraftServer server;

    public MCLink()
    {
        // We want mod lifecycle events
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    /**
     * We only do things on a dedicated server, so only register for events if that's where we are.
     */
    @SubscribeEvent
    public void onServerSetupEvent(final FMLDedicatedServerSetupEvent event) throws APIException, IOException
    {
        LOGGER.info("onServerSetupEvent");
        server = event.getServerSupplier().get();
        MinecraftForge.EVENT_BUS.register(this);

        IModInfo info = ModLoadingContext.get().getActiveContainer().getModInfo();
        super.setModVersion(String.valueOf(info.getVersion()));
        super.setMcVersion(MCPVersion.getMCVersion());
        super.setBranding(BrandingControl.getServerBranding());
        super.setLogger(new Log4jLogger(LOGGER));
        super.setConfigFolder(FMLPaths.CONFIGDIR.get());
        super.init();
    }

    @Override
    @SuppressWarnings("Duplicates")
    protected void authCompleteAsync(IPlayer player, ImmutableCollection<Authentication> authentications, Marker result)
    {
        LOGGER.info("authCompleteAsync({}, {}, {})", player, authentications, result);
        server.addScheduledTask(() -> {
            // Don't kick players unless result was one of the DENIED_* values
            if (result != Marker.ALLOWED)
            {
                EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(player.getUuid());
                if (p != null) // The player may have disconnected before this could happen.
                    p.connection.disconnect(new TextComponentString(getConfig().getMessage(result)));
            }
            // Fire the event in all cases
            MinecraftForge.EVENT_BUS.post(new MCLinkAuthEvent(player.getUuid(), authentications, result));
        });
    }

    @Override
    protected @Nullable String nameFromUUID(UUID uuid)
    {
        GameProfile gp = server.getPlayerProfileCache().getProfileByUUID(uuid);
        if (gp == null) return null;
        return gp.getName();
    }

    @Override
    public void sendMessage(String message)
    {
        LOGGER.info("sendMessage({})", message);
        ITextComponent m = new TextComponentTranslation("chat.type.admin", Constants.MODNAME, message);
        m.setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true));
        if (server.getGameRules().getBoolean("sendCommandFeedback"))
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
        LOGGER.info("sendMessage({}, {})", message, formatCode);
        sendMessage(message);
    }

    @Override
    public void sendMessageAsync(String message)
    {
        server.addScheduledTask(() -> sendMessage(message));
    }

    @Override
    public void sendMessageAsync(String message, FormatCode formatCode)
    {
        server.addScheduledTask(() -> sendMessage(message, formatCode));
    }
}
