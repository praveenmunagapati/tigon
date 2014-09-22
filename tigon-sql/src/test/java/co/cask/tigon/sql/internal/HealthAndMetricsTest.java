/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.tigon.sql.internal;

import co.cask.tigon.api.metrics.Metrics;
import co.cask.tigon.sql.conf.Constants;
import co.cask.tigon.sql.manager.DiscoveryServer;
import co.cask.tigon.sql.manager.HubDataStore;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * HealthInspectorTest
 */

public class HealthAndMetricsTest {
  private static final Logger LOG = LoggerFactory.getLogger(HealthInspector.class);
  private static HealthInspector inspector;
  private static DiscoveryServer discoveryServer;
  private static CountDownLatch latch;
  private static Metrics metrics;

  static class SharedMetrics implements Metrics {
    private static Map<String, Integer> metricsValue = Maps.newHashMap();

    @Override
    public void count(String counterName, int delta) {
      if (!metricsValue.containsKey(counterName)) {
        metricsValue.put(counterName, 0);
      }
      metricsValue.put(counterName, delta + metricsValue.get(counterName));
      LOG.info("[METRICS] CounterName : {}\tValue last second : {}", counterName, delta);
    }

    public static Integer getCounter(String counterName) {
      return metricsValue.get(counterName);
    }
  }

  @BeforeClass
  public static void setup() throws Exception {
    latch = new CountDownLatch(1);
    HubDataStore hubDataStore = new HubDataStore.Builder()
      .setInstanceName("test")
      .build();
    inspector = new HealthInspector(new ProcessMonitor() {
      @Override
      public void notifyFailure(Set<String> errorProcessNames) {
        LOG.info("Heartbeat detection failure notified");
        for (String error : errorProcessNames) {
          LOG.error("No Heartbeat received from " + error);
        }
        latch.countDown();
      }
    });

    metrics = new SharedMetrics();
    MetricsRecorder metricsRecorder = new MetricsRecorder(metrics);
    discoveryServer = new DiscoveryServer(hubDataStore, inspector, metricsRecorder);
    discoveryServer.startAndWait();
  }

  private void register(String ftaID, String ftaName, String pingURL) {
    HttpClient httpClient = new DefaultHttpClient();
    JsonObject bodyJson = new JsonObject();
    bodyJson.addProperty("name", "test");
    bodyJson.addProperty("fta_name", ftaName);
    bodyJson.addProperty("ftaid", ftaID);

    HttpPost httpPost = new HttpPost(pingURL + "/v1/announce-fta-instance");
    StringEntity params = null;
    try {
      params = new StringEntity(bodyJson.toString());
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    httpPost.addHeader("Content-Type", "application/json");
    httpPost.setEntity(params);
    try {
      EntityUtils.consumeQuietly(httpClient.execute(httpPost).getEntity());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testPing() throws InterruptedException {
    final String pingURL = "http://" + discoveryServer.getHubAddress().getAddress().getHostAddress() + ":" +
      discoveryServer.getHubAddress().getPort();
    LOG.info("Discovery Server URL : " + pingURL);

    //Code to emulate pings after every second
    class MockPing implements Runnable {
      @Override
      public void run() {
        HttpClient httpClient = new DefaultHttpClient();
        for (int i = 0; i < 5; i++) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          JsonObject bodyJson = new JsonObject();
          bodyJson.addProperty("name", "test");
          bodyJson.addProperty("ftaid", "IronMan");
          JsonObject metrics = new JsonObject();
          metrics.addProperty("awesomeCounter", i);
          bodyJson.add("metrics", metrics);
          HttpPost httpPost = new HttpPost(pingURL + "/v1/log-metrics");
          StringEntity params = null;
          try {
            params = new StringEntity(bodyJson.toString());
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
          httpPost.addHeader("Content-Type", "application/json");
          httpPost.setEntity(params);
          try {
            EntityUtils.consumeQuietly(httpClient.execute(httpPost).getEntity());
          } catch (IOException e) {
            e.printStackTrace();
          }
          LOG.info("Pinged with Metrics {}", bodyJson.toString());
        }
      }
    }

    inspector.startAndWait();
    LOG.info("Started monitoring...");

    TimeUnit.SECONDS.sleep(Constants.INITIALIZATION_TIMEOUT - 2);
    register("IronMan", "RobertDowneyJr", pingURL);
    LOG.info("IronMan Registered");
    Assert.assertTrue(!latch.await(0, TimeUnit.SECONDS));
    LOG.info("No failure detected");

    //Initiate 5 mock pings. 1 per second
    new Thread(new MockPing()).start();

    LOG.info("Initiated 5 mock pings");
    //Check state a second after the last mock ping
    Assert.assertTrue(!latch.await(2, TimeUnit.SECONDS));
    LOG.info("No failure Detected");

    LOG.info("Expecting heartbeat detection failure");
    //Check state >2 seconds after the last mock ping
    Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));

    //Check value of awesomeCounter at the end
    Assert.assertTrue(SharedMetrics.getCounter("RobertDowneyJr.awesomeCounter").equals(10));
  }
}
