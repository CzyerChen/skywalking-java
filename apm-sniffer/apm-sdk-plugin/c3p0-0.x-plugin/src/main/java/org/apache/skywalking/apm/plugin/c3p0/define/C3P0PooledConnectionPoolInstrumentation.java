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

package org.apache.skywalking.apm.plugin.c3p0.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * C3p0 is a mature, highly concurrent JDBC Connection pooling library, with support for caching and reuse of
 * PreparedStatement objects. Defined by the jdbc3 spec and the optional extensions to jdbc2. c3p0 now also fully
 * supports the jdbc4.
 * <p>
 * HikariDataSource provides a "one stop" solution for database connection pool solution basic requirements.
 * HikariDataSource#getConnection() or HikariDataSource#getConnection(String, String) creates (if necessary) and return
 * a connection.
 */
public class C3P0PooledConnectionPoolInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    private static final String ENHANCE_CLASS = "com.mchange.v2.c3p0.AbstractComboPooledDataSource";
    private static final String ENHANCE_GET_METHOD = "getConnection";
    private static final String ENHANCE_INTT_METHOD = "setJdbcUrl";
    private static final String INTERCEPTOR_GET_CLASS = "org.apache.skywalking.apm.plugin.c3p0.PoolingGetConnectInterceptor";
    private static final String INTERCEPTOR_INIT_CLASS = "org.apache.skywalking.apm.plugin.c3p0.PoolingDataSourceInitInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_GET_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPTOR_GET_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_INTT_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPTOR_INIT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
