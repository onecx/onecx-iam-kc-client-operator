package org.tkit.onecx.iam.kc.client.operator.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigDocFilename("onecx-iam-kc-client-operator.adoc")
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

    /**
     * Leader election configuration
     */
    @WithName("leader-election")
    LeaderElectionConfig leaderElectionConfig();

    /**
     * Leader election config
     */
    interface LeaderElectionConfig {

        /**
         * Lease name
         */
        @WithName("lease-name")
        @WithDefault("onecx-product-store-slot-operator-lease")
        String leaseName();
    }
}
