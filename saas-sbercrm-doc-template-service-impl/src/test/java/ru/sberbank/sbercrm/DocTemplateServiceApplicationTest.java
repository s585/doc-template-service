package ru.sberbank.sbercrm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("test")
class DocTemplateServiceApplicationTest {
  @Test
  void contextLoads() {
    Assertions.assertTrue(true);
  }
}
