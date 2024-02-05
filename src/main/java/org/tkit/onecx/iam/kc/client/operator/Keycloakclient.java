package org.tkit.onecx.iam.kc.client.operator;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;

@Version("v1")
@Group("onecx.tkit.org")
public class Keycloakclient extends CustomResource<KeycloakClientSpec, KeycloakClientStatus> {
}
