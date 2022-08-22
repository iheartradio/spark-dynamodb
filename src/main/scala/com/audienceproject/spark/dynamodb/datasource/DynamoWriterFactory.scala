/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  *
  * Copyright © 2019 AudienceProject. All rights reserved.
  */
package com.audienceproject.spark.dynamodb.datasource

import com.audienceproject.spark.dynamodb.connector.{ColumnSchema, TableConnector}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.{DataWriter, DataWriterFactory}
import org.apache.spark.sql.types.StructType

class DynamoWriterFactory(connector: TableConnector,
                          parameters: Map[String, String],
                          schema: StructType)
    extends DataWriterFactory {

    private val batchSize = parameters.getOrElse("writebatchsize", "25").toInt
    private val update = parameters.getOrElse("update", "false").toBoolean
    private val delete = parameters.getOrElse("delete", "false").toBoolean

    private val region = parameters.get("region")
    private val endpoint = parameters.get("endpoint")
    private val roleArn = parameters.get("rolearn")
    private val providerClassName = parameters.get("providerclassname")
    private val awsAccessKeyId = parameters.get("awsaccesskeyid")
    private val awsSecretKeyId = parameters.get("awssecretkeyid")

    override def createWriter(partitionId: Int, taskId: Long): DataWriter[InternalRow] = {
        val columnSchema = new ColumnSchema(connector.keySchema, schema)
        val client = connector.getDynamoDB(region, endpoint, roleArn, providerClassName, awsAccessKeyId, awsSecretKeyId)
        if (update) {
            assert(!delete, "Please provide exactly one of 'update' or 'delete' options.")
            new DynamoDataUpdateWriter(columnSchema, connector, client)
        } else if (delete) {
            new DynamoDataDeleteWriter(batchSize, columnSchema, connector, client)
        } else {
            new DynamoDataWriter(batchSize, columnSchema, connector, client)
        }
    }

}
