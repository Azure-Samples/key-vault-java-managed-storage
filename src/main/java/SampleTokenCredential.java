import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper class that supplies and caches the token for Azure authentication.
 */
public class SampleTokenCredential extends AzureTokenCredentials {
    static String authorization;
    static String mgmtToken;
    static String authClientId;

    public SampleTokenCredential(AzureEnvironment environment, String domain, String clientId) {
        super(environment, domain);
        authorization = "https://login.windows.net/" + domain;
        authClientId = clientId;
    }

    @Override
    public String getToken(String resource) throws IOException {
        try {
            if (mgmtToken == null) {
                // We only cache the auth token here, but for a longer running process the refresh token should also be cached.
                mgmtToken = KeyVaultSampleBase.getAccessToken(authorization, resource, authClientId);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return mgmtToken;
    }
}
