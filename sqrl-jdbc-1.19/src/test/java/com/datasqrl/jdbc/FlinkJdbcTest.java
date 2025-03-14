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
package com.datasqrl.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.ResultKind;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith(MiniClusterExtension.class)
public class FlinkJdbcTest {

  @Test
  public void testFlinkWithPostgres() throws Exception {
    // Start PostgreSQL container
    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")) {
      postgres.start();
      // Establish a connection and create the PostgreSQL table
      try (Connection conn =
              DriverManager.getConnection(
                  postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
          Statement stmt = conn.createStatement()) {
        String createTableSQL = "CREATE TABLE test_table (" + "    \"arrayOfRows\" JSONB " + ")";
        stmt.executeUpdate(createTableSQL);
      }

      // Set up Flink environment
      StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
      StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

      // Define the schema
      String createSourceTable =
          "CREATE TABLE datagen_source ("
              + "    arrayOfRows ARRAY<ROW<field1 INT, field2 STRING>> "
              + ") WITH ("
              + "    'connector' = 'datagen',"
              + "    'number-of-rows' = '10'"
              + ")";

      String createSinkTable =
          "CREATE TABLE jdbc_sink ("
              + "    arrayOfRows RAW('com.datasqrl.json.FlinkJsonType', 'ADFjb20uZGF0YXNxcmwuanNvbi5GbGlua0pzb25UeXBlU2VyaWFsaXplclNuYXBzaG90AAAAAQApY29tLmRhdGFzcXJsLmpzb24uRmxpbmtKc29uVHlwZVNlcmlhbGl6ZXI=') "
              + ") WITH ("
              + "    'connector' = 'jdbc-sqrl', "
              + "    'url' = '"
              + postgres.getJdbcUrl()
              + "', "
              + "    'table-name' = 'test_table', "
              + "    'username' = '"
              + postgres.getUsername()
              + "', "
              + "    'password' = '"
              + postgres.getPassword()
              + "'"
              + ")";

      // Register tables in the environment
      tableEnv.executeSql(
          "CREATE TEMPORARY FUNCTION IF NOT EXISTS `tojson` AS 'com.datasqrl.json.ToJson' LANGUAGE JAVA");
      tableEnv.executeSql(createSourceTable);
      tableEnv.executeSql(createSinkTable);

      // Set up a simple Flink job
      TableResult tableResult =
          tableEnv.executeSql(
              "INSERT INTO jdbc_sink SELECT tojson(arrayOfRows) AS arrayOfRows FROM datagen_source");
      tableResult.print();

      assertEquals(ResultKind.SUCCESS_WITH_CONTENT, tableResult.getResultKind());
    }
  }

  @Test
  public void testWriteAndReadToPostgres() throws Exception {
    try (PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14")) {
      postgresContainer.start();
      try (Connection conn =
              DriverManager.getConnection(
                  postgresContainer.getJdbcUrl(),
                  postgresContainer.getUsername(),
                  postgresContainer.getPassword());
          Statement stmt = conn.createStatement()) {
        String createTableSQL = "CREATE TABLE test_table (" + "    id BIGINT, name VARCHAR " + ")";
        stmt.executeUpdate(createTableSQL);
      }

      // Set up Flink mini cluster environment
      StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
      EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
      StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);

      // Create a PostgreSQL table using the Table API
      tEnv.executeSql(
          "CREATE TABLE test_table ("
              + "id BIGINT,"
              + "name STRING"
              + ") WITH ("
              + "'connector' = 'jdbc',"
              + "'url' = '"
              + postgresContainer.getJdbcUrl()
              + "',"
              + "'table-name' = 'test_table',"
              + "'username' = '"
              + postgresContainer.getUsername()
              + "',"
              + "'password' = '"
              + postgresContainer.getPassword()
              + "'"
              + ")");

      // Create a DataGen source to generate 10 rows of data
      tEnv.executeSql(
          "CREATE TABLE datagen_source ("
              + "id BIGINT,"
              + "name STRING"
              + ") WITH ("
              + "'connector' = 'datagen',"
              + "'rows-per-second' = '1',"
              + "'fields.id.kind' = 'sequence',"
              + "'fields.id.start' = '1',"
              + "'fields.id.end' = '10',"
              + "'fields.name.length' = '10'"
              + ")");

      // Insert data from the DataGen source into the PostgreSQL table
      tEnv.executeSql("INSERT INTO test_table SELECT * FROM datagen_source").await();

      // Verify the data has been inserted by querying the PostgreSQL database directly
      Connection connection = postgresContainer.createConnection("");
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table");

      int count = 0;
      if (resultSet.next()) {
        count = resultSet.getInt(1);
      }

      // Validate that 10 rows were inserted
      assertEquals(10, count);

      connection.close();
    }
  }
}
