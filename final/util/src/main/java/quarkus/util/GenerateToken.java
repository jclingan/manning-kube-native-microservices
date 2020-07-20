package quarkus.util;

import java.util.HashMap;

import org.eclipse.microprofile.jwt.Claims;

/**
 * A simple utility class to generate and print a JWT token string to stdout. Can be run with:
 * mvn exec:java -Dexec.mainClass=org.acme.security.jwt.GenerateToken -Dexec.classpathScope=test
 */
public class GenerateToken {
  /**
   *
   * @param args - [0]: optional name of classpath resource for json document of claims to add; defaults to "/JwtClaims.json"
   *             [1]: optional time in seconds for expiration of generated token; defaults to 300
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
//    String claimsJson = "/adminClaims.json";
//    String claimsJson = "/debbieClaims.json";
    String claimsJson = "/tomClaims.json";
    if (args.length > 0) {
      claimsJson = args[0];
    }
    HashMap<String, Long> timeClaims = new HashMap<>();
    if (args.length > 1) {
      long duration = Long.parseLong(args[1]);
      long exp = TokenUtils.currentTimeInSecs() + duration;
      timeClaims.put(Claims.exp.name(), exp);
    }
    String token = TokenUtils.generateTokenString(claimsJson, timeClaims);
    System.out.println(token);
  }

  public static String generateAdminToken() throws Exception {
    HashMap<String, Long> timeClaims = new HashMap<>();
    long exp = TokenUtils.currentTimeInSecs() + 300;
    timeClaims.put(Claims.exp.name(), exp);

    return TokenUtils.generateTokenString("/adminClaims.json", timeClaims);
  }
}