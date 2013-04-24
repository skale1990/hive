/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.exec.vector.expressions;

import static org.junit.Assert.assertEquals;

import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.junit.Assert;
import org.junit.Test;


public class TestVectorLogicalExpressions {

  @Test
  public void testLongColOrLongCol() {
    VectorizedRowBatch batch = getBatchThreeBooleanCols();
    ColOrCol expr = new ColOrCol(0,1,2);
    LongColumnVector outCol = (LongColumnVector) batch.cols[2];
    expr.evaluate(batch);
    // verify
    Assert.assertEquals(0, outCol.vector[0]);
    Assert.assertEquals(1, outCol.vector[1]);
    Assert.assertEquals(1, outCol.vector[2]);
    Assert.assertEquals(1, outCol.vector[3]);
    Assert.assertFalse(outCol.isNull[3]);
    Assert.assertTrue(outCol.isNull[4]);
    Assert.assertEquals(1, outCol.vector[5]);
    Assert.assertTrue(outCol.isNull[6]);
    Assert.assertEquals(1, outCol.vector[7]);
    Assert.assertTrue(outCol.isNull[8]);

    Assert.assertEquals(batch.size, 9);
    Assert.assertFalse(outCol.noNulls);
    Assert.assertFalse(outCol.isRepeating);

    // try non-null path
    batch = getBatchThreeBooleanCols();
    batch.cols[0].noNulls = true;
    batch.cols[1].noNulls = true;
    batch.cols[2].noNulls = false;
    outCol = (LongColumnVector) batch.cols[2];
    expr.evaluate(batch);

    // spot check
    Assert.assertTrue(outCol.noNulls);
    Assert.assertEquals(0, outCol.vector[0]);
    Assert.assertEquals(1, outCol.vector[1]);
    Assert.assertEquals(1, outCol.vector[2]);
    Assert.assertEquals(1, outCol.vector[3]);

    // try isRepeating path (left input only), no nulls
    batch = getBatchThreeBooleanCols();
    batch.cols[0].noNulls = true; batch.cols[0].isRepeating = true;
    batch.cols[1].noNulls = true; batch.cols[1].isRepeating = false;
    batch.cols[2].noNulls = false; batch.cols[2].isRepeating = true;
    outCol = (LongColumnVector) batch.cols[2];
    expr.evaluate(batch);

    // spot check
    Assert.assertFalse(outCol.isRepeating);
    Assert.assertEquals(0, outCol.vector[0]);
    Assert.assertEquals(1, outCol.vector[1]);
    Assert.assertEquals(0, outCol.vector[2]);
    Assert.assertEquals(1, outCol.vector[3]);
  }

  /**
   * Get a batch with three boolean (long) columns.
   */
  private VectorizedRowBatch getBatchThreeBooleanCols() {
    VectorizedRowBatch batch = new VectorizedRowBatch(3, VectorizedRowBatch.DEFAULT_SIZE);
    LongColumnVector v0, v1, v2;
    v0 = new LongColumnVector(VectorizedRowBatch.DEFAULT_SIZE);
    v1 = new LongColumnVector(VectorizedRowBatch.DEFAULT_SIZE);
    v2 = new LongColumnVector(VectorizedRowBatch.DEFAULT_SIZE);
    batch.cols[0] = v0;
    batch.cols[1] = v1;
    batch.cols[2] = v2;

    // add some data and nulls
    int i;
    i = 0; v0.vector[i] = 0; v0.isNull[i] = false; v1.vector[i] = 0; v1.isNull[i] = false;  // 0 0
    i = 1; v0.vector[i] = 0; v0.isNull[i] = false; v1.vector[i] = 1; v1.isNull[i] = false;  // 0 1
    i = 2; v0.vector[i] = 1; v0.isNull[i] = false; v1.vector[i] = 0; v1.isNull[i] = false;  // 1 0
    i = 3; v0.vector[i] = 1; v0.isNull[i] = false; v1.vector[i] = 1; v1.isNull[i] = false;  // 1 1
    i = 4; v0.vector[i] = 0; v0.isNull[i] = true; v1.vector[i] = 0; v1.isNull[i] = false;  // NULL 0
    i = 5; v0.vector[i] = 0; v0.isNull[i] = true; v1.vector[i] = 1; v1.isNull[i] = false;  // NULL 1
    i = 6; v0.vector[i] = 0; v0.isNull[i] = false; v1.vector[i] = 0; v1.isNull[i] = true;  // 0 NULL
    i = 7; v0.vector[i] = 1; v0.isNull[i] = false; v1.vector[i] = 1; v1.isNull[i] = true;  // 1 NULL
    i = 8; v0.vector[i] = 1; v0.isNull[i] = true; v1.vector[i] = 1; v1.isNull[i] = true;  // NULL NULL

    v0.noNulls = false;
    v1.noNulls = false;
    v0.isRepeating = false;
    v1.isRepeating = false;

    v2.isRepeating = true; // this value should get over-written with correct value
    v2.noNulls = true; // ditto

    batch.size = 9;
    return batch;
  }

  @Test
  public void testBooleanNot() {
    VectorizedRowBatch batch = getBatchThreeBooleanCols();
    NotCol expr = new NotCol(0,2);
    LongColumnVector outCol = (LongColumnVector) batch.cols[2];
    expr.evaluate(batch);

    // Case with nulls
    Assert.assertFalse(outCol.isRepeating);
    Assert.assertEquals(1, outCol.vector[0]);    Assert.assertFalse(outCol.isNull[0]);
    Assert.assertEquals(0, outCol.vector[2]);    Assert.assertFalse(outCol.isNull[0]);
    Assert.assertTrue(outCol.isNull[4]);

    // No nulls case
    batch.cols[0].noNulls = true;
    expr.evaluate(batch);
    Assert.assertFalse(outCol.isRepeating);
    Assert.assertTrue(outCol.noNulls);
    Assert.assertEquals(1, outCol.vector[0]);
    Assert.assertEquals(0, outCol.vector[2]);

    // isRepeating, and there are nulls
    batch = getBatchThreeBooleanCols();
    outCol = (LongColumnVector) batch.cols[2];
    batch.cols[0].isRepeating = true;
    batch.cols[0].isNull[0] = true;
    expr.evaluate(batch);
    Assert.assertTrue(outCol.isRepeating);;
    Assert.assertTrue(outCol.isNull[0]);

    // isRepeating, and no nulls
    batch = getBatchThreeBooleanCols();
    outCol = (LongColumnVector) batch.cols[2];
    batch.cols[0].isRepeating = true;
    batch.cols[0].noNulls = true;
    expr.evaluate(batch);
    Assert.assertTrue(outCol.isRepeating);
    Assert.assertTrue(outCol.noNulls);
    Assert.assertEquals(1, outCol.vector[0]);
  }

  @Test
  public void testIsNullExpr () {
    // has nulls, not repeating
    VectorizedRowBatch batch = getBatchThreeBooleanCols();
    IsNull expr = new IsNull(0,2);
    LongColumnVector outCol = (LongColumnVector) batch.cols[2];
    expr.evaluate(batch);
    Assert.assertEquals(0, outCol.vector[0]);
    Assert.assertEquals(1, outCol.vector[4]);
    Assert.assertTrue(outCol.noNulls);
    Assert.assertFalse(outCol.isRepeating);

    // No nulls case, not repeating
    batch.cols[0].noNulls = true;
    expr.evaluate(batch);
    Assert.assertFalse(outCol.isRepeating);
    Assert.assertTrue(outCol.noNulls);
    Assert.assertEquals(0, outCol.vector[0]);
    Assert.assertEquals(0, outCol.vector[4]);

    // isRepeating, and there are nulls
    batch = getBatchThreeBooleanCols();
    outCol = (LongColumnVector) batch.cols[2];
    batch.cols[0].isRepeating = true;
    batch.cols[0].isNull[0] = true;
    expr.evaluate(batch);
    Assert.assertTrue(outCol.isRepeating);;
    Assert.assertEquals(1, outCol.vector[0]);
    Assert.assertTrue(outCol.noNulls);

    // isRepeating, and no nulls
    batch = getBatchThreeBooleanCols();
    outCol = (LongColumnVector) batch.cols[2];
    batch.cols[0].isRepeating = true;
    batch.cols[0].noNulls = true;
    expr.evaluate(batch);
    Assert.assertTrue(outCol.isRepeating);
    Assert.assertTrue(outCol.noNulls);
    Assert.assertEquals(0, outCol.vector[0]);
  }

  @Test
  public void testBooleanFiltersOnColumns() {
    VectorizedRowBatch batch = getBatchThreeBooleanCols();

    SelectColumnIsTrue expr = new SelectColumnIsTrue(0);
    expr.evaluate(batch);
    assertEquals(3, batch.size);
    assertEquals(2, batch.selected[0]);
    assertEquals(3, batch.selected[1]);
    assertEquals(7, batch.selected[2]);

    batch = getBatchThreeBooleanCols();
    SelectColumnIsFalse expr1 = new SelectColumnIsFalse(1);
    expr1.evaluate(batch);
    assertEquals(3, batch.size);
    assertEquals(0, batch.selected[0]);
    assertEquals(2, batch.selected[1]);
    assertEquals(4, batch.selected[2]);

    batch = getBatchThreeBooleanCols();
    SelectColumnIsNull expr2 = new SelectColumnIsNull(0);
    expr2.evaluate(batch);
    assertEquals(3, batch.size);
    assertEquals(4, batch.selected[0]);
    assertEquals(5, batch.selected[1]);
    assertEquals(8, batch.selected[2]);

    batch = getBatchThreeBooleanCols();
    SelectColumnIsNotNull expr3 = new SelectColumnIsNotNull(1);
    expr3.evaluate(batch);
    assertEquals(6, batch.size);
    assertEquals(0, batch.selected[0]);
    assertEquals(1, batch.selected[1]);
    assertEquals(2, batch.selected[2]);
    assertEquals(3, batch.selected[3]);
    assertEquals(4, batch.selected[4]);
    assertEquals(5, batch.selected[5]);
  }
}
