/**
 * This software is copyright (c) 2012-2022 by
 *  - Leibniz-Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 * This is free software. You can redistribute it
 * and/or modify it under the terms described in
 * the GNU General Public License v3 of which you
 * should have received a copy. Otherwise you can download
 * it from
 *
 *   http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * @copyright Leibniz-Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.client.auth;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

import eu.clarin.sru.client.SRUOperation;
import eu.clarin.sru.client.SRURequestAuthenticator;


public class ClarinFCSRequestAuthenticator implements SRURequestAuthenticator {
    private static final Logger logger =
            LoggerFactory.getLogger(ClarinFCSRequestAuthenticator.class);
    private final AuthenticationInfoProvider authInfoProvider;
    private final String issuer;
    private final Algorithm algorithm;
    private final AtomicLong nextTokenId;
    private final boolean withNotBefore;
    private final long expireTokenTime;


    private ClarinFCSRequestAuthenticator(
            AuthenticationInfoProvider authInfoPovider, String issuer,
            boolean withTokenIds, boolean withNotBefore, long expireTokenTime,
            Algorithm algorithm) {
        if (authInfoPovider == null) {
            throw new IllegalArgumentException("authInfoPovider == null");
        }
        if (issuer == null) {
            throw new IllegalArgumentException("issuer == null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm == null");
        }

        this.authInfoProvider = authInfoPovider;
        this.issuer = issuer;

        if (withTokenIds) {
            this.nextTokenId = new AtomicLong((new SecureRandom()).nextLong());
        } else {
            this.nextTokenId = null;
        }
        this.withNotBefore = withNotBefore;
        this.expireTokenTime = expireTokenTime;
        this.algorithm = algorithm;
    }


    @Override
    public String createAuthenticationHeaderValue(SRUOperation operation,
            String endpointURI, Map<String, String> context) {
        switch (operation) {
        case SEARCH_RETRIEVE:
            String audience = authInfoProvider.getAudience(endpointURI, context);
            if (audience == null) {
                audience = endpointURI;
            }
            JWTCreator.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withAudience(audience);
            if (nextTokenId != null) {
                builder.withJWTId(Long.toHexString(nextTokenId.getAndIncrement()));
            }
            Instant now = Instant.now();
            Date nowDate = Date.from(now);
            builder.withIssuedAt(nowDate);
            if (withNotBefore) {
                builder.withNotBefore(nowDate);
            }
            if (expireTokenTime > -1) {
                Date expireDate = Date.from(now.plusSeconds(expireTokenTime));
                builder.withExpiresAt(expireDate);
            }

            builder.withSubject(authInfoProvider.getSubject(endpointURI, context));
            String token = builder.sign(algorithm);
            logger.debug("using JWT token: {}", token);
            return "Bearer " + token;
        default:
            return null;
        }
    }


    public interface AuthenticationInfoProvider {
        public String getAudience(String endpointURI, Map<String, String> context);

        public String getSubject(String endpointURI, Map<String, String> context);
    }


    public static class Builder {
        private String issuer;
        private Algorithm algorithm;
        private AuthenticationInfoProvider provider;
        private boolean withTokenIds = true;
        private boolean withNotBefore = true;
        private long expireTokenTime = -1;

        private Builder() {
            /* hide constructor */
        }

        public Builder withIssuer(String issuer) {
            if ((issuer == null) || issuer.isEmpty()) {
                throw new IllegalArgumentException("issuer invalid");
            }
            this.issuer = issuer;
            return this;
        }


        public Builder withAuthenticationInfoProvider(
                AuthenticationInfoProvider provider) {
            if (provider == null) {
                throw new IllegalArgumentException("provider == null");
            }
            this.provider = provider;
            return this;
        }


        public Builder withTokenIds(boolean withTokenIds) {
            this.withTokenIds = withTokenIds;
            return this;
        }


        public Builder withNotBefore(boolean withNotBefore) {
            this.withNotBefore = withNotBefore;
            return this;
        }


        public Builder withExpireTokens(long expireTokenTime) {
            if (expireTokenTime < 0) {
                this.expireTokenTime = -1;
            } else {
                this.expireTokenTime = expireTokenTime;
            }
            return this;
        }


        public Builder withKeyPair(String publicKeyFileName,
                String privateKeyFileName) {
            return withKeyPair(new File(publicKeyFileName),
                    new File(privateKeyFileName));
        }


        public Builder withKeyPair(File publicKeyFile, File privateKeyFile) {
            try {
                logger.debug("load public key from file '{}'", publicKeyFile);
                RSAPublicKey publicKey = KeyReaderUtils.readPublicKey(publicKeyFile);
                logger.debug("load private key from file '{}'", privateKeyFile);
                RSAPrivateKey privateKey = KeyReaderUtils.readPrivateKey(privateKeyFile);

                return withKeyPair(publicKey, privateKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                throw new IllegalArgumentException("error reading key pair", e);
            }
        }


        public Builder withKeyPairContents(String publicKeyContent, String privateKeyContent) {
            try {
                logger.debug("load public key from string", publicKeyContent);
                RSAPublicKey publicKey = KeyReaderUtils.readPublicKey(publicKeyContent);
                logger.debug("load private key from string", privateKeyContent);
                RSAPrivateKey privateKey = KeyReaderUtils.readPrivateKey(privateKeyContent);

                return withKeyPair(publicKey, privateKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                throw new IllegalArgumentException("error reading key pair", e);
            }
        }

        public Builder withKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
            algorithm = Algorithm.RSA256(publicKey, privateKey);
            return this;
        }


        // NOTE: with private key only (derive public key)
        // make this a task for the user to avoid edge cases where it might not work


        public ClarinFCSRequestAuthenticator build() {
            return new ClarinFCSRequestAuthenticator(provider,
                    issuer,
                    withTokenIds,
                    withNotBefore,
                    expireTokenTime,
                    algorithm);
        }


        public static Builder create() {
            return new Builder();
        }
    } // inner class Builder

}
