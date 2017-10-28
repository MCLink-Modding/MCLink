package net.dries007.mclink.api;

/**
 * Used when an API request returns an error in an expected place place.
 *
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class APIException extends Throwable
{
    public final int status;

    APIException(int status, String description)
    {
        super(description);
        this.status = status;
    }
}
