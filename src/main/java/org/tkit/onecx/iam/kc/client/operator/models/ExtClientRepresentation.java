package org.tkit.onecx.iam.kc.client.operator.models;

import org.keycloak.representations.idm.ClientRepresentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "type" })
public class ExtClientRepresentation extends ClientRepresentation {

}
