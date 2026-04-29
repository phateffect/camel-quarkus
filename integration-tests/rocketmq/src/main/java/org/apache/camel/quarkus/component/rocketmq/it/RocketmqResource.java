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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;

@Path("/rocketmq")
@ApplicationScoped
public class RocketmqResource {

    @Inject
    CamelContext camelContext;

    @Path("/component")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String component() {
        Component component = camelContext.getComponent("rocketmq");
        return component.getClass().getName();
    }

    @Path("/endpoint")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String endpoint() {
        Endpoint endpoint = camelContext.getEndpoint(
                "rocketmq:test-topic?consumerGroup=test-consumer&namesrvAddr=localhost:9876&producerGroup=test-producer");
        return endpoint.getClass().getName();
    }
}
