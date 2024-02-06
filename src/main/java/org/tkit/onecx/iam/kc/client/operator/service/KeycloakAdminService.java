package org.tkit.onecx.iam.kc.client.operator.service;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tkit.onecx.iam.kc.client.operator.KCConfig;
import org.tkit.onecx.iam.kc.client.operator.KeycloakClient;
import org.tkit.onecx.iam.kc.client.operator.config.KCClientConfig;
import org.tkit.onecx.iam.kc.client.operator.config.KCDefaultConfig;
import org.tkit.quarkus.log.cdi.LogService;

@LogService
@ApplicationScoped
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    public static final String UI_TYPE = "ui";
    public static final String MACHINE_TYPE = "machine";

    public static final String PROTOCOL_OPENID_CONNECT = "openid-connect";

    @Inject
    Keycloak keycloak;

    @Inject
    KCClientConfig kcClientConfig;

    @ActivateRequestContext
    public int createClient(KeycloakClient keycloakClient) {
        var spec = keycloakClient.getSpec();
        var clientId = spec.getKcConfig().getClientId();
        var realm = spec.getRealm() != null ? spec.getRealm() : kcClientConfig.realm();
        ClientRepresentation client = null;
        KCDefaultConfig clientDefaultConfig;

        if (UI_TYPE.equalsIgnoreCase(spec.getType())) {
            clientDefaultConfig = kcClientConfig.config().get(UI_TYPE.toLowerCase());
            client = prepareClient(spec.getKcConfig(), clientDefaultConfig);
        } else if (MACHINE_TYPE.equalsIgnoreCase(spec.getType())) {
            clientDefaultConfig = kcClientConfig.config().get(MACHINE_TYPE.toLowerCase());
            client = prepareClient(spec.getKcConfig(), clientDefaultConfig);
        } else {
            throw new TypeNotSupportedException(spec.getType());
        }

        // check scopes if they exist and create them if necessary
        checkAndCreateScopes(client, realm);

        List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientId);

        if (clientDefaultConfig.addDefaultScopes()) {
            var defaultScopesKC = keycloak.realm(realm).getDefaultDefaultClientScopes().stream()
                    .filter(csr -> csr.getProtocol().equals(PROTOCOL_OPENID_CONNECT)).map(ClientScopeRepresentation::getName)
                    .collect(Collectors.toSet());
            var optionalScopesKC = keycloak.realm(realm).getDefaultOptionalClientScopes().stream()
                    .filter(csr -> csr.getProtocol().equals(PROTOCOL_OPENID_CONNECT)).map(ClientScopeRepresentation::getName)
                    .collect(Collectors.toSet());

            // add default/optional scopes from realm
            client.getDefaultClientScopes().addAll(defaultScopesKC);
            client.getOptionalClientScopes().addAll(optionalScopesKC);
        }

        if (clients.isEmpty()) {
            // do create
            try (var resp = keycloak.realm(realm).clients().create(client)) {
                return resp.getStatus();
            }
        } else {
            // do update
            var defaultClientScopes = client.getDefaultClientScopes();
            var optionalClientScopes = client.getOptionalClientScopes();
            var clientToUpdate = keycloak.realm(realm).clients().get(clients.get(0).getId());
            clientToUpdate.update(client);
            // update default client scopes
            var toRemove = clientToUpdate.getDefaultClientScopes().stream()
                    .filter(rep -> !defaultClientScopes.contains(rep.getName())).map(ClientScopeRepresentation::getId)
                    .collect(Collectors.toSet());
            var toAdd = new ArrayList<>(client.getDefaultClientScopes());
            toAdd.removeAll(clientToUpdate.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName)
                    .collect(Collectors.toSet()));
            toRemove.forEach(scope -> removeDefaultClientScope(clientToUpdate, scope));
            toAdd.forEach(scope -> addDefaultClientScope(clientToUpdate, scope));
            // update optional client scopes
            var toRemoveOpt = clientToUpdate.getOptionalClientScopes().stream()
                    .filter(rep -> !optionalClientScopes.contains(rep.getName())).map(ClientScopeRepresentation::getId)
                    .collect(Collectors.toSet());
            var toAddOpt = new ArrayList<>(client.getOptionalClientScopes());
            toAddOpt.removeAll(clientToUpdate.getOptionalClientScopes().stream().map(ClientScopeRepresentation::getName)
                    .collect(Collectors.toSet()));
            toRemoveOpt.forEach(scope -> removeOptClientScope(clientToUpdate, scope));
            toAddOpt.forEach(scope -> addOptClientScope(clientToUpdate, scope));

            return 200;
        }
    }

    @ActivateRequestContext
    public void deleteClient(KeycloakClient keycloakClient) {
        var spec = keycloakClient.getSpec();
        var clientId = spec.getKcConfig().getClientId();
        var realm = spec.getRealm() != null ? spec.getRealm() : kcClientConfig.realm();

        List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientId);
        if (!clients.isEmpty()) {
            keycloak.realm(realm).clients().get(clients.get(0).getId()).remove();
        }
    }

    void addDefaultClientScope(ClientResource cr, String clientScope) {
        try {
            cr.addDefaultClientScope(clientScope);
        } catch (Exception e) {
            log.error("Error adding default client scope " + clientScope, e);
        }
    }

    void removeDefaultClientScope(ClientResource cr, String clientScope) {
        try {
            cr.removeDefaultClientScope(clientScope);
        } catch (Exception e) {
            log.error("Error removing default client scope " + clientScope, e);
        }
    }

    void addOptClientScope(ClientResource cr, String clientScope) {
        try {
            cr.addOptionalClientScope(clientScope);
        } catch (Exception e) {
            log.error("Error adding optional client scope " + clientScope, e);
        }
    }

    void removeOptClientScope(ClientResource cr, String clientScope) {
        try {
            cr.removeOptionalClientScope(clientScope);
        } catch (Exception e) {
            log.error("Error removing optional client scope " + clientScope, e);
        }
    }

    private void checkAndCreateScopes(ClientRepresentation clientRepresentation, String realm) {
        var kcScopes = keycloak.realm(realm).clientScopes().findAll();
        var scopeNames = kcScopes.stream().map(ClientScopeRepresentation::getName).collect(Collectors.toSet());

        var scopesToAdd = new HashSet<String>();

        // collect default scopes
        scopesToAdd.addAll(clientRepresentation.getDefaultClientScopes());

        // collect optional scopes
        scopesToAdd.addAll(clientRepresentation.getOptionalClientScopes());

        // remove all known scopes
        scopesToAdd.removeAll(scopeNames);

        // add all missing scopes to keycloak
        if (!scopesToAdd.isEmpty()) {
            scopesToAdd.forEach(scopeName -> createClientScope(scopeName, realm));
        }
    }

    private void createClientScope(String clientScopeName, String realm) {
        var clientScope = new ClientScopeRepresentation();
        clientScope.setId(clientScopeName);
        clientScope.setName(clientScopeName);
        clientScope.setProtocol(PROTOCOL_OPENID_CONNECT);
        clientScope.setDescription("Generated scope " + clientScopeName + " by operator");

        try (var resp = keycloak.realm(realm).clientScopes().create(clientScope)) {
            log.info("Client scope {} creation ended with status {}", clientScopeName, resp.getStatus());
        }

    }

    private ClientRepresentation prepareClient(KCConfig client, KCDefaultConfig defaultConfig) {
        ClientRepresentation clientRepresentation = new ClientRepresentation();

        clientRepresentation.setClientId(client.getClientId());
        clientRepresentation.setDescription(client.getDescription());

        clientRepresentation.setEnabled(resolveValue(client.getEnabled(), defaultConfig.enabled()));
        clientRepresentation.setClientAuthenticatorType(
                resolveValue(client.getClientAuthenticatorType(), defaultConfig.clientAuthenticatorType()));
        clientRepresentation.setSecret(client.getSecret());
        clientRepresentation.setRedirectUris(
                resolveValue(client.getRedirectUris(), defaultConfig.redirectUris().orElse(new ArrayList<>())));
        clientRepresentation
                .setWebOrigins(resolveValue(client.getWebOrigins(), defaultConfig.webOrigins().orElse(new ArrayList<>())));
        clientRepresentation.setBearerOnly(resolveValue(client.getBearerOnly(), defaultConfig.bearerOnly()));
        clientRepresentation
                .setStandardFlowEnabled(resolveValue(client.getStandardFlowEnabled(), defaultConfig.standardFlowEnabled()));
        clientRepresentation
                .setImplicitFlowEnabled(resolveValue(client.getImplicitFlowEnabled(), defaultConfig.implicitFlowEnabled()));
        clientRepresentation.setDirectAccessGrantsEnabled(
                resolveValue(client.getDirectAccessGrantsEnabled(), defaultConfig.directAccessGrantsEnabled()));
        clientRepresentation.setServiceAccountsEnabled(
                resolveValue(client.getServiceAccountsEnabled(), defaultConfig.serviceAccountsEnabled()));
        clientRepresentation.setPublicClient(resolveValue(client.getPublicClient(), defaultConfig.publicClient()));
        clientRepresentation.setProtocol(resolveValue(client.getProtocol(), defaultConfig.protocol()));
        clientRepresentation.setAttributes(resolveValue(client.getAttributes(), defaultConfig.attributes()));

        clientRepresentation
                .setDefaultClientScopes(resolveValue(client.getDefaultClientScopes(),
                        defaultConfig.defaultClientScopes().orElse(new ArrayList<>())));
        clientRepresentation.setOptionalClientScopes(
                resolveValue(client.getOptionalClientScopes(), defaultConfig.optionalClientScopes().orElse(new ArrayList<>())));

        return clientRepresentation;
    }

    private Map<String, String> resolveValue(Map<String, String> value, Map<String, String> defaultValue) {
        Map<String, String> finalMap = new HashMap<>(defaultValue);

        if (value != null) {
            finalMap.putAll(value);
        }

        return finalMap;
    }

    private List<String> resolveValue(List<String> value, List<String> defaultValue) {
        if (value != null) {
            return new ArrayList<>(value);
        }

        return new ArrayList<>(defaultValue);
    }

    private Boolean resolveValue(Boolean value, Boolean defaultValue) {
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    private String resolveValue(String value, String defaultValue) {
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

}
