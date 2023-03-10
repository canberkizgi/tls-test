package com.cbi.tls;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TLSTest {


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final boolean ONE_WAY_SSL = false; // no client certificates
    private static final boolean TWO_WAY_SSL = true; // client certificates mandatory
    private static final String PROTOCOL = "TLS";

    private static final KeyStore NO_CLIENT_KEYSTORE = null;
    private static final SSLContext NO_SSL_CONTEXT = null;

    private static final char[] KEYPASS_AND_STOREPASS_VALUE = "snaplogic".toCharArray();
    private static final String JAVA_KEYSTORE = "jks";
    private static final String SERVER_KEYSTORE = "ssl/server_keystore.jks";
    private static final String SERVER_TRUSTSTORE = "ssl/server_truststore.jks";
    private static final String CLIENT_KEYSTORE = "ssl/client_keystore.jks";
    private static final String CLIENT_TRUSTSTORE = "ssl/client_truststore.jks";

    private static final TrustManager[] NO_SERVER_TRUST_MANAGER = null;

    private CloseableHttpClient httpclient;

    @Before
    public void setUp() throws Exception {
        httpclient = HttpClients.createDefault();
    }

    @Test
    public void execute_withNoSchema_ThrowsClientProtocolException() throws Exception {
        fail();
    }

    @Test
    public void httpRequest_Returns200OK() throws Exception {
        fail();
    }

    @Test
    public void httpRequest_WithNoSSLContext_ThrowsSSLException() {
        fail();
    }

    @Test
    public void httpsRequest_WithSSLAndValidatingCertsButNoClientTrustSture_ThrowsSSLException() throws Exception {
        SSLContext serverSSLContext = createServerSSLContext(SERVER_KEYSTORE, NO_SERVER_TRUST_MANAGER, KEYPASS_AND_STOREPASS_VALUE);
        fail();
    }

    @Test
    public void httpsRequest_WithSSLAndTrustingAllCertsButNoClientTrustSture_Returns200OK() throws Exception {
        SSLContext trustedSSLContext = new SSLContextBuilder()
                .loadTrustMaterial(NO_CLIENT_KEYSTORE, (X509Certificate[] arg0, String arg1) -> {
            return true;
        }).build();
        httpclient = HttpClients.custom().setSSLContext(trustedSSLContext).build();
        SSLContext serverSSLContext = createServerSSLContext(SERVER_KEYSTORE, NO_SERVER_TRUST_MANAGER, KEYPASS_AND_STOREPASS_VALUE);
        fail();
    }

    protected HttpServer createLocalTestServer(SSLContext sslContext, boolean forceSSLAuth)
            throws UnknownHostException {
        final HttpServer server = ServerBootstrap.bootstrap()
                .setLocalAddress(Inet4Address.getByName("localhost"))
                .setSslContext(sslContext)
                .setSslSetupHandler(socket -> socket.setNeedClientAuth(forceSSLAuth))
                .registerHandler("*",
                        (request, response, context) -> response.setStatusCode(HttpStatus.SC_OK))
                .create();

        return server;
    }

    protected String getBaseUrl(HttpServer server) {
        return server.getInetAddress().getHostName() + ":" + server.getLocalPort();
    }

    /*
    Create an SSLContext for the server using the server's JKS. This instructs the server to
    present its certificate when clients connect over HTTPS.
     */
    protected SSLContext createServerSSLContext(final String keyStoreFileName,
                                                TrustManager[] serverTrustManagers, final char[] password) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException,
            KeyManagementException {
        KeyStore serverKeyStore = getStore(keyStoreFileName, password);
        KeyManager[] serverKeyManagers = getKeyManagers(serverKeyStore, password);

        SSLContext sslContext = SSLContexts.custom().useProtocol(PROTOCOL).build();
        sslContext.init(serverKeyManagers, serverTrustManagers, new SecureRandom());

        return sslContext;
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
        final KeyStore store = KeyStore.getInstance(JAVA_KEYSTORE);
        URL url = getClass().getClassLoader().getResource(storeFileName);
        InputStream inputStream = url.openStream();
        try {
            store.load(inputStream, password);
        } finally {
            inputStream.close();
        }

        return store;
    }

    /**
     * KeyManagers decide which authentication credentials (e.g. certs) should be sent to the remote
     * host for authentication during the SSL handshake.
     *
     * Server KeyManagers use their private keys during the key exchange algorithm and send
     * certificates corresponding to their public keys to the clients. The certificate comes from
     * the KeyStore.
     */
    protected KeyManager[] getKeyManagers(KeyStore store, final char[] password) throws
            NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(store, password);

        return keyManagerFactory.getKeyManagers();
    }

    /**
     * TrustManagers determine if the remote connection should be trusted or not.
     *
     * Clients will use certificates stored in their TrustStores to verify identities of servers.
     * Servers will use certificates stored in their TrustStores to verify identities of clients.
     */
    protected TrustManager[] getTrustManagers(KeyStore store) throws NoSuchAlgorithmException,
            KeyStoreException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(store);

        return trustManagerFactory.getTrustManagers();
    }
}
