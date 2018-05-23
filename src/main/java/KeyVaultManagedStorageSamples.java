import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.models.StorageAccountAttributes;
import com.microsoft.azure.keyvault.models.StorageAccountItem;
import com.microsoft.azure.keyvault.models.StorageBundle;
import com.microsoft.azure.management.graphrbac.RoleDefinition;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import com.microsoft.azure.management.storage.StorageAccount;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Class to demonstrate Key Vault Managed Storage.
 *
 * demonstrateStorageAccountManagement() must be run first to create a vault and storage account.
 */
public class KeyVaultManagedStorageSamples extends KeyVaultSampleBase {

    public String VAULT_NAME;
    public String VAULT_URI;
    public String STORAGE_ACCOUNT_NAME;

    public KeyVaultManagedStorageSamples() throws InterruptedException, ExecutionException, MalformedURLException, TimeoutException {
        super();
    }

    /**
     * Creates a storage account then adds the storage account to the vault to manage its keys.
     */
    public void demonstrateStorageAccountManagement() {

        VAULT_NAME = SdkContext.randomResourceName("vault", 15);
        STORAGE_ACCOUNT_NAME = SdkContext.randomResourceName("storage", 15);

        System.out.println("Creating new storage account");
        StorageAccount storageAccount = azure.storageAccounts().define(STORAGE_ACCOUNT_NAME)
                .withRegion(VAULT_REGION)
                .withExistingResourceGroup(RESOURCE_GROUP)
                .create();

        // The key vault service must be given the "Storage Account Key Operator Service Role" on
        // the storage account before the storage account can be added to the vault.

        System.out.println("Granting Azure Key Vault the operator service role on the storage account");

        // find the role definition for "Storage Account Key Operator Service Role"
        PagedList<RoleDefinition> roleDefinitions = azure.accessManagement().roleDefinitions().listByScope("\\");
        Iterator<RoleDefinition> roleDefs = roleDefinitions.iterator();
        RoleDefinition keyVaultRole = null;
        while (roleDefs.hasNext()) {
            RoleDefinition definition = roleDefs.next();
            if (definition.roleName().equals("Storage Account Key Operator Service Role")) {
                keyVaultRole = definition;
                break;
            }
        }

        azure.accessManagement().roleAssignments()
                .define(UUID.randomUUID().toString()) //Needs to be a UUID formatted String
                .forObjectId("93c27d83-f79b-4cb2-8dd4-4aa716542e74") //This is the Azure Key Vault Service Principal
                .withRoleDefinition(keyVaultRole.id())
                .withScope(storageAccount.id())
                .create();

        // Setting the storage acocunt can only be called by a user account with access to the keys of the storage account.
        // Therefore, we grant the user that created the storage account access to the vault
        // as well as the storage account.

        System.out.println("Creating new vault");
        Vault vault = azure.vaults().define(VAULT_NAME)
                .withRegion(VAULT_REGION)
                .withExistingResourceGroup(RESOURCE_GROUP)
                .defineAccessPolicy()
                    .forObjectId(USER_OID)
                    .allowSecretAllPermissions()
                    .allowStorageAllPermissions()
                    .attach()
                .withDeploymentDisabled()
                .create();
        VAULT_URI = vault.vaultUri();

        System.out.printf("Adding storage account %s to vault %s%n", storageAccount.name(), vault.name());

        StorageAccountAttributes attributes = new StorageAccountAttributes().withEnabled(true);
        keyVaultClient.setStorageAccount(vault.vaultUri(), storageAccount.name(), storageAccount.id(), "key1", true, "P30D", attributes, null);
    }

    /**
     * Updates a storage account in the vault.
     */
    public void updateStorageAccount() {
        String keyName = SdkContext.randomResourceName("key", 20);
        // Switch the active key for the storage account
        // Update the key regeneration period
        // Stop rotating the storage account keys
        System.out.println("Updating storage account active key, regeneration period, and disabling automatic key regeneration.");
        final StorageBundle storageBundle = keyVaultClient.updateStorageAccount(VAULT_URI, STORAGE_ACCOUNT_NAME, "key2", false, "P60D", null, null);

    }

    /**
     * Regenerates a key of a storage account managed by the vault.
     */
    public void regenerateStorageAccountKeys() {
        // Regenerate storage account keys by calling regenerateStorageAccountKey
        // Note that regenerateStorageAccountKey method can only be called by a user account
        // Not a servicePrincipal so we use the keyVaultClient created in the base sample.
        System.out.println("Regenerating storage account keys");
        keyVaultClient.regenerateStorageAccountKey(VAULT_URI, STORAGE_ACCOUNT_NAME, "key1");
    }

    /**
     * Lists the storage accounts in the vault, then gets each.
     */
    public void getStorageAccounts() {

        System.out.println("List and get storage accounts managed by the vault");
        // List the storage accounts in the vault.
        List<StorageAccountItem> msaList = keyVaultClient.getStorageAccounts(VAULT_URI);

        for (StorageAccountItem item : msaList) {
            String storageID = item.id();

            System.out.println(storageID);
            //StorageBundle storageAccount = keyVaultClient.getStorageAccount(VAULT_URI, );
            //System.out.println(storageID + " " +  storageAccount.resourceId());
        }
    }

    /**
     * Deletes a storage account from the vault.
     */
    public void deleteStorageAccount() {
        //Deletes a storage account from a vault.
        System.out.printf("Delete storage account %s from the vault %n", STORAGE_ACCOUNT_NAME);
        keyVaultClient.deleteStorageAccount(VAULT_URI, STORAGE_ACCOUNT_NAME);
    }



}
