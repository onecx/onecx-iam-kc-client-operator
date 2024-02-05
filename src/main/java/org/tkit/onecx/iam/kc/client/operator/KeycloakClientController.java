package org.tkit.onecx.iam.kc.client.operator;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tkit.onecx.iam.kc.client.operator.service.KeycloakAdminService;
import org.tkit.onecx.iam.kc.client.operator.service.TypeNotSupportedException;

import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.source.filter.OnAddFilter;
import io.javaoperatorsdk.operator.processing.event.source.filter.OnUpdateFilter;

@ControllerConfiguration(name = "kc", onAddFilter = KeycloakClientController.AddFilter.class, onUpdateFilter = KeycloakClientController.UpdateFilter.class)
public class KeycloakClientController implements Reconciler<Keycloakclient>, ErrorStatusHandler<Keycloakclient> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakClientController.class);

    @Inject
    KeycloakAdminService service;

    @Override
    public ErrorStatusUpdateControl<Keycloakclient> updateErrorStatus(Keycloakclient keycloakClient,
            Context<Keycloakclient> context, Exception e) {
        int responseCode = -1;
        if (e.getCause() instanceof WebApplicationException re) {
            responseCode = re.getResponse().getStatus();
        }
        if (e.getCause() instanceof TypeNotSupportedException) {
            // set the response code as 500 INTERNAL Server error
            responseCode = 500;
        }

        log.error("Error reconcile resource", e);
        var status = new KeycloakClientStatus();
        String clientId = null;
        if (keycloakClient.getSpec().getKcConfig() != null) {
            clientId = keycloakClient.getSpec().getKcConfig().getClientId() != null
                    ? keycloakClient.getSpec().getKcConfig().getClientId()
                    : null;
        }
        status.setClientId(clientId);
        status.setResponseCode(responseCode);
        status.setStatus(KeycloakClientStatus.Status.ERROR);
        status.setMessage(e.getMessage());
        keycloakClient.setStatus(status);
        return ErrorStatusUpdateControl.updateStatus(keycloakClient);
    }

    @Override
    public UpdateControl<Keycloakclient> reconcile(Keycloakclient keycloakClient, Context<Keycloakclient> context)
            throws Exception {
        log.info("Reconcile resource: {} appId: {}", keycloakClient.getMetadata().getName(),
                keycloakClient.getSpec().getKcConfig().getClientId());

        int responseCode = service.createClient(keycloakClient);

        updateStatusPojo(keycloakClient, responseCode);
        log.info("Resource '{}' reconciled - updating status", keycloakClient.getMetadata().getName());
        return UpdateControl.updateStatus(keycloakClient);
    }

    private void updateStatusPojo(Keycloakclient keycloakClient, int responseCode) {
        KeycloakClientStatus result = new KeycloakClientStatus();
        KeycloakClientSpec spec = keycloakClient.getSpec();
        result.setClientId(spec.getKcConfig().getClientId());
        result.setResponseCode(responseCode);
        var status = KeycloakClientStatus.Status.UNDEFINED;
        if (responseCode == 200) {
            status = KeycloakClientStatus.Status.UPDATED;
        }
        if (responseCode == 201) {
            status = KeycloakClientStatus.Status.CREATED;
        }

        result.setStatus(status);
        keycloakClient.setStatus(result);
    }

    public static class AddFilter implements OnAddFilter<Keycloakclient> {

        @Override
        public boolean accept(Keycloakclient resource) {
            return resource.getSpec() != null;
        }
    }

    public static class UpdateFilter implements OnUpdateFilter<Keycloakclient> {

        @Override
        public boolean accept(Keycloakclient newResource, Keycloakclient oldResource) {
            return newResource.getSpec() != null;
        }
    }
}
