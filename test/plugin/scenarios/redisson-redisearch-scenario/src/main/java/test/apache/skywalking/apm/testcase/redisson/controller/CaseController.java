/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package test.apache.skywalking.apm.testcase.redisson.controller;

import java.util.Arrays;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.redisson.Redisson;
import org.redisson.api.GeoUnit;
import org.redisson.api.RMap;
import org.redisson.api.RSearch;
import org.redisson.api.RedissonClient;
import org.redisson.api.search.index.FieldIndex;
import org.redisson.api.search.index.IndexOptions;
import org.redisson.api.search.index.IndexType;
import org.redisson.api.search.query.QueryFilter;
import org.redisson.api.search.query.QueryOptions;
import org.redisson.api.search.query.ReturnAttribute;
import org.redisson.api.search.query.SearchResult;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/case")
@PropertySource("classpath:application.properties")
public class CaseController {

    @Value("${redis.servers:127.0.0.1:6379}")
    private String address;

    private RedissonClient client;

    @PostConstruct
    private void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + address);
        client = Redisson.create(config);
    }

    @RequestMapping("/redisson-redisearch-case")
    @ResponseBody
    public String redissonCase() {
        RMap<String, SimpleObject> m = client.getMap(
            "doc:1", new CompositeCodec(StringCodec.INSTANCE, client.getConfig().getCodec()));
        m.put("v1", new SimpleObject("name1"));
        m.put("v2", new SimpleObject("name2"));
        RMap<String, SimpleObject> m2 = client.getMap(
            "doc:2", new CompositeCodec(StringCodec.INSTANCE, client.getConfig().getCodec()));
        m2.put("v1", new SimpleObject("name3"));
        m2.put("v2", new SimpleObject("name4"));

        RSearch s = client.getSearch();
        s.createIndex("idx", IndexOptions.defaults()
                                         .on(IndexType.HASH)
                                         .prefix(Arrays.asList("doc:")),
                      FieldIndex.text("v1"),
                      FieldIndex.text("v2")
        );

        SearchResult r1 = s.search("idx", "*", QueryOptions.defaults()
                                                           .returnAttributes(
                                                               new ReturnAttribute("v1"), new ReturnAttribute("v2")));

        SearchResult r2 = s.search("idx", "*", QueryOptions.defaults()
                                                           .filters(QueryFilter.geo("field")
                                                                               .from(1, 1)
                                                                               .radius(10, GeoUnit.FEET)));
        s.dropIndex("idx");

        Map<String, String> cfg = s.getConfig("TIMEOUT");
        s.setConfig("TIMEOUT", "42");

        return "Success";
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        return "success";
    }
}

