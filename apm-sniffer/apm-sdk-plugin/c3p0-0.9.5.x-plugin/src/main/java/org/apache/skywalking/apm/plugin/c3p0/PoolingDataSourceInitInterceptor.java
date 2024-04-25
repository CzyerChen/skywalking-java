/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.c3p0;

import com.mchange.v2.c3p0.AbstractComboPooledDataSource;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.meter.MeterFactory;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser.URLParser;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

/**
 * {@link PoolingDataSourceInitInterceptor} intercepted the method of getJdbcUrl to init datasource metrics.
 */
public class PoolingDataSourceInitInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(final EnhancedInstance objInst,
                             final Method method,
                             final Object[] allArguments,
                             final Class<?>[] classes,
                             final MethodInterceptResult methodInterceptResult) {

    }

    @Override
    public Object afterMethod(final EnhancedInstance objInst,
                              final Method method,
                              final Object[] allArguments,
                              final Class<?>[] classes,
                              final Object ret) {
        AbstractComboPooledDataSource pooledDataSource = (AbstractComboPooledDataSource) objInst;
        ConnectionInfo connectionInfo = URLParser.parser(pooledDataSource.getJdbcUrl());
        String tagValue = connectionInfo.getDatabaseName() + "_" + connectionInfo.getDatabasePeer();
        Map<String, Function<AbstractComboPooledDataSource, Supplier<Double>>> metricMap = getMetrics();
        metricMap.forEach(
            (key, value) -> MeterFactory.gauge(PoolConstants.METER_NAME, value.apply(pooledDataSource))
                                        .tag(PoolConstants.METER_TAG_NAME, tagValue)
                                        .tag(PoolConstants.METER_TAG_STATUS, key)
                                        .build());
        return ret;
    }

    @Override
    public void handleMethodException(final EnhancedInstance objInst,
                                      final Method method,
                                      final Object[] allArguments,
                                      final Class<?>[] classes,
                                      final Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }

    private Map<String, Function<AbstractComboPooledDataSource, Supplier<Double>>> getMetrics() {
        Map<String, Function<AbstractComboPooledDataSource, Supplier<Double>>> metricMap = new HashMap<>();
        metricMap.put(PoolConstants.NUM_TOTAL_CONNECTIONS, (AbstractComboPooledDataSource pooledDataSource) -> () -> {
            double numConnections = 0;
            try {
                numConnections = pooledDataSource.getNumConnections();
            } catch (SQLException e) {
                ContextManager.activeSpan().errorOccurred().log(e);
            }
            return numConnections;
        });
        metricMap.put(PoolConstants.NUM_BUSY_CONNECTIONS, (AbstractComboPooledDataSource pooledDataSource) -> () -> {
            double numBusyConnections = 0;
            try {
                numBusyConnections = pooledDataSource.getNumBusyConnections();
            } catch (SQLException e) {
                ContextManager.activeSpan().errorOccurred().log(e);
            }
            return numBusyConnections;
        });
        metricMap.put(PoolConstants.NUM_IDLE_CONNECTIONS, (AbstractComboPooledDataSource pooledDataSource) -> () -> {
            double numIdleConnections = 0;
            try {
                numIdleConnections = pooledDataSource.getNumIdleConnections();
            } catch (SQLException e) {
                ContextManager.activeSpan().errorOccurred().log(e);
            }
            return numIdleConnections;
        });
        metricMap.put(
            PoolConstants.MAX_IDLE_TIME,
            (AbstractComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getMaxIdleTime()
        );
        metricMap.put(
            PoolConstants.MIN_POOL_SIZE,
            (AbstractComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getMinPoolSize()
        );
        metricMap.put(
            PoolConstants.MAX_POOL_SIZE,
            (AbstractComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getMaxPoolSize()
        );
        metricMap.put(
            PoolConstants.INITIAL_POOL_SIZE,
            (AbstractComboPooledDataSource pooledDataSource) -> () -> (double) pooledDataSource.getInitialPoolSize()
        );
        return metricMap;
    }
}
