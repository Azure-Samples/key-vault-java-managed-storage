import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        KeyVaultManagedStorageSamples msakSamples = new KeyVaultManagedStorageSamples();
        msakSamples.demonstrateStorageAccountManagement();
        msakSamples.updateStorageAccount();
        msakSamples.regenerateStorageAccountKeys();
        msakSamples.getStorageAccounts();
        msakSamples.deleteStorageAccount();

        SasDefinitionSample sasDefSample = new SasDefinitionSample();
        sasDefSample.createAccountSasDefinition();
        sasDefSample.createBlobSasDefinition();
    }

}
