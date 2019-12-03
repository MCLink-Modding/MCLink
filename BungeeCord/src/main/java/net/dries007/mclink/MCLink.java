/*
 * Copyright (c) 2017 - 2019 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.collect.ImmutableCollection;
import net.dries007.mclink.api.Authentication;
import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.IPlayer;
import net.dries007.mclink.common.JavaLogger;
import net.dries007.mclink.common.MCLinkCommon;
import net.dries007.mclink.common.Player;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Level;

/**
 * @author Dries007
 */
public final class MCLink extends Plugin implements Listener
{
    private final MCLinkCommon common = new MCLinkCommon()
    {
        @Override
        public void sendMessage(String message)
        {
            getProxy().broadcast(TextComponent.fromLegacyText(message));
        }

        @Override
        public void sendMessage(String message, FormatCode formatCode)
        {
            sendMessage(message);
        }

        @Override
        protected void authCompleteAsync(IPlayer player, ImmutableCollection<Authentication> authentications, Marker result)
        {
            getProxy().getScheduler().runAsync(MCLink.this, () -> {
                // Only kick if we should

                if (result != Marker.ALLOWED)
                {
                    ProxiedPlayer p = getProxy().getPlayer(player.getUuid());
                    if (p != null) // null check in case user disconnected
                    {
                        p.disconnect(new TextComponent(getConfig().getMessage(result)));
                    }
                }
                // Fire event in all cases
                getProxy().getPluginManager().callEvent(new MCLinkAuthEvent(getProxy().getPlayer(player.getUuid()), authentications, result));
            });
        }

        @Nullable
        @Override
        protected String nameFromUUID(UUID uuid)
        {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            return player == null ? null : player.getName();
        }
    };

    private Player getPlayerFromOfflinePlayer(PendingConnection player)
    {
        return player.getName() == null ? new Player(null, "???", player.getUniqueId()) : getPlayerFromEntity(player);
    }

    private Player getPlayerFromEntity(PendingConnection player)
    {
        return new Player(new SenderWrapper(getProxy().getPlayer(player.getUniqueId())), player.getName(), player.getUniqueId());
    }

    @Override
    public void onEnable()
    {
        try
        {
            getLogger().setLevel(Level.FINEST);

            common.setModVersion(getDescription().getVersion());
            common.setMcVersion(getProxy().getVersion());
            common.setBranding(getDescription().getName() + "v" + getDescription().getVersion());
            common.setLogger(new JavaLogger(getLogger()));
            common.setConfig(new BungeeCordConfig(this));
            common.setSide(MCLinkCommon.Side.SERVER);

            common.init();

            getProxy().getPluginManager().registerListener(this, this);
            common.registerCommands(e -> getProxy().getPluginManager().registerCommand(this, new CommandWrapper(e, common)));

            common.getLogger().info("Enabled");
        }
        catch (Exception e)
        {
            common.getLogger().error("WARNING! Something went wrong initializing... People won't be able to join.");
            common.getLogger().catching(e);
        }
    }

    @Override
    public void onDisable()
    {
        common.getLogger().info("Disabled");
        common.deInit();
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(PostLoginEvent event)
    {
        ProxiedPlayer pl = event.getPlayer();
        boolean op = pl.hasPermission("bungeecord.command.alert");
        boolean wl = false;

        common.checkAuthStatusAsync(getPlayerFromOfflinePlayer(pl.getPendingConnection()), op, wl, r -> getProxy().getScheduler().runAsync(this, r));
    }

    @EventHandler
    public void onPlayerLoginEvent(ServerConnectEvent event)
    {
        common.login(getPlayerFromEntity(event.getPlayer().getPendingConnection()), event.getPlayer().hasPermission("bungeecord.command.alert"));
    }
}
