package org.tkit.onecx.iam.kc.client.operator.service;

import java.util.List;

public class TypeNotSupportedException extends RuntimeException {

    public TypeNotSupportedException(String typeName) {
        super("Client type " + typeName + " is not supported. Supported options only: "
                + List.of(KeycloakAdminService.UI_TYPE, KeycloakAdminService.MACHINE_TYPE));
    }

}
