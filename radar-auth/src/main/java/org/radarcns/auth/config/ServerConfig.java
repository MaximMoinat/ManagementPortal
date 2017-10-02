package org.radarcns.auth.config;

import java.net.URI;

public interface ServerConfig {

    /**
     * Get the public key endpoint as a URI.
     * @return The public key endpiont URI
     */
    URI getPublicKeyEndpoint();

    /**
     * The name of this resource. It should be in the list of allowed resources for the OAuth
     * client.
     * @return the name of the resource
     */
    String getResourceName();


}
