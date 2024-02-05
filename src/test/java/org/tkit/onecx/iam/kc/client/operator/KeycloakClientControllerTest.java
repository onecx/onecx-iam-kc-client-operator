package org.tkit.onecx.iam.kc.client.operator;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

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

        Keycloakclient data = new Keycloakclient();
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

        Keycloakclient data = new Keycloakclient();
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
        kcConfig.setSecret(CLIENT_ID);
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

        Keycloakclient data = new Keycloakclient();
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

        Keycloakclient data = new Keycloakclient();
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
    }

    @Test
    @Order(10)
    void createMachineClient() {
        var CLIENT_ID = "test-client";
        var CLIENT_SECRET = "test-client-secret";
        operator.start();

        Keycloakclient data = new Keycloakclient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.MACHINE_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setSecret(CLIENT_SECRET);
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

        Keycloakclient data = new Keycloakclient();
        data.setMetadata(new ObjectMetaBuilder().withName(CLIENT_ID).withNamespace(client.getNamespace()).build());
        var kcClientSpec = new KeycloakClientSpec();
        kcClientSpec.setRealm(REALM_QUARKUS);
        kcClientSpec.setType(KeycloakAdminService.MACHINE_TYPE);
        var kcConfig = new KCConfig();
        kcClientSpec.setKcConfig(kcConfig);
        kcConfig.setClientId(CLIENT_ID);
        kcConfig.setSecret(CLIENT_SECRET);
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
    @Order(100)
    void clientErrorTest() {

        operator.start();

        // Null specification
        Keycloakclient data = new Keycloakclient();
        data.setMetadata(new ObjectMetaBuilder().withName("null-spec").withNamespace(client.getNamespace()).build());
        data.setSpec(null);

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status is still null");

        Keycloakclient finalData = data;
        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(finalData).get().getStatus();
            assertThat(mfeStatus).isNull();
        });

        // empty specification
        data = new Keycloakclient();
        data.setMetadata(new ObjectMetaBuilder().withName("empty-spec").withNamespace(client.getNamespace()).build());
        data.setSpec(new KeycloakClientSpec());

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status has an ERROR");

        Keycloakclient finalData2 = data;
        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(finalData2).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });

        // empty config
        data = new Keycloakclient();
        data.setMetadata(new ObjectMetaBuilder().withName("empty-config").withNamespace(client.getNamespace()).build());
        data.setSpec(new KeycloakClientSpec());
        data.getSpec().setKcConfig(new KCConfig());

        log.info("Creating test keycloak client object: {}", data);
        client.resource(data).serverSideApply();

        log.info("Waiting 4 seconds and status has an ERROR");

        Keycloakclient finalData3 = data;
        await().pollDelay(4, SECONDS).untilAsserted(() -> {
            KeycloakClientStatus mfeStatus = client.resource(finalData3).get().getStatus();
            assertThat(mfeStatus).isNotNull();
            assertThat(mfeStatus.getStatus()).isNotNull().isEqualTo(KeycloakClientStatus.Status.ERROR);
        });
    }

    @Test
    @Order(101)
    void clientNotExistingRealmTest() {
        var CLIENT_ID = "wrong-type";
        operator.start();

        Keycloakclient data = new Keycloakclient();
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
    @Order(103)
    void clientWrongTypeTest() {
        var CLIENT_ID = "wrong-type";
        operator.start();

        Keycloakclient data = new Keycloakclient();
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
}
