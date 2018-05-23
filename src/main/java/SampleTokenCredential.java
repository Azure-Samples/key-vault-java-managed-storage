import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;

import java.io.IOException;

/**
 * Wrapper class that supplies the passed-in token for Azure authentication.
 */
public class SampleTokenCredential extends AzureTokenCredentials {
    String authenticationToken;

    public SampleTokenCredential(AzureEnvironment environment, String domain, String token) {
        super(AzureEnvironment.AZURE, domain);
        this.authenticationToken = token;
    }

    @Override
    public String getToken(String resource) throws IOException {
        return this.authenticationToken;
    }
}
