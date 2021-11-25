package com.r3.conclave.sample.enclave;

import com.r3.conclave.host.EnclaveLoadException;
import com.r3.conclave.host.EnclaveHost;
import com.r3.conclave.mail.Curve25519PrivateKey;
import com.r3.conclave.mail.PostOffice;
import org.junit.jupiter.api.Test;
import com.r3.conclave.sample.dataanalysis.common.UserProfile;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the enclave fully in-memory in a mock environment.
 */
public class MockTest {

    @Test
    void dataAnalysis() throws EnclaveLoadException {
        EnclaveHost mockHost = EnclaveHost.load("com.r3.conclave.sample.enclave.DAEnclave");
        mockHost.start(null, null, null, (commands) -> {
        });
        DAEnclave daEnclave = (DAEnclave) mockHost.getMockEnclave();

        //assertNull(daEnclave.previousResult);
        ArrayList<UserProfile> arr = new ArrayList<UserProfile>();
        UserProfile u1 = new UserProfile("John", 45, "USA", "m");
        UserProfile u2 = new UserProfile("Sera", 45, "India", "f");
        arr.add(u1);
        arr.add(u2);
        String response = daEnclave.dataAnalysis(arr);
        assertNotNull(response);
        assertEquals("Age Frequency Distribution: {45=2} Country Frequency Distribution: {USA=1, India=1} Gender Frequency Distribution: {f=1, m=1}", response);
    }
}
