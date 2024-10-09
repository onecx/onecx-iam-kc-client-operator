package org.tkit.onecx.iam.kc.client.operator;

import jakarta.inject.Singleton;

import org.tkit.onecx.iam.kc.client.operator.config.KCClientConfig;

import io.javaoperatorsdk.operator.api.config.LeaderElectionConfiguration;

@Singleton
public class LeaderConfiguration extends LeaderElectionConfiguration {

    public LeaderConfiguration(KCClientConfig config) {
        super(config.leaderElectionConfig().leaseName());
    }
}
