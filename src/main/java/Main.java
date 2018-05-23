import com.microsoft.azure.storage.StorageException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) throws InvalidKeyException, StorageException, ParseException, URISyntaxException, InterruptedException, ExecutionException, IOException, TimeoutException {
        KeyVaultManagedStorageSamples msakSamples = new KeyVaultManagedStorageSamples();
        msakSamples.demonstrateStorageAccountManagement();
        msakSamples.updateStorageAccount();
        msakSamples.regenerateStorageAccountKeys();
        msakSamples.getStorageAccounts();
        msakSamples.deleteStorageAccount();

        SasDefinitionSample sample2 = new SasDefinitionSample();
        sample2.createAccountSasDefinition();
        sample2.createBlobSasDefinition();
        sample2.getSasDefinition();
    }

}