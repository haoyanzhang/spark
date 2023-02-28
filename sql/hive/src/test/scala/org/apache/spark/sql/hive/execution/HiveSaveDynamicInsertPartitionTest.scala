/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.hive.execution

import org.scalatest.BeforeAndAfter

import org.apache.spark.sql.hive.test.TestHive
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SQLTestUtils

class HiveSaveDynamicInsertPartitionTest extends HivePlanTest with SQLTestUtils with BeforeAndAfter{

  private val tableNamePrefix =
    TestHive.getConf(SQLConf.DYNAMIC_PARTITION_SAVE_PARTITIONS_TABLE_NAME_PREFIX.key)
  private val originHiveExecDynamicPartition =
    TestHive.getConf("hive.exec.dynamic.partition")
  private val originHiveExecDynamicPartitionMode =
    TestHive.getConf("hive.exec.dynamic.partition.mode")

  override def beforeAll(): Unit = {
    super.beforeAll()
    TestHive.setConf("hive.exec.dynamic.partition", "true")
    TestHive.setConf("hive.exec.dynamic.partition.mode", "nonstrict")
    TestHive.setConf(SQLConf.ENABLE_DYNAMIC_PARTITION_SAVE_PARTITIONS, true)
  }

  override def afterAll(): Unit = {
    try {
      TestHive.setConf("hive.exec.dynamic.partition", originHiveExecDynamicPartition)
      TestHive.setConf("hive.exec.dynamic.partition.mode", originHiveExecDynamicPartitionMode)
      TestHive.setConf(SQLConf.ENABLE_DYNAMIC_PARTITION_SAVE_PARTITIONS, false)
    } finally {
      super.afterAll()
    }
  }

  test("test_save_dynamic_partition") {
    withTable("test") {
      sql(
        s"""
           |CREATE TABLE test(i int)
           |PARTITIONED BY (p int)
           |STORED AS textfile""".stripMargin)

      sql(
        s"""
           |INSERT OVERWRITE TABLE test PARTITION (p)
           |select 1 as i, 2 as p""".stripMargin)

      val df = sql(
        s"""
           |SELECT * FROM TABLE ${tableNamePrefix}_default_test
           |""".stripMargin
      )

      val rows = df.collect()
      assert(rows.length == 1)
      assert(rows(0).getAs[String]("p") == "2")

    }
  }

}
