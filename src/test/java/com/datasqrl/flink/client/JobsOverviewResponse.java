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
package com.datasqrl.flink.client;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Accessors(fluent = true)
@Value
public class JobsOverviewResponse {
  List<JobInfo> jobs;

  @Builder
  @Jacksonized
  @Accessors(fluent = true)
  @Value
  public static class JobInfo {
    String jid;
    String name;
    String state;
    long startTime;
    long endTime;
    long duration;
  }
}
