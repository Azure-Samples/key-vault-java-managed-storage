import com.azure.core.management.Region;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class AccessTokenUtils {

    public static final Region VAULT_REGION = Region.US_WEST;
    public static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
    public static final String RESOURCE_GROUP = System.getenv("AZURE_RESOURCE_GROUP");
    public static final String AZURE_SUBSCRIPTION_ID = System.getenv("AZURE_SUBSCRIPTION_ID");
    public static final String KEY_VAULT_SERVICE_PRINCIPAL_ID = System.getenv("KEY_VAULT_SERVICE_PRINCIPAL_ID");

    // This is the XPlat command line client id as it is available across all tenants and subscriptions.
    public final static String CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";

    //    private static String userOid;
    private static String userOid;

    // Private helper method that gets the access token for the authorization and
    // resource depending on which variables are supplied in the environment.
    public static String getAccessToken(String authorization, String resource, String clientID)
            throws InterruptedException, ExecutionException, MalformedURLException, TimeoutException {

        // Starts a service to fetch access token.
        ExecutorService service = null;
        service = Executors.newFixedThreadPool(3);
        try {
            final AuthenticationContext context = new AuthenticationContext(authorization, false, service);
            // The key vault storage methods must be called by an authenticated user (not a service principal)
            // so all authentication is through this device code authentication flow.
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

            Future<AuthenticationResult> future = context.acquireTokenByDeviceCode(code, null);
            AuthenticationResult authResult = future.get();
            userOid = authResult.getUserInfo().getUniqueId();
            return authResult.getAccessToken();
        } finally {
            service.shutdown();
        }
    }

    public static String getUserOid() {
        return userOid;
    }

}
