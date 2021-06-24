package com.r3.conclave.sample.enclave;

import com.r3.conclave.host.EnclaveHost;
import com.r3.conclave.host.EnclaveLoadException;
import com.r3.conclave.sample.common.AdDetails;
import com.r3.conclave.sample.common.UserDetails;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test class calculates the ad conversion rate given the inputs.
 */
public class MockTest {

    @Test
    void calculateAdConversionRate() throws EnclaveLoadException {
        EnclaveHost enclaveHost = EnclaveHost.load("com.r3.conclave.sample.enclave.PSIEnclave");
        enclaveHost.start(null, null);

        PSIEnclave psiEnclave = (PSIEnclave) enclaveHost.getMockEnclave();

        List<UserDetails> userDetailsList = new ArrayList();
        UserDetails userDetails1 = new UserDetails("123");
        UserDetails userDetails2 = new UserDetails("456");
        userDetailsList.add(userDetails1);
        userDetailsList.add(userDetails2);

        List<AdDetails> adDetailsList = new ArrayList();
        AdDetails adDetails1 = new AdDetails("123");
        AdDetails adDetails2 = new AdDetails("456");
        AdDetails adDetails3 = new AdDetails("789");
        AdDetails adDetails4 = new AdDetails("111");
        AdDetails adDetails5 = new AdDetails("222");

        adDetailsList.add(adDetails1);
        adDetailsList.add(adDetails2);
        adDetailsList.add(adDetails3);
        adDetailsList.add(adDetails4);
        adDetailsList.add(adDetails5);

        Double adConversionRate = psiEnclave.getAdConversionRate(userDetailsList, adDetailsList);

        assertNotNull(adConversionRate);
        assertEquals(40.0, adConversionRate);
    }
}