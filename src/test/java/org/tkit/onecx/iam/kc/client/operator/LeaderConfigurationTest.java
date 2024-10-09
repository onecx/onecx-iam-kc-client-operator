package org.tkit.onecx.iam.kc.client.operator;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.iam.kc.client.operator.config.KCClientConfig;
import org.tkit.onecx.iam.kc.client.test.AbstractTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LeaderConfigurationTest extends AbstractTest {

    @Inject
    KCClientConfig dataConfig;

    @Inject
    LeaderConfiguration leaderConfiguration;

    @Test
    void testLeaderConfiguration() {
        assertThat(dataConfig).isNotNull();
        assertThat(dataConfig.leaderElectionConfig()).isNotNull();
        assertThat(leaderConfiguration).isNotNull();
        assertThat(leaderConfiguration.getLeaseName()).isNotNull().isEqualTo(dataConfig.leaderElectionConfig().leaseName());
    }
}
