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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Server.BROADCAST_CHANNEL_ADMINISTRATIVE;

/**
 * @author Dries007
 */
public final class MCLink extends JavaPlugin implements Listener
{
    private final MCLinkCommon common = new MCLinkCommon()
    {
        @Override
        public void sendMessage(String message)
        {
            Bukkit.getServer().broadcast(message, BROADCAST_CHANNEL_ADMINISTRATIVE);
        }

        @Override
        public void sendMessage(String message, FormatCode formatCode)
        {
            sendMessage(message);
        }

        @Override
        public void sendMessageAsync(String message)
        {
            Bukkit.getScheduler().runTask(MCLink.this, () -> sendMessage(message));
        }

        @Override
        public void sendMessageAsync(String message, FormatCode formatCode)
        {
            Bukkit.getScheduler().runTask(MCLink.this, () -> sendMessage(message, formatCode));
        }

        @Override
        protected void authCompleteAsync(IPlayer player, ImmutableCollection<Authentication> authentications, Marker result)
        {
            Bukkit.getScheduler().runTask(MCLink.this, () -> {
                // Only kick if we should
                if (result != Marker.ALLOWED)
                {
                    org.bukkit.entity.Player p = Bukkit.getPlayer(player.getUuid());
                    if (p != null) // null check in case user disconnected
                        p.kickPlayer(getConfig().getMessage(result));
                }
                // Fire event in all cases
                Bukkit.getServer().getPluginManager().callEvent(new MCLinkAuthEvent(Bukkit.getOfflinePlayer(player.getUuid()), authentications, result));
            });
        }

        @Nullable
        @Override
        protected String nameFromUUID(UUID uuid)
        {
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
            return p.getPlayer() == null ? null : p.getName();
        }
    };

    private Player getPlayerFromOfflinePlayer(OfflinePlayer player)
    {
        return player.getPlayer() == null ? new Player(null, player.getName(), player.getUniqueId()) : getPlayerFromEntity(player.getPlayer());
    }

    private Player getPlayerFromEntity(org.bukkit.entity.Player player)
    {
        return new Player(new SenderWrapper(player), player.getDisplayName(), player.getUniqueId());
    }

    @Override
    public void onEnable()
    {
        try
        {
            getLogger().setLevel(Level.FINEST);

            common.setModVersion(getDescription().getVersion());
            common.setMcVersion(Bukkit.getVersion());
            common.setBranding(Bukkit.getName() + "v" + Bukkit.getBukkitVersion());
            common.setLogger(new JavaLogger(getLogger()));
            common.setConfigFolder(this.getDataFolder());

            common.init();

            Bukkit.getServer().getPluginManager().registerEvents(this, this);
            common.registerCommands(e -> getCommand(e.getName()).setExecutor(new CommandWrapper(this, common, e)));

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

    @EventHandler()
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        OfflinePlayer pl = Bukkit.getOfflinePlayer(event.getUniqueId());
        boolean op = Bukkit.getOperators().contains(pl);
        boolean wl = Bukkit.getWhitelistedPlayers().contains(pl);

        common.checkAuthStatusAsync(getPlayerFromOfflinePlayer(pl), op, wl, r -> Bukkit.getScheduler().runTaskAsynchronously(this, r));
    }

    @EventHandler
    public void onPlayerLoginEvent(PlayerLoginEvent event)
    {
        common.login(getPlayerFromEntity(event.getPlayer()), event.getPlayer().isOp());
    }
}
