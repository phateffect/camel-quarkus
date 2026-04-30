/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.rocketmq.it;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

public class RocketmqTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(RocketmqTestResource.class);
    private static final String ROCKETMQ_IMAGE = ConfigProvider.getConfig()
            .getValue("rocketmq.container.image", String.class);
    private static final int NAMESRV_PORT = 9876;
    private static final int BROKER_PORT = 10911;

    private GenericContainer<?> namesrvContainer;
    private GenericContainer<?> brokerContainer;

    @Override
    public Map<String, String> start() {
        Network network = Network.newNetwork();

        namesrvContainer = new FixedHostPortGenericContainer<>(ROCKETMQ_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("namesrv")
                .withFixedExposedPort(NAMESRV_PORT, NAMESRV_PORT)
                .withCommand("sh", "mqnamesrv")
                .waitingFor(Wait.forLogMessage(".*The Name Server boot success.*", 1));

        namesrvContainer.start();

        String brokerConf = "brokerIP1=127.0.0.1\nbrokerName=broker-a\nbrokerClusterName=DefaultCluster\n";

        brokerContainer = new FixedHostPortGenericContainer<>(ROCKETMQ_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("broker")
                .withFixedExposedPort(BROKER_PORT, BROKER_PORT)
                .withFixedExposedPort(10909, 10909)
                .withCopyToContainer(Transferable.of(brokerConf.getBytes(StandardCharsets.UTF_8)), "/tmp/broker.conf")
                .withCommand("sh", "mqbroker", "-n", "namesrv:9876", "-c", "/tmp/broker.conf")
                .dependsOn(namesrvContainer)
                .waitingFor(Wait.forLogMessage(".*The broker\\[.*\\] boot success.*", 1));

        brokerContainer.start();

        try {
            org.testcontainers.containers.Container.ExecResult result = brokerContainer.execInContainer(
                    "sh", "mqadmin", "updateTopic", "-n", "namesrv:9876", "-t", "camel-test", "-c", "DefaultCluster");
            LOG.info("Topic creation stdout: {}", result.getStdout());
            if (result.getExitCode() != 0) {
                LOG.warn("Topic creation stderr: {}", result.getStderr());
            }
        } catch (Exception e) {
            LOG.warn("Could not create topic via mqadmin", e);
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("rocketmq.namesrv.addr", "localhost:" + NAMESRV_PORT);
        LOG.info("RocketMQ test properties: {}", properties);
        return properties;
    }

    @Override
    public void stop() {
        try {
            if (brokerContainer != null) {
                brokerContainer.stop();
            }
        } catch (Exception e) {
            LOG.error("Error stopping broker container", e);
        }
        try {
            if (namesrvContainer != null) {
                namesrvContainer.stop();
            }
        } catch (Exception e) {
            LOG.error("Error stopping nameserver container", e);
        }
    }
}
