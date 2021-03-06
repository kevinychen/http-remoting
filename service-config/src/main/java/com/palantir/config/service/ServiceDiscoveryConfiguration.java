/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.config.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.palantir.remoting.ssl.SslConfiguration;
import com.palantir.tokens.auth.BearerToken;
import io.dropwizard.util.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Style;

/**
 * Configuration class that contains a map of {@code serviceName}s and their respective {@link ServiceConfiguration}s
 * and provides utility methods for apps to build service clients.
 */
@Immutable
@JsonDeserialize(as = ImmutableServiceDiscoveryConfiguration.class)
@Style(visibility = Style.ImplementationVisibility.PACKAGE)
public abstract class ServiceDiscoveryConfiguration {

    /**
     * Fallback API token to be used if the service specific API token is not defined in the
     * {@link ServiceConfiguration}.
     */
    @JsonProperty("apiToken")
    public abstract Optional<BearerToken> defaultApiToken();

    /**
     * Fallback SSL Configuration to be used if the service specific SSL configuration is not defined in the
     * {@link ServiceConfiguration}.
     */
    @JsonProperty("security")
    public abstract Optional<SslConfiguration> defaultSecurity();

    @JsonProperty("services")
    abstract Map<String, ServiceConfiguration> originalServices();

    /**
     * Default global proxy configuration for connecting to the services.
     */
    @JsonProperty("proxyConfiguration")
    public abstract Optional<ProxyConfiguration> defaultProxyConfiguration();

    /**
     * Default global connect timeout.
     */
    @JsonProperty("connectTimeout")
    public abstract Optional<Duration> defaultConnectTimeout();

    /**
     * Default global read timeout.
     */
    @JsonProperty("readTimeout")
    public abstract Optional<Duration> defaultReadTimeout();

    /**
     * A map of {@code serviceName}s as the keys and their respective {@link ServiceConfiguration}s as the values.
     */
    @Lazy
    @SuppressWarnings("checkstyle:designforextension")
    @JsonIgnore
    public Map<String, ServiceConfiguration> getServices() {
        Map<String, ServiceConfiguration> intializedServices = new HashMap<String, ServiceConfiguration>();

        for (Map.Entry<String, ServiceConfiguration> entry : originalServices().entrySet()) {
            ServiceConfiguration originalConfig = entry.getValue();
            ServiceConfiguration configWithDefaults = ImmutableServiceConfiguration.builder()
                    .apiToken(originalConfig.apiToken().or(defaultApiToken()))
                    .security(originalConfig.security().or(defaultSecurity()))
                    .proxyConfiguration(originalConfig.proxyConfiguration().or(defaultProxyConfiguration()))
                    .connectTimeout(originalConfig.connectTimeout().or(defaultConnectTimeout()))
                    .readTimeout(originalConfig.readTimeout().or(defaultReadTimeout()))
                    .uris(originalConfig.uris()).build();

            intializedServices.put(entry.getKey(), configWithDefaults);
        }

        return Collections.unmodifiableMap(intializedServices);
    }

    /**
     * Uses the provided service name as the key to retrieve the API token for the respective
     * {@link ServiceConfiguration}. If the API token is not defined in the {@link ServiceConfiguration}, the default
     * API token will be returned.
     *
     * @param serviceName the name of the service
     * @return the API token for the specified service
     */
    public final Optional<BearerToken> getApiToken(String serviceName) {
        return getServiceConfig(serviceName).apiToken();
    }

    /**
     * Uses the service name as the key to retrieve the {@link SslConfiguration} for the respective
     * {@link ServiceConfiguration}. If the {@link SslConfiguration} is not defined in the {@link ServiceConfiguration},
     * the default {@link SslConfiguration} will be returned.
     *
     * @param serviceName the name of the service
     * @return the {@link SslConfiguration} for the specified service
     */
    public final Optional<SslConfiguration> getSecurity(String serviceName) {
        return getServiceConfig(serviceName).security();
    }

    /**
     * Uses the service name as the key to retrieve the service URIs for the respective {@link ServiceConfiguration}.
     *
     * @param serviceName the name of the service
     * @return the URIs for the specified service
     */
    public final List<String> getUris(String serviceName) {
        return getServiceConfig(serviceName).uris();
    }

    /**
     * Uses the provided service name as the key to retrieve the proxy configuration for the respective
     * {@link ServiceConfiguration}.
     *
     * @param serviceName the name of the service
     * @return the {@link ProxyConfiguration} for the specified service
     */
    public final Optional<ProxyConfiguration> getProxyConfiguration(String serviceName) {
        return getServiceConfig(serviceName).proxyConfiguration();
    }

    public final Optional<Duration> getConnectTimeout(String serviceName) {
        return getServiceConfig(serviceName).connectTimeout();
    }

    public final Optional<Duration> getReadTimeout(String serviceName) {
        return getServiceConfig(serviceName).readTimeout();
    }

    /**
     * Checks if a service is enabled.
     * <p>
     * A service is enabled iff the configuration for it exists and its list of URIs is non-empty.
     *
     * @param serviceName the name of the service
     * @return whether or not the service is enabled
     */
    public final boolean isServiceEnabled(String serviceName) {
        ServiceConfiguration serviceConfig = getServices().get(serviceName);

        return serviceConfig != null && !serviceConfig.uris().isEmpty();
    }

    private ServiceConfiguration getServiceConfig(String serviceName) {
        return Preconditions.checkNotNull(getServices().get(serviceName),
                "Unable to find the configuration for " + serviceName + ".");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ImmutableServiceDiscoveryConfiguration.Builder {}
}
