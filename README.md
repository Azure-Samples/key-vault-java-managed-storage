---
services: key-vault
platforms: java
author: tiffanyachen
---

# Managed Storage Authentication sample for Azure Key Vault using the Azure Key Vault SDK

This sample repo includes sample code demonstrating common mechanisms for managing storage account keys using Key Vault.

# Samples in this repo
* KeyVaultManagedStorageSample
  * demonstrateSTorageAccountManagement - creates a storage account and then adds the storage account to the vault to manage its keys.
  * updateStorageAccount - updates a storage account in the vault
  * regenerateStorageAccountKeys - regenerates a key of a storage account managed by the vault
  * getStorageAccounts - list the storage accounts in the vault, then gets each
  * deletStorageAccount - deletes a storage account from the vault
* SasDefinitionSample
  * createAccountSasDefinition - creates an account SAS definition to manage storage account and its entities
  * createBlobSasDefinition - creates a service SAS definition to manage access to a blob container
  * getSasDefinition - lists and gets each of the sas definitions for this account

## Running the samples
1. If not installed, install [Java](https://www.java.com/en/download/help/download_options.xml).

2. Clone the repository.
```
git clone https://github.com/Azure-Samples/key-vault-java-authentication.git
```
3. Create an Azure service principal, using
[Azure CLI](http://azure.microsoft.com/documentation/articles/resource-group-authenticate-service-principal-cli/),
[PowerShell](http://azure.microsoft.com/documentation/articles/resource-group-authenticate-service-principal/)
or [Azure Portal](http://azure.microsoft.com/documentation/articles/resource-group-create-service-principal-portal/).
Note that if you wish to authenticate with the certificate authenticator the certificate should be saved locally.

4. Export these environment variables into your current shell or IDE.
```
    AZURE_TENANT_ID={your tenant id}
    RESOURCE_GROUP={your resource group}
```

5. Run main.java for a sample run through. This project uses maven so you can do so either through an IDE or on the command line.


## More information

* [What is Key Vault?](https://docs.microsoft.com/en-us/azure/key-vault/key-vault-whatis)
* [Get started with Azure Key Vault](https://docs.microsoft.com/en-us/azure/key-vault/key-vault-get-started)
* [Azure Key Vault General Documentation](https://docs.microsoft.com/en-us/azure/key-vault/)
* [Azure Key Vault REST API Reference](https://docs.microsoft.com/en-us/rest/api/keyvault/)
* [Azure SDK for Java Documentation](https://docs.microsoft.com/en-us/java/api/overview/azure/keyvault)
* [Azure Active Directory Documenation](https://docs.microsoft.com/en-us/azure/active-directory/)
