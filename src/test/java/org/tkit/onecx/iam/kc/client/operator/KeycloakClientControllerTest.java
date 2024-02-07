package org.tkit.onecx.iam.kc.client.operator;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.groovy.util.Maps;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tkit.onecx.iam.kc.client.operator.service.KeycloakAdminService;
import org.tkit.onecx.iam.kc.client.test.AbstractTest;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakClientControllerTest extends AbstractTest {

    final static Logger log = LoggerFactory.getLogger(KeycloakClientControllerTest.class);

    @Inject
    Operator operator;

    @Inject
    KubernetesClient client;

    private static final KeycloakTestClient keycloakClient = new KeycloakTestClient();

    @Inject
    Keycloak keycloak;

    @BeforeAll
    public static void init() {
        Awaitility.setDefaultPollDelay(2, SECONDS);
        Awaitility.setDefaultPollInterval(2, SECONDS);
        Awaitility.setDefaultTimeout(10, SECONDS);
    }

    @Test
    @Order(1)
    void createUIClient() {
        var CLIENT_ID = "test-ui-client";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.UI_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setDescription(CLIENT_ID);
        kcConfig.setDefaultClientScopes(List.of("create-scope-1", "create-scope-2"));
        kcConfig.setOptionalClientScopes(List.of("opt-scope-1", "opt-scope-2"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "create.values.1", "create.attr.2", "create.values.2"));
        data.setSpec(kcClientSpec);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var clients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(clients).isNotEmpty();
        var clientRep = clients.get(0);
        assertThat(clientRep.getDescription()).isEqualTo(kcConfig.getDescription());
        // validate that attributes are all in
        assertThat(clientRep.getAttributes()).containsAllEntriesOf(kcConfig.getAttributes());
        assertThat(clientRep.getOptionalClientScopes()).containsAll(kcConfig.getOptionalClientScopes());

        var token = keycloakClient.getAccessToken(USER_ALICE, CLIENT_ID);
        assertThat(token).isNotNull();

        var jws = resolveToken(token);
        assertThat((String) jws.getClaim(UI_TOKEN_CLIENT_CLAIM_NAME)).isEqualTo(CLIENT_ID);
        var scopeString = (String) jws.getClaim(SCOPE_CLAIM_NAME);
        var scopes = scopeString.split(" ");
        // validate all scopes are in
        assertThat(scopes).containsAll(kcConfig.getDefaultClientScopes());
    }

    @Test
    @Order(3)
    void createUIClientAllOptionsFilled() {
        var CLIENT_ID = "test-ui-client-all-ops";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.UI_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setDescription(CLIENT_ID);
        kcConfig.setEnabled(true);
        kcConfig.setClientAuthenticatorType("client-secret");
        kcConfig.setPassword(CLIENT_ID);
        kcConfig.setRedirectUris(List.of("*", "localhost"));
        kcConfig.setWebOrigins(List.of("*", "localhost"));
        kcConfig.setBearerOnly(false);
        kcConfig.setStandardFlowEnabled(true);
        kcConfig.setImplicitFlowEnabled(false);
        kcConfig.setDirectAccessGrantsEnabled(true);
        kcConfig.setServiceAccountsEnabled(false);
        kcConfig.setPublicClient(true);
        kcConfig.setProtocol(KeycloakAdminService.PROTOCOL_OPENID_CONNECT);
        kcConfig.setDefaultClientScopes(List.of("create-scope-1", "create-scope-2"));
        kcConfig.setOptionalClientScopes(List.of("opt-scope-1", "opt-scope-2"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "create.values.1", "create.attr.2", "create.values.2"));
        data.setSpec(kcClientSpec);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var clients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(clients).isNotEmpty();
        var clientRep = clients.get(0);
        assertThat(clientRep.getDescription()).isEqualTo(kcConfig.getDescription());
        // validate that attributes are all in
        assertThat(clientRep.getAttributes()).containsAllEntriesOf(kcConfig.getAttributes());
        assertThat(clientRep.getOptionalClientScopes()).containsAll(kcConfig.getOptionalClientScopes());

        var token = keycloakClient.getAccessToken(USER_ALICE, CLIENT_ID);
        assertThat(token).isNotNull();

        var jws = resolveToken(token);
        assertThat((String) jws.getClaim(UI_TOKEN_CLIENT_CLAIM_NAME)).isEqualTo(CLIENT_ID);
        var scopeString = (String) jws.getClaim(SCOPE_CLAIM_NAME);
        var scopes = scopeString.split(" ");
        // validate all scopes are in
        assertThat(scopes).containsAll(kcConfig.getDefaultClientScopes());
    }

    @Test
    @Order(2)
    void updateUIClient() {
        var CLIENT_ID = "test-ui-client";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.UI_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setDescription("UPDATED-" + CLIENT_ID);
        kcConfig.setDefaultClientScopes(List.of("test-scope-1", "test-scope-2"));
        kcConfig.setOptionalClientScopes(List.of("opt-scope-1", "opt-scope-2-updated"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "udpate.values.1", "update.attr.2", "update.values.2"));
        data.setSpec(kcClientSpec);

        log.info("Update test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is UPDATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.UPDATED);
        });

        var clients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(clients).isNotEmpty();
        var clientRep = clients.get(0);
        assertThat(clientRep.getDescription()).isEqualTo(kcConfig.getDescription());
        // validate that attributes are all in
        assertThat(clientRep.getAttributes()).containsAllEntriesOf(kcConfig.getAttributes());
        assertThat(clientRep.getOptionalClientScopes()).containsAll(kcConfig.getOptionalClientScopes());

        var token = keycloakClient.getAccessToken(USER_ALICE, CLIENT_ID);
        assertThat(token).isNotNull();

        var jws = resolveToken(token);
        assertThat((String) jws.getClaim(UI_TOKEN_CLIENT_CLAIM_NAME)).isEqualTo(CLIENT_ID);
        var scopeString = (String) jws.getClaim(SCOPE_CLAIM_NAME);
        var scopes = scopeString.split(" ");
        // validate all scopes are in
        assertThat(scopes).containsAll(kcConfig.getDefaultClientScopes());
    }

    @Test
    @Order(4)
    void createUIClientMinimumOption() {
        var CLIENT_ID = "test-ui-client-min-ops";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setType(KeycloakAdminService.UI_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        data.setSpec(kcClientSpec);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var clients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(clients).isNotEmpty();

        var token = keycloakClient.getRealmAccessToken(REALM_QUARKUS, USER_ALICE, CLIENT_ID);
        assertThat(token).isNotNull();

        var jws = resolveToken(token);
        assertThat((String) jws.getClaim(UI_TOKEN_CLIENT_CLAIM_NAME)).isEqualTo(CLIENT_ID);

        log.info("Deleting test keycloak client object: {}", data);
        var statusDetails = client.resource(data).delete();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            var mfeStatus = client.resource(data).get();
            assertThat(mfeStatus).isNull();
        });
    }

    @Test
    @Order(5)
    void deleteUIClientMinimumOption() {
        var CLIENT_ID = "test-ui-client-min-ops-for-del";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setType(KeycloakAdminService.UI_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        data.setSpec(kcClientSpec);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var foundClients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(foundClients).isNotEmpty();

        log.info("Deleting test keycloak client object: {}", data);
        client.resource(data).delete();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            var clientResource = client.resource(data).get();
            assertThat(clientResource).isNull();
        });

        foundClients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(foundClients).isEmpty();
    }

    @Test
    @Order(6)
    void deleteAlreadyDeletedUIClient() {
        var CLIENT_ID = "test-ui-client-min-ops-for-del";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setType(KeycloakAdminService.UI_TYPE);
        kcClientSpec.setRealm("quarkus");
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        data.setSpec(kcClientSpec);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var foundClients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(foundClients).isNotEmpty();

        keycloak.realm(REALM_QUARKUS).clients().get(foundClients.get(0).getId()).remove();

        log.info("Deleting test keycloak client object: {}", data);
        client.resource(data).delete();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            var clientResource = client.resource(data).get();
            assertThat(clientResource).isNull();
        });

        foundClients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(foundClients).isEmpty();
    }

    @Test
    @Order(10)
    void createMachineClient() {
        var CLIENT_ID = "test-client";
        var CLIENT_SECRET = "test-client-secret";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.MACHINE_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setPassword(CLIENT_SECRET);
        kcConfig.setDefaultClientScopes(List.of("create-scope-1", "create-scope-2"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "create.values.1", "create.attr.2", "create.values.2"));
        data.setSpec(kcClientSpec);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var clients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(clients).isNotEmpty();
        var clientRep = clients.get(0);
        assertThat(clientRep.getDescription()).isEqualTo(kcConfig.getDescription());
        // validate that attributes are all in
        assertThat(clientRep.getAttributes()).containsAllEntriesOf(kcConfig.getAttributes());

        var token = keycloakClient.getClientAccessToken(CLIENT_ID, CLIENT_SECRET);
        assertThat(token).isNotNull();

        var jws = resolveToken(token);
        assertThat((String) jws.getClaim(UI_TOKEN_CLIENT_CLAIM_NAME)).isEqualTo(CLIENT_ID);
        var scopeString = (String) jws.getClaim(SCOPE_CLAIM_NAME);
        var scopes = scopeString.split(" ");
        // validate all scopes are in
        assertThat(scopes).containsAll(kcConfig.getDefaultClientScopes());
    }

    @Test
    @Order(11)
    void updateMachineClient() {
        var CLIENT_ID = "test-client";
        var CLIENT_SECRET = "test-client-secret";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.MACHINE_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setPassword(CLIENT_SECRET);
        kcConfig.setDefaultClientScopes(List.of("create-scope-1", "update-scope-2"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "create.values.1.update", "update.attr.2", "update.values.2"));
        data.setSpec(kcClientSpec);

        log.info("Updating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is UPDATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.UPDATED);
        });

        var clients = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID);
        assertThat(clients).isNotEmpty();
        var clientRep = clients.get(0);
        assertThat(clientRep.getDescription()).isEqualTo(kcConfig.getDescription());
        // validate that attributes are all in
        assertThat(clientRep.getAttributes()).containsAllEntriesOf(kcConfig.getAttributes());

        var token = keycloakClient.getClientAccessToken(CLIENT_ID, CLIENT_SECRET);
        assertThat(token).isNotNull();

        var jws = resolveToken(token);
        assertThat((String) jws.getClaim(UI_TOKEN_CLIENT_CLAIM_NAME)).isEqualTo(CLIENT_ID);
        var scopeString = (String) jws.getClaim(SCOPE_CLAIM_NAME);
        var scopes = scopeString.split(" ");
        // validate all scopes are in
        assertThat(scopes).containsAll(kcConfig.getDefaultClientScopes());

        // update machine client with empty spec
        data.setSpec(null);
        log.info("Updating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.UPDATED);
        });
    }

    @Test
    void updateMachinePwdClient() {
        var CLIENT_ID = "test-client-pwd-chg";
        var CLIENT_SECRET = "test-client-secret";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.MACHINE_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setPassword(CLIENT_SECRET);
        kcConfig.setDefaultClientScopes(List.of("create-scope-1", "update-scope-2"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "create.values.1.update", "update.attr.2", "update.values.2"));
        data.setSpec(kcClientSpec);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is UPDATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var secret = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID).get(0).getSecret();
        log.info("Old secret {}", secret);

        // update the password
        var NEW_CLIENT_PASSWORD = "test-client-secret-new";
        data.getSpec().getKcConfig().setPassword(NEW_CLIENT_PASSWORD);

        log.info("Updating test keycloak client with new password object: {}", data);
        client.resource(data).update();

        log.info("Waiting 4 seconds and status is UPDATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.UPDATED);
        });

        secret = keycloak.realm(REALM_QUARKUS).clients().findByClientId(CLIENT_ID).get(0).getSecret();
        log.info("New secret {}", secret);

        var tokenWithOldPwd = keycloakClient.getClientAccessToken(CLIENT_ID, CLIENT_SECRET);
        var tokenWithNewPwd = keycloakClient.getClientAccessToken(CLIENT_ID, NEW_CLIENT_PASSWORD);

        assertThat(tokenWithOldPwd).isNull();
        assertThat(tokenWithNewPwd).isNotNull();
    }

    @Test
    void createUpdatePasswordFromSecretTest() {
        Base64.Encoder encoder = Base64.getEncoder();

        var CLIENT_ID = "test-machine-secret-client";
        var CLIENT_SECRET = "test-client-secret";
        var CLIENT_PWD_SECRET = "test-machine-secret-client-secret";
        var CLIENT_PWD_KEY = "pwd";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setPasswordKey(CLIENT_PWD_KEY);
        kcClientSpec.setPasswordSecrets(CLIENT_PWD_SECRET);
        kcClientSpec.setType(KeycloakAdminService.MACHINE_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setPassword("someRandomPwdShouldBeIgnored");
        kcConfig.setDefaultClientScopes(List.of("create-scope-1", "create-scope-2"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "create.values.1", "create.attr.2", "create.values.2"));
        data.setSpec(kcClientSpec);

        Secret secret = new Secret();
        secret.setMetadata(new ObjectMetaBuilder().withName(kcClientSpec.getPasswordSecrets())
                .withNamespace(client.getNamespace()).build());
        secret.setData(Map.of(kcClientSpec.getPasswordKey(), encoder.encodeToString(CLIENT_SECRET.getBytes())));

        log.info("Creating secret object: {}", secret);
        client.resource(secret).serverSideApply();

        log.info("Creating keycloak client object {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is CREATED");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.CREATED);
        });

        var token = keycloakClient.getClientAccessToken(CLIENT_ID, CLIENT_SECRET);
        assertThat(token).isNotNull();

        // update the password
        var CLIENT_SECRET_NEW = "new-machine-client-secret";
        secret.setData(Map.of(kcClientSpec.getPasswordKey(), encoder.encodeToString(CLIENT_SECRET_NEW.getBytes())));
        log.info("Updating secret object: {}", secret);
        client.resource(secret).update();

        log.info("Waiting 6 seconds and status is UPDATED");

        await().pollDelay(6, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.UPDATED);
        });
        // old password token empty
        var oldSecretToken = keycloakClient.getClientAccessToken(CLIENT_ID, CLIENT_SECRET);
        assertThat(oldSecretToken).isNull();

        // new password generates token
        var newSecretToken = keycloakClient.getClientAccessToken(CLIENT_ID, CLIENT_SECRET_NEW);
        assertThat(newSecretToken).isNotNull();
    }

    @Test
    void clientErrorTest() {
        operator.start();

        // Null specification
        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName("null-spec").withNamespace(client.getNamespace()).build());
        data.setSpec(null);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is still null");

        KeycloakClient finalData = data;
        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(finalData).get().getStatus();
            assertThat(mfeStatus).isNull();
        });

        // empty specification
        data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName("empty-spec").withNamespace(client.getNamespace()).build());
        data.setSpec(new KeycloakClientSpec());

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status has an ERROR");

        KeycloakClient finalData2 = data;
        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(finalData2).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });

        // empty config
        data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName("empty-config").withNamespace(client.getNamespace()).build());
        data.setSpec(new KeycloakClientSpec());
        data.getSpec().setKcConfig(new KCConfig());

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status has an ERROR");

        KeycloakClient finalData3 = data;
        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(finalData3).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });
    }

    @Test
    void clientNotExistingRealmTest() {
        var CLIENT_ID = "wrong-type";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        data.setSpec(new KeycloakClientSpec());
        data.getSpec().setType(KeycloakAdminService.MACHINE_TYPE);
        data.getSpec().setRealm("NOT_EXISTING");
        data.getSpec().setKcConfig(new KCConfig());
        data.getSpec().getKcConfig().setClientId(CLIENT_ID);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status has an ERROR");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });
    }

    @Test
    void clientWrongTypeTest() {
        var CLIENT_ID = "wrong-type";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        data.setSpec(new KeycloakClientSpec());
        data.getSpec().setType("CUSTOM_TYPE");
        data.getSpec().setKcConfig(new KCConfig());
        data.getSpec().getKcConfig().setClientId(CLIENT_ID);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status has an ERROR");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });
    }

    @Test
    void createUpdatePasswordFromSecretErrorTest() {
        Base64.Encoder encoder = Base64.getEncoder();

        var CLIENT_ID = "test-machine-secret-client-err1";
        var CLIENT_SECRET = "test-client-secret";
        var CLIENT_PWD_SECRET = "err-machine-secret-client-secret";
        var CLIENT_PWD_KEY = "pwd";
        operator.start();

        KeycloakClient data = new KeycloakClient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setPasswordSecrets(CLIENT_PWD_SECRET);
        kcClientSpec.setType(KeycloakAdminService.MACHINE_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setPassword("someRandomPwdShouldBeIgnored");
        kcConfig.setDefaultClientScopes(List.of("create-scope-1", "create-scope-2"));
        kcConfig.setAttributes(Maps.of("create.attr.1", "create.values.1", "create.attr.2", "create.values.2"));
        data.setSpec(kcClientSpec);

        Secret secret = new Secret();
        secret.setMetadata(new ObjectMetaBuilder().withName(kcClientSpec.getPasswordSecrets())
                .withNamespace(client.getNamespace()).build());
        secret.setData(Map.of("other-key", encoder.encodeToString(CLIENT_SECRET.getBytes())));

        log.info("Creating secret object: {}", secret);
        client.resource(secret).serverSideApply();

        // test when the client does container pwd secret name but not the pwd key
        log.info("Creating keycloak client object {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is ERROR");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getResponseCode()).isEqualTo(500);
            assertThat(mfeStatus.getMessage()).isEqualTo("Secret key is mandatory. No key found!");
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });

        // test error when secret does not contain the right key
        KeycloakClient data1 = new KeycloakClient();
        data1.setMetadata(new ObjectMetaBuilder().withName("test-machine-secret-client-err2")
                .withNamespace(client.getNamespace()).build());
        data1.setSpec(kcClientSpec);
        kcClientSpec.setPasswordKey(CLIENT_PWD_KEY);

        log.info("Creating keycloak client object {}", data1);
        client.resource(data1).serverSideApply();

        log.info("Waiting 4 seconds and status is ERROR");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data1).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getMessage()).isEqualTo("Secret key is mandatory. No key secret found!");
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });

        // test error when secret has the right key but the value is empty
        secret.setData(Map.of(CLIENT_PWD_KEY, ""));

        log.info("Update secret object {}", secret);
        client.resource(secret).update();

        log.info("Waiting 4 seconds and status is ERROR");

        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(data1).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getMessage()).isEqualTo("Secret key '" + CLIENT_PWD_KEY + "' is mandatory. No value found!");
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });
    }
}
