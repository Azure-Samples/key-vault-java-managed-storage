import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.models.*;
import com.microsoft.azure.management.graphrbac.RoleDefinition;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class SasDefinitionSample extends KeyVaultSampleBase {
    String STORAGE_ACCOUNT_NAME;
    String VAULT_URI;
    String VAULT_NAME;

    private final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd-MM-yyyy");

    public SasDefinitionSample() throws InterruptedException, ExecutionException, MalformedURLException, TimeoutException {
        setUpStorageAccount();
    }


    /**
     * Creates an account sas definition, to manage storage account and its entities.
     */
    public void createAccountSasDefinition() throws ParseException, URISyntaxException, StorageException, InvalidKeyException, IOException {


        // In order to create an account sas definition, we have to first create a template. The
        // template_uri for an account sas definition is the intended sas token signed with an arbitrary key.
        // We create the generateSharedAccessSignature method in CloudStorageAccount to generate an account sas token.
        StorageCredentials sas = new StorageCredentialsAccountAndKey(STORAGE_ACCOUNT_NAME, "00000001");
        // Note that the key passed in is just a dummy key such that we can generate the correct signature for the template.
        CloudStorageAccount account = new CloudStorageAccount(sas);

        //Create a new policy
        SharedAccessAccountPolicy sharedAccessAccountPolicy = new SharedAccessAccountPolicy();
        sharedAccessAccountPolicy.setServices(EnumSet.of(SharedAccessAccountService.BLOB, SharedAccessAccountService.FILE, SharedAccessAccountService.QUEUE, SharedAccessAccountService.TABLE));
        sharedAccessAccountPolicy.setPermissions(EnumSet.of(SharedAccessAccountPermissions.ADD, SharedAccessAccountPermissions.CREATE, SharedAccessAccountPermissions.DELETE,
                SharedAccessAccountPermissions.LIST, SharedAccessAccountPermissions.PROCESS_MESSAGES, SharedAccessAccountPermissions.READ, SharedAccessAccountPermissions.UPDATE, SharedAccessAccountPermissions.WRITE));
        sharedAccessAccountPolicy.setResourceTypes(EnumSet.of(SharedAccessAccountResourceType.CONTAINER, SharedAccessAccountResourceType.OBJECT, SharedAccessAccountResourceType.SERVICE)); //all resources service, container, object
        sharedAccessAccountPolicy.setSharedAccessExpiryTime(FORMATTER.parse("01-01-2020"));

        //Generate a signature based off of the policy and account.
        String sasSignature = account.generateSharedAccessSignature(sharedAccessAccountPolicy);

        // Currently the key vault service cannot process the sr signature, so this needs to be manually removed.
        sasSignature = sasSignature.substring(0, sasSignature.lastIndexOf("&"));
        System.out.println("Generated sasSignature " + sasSignature);


        // Use the created template to create a sas definition in the vault.
        SasDefinitionAttributes attributes = new SasDefinitionAttributes().withEnabled(true);
        SasDefinitionBundle sasDefinition = keyVaultClient.setSasDefinition(VAULT_URI, STORAGE_ACCOUNT_NAME, "acctall", sasSignature, SasTokenType.ACCOUNT, "PT2H", attributes, null);

        // When the sas definition is created, a corresponding managed secret is also created. This secret
        // is used to provision sas tokens according to the sas definition, which can be retrieved
        // through the getSecret method.

        // Grab the secretName from the full secretId (the trailing characters after the last slash)
        String sasSecretId = sasDefinition.secretId();
        String secretName = sasSecretId.substring(sasSecretId.lastIndexOf("/")).substring(1);
        System.out.println("Retrieving secret name, " + secretName);
        SecretBundle acctSasToken = keyVaultClient.getSecret(VAULT_URI, secretName);

        // Generating new cloud storage account object off of the new acctSasToken
        StorageCredentials sasCreds = new StorageCredentialsSharedAccessSignature(acctSasToken.value());
        CloudStorageAccount cloudStorageAccount = new CloudStorageAccount(sasCreds, true, null, STORAGE_ACCOUNT_NAME);
        CloudBlobClient cloudBlobClient = cloudStorageAccount.createCloudBlobClient();
        CloudBlobContainer cloudstorageblob  = cloudBlobClient.getContainerReference("cloudstorageblob");

        // Creating a blob with the account storage token.
        System.out.println("Creating container: " + cloudstorageblob.getName());
        cloudstorageblob.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(), new OperationContext());
        CloudBlockBlob blob = cloudstorageblob.getBlockBlobReference("blobName");
        String text = "test blob data";
        System.out.printf("Uploading text: \"%s\" to blob%n", text);
        blob.uploadText("test blob data");
        System.out.println("Downloading text: " + blob.downloadText());
        blob.delete();
        System.out.println("Blob deleted");
    }

    /**
     * Creates a service SAS definition with access to a blob container.
     */
    public void createBlobSasDefinition() throws URISyntaxException, StorageException, InvalidKeyException, ParseException, IOException {

        // Create the blob sas definition template
        // The sas template uri for service sas definitions contains the storage entity uri with the template token
        // This sample demonstrates constructing the template uri for a blob container, but a similar approach can
        // be used for all other storage service, i.e. File, Queue, table

        // Create a template sas token for a container
        StorageCredentials sas = new StorageCredentialsAccountAndKey(STORAGE_ACCOUNT_NAME, "00000001");
        // Note that the key passed in is just a dummy key such that we can generate the correct signature for the template.

        CloudStorageAccount cloudStorageAccount = new CloudStorageAccount(sas);
        CloudBlobClient cloudBlobClient = cloudStorageAccount.createCloudBlobClient();
        CloudBlobContainer cloudStorageBlob  = cloudBlobClient.getContainerReference("cloudstorageblob");
        SharedAccessBlobPolicy sharedAccessBlobPolicy = new SharedAccessBlobPolicy();
        sharedAccessBlobPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.ADD, SharedAccessBlobPermissions.CREATE, SharedAccessBlobPermissions.DELETE, SharedAccessBlobPermissions.LIST,
                SharedAccessBlobPermissions.READ, SharedAccessBlobPermissions.WRITE));
        sharedAccessBlobPolicy.setSharedAccessExpiryTime(FORMATTER.parse("01-01-2020"));

        // Generate shared access signature
        String sasSignature = cloudStorageBlob.generateSharedAccessSignature(sharedAccessBlobPolicy, null);

        // Creating the template Uri
        StorageCredentialsSharedAccessSignature storageSas = new StorageCredentialsSharedAccessSignature(sasSignature);
        URI storageUri = cloudStorageBlob.getStorageUri().getPrimaryUri();
        String templateUri = storageSas.transformUri(storageUri).toString();
        templateUri = templateUri.replace("http", "https"); // Make into https

        SasDefinitionAttributes attributes = new SasDefinitionAttributes().withEnabled(true);
        SasDefinitionBundle sasDefinition = keyVaultClient.setSasDefinition(VAULT_URI, STORAGE_ACCOUNT_NAME, "acctall", templateUri, SasTokenType.SERVICE, "PT2H", attributes, null);

        // Grab the secretName from the full secretId (the trailing characters after the last slash)
        String sasSecretId = sasDefinition.secretId();
        String secretName = sasSecretId.substring(sasSecretId.lastIndexOf("/")).substring(1);
        System.out.println("Retrieving secret name, " + secretName);
        SecretBundle serviceSasToken = keyVaultClient.getSecret(VAULT_URI, secretName);

        // Generating new cloud storage account object off of the new acctSasToken
        StorageCredentials sasCreds = new StorageCredentialsSharedAccessSignature(serviceSasToken.value());
        CloudStorageAccount newCloudAccount = new CloudStorageAccount(sasCreds, true, null, STORAGE_ACCOUNT_NAME);
        CloudBlobClient newCloudClient = newCloudAccount.createCloudBlobClient();

        // Grabbing the container created in the previous method, since Service tokens can't create containers.
        CloudBlobContainer newblob  = newCloudClient.getContainerReference("cloudstorageblob");

        // Creating a blob with the service storage token.
        CloudBlockBlob blob = newblob.getBlockBlobReference("blobName");
        String text = "test blob data";
        System.out.printf("Uploading text: \"%s\" to blob%n", text);
        blob.uploadText("test blob data");
        System.out.println("Downloading text: " + blob.downloadText());

    }

    /**
     * Lists and gets each of the sas definitions for the account.
     */
    public void getSasDefinition() {
        System.out.println("List and get sas definitions for the storage account");
        PagedList<SasDefinitionItem> sasDefinitions = keyVaultClient.getSasDefinitions(VAULT_URI, STORAGE_ACCOUNT_NAME);
        Iterator<SasDefinitionItem> iterator = sasDefinitions.iterator();

        while (iterator.hasNext()){
            SasDefinitionItem item = iterator.next();
            String id = item.id();
            String sasName = id.substring(id.lastIndexOf("/")).substring(1);
            SasDefinitionBundle sasDefinition = keyVaultClient.getSasDefinition(VAULT_URI, STORAGE_ACCOUNT_NAME, sasName);
            System.out.println(id + " " + sasDefinition.templateUri());
        }

    }

    // This is the same method as the one in KeyVaultManagedStorage - just sets up a storage account.
    private void setUpStorageAccount() {

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

}
