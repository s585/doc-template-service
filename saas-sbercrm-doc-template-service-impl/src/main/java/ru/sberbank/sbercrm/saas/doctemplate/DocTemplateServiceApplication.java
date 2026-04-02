package ru.sberbank.sbercrm.saas.doctemplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class DocTemplateServiceApplication {
  public static void main(String[] args) {
    ConfigurableApplicationContext context = run(args);
    if (Boolean.getBoolean("app.exit-after-start")) {
      context.close();
    }
  }

  static ConfigurableApplicationContext run(String[] args) {
    return SpringApplication.run(DocTemplateServiceApplication.class, args);
  }
}
