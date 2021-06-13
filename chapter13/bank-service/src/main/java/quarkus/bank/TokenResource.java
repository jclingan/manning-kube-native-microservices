package quarkus.bank;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

@Authenticated
@Path("/token")
public class TokenResource {
    /**
     * Injection point for the Access Token issued
     * by the OpenID Connect Provider
     */
    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("/tokeninfo")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> token() {
        HashSet<String> set = new HashSet<String>();
        for (String t : accessToken.getClaimNames()) {
            set.add(t + " = " + accessToken.getClaim(t));
        }
        return set;
    }
}