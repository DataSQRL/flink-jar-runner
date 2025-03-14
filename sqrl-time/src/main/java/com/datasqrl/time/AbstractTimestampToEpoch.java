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
package com.datasqrl.time;

import com.datasqrl.function.FlinkTypeUtil;
import com.datasqrl.function.StandardLibraryFunction;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.DataTypeFactory;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.types.inference.TypeInference;

@AllArgsConstructor
public abstract class AbstractTimestampToEpoch extends ScalarFunction
    implements StandardLibraryFunction {

  private final boolean isMilli;

  public Long eval(Instant instant) {
    long epoch = instant.toEpochMilli();
    if (!isMilli) {
      epoch = epoch / 1000;
    }
    return epoch;
  }

  @Override
  public TypeInference getTypeInference(DataTypeFactory typeFactory) {
    return FlinkTypeUtil.basicNullInference(
        DataTypes.BIGINT(), DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3));
  }
  //
  //    @Override
  //    public String getDocumentation() {
  //      Instant DEFAULT_DOC_TIMESTAMP = Instant.parse("2023-03-12T18:23:34.083Z");
  //      String functionCall = String.format("%s(%s(%s))",
  //          getFunctionName(),
  //          STRING_TO_TIMESTAMP.getFunctionName(),
  //          DEFAULT_DOC_TIMESTAMP.toString());
  //      String result = this.eval(DEFAULT_DOC_TIMESTAMP).toString();
  //      return String.format("Returns the %s since epoch for the given timestamp.<br />E.g. `%s`
  // returns the number `%s`",
  //          isMilli?"milliseconds":"seconds",
  //          functionCall, result);
  //    }

}
