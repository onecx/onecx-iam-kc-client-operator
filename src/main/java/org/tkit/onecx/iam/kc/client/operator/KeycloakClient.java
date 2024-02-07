package org.tkit.onecx.iam.kc.client.operator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;

@Version("v1")
@Group("onecx.tkit.org")
public class KeycloakClient extends CustomResource<KeycloakClientSpec, KeycloakClientStatus> implements Namespaced {
}
