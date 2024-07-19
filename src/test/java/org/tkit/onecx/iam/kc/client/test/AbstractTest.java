package org.tkit.onecx.iam.kc.client.test;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwx.JsonWebStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

public abstract class AbstractTest {

    static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

    public static final String REALM_QUARKUS = "quarkus";
    public static final String USER_ALICE = "alice";

    public static final String UI_TOKEN_CLIENT_CLAIM_NAME = Claims.azp.name();

    public static final String SCOPE_CLAIM_NAME = "scope";

    public DefaultJWTCallerPrincipal resolveToken(String token) {
        try {
            var jws = (JsonWebSignature) JsonWebStructure.fromCompactSerialization(token);
            var jwtClaims = JwtClaims.parse(jws.getUnverifiedPayload());

            return new DefaultJWTCallerPrincipal(token, jws.getKeyType(), jwtClaims);
        } catch (Exception e) {
            log.error("Error parse token {}", token);
        }
        return null;
    }
}
