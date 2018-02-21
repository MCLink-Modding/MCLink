package net.dries007.mclink;

/*
 * Copyright (c) 2018 Dries007 & Brian Ronald. All rights reserved
 */

import com.google.common.collect.ImmutableCollection;
import net.dries007.mclink.api.Authentication;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MCLinkAuthEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private ImmutableCollection<Authentication> authentication;
    private org.bukkit.entity.Player player;

    public MCLinkAuthEvent(org.bukkit.entity.Player player, ImmutableCollection<Authentication> authentication) {
        this.player = player;
        this.authentication = authentication;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public org.bukkit.entity.Player getPlayer() {
        return player;
    }

    public ImmutableCollection<Authentication> getAuthentication() {
        return authentication;
    }
}
