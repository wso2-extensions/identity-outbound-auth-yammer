package org.wso2.carbon.identity.authenticator;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.authenticator.internal.YammerAuthenticatorServiceComponent;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import org.osgi.service.component.ComponentContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OAuthAuthzResponse.class, AuthenticatedUser.class,
        OAuthClientRequest.class, URL.class})

public class YammerTest {

    @Mock
    OAuthClientResponse oAuthClientResponse;
    ComponentContext componentContext;
    @Mock
    HttpServletRequest httpServletRequest;
    @Mock
    OAuthAuthzResponse mockOAuthAuthzResponse;
    @Mock
    private AuthenticatedUser authenticatedUser;
    @Spy
    private AuthenticationContext context = new AuthenticationContext();
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private OAuthClient mockOAuthClient;
    @Mock
    private OAuthClientRequest mockOAuthClientRequest;
    @Mock
    private OAuthJSONAccessTokenResponse oAuthJSONAccessTokenResponse;

    YammerOAuth2Authenticator yammerOAuth2Authenticator;
    YammerAuthenticatorServiceComponent yammerAuthenticatorServiceComponent;

    @DataProvider(name = "authenticatorProperties")
    public Object[][] getAuthenticatorPropertiesData() {
        Map<String, String> authenticatorProperties = new HashMap<>();
        authenticatorProperties.put(OIDCAuthenticatorConstants.CLIENT_ID, "test-client-id");
        authenticatorProperties.put(OIDCAuthenticatorConstants.CLIENT_SECRET, "test-client-secret");
        authenticatorProperties.put("callbackUrl", "http://localhost:9443/commonauth");
        authenticatorProperties.put("scope", "");
        return new Object[][]{{authenticatorProperties}};
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

    @BeforeMethod
    public void setUp() {
        yammerOAuth2Authenticator = new YammerOAuth2Authenticator();
        yammerAuthenticatorServiceComponent = new YammerAuthenticatorServiceComponent();
        initMocks(this);
    }

    @Test(description = "Test case for getTokenEndpoint method", dataProvider = "authenticatorProperties")
    public void testGetTokenEndpoint(Map<String, String> authenticatorProperties) {
        String tokenEndpoint = yammerOAuth2Authenticator.getTokenEndpoint(authenticatorProperties);
        Assert.assertEquals(YammerOAuth2AuthenticatorConstants.YAMMER_TOKEN_ENDPOINT, tokenEndpoint);
    }

    @Test(description = "Test case for getUserInfoEndpoint method", dataProvider = "authenticatorProperties")
    public void testGetUserInfoEndpoint(Map<String, String> authenticatorProperties) {
        String userInfoEndpoint = yammerOAuth2Authenticator.getUserInfoEndpoint(oAuthClientResponse,
                authenticatorProperties);
        Assert.assertEquals(YammerOAuth2AuthenticatorConstants.YAMMER_USERINFO_ENDPOINT, userInfoEndpoint);
    }

    @Test(description = "Test case for requiredIDToken method", dataProvider = "authenticatorProperties")
    public void testRequiredIDToken(Map<String, String> authenticatorProperties) {
        Assert.assertFalse(yammerOAuth2Authenticator.requiredIDToken(authenticatorProperties));
    }

    @Test(description = "Test case for getFriendlyName method")
    public void testGetFriendlyName() {
        Assert.assertEquals(YammerOAuth2AuthenticatorConstants.YAMMER_CONNECTOR_FRIENDLY_NAME,
                yammerOAuth2Authenticator.getFriendlyName());
    }

    @Test(description = "Test case for getAuthorizationServerEndpoint method", dataProvider = "authenticatorProperties")
    public void testGetAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {
        Assert.assertEquals(YammerOAuth2AuthenticatorConstants.YAMMER_OAUTH_ENDPOINT,
                yammerOAuth2Authenticator.getAuthorizationServerEndpoint(authenticatorProperties));
    }

    @Test(description = "Test case for getSubjectAttributes method ", dataProvider = "authenticatorProperties")
    public void testGetSubjectAttributest(Map<String, String> authenticateproperties) throws Exception {
        YammerOAuth2Authenticator spyAuthenticator = PowerMockito.spy(new YammerOAuth2Authenticator());
        Mockito.when(oAuthClientResponse.getParam("access_token")).thenReturn("{token:dummytoken}");
        PowerMockito.doReturn("{\"id\":\"testuser\"}")
                .when(spyAuthenticator, "sendRequest", Mockito.anyString(), Mockito.anyString());
        Map<ClaimMapping, String> claims = yammerOAuth2Authenticator.getSubjectAttributes(oAuthClientResponse,
                authenticateproperties);
        Assert.assertEquals(0, claims.size());
    }

    @Test(description = "Test case for getName method")
    public void testGetName() {
        Assert.assertEquals(YammerOAuth2AuthenticatorConstants.YAMMER_CONNECTOR_NAME, yammerOAuth2Authenticator.getName());
    }

    @Test(description = "Test case for canHandle method")
    public void testCanHandle() {
        Assert.assertNotNull(yammerOAuth2Authenticator.canHandle(httpServletRequest));
    }

    @Test(description = "Test case for getConfigurationProperties method")
    public void testGetConfigurationProperties() {
        Assert.assertEquals(3, yammerOAuth2Authenticator.getConfigurationProperties().size());
    }

    @Test(expectedExceptions = AuthenticationFailedException.class,
            description = "Test case for processAuthenticationResponse", dataProvider = "authenticatorProperties")
    public void testProcessAuthenticationResponse(Map<String, String> authenticatorProperties) throws Exception {
        YammerOAuth2Authenticator spyAuthenticator = PowerMockito.spy(new YammerOAuth2Authenticator());
        PowerMockito.mockStatic(OAuthAuthzResponse.class);
        when(OAuthAuthzResponse.oauthCodeAuthzResponse(Mockito.any(HttpServletRequest.class)))
                .thenReturn(mockOAuthAuthzResponse);
        when(oAuthClientResponse.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN))
                .thenReturn("test-token");
        PowerMockito.doReturn("{\"token\":\"test-token\",\"id\":\"testuser\"}")
                .when(spyAuthenticator, "sendRequest", Mockito.anyString(), Mockito.anyString());
        PowerMockito.mockStatic(AuthenticatedUser.class);
        when(AuthenticatedUser.createFederateAuthenticatedUserFromSubjectIdentifier(Mockito.anyString()))
                .thenReturn(authenticatedUser);
        context.setAuthenticatorProperties(authenticatorProperties);
        spyAuthenticator.processAuthenticationResponse(httpServletRequest, httpServletResponse, context);
        Assert.assertNotNull(context.getSubject());
    }

    @Test(description = "Test case for getOauthResponse method")
    public void testGetOauthResponse() throws Exception {
        OAuthClientResponse oAuthClientResponse = GetOauthResponse(mockOAuthClient, mockOAuthClientRequest);
        Assert.assertNotNull(oAuthClientResponse);
    }

    public OAuthClientResponse GetOauthResponse(OAuthClient mockOAuthClient, OAuthClientRequest mockOAuthClientRequest)
            throws Exception {
        Mockito.when(mockOAuthClient.accessToken(mockOAuthClientRequest)).thenReturn(oAuthJSONAccessTokenResponse);
        OAuthClientResponse oAuthClientResponse = Whitebox.invokeMethod(yammerOAuth2Authenticator,
                "getOauthResponse", mockOAuthClient, mockOAuthClientRequest);
        return oAuthClientResponse;
    }

    @Test(description = "Test case for YammerAuthenticatorServiceComponent activate and deactivate method")
    public void testYammerAuthenticatorServiceComponentDeactivateAndActivate() throws Exception {
        Whitebox.invokeMethod(yammerAuthenticatorServiceComponent, "activate", componentContext);
        Whitebox.invokeMethod(yammerAuthenticatorServiceComponent, "deactivate", componentContext);
    }
}
