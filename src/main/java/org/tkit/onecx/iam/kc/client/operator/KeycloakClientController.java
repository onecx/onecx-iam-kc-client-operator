package org.tkit.onecx.iam.kc.client.operator;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tkit.onecx.iam.kc.client.operator.service.KeycloakAdminService;
import org.tkit.onecx.iam.kc.client.operator.service.TypeNotSupportedException;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.filter.OnAddFilter;
import io.javaoperatorsdk.operator.processing.event.source.filter.OnUpdateFilter;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

@ControllerConfiguration(name = "kc", namespaces = Constants.WATCH_CURRENT_NAMESPACE, onAddFilter = KeycloakClientController.AddFilter.class, onUpdateFilter = KeycloakClientController.UpdateFilter.class)
public class KeycloakClientController
        implements Reconciler<KeycloakClient>, ErrorStatusHandler<KeycloakClient>, Cleaner<KeycloakClient>,
        EventSourceInitializer<KeycloakClient> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakClientController.class);

    @Inject
    KeycloakAdminService service;

    @Override
    public ErrorStatusUpdateControl<KeycloakClient> updateErrorStatus(KeycloakClient keycloakClient,
            Context<KeycloakClient> context, Exception e) {
        int responseCode = -1;
        String message = e.getMessage();
        if (e.getCause() instanceof WebApplicationException re) {
            responseCode = re.getResponse().getStatus();
            message = re.getResponse().readEntity(String.class);
        }
        if (e.getCause() instanceof TypeNotSupportedException tnse) {
            // set the response code as 500 INTERNAL Server error
            responseCode = 500;
            message = tnse.getMessage();
        }
        if (e.getCause() instanceof MissingMandatoryKeyException mmke) {
            // set the response code as 500 INTERNAL Server error
            responseCode = 500;
            message = mmke.getMessage();
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
        status.setMessage(message);
        keycloakClient.setStatus(status);
        return ErrorStatusUpdateControl.updateStatus(keycloakClient);
    }

    @Override
    public UpdateControl<KeycloakClient> reconcile(KeycloakClient keycloakClient, Context<KeycloakClient> context)
            throws Exception {
        log.info("Reconcile resource: {} appId: {}", keycloakClient.getMetadata().getName(),
                keycloakClient.getSpec().getKcConfig().getClientId());

        Optional<Secret> secret = context.getSecondaryResource(Secret.class);
        if (secret.isPresent()) {

            String name = keycloakClient.getMetadata().getName();
            String namespace = keycloakClient.getMetadata().getNamespace();

            log.info("Reconcile password from secret for client: {} namespace: {}", name, namespace);
            byte[] password = createRequestData(keycloakClient.getSpec(), secret.get());
            keycloakClient.getSpec().getKcConfig().setPassword(new String(password));
        }

        int responseCode = service.createClient(keycloakClient);

        updateStatusPojo(keycloakClient, responseCode);
        log.info("Resource '{}' reconciled - updating status", keycloakClient.getMetadata().getName());
        return UpdateControl.updateStatus(keycloakClient);
    }

    private void updateStatusPojo(KeycloakClient keycloakClient, int responseCode) {
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

    @Override
    public DeleteControl cleanup(KeycloakClient keycloakclient, Context<KeycloakClient> context) {
        service.deleteClient(keycloakclient);
        return DeleteControl.defaultDelete();
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<KeycloakClient> context) {
        final SecondaryToPrimaryMapper<Secret> webappsMatchingTomcatName = (Secret t) -> context.getPrimaryCache()
                .list(keycloakClient -> {
                    if (keycloakClient.getSpec() != null) {
                        return t.getMetadata().getName().equals(keycloakClient.getSpec().getPasswordSecrets());
                    }
                    return false;
                })
                .map(ResourceID::fromResource)
                .collect(Collectors.toSet());

        InformerConfiguration<Secret> configuration = InformerConfiguration.from(Secret.class, context)
                .withSecondaryToPrimaryMapper(webappsMatchingTomcatName)
                .withPrimaryToSecondaryMapper(
                        (KeycloakClient primary) -> Set.of(new ResourceID(primary.getSpec().getPasswordSecrets(),
                                primary.getMetadata().getNamespace())))
                .build();
        return EventSourceInitializer
                .nameEventSources(new InformerEventSource<>(configuration, context));
    }

    public static class AddFilter implements OnAddFilter<KeycloakClient> {

        @Override
        public boolean accept(KeycloakClient resource) {
            return resource.getSpec() != null;
        }
    }

    public static class UpdateFilter implements OnUpdateFilter<KeycloakClient> {

        @Override
        public boolean accept(KeycloakClient newResource, KeycloakClient oldResource) {
            return newResource.getSpec() != null;
        }
    }

    private static byte[] createRequestData(KeycloakClientSpec spec, Secret secret) throws MissingMandatoryKeyException {
        Map<String, String> data = secret.getData();

        String key = spec.getPasswordKey();
        if (key == null) {
            throw new MissingMandatoryKeyException("Secret key is mandatory. No key found!");
        }
        if (!data.containsKey(key)) {
            throw new MissingMandatoryKeyException("Secret key is mandatory. No key secret found!");
        }
        String value = data.get(key);
        if (value.isEmpty()) {
            throw new MissingMandatoryKeyException("Secret key '" + key + "' is mandatory. No value found!");
        }
        return Base64.getDecoder().decode(value);
    }

    public static class MissingMandatoryKeyException extends RuntimeException {

        public MissingMandatoryKeyException(String msg) {
            super(msg);
        }
    }
}
