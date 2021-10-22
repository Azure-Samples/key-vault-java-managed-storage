import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
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
