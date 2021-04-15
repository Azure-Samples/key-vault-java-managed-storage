import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.azure.resourcemanager.keyvault.models.Key;
import com.azure.resourcemanager.keyvault.models.KeyPermissions;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.security.keyvault.keys.models.KeyCurveName;
import com.azure.security.keyvault.keys.models.KeyOperation;
import com.azure.security.keyvault.keys.models.KeyType;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class SasDefinitionSample extends KeyVaultSampleBase {
    private String STORAGE_ACCOUNT_NAME;
    private String VAULT_URI;
    private String VAULT_NAME;
    private StorageAccount storageAccount;
    private Vault vault;

    public SasDefinitionSample() throws InterruptedException, ExecutionException, IOException, TimeoutException {
        super();
        setUpStorageAccount();
    }


    /**
     * Creates an account sas definition, to manage storage account and its entities.
     */
    public void createAccountSasDefinition() {
        // In order to create an account sas definition, we have to first create a template. The
        // template_uri for an account sas definition is the intended sas token signed with an arbitrary key.
        // We create the generateSharedAccessSignature method in CloudStorageAccount to generate an account sas token.
        StorageSharedKeyCredential sas = new StorageSharedKeyCredential(STORAGE_ACCOUNT_NAME, storageAccount.getKeys().get(0).value());

        //Create a new policy
        AccountSasService accountSasService = AccountSasService.parse("bfqt");
        AccountSasResourceType accountSasResourceType = AccountSasResourceType.parse("sco");
        AccountSasPermission accountSasPermission = AccountSasPermission.parse("rwdlacup");
        AccountSasSignatureValues accountSasSignatureValues =
                new AccountSasSignatureValues(
                        OffsetDateTime.now().plusDays(1),
                        accountSasPermission,
                        accountSasService,
                        accountSasResourceType);
        //Generate a signature based off of the policy and account.
        com.azure.storage.blob.BlobServiceClient blobServiceClient
                = new com.azure.storage.blob.BlobServiceClientBuilder()
                .endpoint(storageAccount.endPoints().primary().blob())
                .credential(sas).buildClient();
        String sasSignature = blobServiceClient.generateAccountSas(accountSasSignatureValues);
        System.out.println("Generated sasSignature " + sasSignature);

        // Generating new cloud storage account object off of the new acctSasToken
        blobServiceClient = new com.azure.storage.blob.BlobServiceClientBuilder()
                .endpoint(storageAccount.endPoints().primary().blob())
                .sasToken(sasSignature).buildClient();
        BlobContainerClient blobContainerClient
                = blobServiceClient.getBlobContainerClient("cloudstorageblob");
        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
        }

        // Creating a blob with the account storage token.
        System.out.println("Creating container: " + blobContainerClient.getBlobContainerName());
        BlockBlobClient blockBlobClient =blobContainerClient.getBlobClient("blobName").getBlockBlobClient();
        String text = "test blob data";
        System.out.printf("Uploading text: \"%s\" to blob%n", text);
        blockBlobClient.upload(new ByteArrayInputStream(text.getBytes()), text.getBytes().length);
        System.out.println("Downloading text: " + blockBlobClient.downloadToFile("<download_file_path>"));
        blockBlobClient.delete();
        System.out.println("Blob deleted");
    }

    /**
     * Creates a service SAS definition with access to a blob container.
     */
    public void createBlobSasDefinition() {

        // Create the blob sas definition template
        // The sas template uri for service sas definitions contains the storage entity uri with the template token
        // This sample demonstrates constructing the template uri for a blob container, but a similar approach can
        // be used for all other storage service, i.e. File, Queue, table

        // Create a template sas token for a container
        StorageSharedKeyCredential sas = new StorageSharedKeyCredential(STORAGE_ACCOUNT_NAME, storageAccount.getKeys().get(0).value());
        // Note that the key passed in is just a dummy key such that we can generate the correct signature for the template.

        com.azure.storage.blob.BlobServiceClient blobServiceClient
                = new com.azure.storage.blob.BlobServiceClientBuilder()
                .endpoint(storageAccount.endPoints().primary().blob())
                .credential(sas).buildClient();
        BlobContainerClient blobContainerClient
                = blobServiceClient.getBlobContainerClient("cloudstorageblob");
        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
        }
        BlobServiceSasSignatureValues blobServiceSasSignatureValues =
                new BlobServiceSasSignatureValues(
                        OffsetDateTime.now().plusDays(1),
                        BlobSasPermission.parse("acdlrw")).
                        setProtocol(SasProtocol.HTTPS_HTTP);
        String sasSignature = blobContainerClient.generateSas(blobServiceSasSignatureValues);

        blobServiceClient
                = new com.azure.storage.blob.BlobServiceClientBuilder()
                .endpoint(storageAccount.endPoints().primary().blob())
                .sasToken(sasSignature).buildClient();
        BlockBlobClient blockBlobClient =
                blobServiceClient.getBlobContainerClient("cloudstorageblob")
                        .getBlobClient("blobName")
                        .getBlockBlobClient();

        String text = "test blob data";
        System.out.printf("Uploading text: \"%s\" to blob%n", text);
        blockBlobClient.upload(new ByteArrayInputStream(text.getBytes()), text.getBytes().length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blockBlobClient.download(outputStream);
        System.out.println("Downloading text: " + new String(outputStream.toByteArray(),"UTF-8"));

    }

    // This is the same method as the one in KeyVaultManagedStorage - just sets up a storage account.
    private void setUpStorageAccount() {

        VAULT_NAME = azureResourceManager.resourceGroups().manager().internalContext().randomResourceName("vault", 15);
        STORAGE_ACCOUNT_NAME = azureResourceManager.resourceGroups().manager().internalContext().randomResourceName("storage", 15);

        System.out.println("Creating new storage account");
        storageAccount = azureResourceManager
                .storageAccounts().define(STORAGE_ACCOUNT_NAME)
                .withRegion(VAULT_REGION)
                .withExistingResourceGroup(RESOURCE_GROUP)
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
                    .forObjectId("93c27d83-f79b-4cb2-8dd4-4aa716542e74") //This is the Azure Key Vault Service Principal
                    .withRoleDefinition(operationDefinition.get().id())
                    .withScope(storageAccount.id())
                    .create();
        }

        // Setting the storage acocunt can only be called by a user account with access to the keys of the storage account.
        // Therefore, we grant the user that created the storage account access to the vault
        // as well as the storage account.

        System.out.println("Creating new vault");
        vault = azureResourceManager
                .vaults().define(VAULT_NAME)
                .withRegion(VAULT_REGION)
                .withExistingResourceGroup(RESOURCE_GROUP)
                .defineAccessPolicy()
                .forObjectId(USER_OID)
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
        VAULT_URI = vault.vaultUri();
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
        storageAccount.update()
                .enableBlobPublicAccess()
                .enableSharedKeyAccess()
                .withEncryptionKeyFromKeyVault(VAULT_URI, keyVaultKey.name(), null).apply();
    }

}
