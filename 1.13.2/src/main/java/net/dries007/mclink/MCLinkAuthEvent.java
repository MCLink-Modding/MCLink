/*
 * Copyright (c) 2017 - 2019 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.collect.ImmutableCollection;
import net.dries007.mclink.api.Authentication;
import net.dries007.mclink.common.MCLinkCommon;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

/**
 * @author Dries007
 */
public class MCLinkAuthEvent extends Event
{
    public final UUID uuid;
    public final ImmutableCollection<Authentication> authentications;
    public final MCLinkCommon.Marker result;

    public MCLinkAuthEvent(final UUID uuid, final ImmutableCollection<Authentication> authentications, final MCLinkCommon.Marker result)
    {
        this.uuid = uuid;
        this.authentications = authentications;
        this.result = result;
    }
}
