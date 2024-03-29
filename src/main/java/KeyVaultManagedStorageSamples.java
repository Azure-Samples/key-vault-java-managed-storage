import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.azure.resourcemanager.keyvault.models.Key;
import com.azure.resourcemanager.keyvault.models.KeyPermissions;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountEncryptionKeySource;
import com.azure.security.keyvault.keys.models.KeyCurveName;
import com.azure.security.keyvault.keys.models.KeyOperation;
import com.azure.security.keyvault.keys.models.KeyType;

import java.util.Optional;
import java.util.UUID;

/**
 * Class to demonstrate Key Vault Managed Storage.
 *
 * demonstrateStorageAccountManagement() must be run first to create a vault and storage account.
 */
public class KeyVaultManagedStorageSamples extends KeyVaultSampleBase {

    private String vaultName;
    private String vaultUri;
    private String storageAccountName;
    private StorageAccount storageAccount;
    private Vault vault;

    public KeyVaultManagedStorageSamples() {
        super();
    }

    /**
     * Creates a storage account then adds the storage account to the vault to manage its keys.
     */
    public void demonstrateStorageAccountManagement() {

        vaultName = azureResourceManager.resourceGroups().manager().internalContext()
                .randomResourceName("vault", 15);
        storageAccountName = azureResourceManager.resourceGroups().manager().internalContext()
                .randomResourceName("storage", 15);

        // Setting the storage acocunt can only be called by a user account with access to the keys of the storage account.
        // Therefore, we grant the user that created the storage account access to the vault
        // as well as the storage account.
        System.out.println("Creating new storage account");
        storageAccount = azureResourceManager
                .storageAccounts().define(storageAccountName)
                .withRegion(AccessTokenUtils.VAULT_REGION)
                .withExistingResourceGroup(AccessTokenUtils.RESOURCE_GROUP)
                .withFileEncryption()
                .withBlobEncryption()
                .withGeneralPurposeAccountKindV2()
                .withSystemAssignedManagedServiceIdentity()
                .create();

        // The key vault service must be given the "Storage Account Key Operator Service Role" on
        // the storage account before the storage account can be added to the vault.
        System.out.println("Granting Azure Key Vault the operator service role on the storage account");

        // find the role definition for "Storage Account Key Operator Service Role"
        Optional<RoleDefinition> operationDefinition =
                azureResourceManager.accessManagement()
                        .roleDefinitions().listByScope("/")
                        .stream()
                        .filter(roleDefinition -> "Storage Account Key Operator Service Role".equals(roleDefinition.roleName()))
                        .findFirst();
        if (operationDefinition.isPresent()) {
            azureResourceManager.accessManagement().roleAssignments()
                    .define(UUID.randomUUID().toString()) //Needs to be a UUID formatted String
                    .forObjectId(AccessTokenUtils.KEY_VAULT_SERVICE_PRINCIPAL_ID) //This is the Azure Key Vault Service Principal
                    .withRoleDefinition(operationDefinition.get().id())
                    .withScope(storageAccount.id())
                    .create();
        }

        System.out.println("Creating new vault");
        vault = azureResourceManager
                .vaults().define(vaultName)
                .withRegion(AccessTokenUtils.VAULT_REGION)
                .withExistingResourceGroup(AccessTokenUtils.RESOURCE_GROUP)
                .defineAccessPolicy()
                .forObjectId(AccessTokenUtils.getUserOid())
                .allowSecretAllPermissions()
                .allowStorageAllPermissions()
                .allowCertificateAllPermissions()
                .allowKeyAllPermissions()
                .attach()
                .defineAccessPolicy()
                .forObjectId(storageAccount.innerModel().identity().principalId())
                .allowKeyPermissions(KeyPermissions.UNWRAP_KEY, KeyPermissions.WRAP_KEY, KeyPermissions.GET)
                .attach()
                .withPurgeProtectionEnabled()
                .withSoftDeleteEnabled()
                .create();
        vaultUri = vault.vaultUri();
        Key keyVaultKey = vault.keys()
                .define("key1")
                .withKeyTypeToCreate(KeyType.RSA)
                .withKeyCurveName(KeyCurveName.P_256)
                .withKeyOperations(
                        KeyOperation.UNWRAP_KEY,
                        KeyOperation.WRAP_KEY,
                        KeyOperation.DECRYPT,
                        KeyOperation.ENCRYPT,
                        KeyOperation.SIGN,
                        KeyOperation.VERIFY)
                .withKeySize(2048)
                .create();
        System.out.printf("Adding storage account %s to vault %s%n", storageAccount.name(), vault.name());
        storageAccount.update().withEncryptionKeyFromKeyVault(vaultUri, keyVaultKey.name(), null).apply();
    }

    /**
     * Updates a storage account in the vault.
     */
    public void updateStorageAccount() {
        // Switch the active key for the storage account
        // Update the key regeneration period
        // Stop rotating the storage account keys
        System.out.println("Updating storage account active key, regeneration period, and disabling automatic key regeneration.");
        Key keyVaultKey = vault.keys()
                .define("key2")
                .withKeyTypeToCreate(KeyType.RSA)
                .withKeyCurveName(KeyCurveName.P_256)
                .withKeyOperations(
                        KeyOperation.UNWRAP_KEY,
                        KeyOperation.WRAP_KEY,
                        KeyOperation.DECRYPT,
                        KeyOperation.ENCRYPT,
                        KeyOperation.SIGN,
                        KeyOperation.VERIFY)
                .withKeySize(2048)
                .create();
        storageAccount.update().withEncryptionKeyFromKeyVault(vaultUri, keyVaultKey.name(), null);

    }

    /**
     * Regenerates a key of a storage account managed by the vault.
     */
    public void regenerateStorageAccountKeys() {
        // Regenerate storage account keys by calling regenerateStorageAccountKey
        // Note that regenerateStorageAccountKey method can only be called by a user account
        // Not a servicePrincipal so we use the keyVaultClient created in the base sample.
        System.out.println("Regenerating storage account keys");
        storageAccount.regenerateKey("key1");
    }

    /**
     * Lists the storage accounts in the vault, then gets each.
     */
    public void getStorageAccounts() {

        System.out.println("List and get storage accounts managed by the vault");
        // List the storage accounts in the vault.
        azureResourceManager.storageAccounts()
                .list().stream()
                .forEach(storageAccount -> {
                    if ( StorageAccountEncryptionKeySource.MICROSOFT_KEYVAULT.equals(storageAccount.encryptionKeySource())
                        && vaultUri.equals(storageAccount.innerModel().encryption().keyVaultProperties().keyVaultUri()) ) {
                        System.out.println(storageAccount.id());
                    }
                });
    }

    /**
     * Deletes a storage account from the vault.
     */
    public void deleteStorageAccount() {
        //Deletes a storage account from a vault.
        System.out.printf("Delete storage account %s from the vault %n", storageAccountName);
        azureResourceManager.storageAccounts()
                .list().stream()
                .forEach(storageAccount -> {
                    if (StorageAccountEncryptionKeySource.MICROSOFT_KEYVAULT.equals(storageAccount.encryptionKeySource())
                            && vaultUri.equals(storageAccount.innerModel().encryption().keyVaultProperties().keyVaultUri())
                            && storageAccountName.equals(storageAccount.name())) {
                        azureResourceManager.storageAccounts().deleteById(storageAccount.id());
                    }
                });
    }


}
