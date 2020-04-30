package com.test;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.EnvironmentCredentialBuilder;
import com.azure.management.Azure;
import com.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.azure.management.compute.VirtualMachine;
import com.azure.management.resources.fluentcore.arm.Region;
import com.azure.management.resources.fluentcore.model.Indexable;
import com.azure.management.resources.fluentcore.profile.AzureProfile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class Track2Differ {
    public static void customizeHttpClient() {
        TokenCredential credential = new EnvironmentCredentialBuilder().build();
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE, true);

        Azure azure = Azure.configure()
                .withHttpClient(new SimpleHttpClientWithLogging())
                .authenticate(credential, profile)
                .withDefaultSubscription();

        azure.resourceGroups().list().forEach(rg -> System.out.println(rg.name()));
    }

    public static void asynchronous(Azure azure) {
        Flux<Indexable> vm1 = azure.virtualMachines().define("vm1")
                .withRegion(Region.US_WEST)
                .withNewResourceGroup("randomRG")
                .withNewPrimaryNetwork("10.1.0.0/24")
                .withPrimaryPrivateIPAddressDynamic()
                .withoutPrimaryPublicIPAddress()
                .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
                .withRootUsername("username")
                .withRootPassword("Pa5$word1234")
                .createAsync();

        Flux<Indexable> vm2 = azure.virtualMachines().define("vm2")
                .withRegion(Region.US_WEST)
                .withNewResourceGroup("randomRG")
                .withNewPrimaryNetwork("10.1.1.0/24")
                .withPrimaryPrivateIPAddressDynamic()
                .withoutPrimaryPublicIPAddress()
                .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
                .withRootUsername("username")
                .withRootPassword("Pa5$word1234")
                .createAsync();

        Flux.merge(vm1, Mono.delay(Duration.ofSeconds(1)).thenMany(vm2))
                .doOnNext(indexable -> {
                    if (indexable instanceof VirtualMachine) {
                        System.out.printf("Created virtual machine %s\n", ((VirtualMachine) indexable).name());
                    }
                })
                .blockLast();
    }
}
