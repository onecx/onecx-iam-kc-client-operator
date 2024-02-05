package org.tkit.onecx.iam.kc.client.test;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwx.JsonWebStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

public abstract class AbstractTest {

    final static Logger log = LoggerFactory.getLogger(AbstractTest.class);

    public static String REALM_QUARKUS = "quarkus";
    public static String USER_ALICE = "alice";
    public static String USER_BOB = "bob";

    public static String UI_TOKEN_CLIENT_CLAIM_NAME = Claims.azp.name();

    public static String MACHINE_TOKEN_CLIENT_CLAIM_NAME = "client_id";
    public static String SCOPE_CLAIM_NAME = "scope";

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
