package com.test;


import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.EnvironmentCredentialBuilder;
import com.azure.management.Azure;
import com.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.azure.management.compute.VirtualMachine;
import com.azure.management.network.Network;
import com.azure.management.resources.ResourceGroup;
import com.azure.management.resources.fluentcore.arm.Region;
import com.azure.management.resources.fluentcore.profile.AzureProfile;

import java.io.IOException;
import java.util.UUID;


public class Track2 {
    public static String randomString(String prefix, int len) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, len - prefix.length());
    }

    public static Azure authentication() throws IOException {

        // Use azure-cli or set following environment
        // export AZURE_CLIENT_ID=<clientId>
        // export AZURE_CLIENT_SECRET=<clientSecret>
        // export AZURE_TENANT_ID=<tenantId>
        // export AZURE_SUBSCRIPTION_ID=<subscriptionId>
        TokenCredential credential = new EnvironmentCredentialBuilder().build();

        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE, true);

        return Azure.authenticate(credential, profile).withDefaultSubscription();

        // You can use the following to list all subscriptions and choose one
        // Azure.authenticate(credential).subscriptions().list();
    }

    public static void operateVM(Azure azure) throws IOException {
        final String resourceGroupName = randomString("rg", 8);
        final String virtualMachineName = randomString("vm", 8);
        final Region region = Region.US_WEST;
        final String primaryNetworkSpace = "10.0.0.0/24";
        final String dnsPrefix = randomString("ds", 8);
        final String rootUsername = "roottest";
        final String rootPassword = "Pa$5word1234";

        // This is the usage of dnsPrefix
        // final String vmIp = String.format("%s.%s.cloudapp.azure.com", dnsPrefix, region);

        // create virtual machine
        VirtualMachine vm = azure.virtualMachines().define(virtualMachineName)
                .withRegion(region)
                .withNewResourceGroup(resourceGroupName)
                .withNewPrimaryNetwork(primaryNetworkSpace)
                .withPrimaryPrivateIPAddressDynamic()
                .withNewPrimaryPublicIPAddress(dnsPrefix)
                .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
                .withRootUsername(rootUsername)
                .withRootPassword(rootPassword)
                .create();
        // you could add more options before create if you like

        // update virtual machine
        vm.update()
                .withTag("test", "sample")
                .apply();

        // restart virtual machine
        vm.restart();

        // list virtual machine
        azure.virtualMachines().listByResourceGroup(resourceGroupName)
                .forEach(vm1 -> System.out.println(vm1.name()));
        // for (VirtualMachine vm1 : azure.virtualMachines().listByResourceGroup(resourceGroupName)) {
        //     System.out.println(vm1.name());
        // }

        // delete virtual machine
        azure.virtualMachines().deleteById(vm.id());
        // azure.virtualMachines().deleteByResourceGroup(resourceGroupName, virtualMachineName);

        // delete resource group
        azure.resourceGroups().beginDeleteByName(resourceGroupName);
    }

    public static void operateNetwork(Azure azure) {
        final String resourceGroupName = randomString("rg", 8);
        final String networkName = randomString("net", 8);
        final String subnetName = "subnet1";
        final Region region = Region.US_WEST;
        final String networkSpace = "10.0.0.0/24";
        final String subnetSpace = "10.0.0.0/28";
        final String anotherNetworkName = randomString("net", 8);
        final String anotherNetworkSpace = "10.0.1.0/24";

        // create network
        Network network = azure.networks().define(networkName)
                .withRegion(region)
                .withNewResourceGroup(resourceGroupName)
                .withAddressSpace(networkSpace)
                .defineSubnet(subnetName)
                .withAddressPrefix(subnetSpace)
                .attach()
                .create();

        // update network
        network.update()
                .withTag("test", "sample")
                .apply();

        // create another network
        azure.networks().define(anotherNetworkName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroupName)
                .withAddressSpace(anotherNetworkSpace)
                .create();

        // list network
        azure.networks().listByResourceGroup(resourceGroupName)
                .forEach(network1 -> System.out.println(network1.name()));

        // delete network
        azure.networks().deleteById(network.id());

        // delete resource group
        azure.resourceGroups().beginDeleteByName(resourceGroupName);
    }

    public static void operateResourceGroup(Azure azure) {
        final String resourceGroupName = randomString("rg", 8);
        final String anotherResourceGroupName = randomString("rg", 8);
        final Region region = Region.US_WEST;

        // create resource group
        ResourceGroup rg = azure.resourceGroups().define(resourceGroupName)
                .withRegion(region)
                .create();

        // update resource group
        rg.update()
                .withTag("test", "sample")
                .apply();

        // create another resource group
        azure.resourceGroups().define(anotherResourceGroupName)
                .withRegion(region)
                .create();

        // list resource group
        azure.resourceGroups().list()
                .forEach(rg1 -> System.out.println(rg1.name()));

        // delete resource group
        azure.resourceGroups().deleteByName(rg.name());
        azure.resourceGroups().beginDeleteByName(anotherResourceGroupName);
    }

    public static void errorHandling(Azure azure) {
        final String resourceGroupName = randomString("rg", 8);
        final String networkName = randomString("net", 8);
        final Region region = Region.US_WEST;

        try {
            azure.resourceGroups().getByName(resourceGroupName);

            azure.networks().define(networkName)
                    .withRegion(region)
                    .withExistingResourceGroup(resourceGroupName)
                    .withAddressSpace("")
                    .create();
        } catch (ResourceNotFoundException e) { // not work currently
            System.err.printf("Catch NotFoundException: %s\n", e);
        } catch (HttpResponseException e) {
            System.err.printf("Catch ResponseException: %s\n", e);
        }
    }

    public static void main() throws Exception {
        Azure azure = authentication();
        operateResourceGroup(azure);
        operateNetwork(azure);
        operateVM(azure);
        errorHandling(azure);
    }
}
