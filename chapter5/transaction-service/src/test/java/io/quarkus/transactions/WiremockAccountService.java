package io.quarkus.transactions;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WiremockAccountService implements QuarkusTestResourceLifecycleManager {
  private WireMockServer wireMockServer;

  @Override
  public Map<String, String> start() {
    wireMockServer = new WireMockServer();
    wireMockServer.start();

    stubFor(get(urlEqualTo("/accounts/121212/balance"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("435.76")
        ));

    stubFor(post(urlEqualTo("/accounts/121212/transaction"))
        .willReturn(aResponse()
            //noContent() needed to be changed once the external service returned a Map
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody("{}")
        ));

    return Collections.singletonMap("io.quarkus.transactions.AccountService/mp-rest/url", wireMockServer.baseUrl());
  }

  @Override
  public void stop() {
    if (null != wireMockServer) {
      wireMockServer.stop();
    }
  }
}
