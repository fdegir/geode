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

package org.apache.geode.management.api;

import java.util.concurrent.CompletableFuture;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.cache.configuration.CacheElement;
import org.apache.geode.management.runtime.RuntimeInfo;

/**
 * this is responsible for applying and persisting cache configuration changes on locators and/or
 * servers.
 */
@Experimental
public interface ClusterManagementService extends AutoCloseable {
  /**
   * This method will create the element on all the applicable members in the cluster and persist
   * the configuration in the cluster configuration if persistence is enabled.
   *
   * @param config this holds the configuration attributes of the element to be created on the
   *        cluster, as well as the group this config belongs to
   * @see CacheElement
   */
  <T extends CacheElement> ClusterManagementResult create(T config);

  /**
   * This method will delete the element on all the applicable members in the cluster and update the
   * configuration in the cluster configuration if persistence is enabled.
   *
   * @param config this holds the configuration attributes of the element to be deleted on the
   *        cluster
   * @throws IllegalArgumentException, NoMemberException, EntityExistsException
   * @see CacheElement
   */
  <T extends CacheElement> ClusterManagementResult delete(T config);

  /**
   * This method will update the element on all the applicable members in the cluster and persist
   * the updated configuration in the cluster configuration if persistence is enabled.
   *
   * @param config this holds the configuration attributes of the element to be updated on the
   *        cluster
   * @throws IllegalArgumentException, NoMemberException, EntityExistsException
   * @see CacheElement
   */
  <T extends CacheElement> ClusterManagementResult update(T config);

  <T extends CacheElement & CorrespondWith<R>, R extends RuntimeInfo> ClusterManagementListResult<T, R> list(
      T config);

  <T extends CacheElement & CorrespondWith<R>, R extends RuntimeInfo> ClusterManagementListResult<T, R> get(
      T config);

  /**
   * This method will launch a cluster management operation asynchronously.
   *
   * @param op the operation type plus any parameters.
   * @param <A> the operation type (a subclass of {@link ClusterManagementOperation}
   * @param <V> the return type of the operation
   * @return a {@link ClusterManagementOperationResult} holding a {@link CompletableFuture} (if the
   *         operation was launched successfully) or an error code otherwise.
   */
  <A extends ClusterManagementOperation<V>, V extends JsonSerializable> ClusterManagementOperationResult<V> startOperation(
      A op);

  /**
   * Test to see if this instance of ClusterManagementService retrieved from the client side is
   * properly connected to the locator or not
   *
   * @return true if connected
   */
  boolean isConnected();

  /**
   * release any resources controlled by this service
   */
  @Override
  void close();
}
