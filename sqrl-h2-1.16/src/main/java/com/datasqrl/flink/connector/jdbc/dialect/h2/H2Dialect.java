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
package com.datasqrl.flink.connector.jdbc.dialect.h2;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.flink.connector.jdbc.converter.JdbcRowConverter;
import org.apache.flink.connector.jdbc.dialect.AbstractDialect;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.RowType;

public class H2Dialect extends AbstractDialect {

  private static final long serialVersionUID = 1L;

  @Override
  public JdbcRowConverter getRowConverter(RowType rowType) {
    return new H2RowConverter(rowType);
  }

  @Override
  public String getLimitClause(long limit) {
    return "LIMIT " + limit;
  }

  @Override
  public Optional<String> defaultDriverName() {
    return Optional.of("org.h2.Driver");
  }

  /**
   * MERGE INTO tableName [ ( columnName [,...] ) ] [ KEY ( columnName [,...] ) ] { VALUES { ( {
   * DEFAULT | expression } [,...] ) } [,...] | select }
   */
  @Override
  public Optional<String> getUpsertStatement(
      String tableName, String[] fieldNames, String[] uniqueKeyFields) {
    String uniqueColumns =
        Arrays.stream(uniqueKeyFields).map(this::quoteIdentifier).collect(Collectors.joining(", "));

    String columns =
        Arrays.stream(fieldNames).map(this::quoteIdentifier).collect(Collectors.joining(", "));
    String placeholders =
        Arrays.stream(fieldNames).map((f) -> ":" + f).collect(Collectors.joining(", "));

    return Optional.of(
        "MERGE INTO "
            + this.quoteIdentifier(tableName)
            + "("
            + columns
            + ")"
            + " KEY ("
            + uniqueColumns
            + ") VALUES ("
            + placeholders
            + ")");
  }

  @Override
  public String quoteIdentifier(String identifier) {
    return "\"" + identifier + "\"";
  }

  @Override
  public String dialectName() {
    return "H2";
  }

  @Override
  public Optional<Range> decimalPrecisionRange() {
    return Optional.of(Range.of(0, 100000));
  }

  @Override
  public Optional<Range> timestampPrecisionRange() {
    return Optional.of(Range.of(0, 9));
  }

  @Override
  public Set<LogicalTypeRoot> supportedTypes() {
    return EnumSet.of(
        LogicalTypeRoot.CHAR,
        LogicalTypeRoot.VARCHAR,
        LogicalTypeRoot.BOOLEAN,
        LogicalTypeRoot.VARBINARY,
        LogicalTypeRoot.DECIMAL,
        LogicalTypeRoot.TINYINT,
        LogicalTypeRoot.SMALLINT,
        LogicalTypeRoot.INTEGER,
        LogicalTypeRoot.BIGINT,
        LogicalTypeRoot.FLOAT,
        LogicalTypeRoot.DOUBLE,
        LogicalTypeRoot.DATE,
        LogicalTypeRoot.TIME_WITHOUT_TIME_ZONE,
        LogicalTypeRoot.TIMESTAMP_WITHOUT_TIME_ZONE,
        LogicalTypeRoot.TIMESTAMP_WITH_LOCAL_TIME_ZONE,
        LogicalTypeRoot.ARRAY);
  }
}
