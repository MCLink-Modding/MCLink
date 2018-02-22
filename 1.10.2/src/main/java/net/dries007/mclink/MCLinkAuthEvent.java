/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.collect.ImmutableCollection;
import net.dries007.mclink.api.Authentication;
import net.dries007.mclink.common.MCLinkCommon;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.UUID;

/**
 * API
 * <p>
 * Fired when the authentication is done, see marker for if the player was allowed to join or not.
 * Offline player because the player might have logged off or has been kicked.
 *
 * @author Dries007
 */
public class MCLinkAuthEvent extends Event
{
    private final UUID uuid;
    private final ImmutableCollection<Authentication> authentications;
    private final MCLinkCommon.Marker result;

    MCLinkAuthEvent(UUID uuid, ImmutableCollection<Authentication> authentications, MCLinkCommon.Marker result)
    {
        this.uuid = uuid;
        this.authentications = authentications;
        this.result = result;
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public ImmutableCollection<Authentication> getAuthentications()
    {
        return authentications;
    }

    public MCLinkCommon.Marker getResultMarker()
    {
        return result;
    }
}
