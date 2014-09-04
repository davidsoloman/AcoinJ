/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.acoin.params;

import com.google.acoin.core.NetworkParameters;
import com.google.acoin.core.Utils;
import org.spongycastle.util.encoders.Hex;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNet3Params extends NetworkParameters {
    public TestNet3Params() {
        super();
        id = ID_TESTNET;
        // Genesis hash is 000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943
        packetMagic = 0x1aeea50dL;
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        proofOfWorkLimit = Utils.decodeCompactBits(0x1e0fffffL);
        port = 27883;
        addressHeader = 87;
        p2shHeader = 187;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        dumpedPrivateKeyHeader = 238;
        genesisBlock.setTime(1408310900L);
        genesisBlock.setDifficultyTarget(0x1e0fffffL);
        genesisBlock.setNonce(415697184);
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 80640;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("000007e7f8efb559f64091425b1afec74144102039aee6ee38c613976d717b8c"));
        alertSigningKey = Hex.decode("04008f443ff94075f465b35a58705da4b8884a910916b2906be8092a762e4a496555d0abe51b8cea8196a1b7521eeb017ad9aebf281954d5e75d93ae7c0a7faa6a");

         dnsSeeds = new String[] {
                "209.73.144.179",
        };
    }

    private static TestNet3Params instance;
    public static synchronized TestNet3Params get() {
        if (instance == null) {
            instance = new TestNet3Params();
        }
        return instance;
    }

    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }
}
