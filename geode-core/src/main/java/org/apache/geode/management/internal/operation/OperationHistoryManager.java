/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management.internal.operation;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.map.LRUMap;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.lang.Identifiable;
import org.apache.geode.management.api.ClusterManagementOperation;
import org.apache.geode.management.api.JsonSerializable;

@Experimental
public class OperationHistoryManager {
  private final ConcurrentMap<String, CompletableFuture> inProgressHistory;
  private final Map<String, CompletableFuture> completedHistory;

  public OperationHistoryManager() {
    this(100);
  }

  @SuppressWarnings("unchecked")
  public OperationHistoryManager(int historySize) {
    inProgressHistory = new ConcurrentHashMap<>();
    completedHistory = Collections.synchronizedMap(new LRUMap(historySize));
  }

  /**
   * looks up the future for an async operation by id
   */
  @SuppressWarnings("unchecked")
  public <V extends JsonSerializable> CompletableFuture<V> getStatus(String opId) {
    CompletableFuture<V> ret = inProgressHistory.get(opId);
    if (ret == null) {
      ret = completedHistory.get(opId);
    }
    return ret;
  }

  public <A extends ClusterManagementOperation<V>, V extends JsonSerializable> OperationInstance<A, V> save(
      OperationInstance<A, V> operationInstance) {
    String opId = operationInstance.getId();
    CompletableFuture<V> future = operationInstance.getFuture();

    inProgressHistory.put(opId, future);

    CompletableFuture<V> newFuture = future.whenComplete((result, exception) -> {
      completedHistory.put(opId, future);
      inProgressHistory.remove(opId);
    });

    // we want to replace only if still in in-progress.
    inProgressHistory.replace(opId, future, newFuture);

    return new OperationInstance<>(newFuture, opId, operationInstance.getOperation());
  }

  public static final class OperationInstance<A extends ClusterManagementOperation<V>, V extends JsonSerializable>
      implements Identifiable<String> {
    private final CompletableFuture<V> future;
    private final String opId;
    private final A operation;

    public OperationInstance(CompletableFuture<V> future, String opId, A operation) {
      this.future = future;
      this.opId = opId;
      this.operation = operation;
    }

    @Override
    public String getId() {
      return opId;
    }

    public CompletableFuture<V> getFuture() {
      return future;
    }

    public A getOperation() {
      return operation;
    }
  }
}
