package quarkus.accounts.repository;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeAccountResourceIT extends AccountResourceTest {
  // Execute the same tests but in native mode.
}
