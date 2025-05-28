package org.tkit.onecx.iam.kc.client.operator;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KeycloakClientStatus {

    @JsonProperty("observedGeneration")
    private Long observedGeneration;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("response-code")
    private int responseCode;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("message")
    private String message;

    public enum Status {

        ERROR,

        CREATED,

        UPDATED,

        UNDEFINED;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getObservedGeneration() {
        return observedGeneration;
    }
}
