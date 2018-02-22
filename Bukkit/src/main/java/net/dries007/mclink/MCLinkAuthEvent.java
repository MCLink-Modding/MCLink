/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.collect.ImmutableCollection;
import net.dries007.mclink.api.Authentication;
import net.dries007.mclink.common.MCLinkCommon;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * API (Sync event)
 * Fired when the authentication is done, see marker for if the player was allowed to join or not.
 * Offline player because the player might have logged off or has been kicked.
 *
 * @author Brian Ronald
 * @author Dries007
 */
public class MCLinkAuthEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();
    private final ImmutableCollection<Authentication> authentication;
    private final MCLinkCommon.Marker result;
    private final OfflinePlayer player;

    MCLinkAuthEvent(OfflinePlayer player, ImmutableCollection<Authentication> authentication, MCLinkCommon.Marker result)
    {
        super(false);
        if (result == MCLinkCommon.Marker.IN_PROGRESS) throw new IllegalStateException();
        this.player = player;
        this.authentication = authentication;
        this.result = result;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public OfflinePlayer getPlayer()
    {
        return player;
    }

    public ImmutableCollection<Authentication> getAuthentication()
    {
        return authentication;
    }

    public MCLinkCommon.Marker getResult()
    {
        return result;
    }
}
