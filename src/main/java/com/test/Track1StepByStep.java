package com.test;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.AvailabilitySetSkuTypes;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;

public class Track1StepByStep {
    public static void CreateVm(Azure azure,
                                String resourceGroupName,
                                String location,
                                String vmName,
                                String rootPassword) {
        System.out.println("Creating resource group...");
        azure.resourceGroups().define(resourceGroupName)
                .withRegion(location)
                .create();

        System.out.println("Creating availability set...");
        AvailabilitySet availabilitySet = azure.availabilitySets().define("myAVSet")
                .withRegion(location)
                .withExistingResourceGroup(resourceGroupName)
                .withSku(AvailabilitySetSkuTypes.ALIGNED)
                .create();

        System.out.println("Creating public IP address...");
        PublicIPAddress publicIPAddress = azure.publicIPAddresses().define("myPublicIP")
                .withRegion(location)
                .withExistingResourceGroup(resourceGroupName)
                .withDynamicIP()
                .create();

        System.out.println("Creating virtual network...");
        Network network = azure.networks().define("myVNet")
                .withRegion(location)
                .withExistingResourceGroup(resourceGroupName)
                .withAddressSpace("10.0.0.0/16")
                .withSubnet("mySubnet", "10.0.0.0/24")
                .create();

        System.out.println("Creating network interface...");
        NetworkInterface networkInterface = azure.networkInterfaces().define("myNIC")
                .withRegion(location)
                .withExistingResourceGroup(resourceGroupName)
                .withExistingPrimaryNetwork(network)
                .withSubnet("mySubnet")
                .withPrimaryPrivateIPAddressDynamic()
                .withExistingPrimaryPublicIPAddress(publicIPAddress)
                .create();

        System.out.println("Creating virtual machine...");
        azure.virtualMachines().define(vmName)
                .withRegion(location)
                .withNewResourceGroup(resourceGroupName)
                .withExistingPrimaryNetworkInterface(networkInterface)
                .withLatestWindowsImage("MicrosoftWindowsServer", "WindowsServer", "2012-R2-Datacenter")
                .withAdminUsername("azureuser")
                .withAdminPassword(rootPassword)
                .withComputerName(vmName)
                .withExistingAvailabilitySet(availabilitySet)
                .withSize(VirtualMachineSizeTypes.BASIC_A0)
                .create();
    }
}
