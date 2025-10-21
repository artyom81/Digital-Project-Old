package eu.clarin.sru.client.auth;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public final class KeyReaderUtils {
    private KeyReaderUtils() {
    }

    public static RSAPublicKey readPublicKey(File file)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return readPublicKey(stream);
        }
    }

    public static RSAPrivateKey readPrivateKey(File file)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return readPrivateKey(stream);
        }
    }

    public static RSAPublicKey readPublicKey(String keyContents)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        byte[] keyContentBytes = keyContents.getBytes(StandardCharsets.UTF_8);
        try (InputStream stream = new ByteArrayInputStream(keyContentBytes)) {
            return readPublicKey(stream);
        }
    }

    public static RSAPrivateKey readPrivateKey(String keyContents)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        byte[] keyContentBytes = keyContents.getBytes(StandardCharsets.UTF_8);
        try (InputStream stream = new ByteArrayInputStream(keyContentBytes)) {
            return readPrivateKey(stream);
        }
    }

    public static RSAPublicKey readPublicKey(InputStream stream)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (InputStreamReader keyReader = new InputStreamReader(stream);
                PemReader pemReader = new PemReader(keyReader)) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
            return (RSAPublicKey) factory.generatePublic(pubKeySpec);
        }
    }

    public static RSAPrivateKey readPrivateKey(InputStream stream)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (InputStreamReader keyReader = new InputStreamReader(stream);
                PemReader pemReader = new PemReader(keyReader)) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
        }
    }
}
