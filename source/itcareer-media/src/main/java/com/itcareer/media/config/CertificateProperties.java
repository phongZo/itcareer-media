package com.itcareer.media.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "certificate")
@Getter
@Setter
public class CertificateProperties {
  private String keystore;
  private String alias;
  private String storepass;
  private String keypass;
}
