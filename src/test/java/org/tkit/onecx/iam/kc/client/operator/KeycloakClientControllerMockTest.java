package org.tkit.onecx.iam.kc.client.operator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.*;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.*;
import org.tkit.onecx.iam.kc.client.operator.service.KeycloakAdminService;
import org.tkit.onecx.iam.kc.client.test.AbstractTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class KeycloakClientControllerMockTest extends AbstractTest {

    @Inject
    KeycloakAdminService kas;

    @Test
    void testThrowException() throws NoSuchMethodException {

        MockClientResource mockClientResource = new MockClientResource();

        var addOptMethod = KeycloakAdminService.class.getDeclaredMethod("addOptClientScope", ClientResource.class,
                String.class);
        addOptMethod.setAccessible(true);
        assertDoesNotThrow(() -> addOptMethod.invoke(kas, mockClientResource, "test"));

        var removeOptMethod = KeycloakAdminService.class.getDeclaredMethod("removeOptClientScope", ClientResource.class,
                String.class);
        removeOptMethod.setAccessible(true);
        assertDoesNotThrow(() -> removeOptMethod.invoke(kas, mockClientResource, "test"));

        var addDefaultMethod = KeycloakAdminService.class.getDeclaredMethod("addDefaultClientScope", ClientResource.class,
                String.class);
        addDefaultMethod.setAccessible(true);
        assertDoesNotThrow(() -> addDefaultMethod.invoke(kas, mockClientResource, "test"));

        var removeDefaultMethod = KeycloakAdminService.class.getDeclaredMethod("removeDefaultClientScope", ClientResource.class,
                String.class);
        removeDefaultMethod.setAccessible(true);
        assertDoesNotThrow(() -> removeDefaultMethod.invoke(kas, mockClientResource, "test"));

    }

    public static class MockClientResource implements ClientResource {

        @Override
        public ManagementPermissionReference setPermissions(
                ManagementPermissionRepresentation managementPermissionRepresentation) {
            return null;
        }

        @Override
        public ManagementPermissionReference getPermissions() {
            return null;
        }

        @Override
        public ProtocolMappersResource getProtocolMappers() {
            return null;
        }

        @Override
        public ClientRepresentation toRepresentation() {
            return null;
        }

        @Override
        public void update(ClientRepresentation clientRepresentation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CredentialRepresentation generateNewSecret() {
            return null;
        }

        @Override
        public CredentialRepresentation getSecret() {
            return null;
        }

        @Override
        public ClientRepresentation regenerateRegistrationAccessToken() {
            return null;
        }

        @Override
        public ClientAttributeCertificateResource getCertficateResource(String s) {
            return null;
        }

        @Override
        public String getInstallationProvider(String s) {
            return null;
        }

        @Override
        public Response getInstallationProviderAsResponse(String s) {
            return Response.ok().build();
        }

        @Override
        public Map<String, Integer> getApplicationSessionCount() {
            return null;
        }

        @Override
        public List<UserSessionRepresentation> getUserSessions(Integer integer, Integer integer1) {
            return null;
        }

        @Override
        public Map<String, Long> getOfflineSessionCount() {
            return null;
        }

        @Override
        public List<UserSessionRepresentation> getOfflineUserSessions(Integer integer, Integer integer1) {
            return null;
        }

        @Override
        public void pushRevocation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RoleMappingResource getScopeMappings() {
            return null;
        }

        @Override
        public RolesResource roles() {
            return null;
        }

        @Override
        public ClientScopeEvaluateResource clientScopesEvaluate() {
            return null;
        }

        @Override
        public List<ClientScopeRepresentation> getDefaultClientScopes() {
            return null;
        }

        @Override
        public void addDefaultClientScope(String s) {
            throw new RuntimeException("error");
        }

        @Override
        public void removeDefaultClientScope(String s) {
            throw new RuntimeException("error");
        }

        @Override
        public List<ClientScopeRepresentation> getOptionalClientScopes() {
            return null;
        }

        @Override
        public void addOptionalClientScope(String s) {
            throw new RuntimeException("error");
        }

        @Override
        public void removeOptionalClientScope(String s) {
            throw new RuntimeException("error");
        }

        @Override
        public UserRepresentation getServiceAccountUser() {
            return null;
        }

        @Override
        public void registerNode(Map<String, String> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unregisterNode(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GlobalRequestResult testNodesAvailable() {
            return null;
        }

        @Override
        public AuthorizationResource authorization() {
            return null;
        }

        @Override
        public CredentialRepresentation getClientRotatedSecret() {
            return null;
        }

        @Override
        public void invalidateRotatedSecret() {
            throw new UnsupportedOperationException();
        }
    }

}
