package com.cbi.tls;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TLSIntegrationTest.class, properties = "server.port=8081")
@TestPropertySource(locations = "classpath:integration-test.properties")
public class TLSIntegrationTest {

    public static final String CLIENT_TRUSTSTORE = "ssl/client_truststore.jks";
    public static final String CLIENT_KEYSTORE = "ssl/client_keystore.jks";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Value("8080")
    private int port = 0;

    @Value("${ssl.port}")
    private int sslPort = 0;

    @Value("${ssl.store-password}")
    private String storePassword;

    @Test
    public void rest_OverPlainHttp_GetsExpectedResponse() throws Exception {
        fail();
    }

    @Test
    public void rest_WithUntrustedServerCert_ThrowsSSLHandshakeException() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(
                        getStore(CLIENT_KEYSTORE, storePassword.toCharArray()),
                        storePassword.toCharArray())
                // no trust store
                .useProtocol("TLS").build();
        fail();
    }
    /**
     * KeyStores provide credentials, TrustStores verify credentials.
     *
     * Server KeyStores stores the server's private keys, and certificates for corresponding public
     * keys. Used here for HTTPS connections over localhost.
     *
     * Client TrustStores store servers' certificates.
     */
    protected KeyStore getStore(final String storeFileName, final char[] password) throws
            KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore store = KeyStore.getInstance("jks");
        URL url = getClass().getClassLoader().getResource(storeFileName);
        InputStream inputStream = url.openStream();
        try {
            store.load(inputStream, password);
        } finally {
            inputStream.close();
        }

        return store;
    }

    private RestTemplate getRestTemplateForHTTPS(SSLContext sslContext) {
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext,
                new DefaultHostnameVerifier());

        CloseableHttpClient closeableHttpClient =
                HttpClientBuilder.create().setSSLSocketFactory(connectionFactory).build();

        TestRestTemplate testRestTemplate = new TestRestTemplate();
        RestTemplate template = testRestTemplate.getRestTemplate();
        HttpComponentsClientHttpRequestFactory httpRequestFactory =
                (HttpComponentsClientHttpRequestFactory) template.getRequestFactory();
        httpRequestFactory.setHttpClient(closeableHttpClient);
        return template;
    }

}
