import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;
import com.microsoft.rest.credentials.ServiceClientCredentials;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.*;

public class KeyVaultSampleBase {

    protected static KeyVaultClient keyVaultClient;
    protected static Azure azure;

    protected static String kvAuthToken;
    protected static String USER_OID;
    protected static SampleTokenCredential mgmtCredentials;

    protected static final Region VAULT_REGION = Region.US_WEST;
    protected static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    protected static final String RESOURCE_GROUP = System.getenv("AZURE_RESOURCE_GROUP");

    // This is the XPlat command line client id as it is available across all tenants and subscriptions.
    private final static String CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";


    public KeyVaultSampleBase() throws InterruptedException, ExecutionException, IOException, TimeoutException {
        mgmtCredentials = new SampleTokenCredential(AzureEnvironment.AZURE, AZURE_TENANT_ID, CLIENT_ID );
        azure = authenticateToAzure(mgmtCredentials);
        keyVaultClient = new KeyVaultClient(createCredentials());
    }

    /**
     * Creates a new KeyVaultCredential based on the access token obtained.
     *
     * @return
     */
    private static ServiceClientCredentials createCredentials() {
        return new KeyVaultCredentials() {

            // Callback that supplies the token type and access token on request.
            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
                try {
                    // We only cache the auth token here, but for a longer running process the refresh token should also be cached.
                    if (kvAuthToken == null) {
                        kvAuthToken = getAccessToken(authorization, resource, CLIENT_ID);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                return kvAuthToken;
            }
        };
    }

    // Private helper method that gets the access token for the authorization and
    // resource depending on which variables are supplied in the environment.
    public static String getAccessToken(String authorization, String resource, String clientID)
            throws InterruptedException, ExecutionException, MalformedURLException, TimeoutException {

        AuthenticationResult result = null;

        // Starts a service to fetch access token.
        ExecutorService service = null;
        service = Executors.newFixedThreadPool(3);
        try {
            final AuthenticationContext context = new AuthenticationContext(authorization, false, service);

            // The key vault storage methods must be called by an authenticated user (not a service principal)
            // so all authentication is through this device code authentication flow.
            Future<AuthenticationResult> future = null;
            Future<DeviceCode> deviceFuture = context.acquireDeviceCode( clientID, resource, null);
            DeviceCode code = deviceFuture.get();
            System.out.println("###############################################################");
            System.out.println("To continue with the test run, please do the following:");
            System.out.println(code.getMessage());
            System.out.println("Press any key here when you return from entering your credentials.");

            // acquireTokenByDeviceCode doesn't actually wait on an actual response;
            // it requires the user to acknowledge that they have logged in.
            try {
                int read = System.in.read(new byte[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            future = context.acquireTokenByDeviceCode(code, null);
            AuthenticationResult authResult = future.get();
            USER_OID = authResult.getUserInfo().getUniqueId();
            return authResult.getAccessToken();
        } finally {
            service.shutdown();
        }
    }

    private static Azure authenticateToAzure(AzureTokenCredentials credentials) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        return Azure.configure().withLogLevel(LogLevel.BASIC).authenticate(credentials).withDefaultSubscription();
    }
}
