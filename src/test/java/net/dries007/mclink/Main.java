package net.dries007.mclink;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import net.dries007.mclink.api.API;
import net.dries007.mclink.api.APIException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author Dries007
 */
public class Main
{
    public static void main(String[] args) throws IOException, APIException
    {
        API.setMetaData("0.0.0", null);
        System.out.println();
        System.out.println(API.getStatus());
        System.out.println();
        System.out.println(API.getServices());
        System.out.println();
        System.out.println(API.getUUIDsFromTokens("43U9KAKMYb4KN1Ptjwdjq0d74wqIgchY", "foo"));
        System.out.println();
        System.out.println(API.getUUIDsFromTokens());
        System.out.println();
        System.out.println(API.getInfo(UUID.fromString("c93ca410-8003-40ef-81d7-ac88719e2038")));
        System.out.println();
        HashBasedTable<String, String, List<String>> tokenCfg = HashBasedTable.create();
        tokenCfg.put("43U9KAKMYb4KN1Ptjwdjq0d74wqIgchY", "Twitch", ImmutableList.<String>of());
        System.out.println(API.getAuthorization(tokenCfg, UUID.fromString("c93ca410-8003-40ef-81d7-ac88719e2038")));
    }
}
