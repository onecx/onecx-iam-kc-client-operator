package org.tkit.onecx.iam.kc.client.operator.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "onecx.iam.kc.client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface KCClientConfig {

    /**
     * Define realm where to insert/update/delete the clients
     */
    @WithName("realm")
    @WithDefault("onecx")
    String realm();

    /**
     * Default configuration for the (ui|machine) clients
     */
    @WithName("config")
    Map<String, KCDefaultConfig> config();

}
