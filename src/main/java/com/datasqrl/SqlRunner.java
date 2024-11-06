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

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.util.FileUtils;

/** Main class for executing SQL scripts using picocli. */
@Slf4j
@RequiredArgsConstructor
public class SqlRunner implements Callable<Integer> {

  private final File sqlFile;

  private boolean block;

  private File planFile;

  private File configFile;

  private String udfPath;

  public static void main(String[] args) throws Exception {
    System.out.println(String.format("Executing flink-jar-runner with: %s", Arrays.toString(args)));
    int exitCode = new SqlRunner(new File("/opt/flink/usrlib/flink-files/flink.sql")).call();
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    // Determine UDF path
    if (udfPath == null) {
      udfPath = System.getenv("UDF_PATH");
    }

    // Load configuration if configFile is provided
    Configuration configuration = new Configuration();
    if (configFile != null) {
      configuration = loadConfigurationFromYaml(configFile);
    }

    // Initialize SqlExecutor
    SqlExecutor sqlExecutor = new SqlExecutor(configuration, udfPath);
    TableResult tableResult;
    // Input validation and execution logic
    if (sqlFile != null) {
      // Single SQL file mode
      String script = FileUtils.readFileUtf8(sqlFile);

      Set<String> missingEnvironmentVariables =
          EnvironmentVariablesUtils.validateEnvironmentVariables(script);
      if (!missingEnvironmentVariables.isEmpty()) {
        throw new IllegalStateException(
            String.format(
                "Could not find the following environment variables: %s",
                missingEnvironmentVariables));
      }

      tableResult = sqlExecutor.executeScript(script);
    } else if (planFile != null) {
      // Compiled plan JSON file
      String planJson = FileUtils.readFileUtf8(planFile);
      planJson = replaceScriptWithEnv(planJson);

      tableResult = sqlExecutor.executeCompiledPlan(planJson);
    } else {
      log.error("Invalid input. Please provide one of the following combinations:");
      log.error("- A single SQL file (--sqlfile)");
      log.error("- A plan JSON file (--planfile)");
      return 1;
    }

    if (block) {
      tableResult.await();
    }

    return 0;
  }

  @SneakyThrows
  private String replaceScriptWithEnv(String script) {
    ObjectMapper objectMapper = getObjectMapper();
    Map map = objectMapper.readValue(script, Map.class);
    return objectMapper.writeValueAsString(map);
  }

  public static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    // Register the custom deserializer module
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new JsonEnvVarDeserializer());
    objectMapper.registerModule(module);
    return objectMapper;
  }

  /**
   * Loads configuration from a YAML file.
   *
   * @param configFile The YAML configuration file.
   * @return A Configuration object.
   * @throws Exception If an error occurs while reading the file.
   */
  private Configuration loadConfigurationFromYaml(File configFile) throws Exception {
    log.info("Loading configuration from {}", configFile.getAbsolutePath());
    Configuration configuration =
        GlobalConfiguration.loadConfiguration(configFile.getAbsolutePath());
    return configuration;
  }
}
