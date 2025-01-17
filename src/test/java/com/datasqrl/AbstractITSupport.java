/*
 * Copyright © 2024 DataSQRL (contact@datasqrl.com)
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
package com.datasqrl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.nextbreakpoint.flinkclient.api.ApiException;
import com.nextbreakpoint.flinkclient.api.FlinkApi;
import com.nextbreakpoint.flinkclient.model.JobIdsWithStatusOverview;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
public class AbstractITSupport {

  protected static FlinkApi client;

  @BeforeAll
  static void waitServiceStart() throws ApiException {
    client = new FlinkApi();
    client.getApiClient().setBasePath(serverUrl());

    int timeout = (int) TimeUnit.MINUTES.toMillis(2);
    client.getApiClient().setConnectTimeout(timeout);
    client.getApiClient().setReadTimeout(timeout);
    client.getApiClient().setWriteTimeout(timeout);

    await()
        .atMost(100, SECONDS)
        .pollInterval(500, MILLISECONDS)
        .ignoreExceptions()
        .until(
            () -> {
              log.info("Awaiting for custody-api");
              return client.getJobs() != null;
            });

    final JobIdsWithStatusOverview statusOverview = client.getJobs();
    statusOverview
        .getJobs()
        .forEach(
            jobIdWithStatus -> {
              try {
                client.terminateJob(jobIdWithStatus.getId(), "cancel");
              } catch (ApiException ignored) {
              }
            });
  }

  protected static String serverUrl() {
    var serverPort = Optional.ofNullable(System.getProperty("server.port")).orElse("8081");
    return "http://localhost:" + serverPort;
  }
}
