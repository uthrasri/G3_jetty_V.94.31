//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.security.openid;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * <p>The credentials of an user to be authenticated with OpenID Connect. This will contain
 * the OpenID ID Token and the OAuth 2.0 Access Token.</p>
 *
 * <p>
 * This is constructed with an authorization code from the authentication request. This authorization code
 * is then exchanged using {@link #redeemAuthCode(OpenIdConfiguration)} for a response containing the ID Token and Access Token.
 * The response is then validated against the {@link OpenIdConfiguration}.
 * </p>
 */
public class OpenIdCredentials implements Serializable
{
    private static final Logger LOG = Log.getLogger(OpenIdCredentials.class);
    private static final long serialVersionUID = 4766053233370044796L;

    private final String redirectUri;
    private String authCode;
    private Map<String, Object> response;
    private Map<String, Object> claims;

    public OpenIdCredentials(String authCode, String redirectUri)
    {
        this.authCode = authCode;
        this.redirectUri = redirectUri;
    }

    public String getUserId()
    {
        return (String)claims.get("sub");
    }

    public Map<String, Object> getClaims()
    {
        return claims;
    }

    public Map<String, Object> getResponse()
    {
        return response;
    }

    public boolean isExpired()
    {
        if (authCode != null || claims == null)
            return true;

        // Check expiry
        long expiry = (Long)claims.get("exp");
        long currentTimeSeconds = (long)(System.currentTimeMillis() / 1000F);
        if (currentTimeSeconds > expiry)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("OpenId Credentials expired {}", this);
            return true;
        }

        return false;
    }

    public void redeemAuthCode(OpenIdConfiguration configuration) throws Exception
    {
        if (LOG.isDebugEnabled())
            LOG.debug("redeemAuthCode() {}", this);

        if (authCode != null)
        {
            try
            {
                response = claimAuthCode(configuration);
                if (LOG.isDebugEnabled())
                    LOG.debug("response: {}", response);

                String idToken = (String)response.get("id_token");
                if (idToken == null)
                    throw new IllegalArgumentException("no id_token");

                String accessToken = (String)response.get("access_token");
                if (accessToken == null)
                    throw new IllegalArgumentException("no access_token");

                String tokenType = (String)response.get("token_type");
                if (!"Bearer".equalsIgnoreCase(tokenType))
                    throw new IllegalArgumentException("invalid token_type");

                claims = JwtDecoder.decode(idToken);
                if (LOG.isDebugEnabled())
                    LOG.debug("claims {}", claims);
                validateClaims(configuration);
            }
            finally
            {
                // reset authCode as it can only be used once
                authCode = null;
            }
        }
    }

    private void validateClaims(OpenIdConfiguration configuration)
    {
        // Issuer Identifier for the OpenID Provider MUST exactly match the value of the iss (issuer) Claim.
        if (!configuration.getIssuer().equals(claims.get("iss")))
            throw new IllegalArgumentException("Issuer Identifier MUST exactly match the iss Claim");

        // The aud (audience) Claim MUST contain the client_id value.
        validateAudience(configuration);

        // If an azp (authorized party) Claim is present, verify that its client_id is the Claim Value.
        Object azp = claims.get("azp");
        if (azp != null && !configuration.getClientId().equals(azp))
            throw new IllegalArgumentException("Authorized party claim value should be the client_id");
    }

    private void validateAudience(OpenIdConfiguration configuration)
    {
        Object aud = claims.get("aud");
        String clientId = configuration.getClientId();
        boolean isString = aud instanceof String;
        boolean isList = aud instanceof Object[];
        boolean isValidType = isString || isList;

        if (isString && !clientId.equals(aud))
            throw new IllegalArgumentException("Audience Claim MUST contain the client_id value");
        else if (isList)
        {
            if (!Arrays.asList((Object[])aud).contains(clientId))
                throw new IllegalArgumentException("Audience Claim MUST contain the client_id value");

            if (claims.get("azp") == null)
                throw new IllegalArgumentException("A multi-audience ID token needs to contain an azp claim");
        }
        else if (!isValidType)
            throw new IllegalArgumentException("Audience claim was not valid");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> claimAuthCode(OpenIdConfiguration configuration) throws Exception
    {
        Fields fields = new Fields();
        fields.add("code", authCode);
        fields.add("client_id", configuration.getClientId());
        fields.add("client_secret", configuration.getClientSecret());
        fields.add("redirect_uri", redirectUri);
        fields.add("grant_type", "authorization_code");
        FormContentProvider formContentProvider = new FormContentProvider(fields);
        Request request = configuration.getHttpClient().POST(configuration.getTokenEndpoint())
                .content(formContentProvider)
                .timeout(10, TimeUnit.SECONDS);
        ContentResponse response = request.send();
        String responseBody = response.getContentAsString();
        if (LOG.isDebugEnabled())
            LOG.debug("Authentication response: {}", responseBody);

        Object parsedResponse = JSON.parse(responseBody);
        if (!(parsedResponse instanceof Map))
            throw new IllegalStateException("Malformed response from OpenID Provider");
        return (Map<String, Object>)parsedResponse;
    }
}
