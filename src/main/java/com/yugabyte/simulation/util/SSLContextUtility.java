package com.yugabyte.simulation.util;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class SSLContextUtility {

    // Load the cluster root certificate
    public static SSLContext createSSLHandler(String certfile) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream fis = new FileInputStream(certfile);
            X509Certificate ca;
            try {
                ca = (X509Certificate) cf.generateCertificate(fis);
            } catch (Exception e) {
                System.err.println("Exception generating certificate from input file: " + e);
                return null;
            } finally {
                fis.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext;
        } catch (Exception e) {
            System.err.println("Exception creating sslContext: " + e);
            return null;
        }
    }

    public static void main(String args[]) {
        try {
            // Create a YCQL client.
            CqlSession session = CqlSession
                    .builder()
                    .addContactPoint(new InetSocketAddress("XXXXXXXX", 9042))
                    .withSslContext(createSSLHandler("/Users/amitchauhan/Downloads/root.crt"))
                    .withAuthCredentials("admin","XXXXXXX")
                    .withLocalDatacenter("us-east-1")
                    .build();
            // Create keyspace 'ybdemo' if it does not exist.
            String createKeyspace = "CREATE KEYSPACE IF NOT EXISTS ybdemo;";
            session.execute(createKeyspace);
            System.out.println("Created keyspace ybdemo");
            // Create table 'employee', if it does not exist.
            String createTable = "CREATE TABLE IF NOT EXISTS ybdemo.employee (id int PRIMARY KEY, " +
                    "name varchar, " + "age int, " + "language varchar);";
            session.execute(createTable);
            System.out.println("Created table employee");
            // Insert a row.
            String insert = "INSERT INTO ybdemo.employee (id, name, age, language)" +
                    " VALUES (1, 'John', 35, 'Java');";
            session.execute(insert);
            System.out.println("Inserted data: " + insert);
            // Query the row and print out the result.
            String select = "SELECT name, age, language FROM ybdemo.employee WHERE id = 1;";
            ResultSet selectResult = session.execute(select);
            List<Row> rows = selectResult.all();
            String name = rows.get(0).getString(0);
            int age = rows.get(0).getInt(1);
            String language = rows.get(0).getString(2);
            System.out.println("Query returned " + rows.size() + " row: " + "name=" + name +
                    ", age=" + age + ", language: " + language);
            // Close the client.
            session.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }



}
