import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.*;

public class KeyVaultSampleBase {

    protected static AzureResourceManager azureResourceManager;

    protected static String USER_OID;

    protected static final Region VAULT_REGION = Region.US_WEST;
    protected static final String AZURE_TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";//"<your_application_tenant_id>";
    protected static final String RESOURCE_GROUP = "v-zhanlt-MonthlyReleaseTestPass";//""<your_resource_group>";
    protected static final String AZURE_SUBSCRIPTION_ID = "faa080af-c1d8-40ad-9cce-e1a450ca5b57";//""<your_subscription_id>";

    // This is the XPlat command line client id as it is available across all tenants and subscriptions.
    protected final static String CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";


    public KeyVaultSampleBase() throws InterruptedException, ExecutionException, IOException, TimeoutException {
        SampleTokenCredential mgmtCredentials = new SampleTokenCredential(AZURE_TENANT_ID,CLIENT_ID);
        azureResourceManager = authenticateToAzure(mgmtCredentials);
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
            Future<DeviceCode> deviceFuture = context.acquireDeviceCode(clientID, resource, null);
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

    private static AzureResourceManager authenticateToAzure(TokenCredential credentials) {
        return AzureResourceManager
                .configure().withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                .authenticate(credentials, new AzureProfile(AZURE_TENANT_ID,AZURE_SUBSCRIPTION_ID,AzureEnvironment.AZURE))
                .withSubscription(AZURE_SUBSCRIPTION_ID);
    }

}
