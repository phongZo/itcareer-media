package com.itcareer.media.service;

import com.itcareer.media.config.CertificateProperties;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class CertificateService {
  @Autowired
  CertificateProperties certificateProperties;
  @Autowired
  ResourceLoader resourceLoader;

  public KeyStore loadKeyStore() throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");

    // Resolve resource từ classpath hoặc file system
    Resource res = resourceLoader.getResource(certificateProperties.getKeystore());

    try (InputStream is = res.getInputStream()) {
      ks.load(is, certificateProperties.getStorepass().toCharArray());
    }
    return ks;
  }

  public PrivateKey getPrivateKey() throws Exception {
    KeyStore ks = loadKeyStore();
    return (PrivateKey) ks.getKey(certificateProperties.getAlias(), certificateProperties.getKeypass().toCharArray());
  }

  // Lấy chuỗi certificate (để gắn vào PDF khi ký)
  public Certificate[] getCertificateChain() throws Exception {
    KeyStore ks = loadKeyStore();
    return ks.getCertificateChain(certificateProperties.getAlias());
  }

  // Lấy certificate public để xác minh chữ ký
  public Certificate getCertificate() throws Exception {
    KeyStore ks = loadKeyStore();
    return ks.getCertificate(certificateProperties.getAlias());
  }
}
