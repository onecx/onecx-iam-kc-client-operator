package org.tkit.onecx.iam.kc.client.operator.models;

import org.keycloak.representations.idm.ClientRepresentation;
import org.mapstruct.Mapper;

@Mapper
public interface ClientRepresentationMapper {

    ExtClientRepresentation create(ClientRepresentation data);
}
