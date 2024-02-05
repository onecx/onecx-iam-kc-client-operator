package org.tkit.onecx.iam.kc.client.operator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeycloakClientSpec {

    private String realm;

    @JsonProperty(required = true)
    private String type;

    @JsonProperty(required = true)
    private KCConfig kcConfig;

    public String getRealm() {
        return realm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public KCConfig getKcConfig() {
        return kcConfig;
    }

    public void setKcConfig(KCConfig kcConfig) {
        this.kcConfig = kcConfig;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

}
