package io.quarkus.bank;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class BankTest {
  @Test
  public void testGetSecrets() throws IOException {
    try (final WebClient webClient = createWebClient()) {
      HtmlPage page = webClient.getPage("http://localhost:8081/bank/secure/secrets");

      HtmlForm loginForm = page.getForms().get(0);

      loginForm.getInputByName("username").setValueAttribute("admin");
      loginForm.getInputByName("password").setValueAttribute("admin");

      UnexpectedPage json = loginForm.getInputByValue("login").click();

      System.out.println(json.getWebResponse().getContentAsString());

      Jsonb jsonb = JsonbBuilder.create();
      HashMap<String, String> credentials =
        jsonb.fromJson(json.getWebResponse().getContentAsString(), HashMap.class);
      assertTrue(credentials.get("username").equals("admin"));
      assertTrue(credentials.get("password").equals("secret"));

    }

  }

  private WebClient createWebClient() {
    WebClient webClient = new WebClient();
    webClient.setCssErrorHandler(new SilentCssErrorHandler());
    return webClient;
  }
}
