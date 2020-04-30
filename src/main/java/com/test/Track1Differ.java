package com.test;

import com.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import rx.Observable;
import rx.functions.Action1;

import java.util.concurrent.TimeUnit;


public class Track1Differ {
    public static void asynchronous(Azure azure) {
        Observable<Indexable> vm1 = azure.virtualMachines().define("vm1")
                .withRegion(Region.US_WEST)
                .withNewResourceGroup("randomRG")
                .withNewPrimaryNetwork("10.1.0.0/24")
                .withPrimaryPrivateIPAddressDynamic()
                .withoutPrimaryPublicIPAddress()
                .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
                .withRootUsername("username")
                .withRootPassword("Pa5$word1234")
                .createAsync();

        Observable<Indexable> vm2 = azure.virtualMachines().define("vm2")
                .withRegion(Region.US_WEST)
                .withNewResourceGroup("randomRG")
                .withNewPrimaryNetwork("10.1.1.0/24")
                .withPrimaryPrivateIPAddressDynamic()
                .withoutPrimaryPublicIPAddress()
                .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
                .withRootUsername("username")
                .withRootPassword("Pa5$word1234")
                .createAsync();

        Observable.merge(vm1,
                Observable.empty().delay(1, TimeUnit.SECONDS)
                    .flatMap(empty -> vm2)
                    .switchIfEmpty(vm2)
        ).doOnNext(new Action1<Indexable>() {
            @Override
            public void call(Indexable indexable) {
                if (indexable instanceof VirtualMachine) {
                    System.out.println(((VirtualMachine) indexable).id());
                }
            }
        }).toBlocking().last();
    }
}
