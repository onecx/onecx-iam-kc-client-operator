package org.tkit.onecx.iam.kc.client.operator.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface KCDefaultConfig {

    /**
     * Enable the client
     */
    @WithName("enabled")
    @WithDefault(value = "true")
    Boolean enabled();

    /**
     * Authentication type.
     */
    @WithName("auth-type")
    @WithDefault("client-secret")
    String clientAuthenticatorType();

    /**
     * List of redirect uris.
     */
    @WithName("redirect-uris")
    Optional<List<String>> redirectUris();

    /**
     * List of web origins
     */
    @WithName("web-origins")
    Optional<List<String>> webOrigins();

    /**
     * Bearer token only.
     */
    @WithName("bearer-only")
    @WithDefault(value = "false")
    Boolean bearerOnly();

    /**
     * Standard flow enabled.
     */
    @WithName("standard-flow")
    @WithDefault("false")
    Boolean standardFlowEnabled();

    /**
     * Implicit flow enabled.
     */
    @WithName("implicit-flow")
    @WithDefault("false")
    Boolean implicitFlowEnabled();

    /**
     * Enable direct access grants.
     */
    @WithName("direct-access")
    @WithDefault("false")
    Boolean directAccessGrantsEnabled();

    /**
     * Enable service account.
     */
    @WithName("service-account")
    @WithDefault("true")
    Boolean serviceAccountsEnabled();

    /**
     * Public client flag.
     */
    @WithName("public")
    @WithDefault("false")
    Boolean publicClient();

    /**
     * Protocol used with the client.
     */
    @WithName("protocol")
    @WithDefault("openid-connect")
    String protocol();

    /**
     * Attributes map for the client.
     */
    @WithName("attributes")
    Map<String, String> attributes();

    /**
     * Default client scopes.
     */
    @WithName("default-scopes")
    Optional<List<String>> defaultClientScopes();

    /**
     * Optional client scopes.
     */
    @WithName("optional-scopes")
    Optional<List<String>> optionalClientScopes();
}
