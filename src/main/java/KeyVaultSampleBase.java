import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;

public class KeyVaultSampleBase {

    protected static AzureResourceManager azureResourceManager;

    public KeyVaultSampleBase() {
        SampleTokenCredential mgmtCredentials = new SampleTokenCredential(AccessTokenUtils.AZURE_TENANT_ID, AccessTokenUtils.CLIENT_ID);
        azureResourceManager = authenticateToAzure(mgmtCredentials);
    }

    private AzureResourceManager authenticateToAzure(TokenCredential credentials) {
        return AzureResourceManager
                .configure().withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                .authenticate(credentials, new AzureProfile(AccessTokenUtils.AZURE_TENANT_ID,
                        AccessTokenUtils.AZURE_SUBSCRIPTION_ID, AzureEnvironment.AZURE))
                .withSubscription(AccessTokenUtils.AZURE_SUBSCRIPTION_ID);
    }

}
