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
package com.datasqrl.vector;

import com.datasqrl.function.SqrlCastFunction;
import com.datasqrl.function.StandardLibraryFunction;
import com.google.auto.service.AutoService;
import org.apache.flink.table.functions.ScalarFunction;

/** Converts a double array to a vector */
@AutoService(StandardLibraryFunction.class)
public class DoubleToVector extends ScalarFunction
    implements StandardLibraryFunction, SqrlCastFunction {

  public FlinkVectorType eval(double[] array) {
    return new FlinkVectorType(array);
  }
}
