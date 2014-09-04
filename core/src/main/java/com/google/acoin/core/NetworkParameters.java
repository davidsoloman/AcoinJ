/**
 * Copyright 2011 Google Inc.
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

package com.google.acoin.core;

import com.google.acoin.params.MainNetParams;
import com.google.acoin.params.RegTestParams;
import com.google.acoin.params.TestNet2Params;
import com.google.acoin.params.TestNet3Params;
import com.google.acoin.params.UnitTestParams;
import com.google.acoin.script.Script;
import com.google.acoin.script.ScriptOpCodes;
import com.google.common.base.Objects;

import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import static com.google.acoin.core.Utils.COIN;

/**
 * <p>NetworkParameters contains the data needed for working with an instantiation of a Bitcoin chain.</p>
 *
 * <p>This is an abstract class, concrete instantiations can be found in the params package. There are four:
 * one for the main network ({@link MainNetParams}), one for the public test network, and two others that are
 * intended for unit testing and local app development purposes. Although this class contains some aliases for
 * them, you are encouraged to call the static get() methods on each specific params class directly.</p>
 */
public abstract class NetworkParameters implements Serializable {
    /**
     * The protocol version this library implements.
     */
    public static final int PROTOCOL_VERSION = 70002;

    /**
     * The alert signing key originally owned by Satoshi, and now passed on to Gavin along with a few others.
     */
    public static final byte[] SATOSHI_KEY = Hex.decode("0498288f9bc78ea9d9aeb73a7c5136538af11d24533826af034e6ee1728cc683e5e0927a0e395a84a5f1a53c53c45c2d1e3a12ac944e9b2303683cf7c085bbc1e5");
    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String ID_MAINNET = "org.acoin.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_TESTNET = "org.acoin.test";
    /** Unit test network. */
    public static final String ID_UNITTESTNET = "com.google.acoin.unittest";

    /** The string used by the payment protocol to represent the main net. */
    public static final String PAYMENT_PROTOCOL_ID_MAINNET = "main";
    /** The string used by the payment protocol to represent the test net. */
    public static final String PAYMENT_PROTOCOL_ID_TESTNET = "test";

    // TODO: Seed nodes should be here as well.

    protected Block genesisBlock;
    protected BigInteger proofOfWorkLimit;
    protected int port;
	protected BigInteger maxTarget;
    protected long packetMagic;
    protected int addressHeader;
    protected int p2shHeader;
    protected int dumpedPrivateKeyHeader;
    protected int interval;
    protected int averagingInterval = AVERAGING_INTERVAL;
    protected int targetTimespan;
    protected int minActualTimespan = MIN_ACTUAL_TIMESPAN;
    protected int maxActualTimespan = MAX_ACTUAL_TIMESPAN;
    protected int averatingTargetTimespan = AVERAGING_TARGET_TIMESPAN;
    protected byte[] alertSigningKey;

    /**
     * See getId(). This may be null for old deserialized wallets. In that case we derive it heuristically
     * by looking at the port number.
     */
    protected String id;

    /**
     * The depth of blocks required for a coinbase transaction to be spendable.
     */
    protected int spendableCoinbaseDepth;
    protected int subsidyDecreaseBlockCount;
    
    protected int[] acceptableAddressCodes;
    protected String[] dnsSeeds;
    protected Map<Integer, Sha256Hash> checkpoints = new HashMap<Integer, Sha256Hash>();

    protected NetworkParameters() {
        alertSigningKey = SATOSHI_KEY;
        genesisBlock = createGenesis(this);
    }

    private static Block createGenesis(NetworkParameters n) {
        Block genesisBlock = new Block(n);
        Transaction t = new Transaction(n);
        try {
            // A script containing the difficulty bits and the following message:
            //
            //   "3 Aug 2013 - M&G - Mugabe wins Zim election with more than 60% of votes"
            byte[] bytes = Hex.decode
                    ("04ffff001d01044533204175672032303133202d204d2647202d204d75676162652077696e73205a696d20656c656374696f6e2077697468206d6f7265207468616e20363025206f6620766f746573");
            t.addInput(new TransactionInput(n, t, bytes));
            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(scriptPubKeyBytes, Hex.decode
                    ("04ffff001d0104433137303831342d444e2045677970742d4368696e6120616e642049737261656c2070726573656e7420616c7465726e61746976657320746f205375657a2043616e616c"));
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);
            t.addOutput(new TransactionOutput(n, t, Utils.toNanoCoins(1, 0), scriptPubKeyBytes.toByteArray()));
        } catch (Exception e) {
            // Cannot happen.
            throw new RuntimeException(e);
        }
        genesisBlock.addTransaction(t);
        // Unable to figure out the exact transaction input script therefore taking the shortcut by setting merkle root directly
        genesisBlock.setMerkleRoot(new Sha256Hash("01fe7bbb147e50de0dfa5b1ab6fc395f48faabecdb2a4e85a1a0207f21735a63"));
        return genesisBlock;
    }

    // Difficulty calculation parameters
    public static final int TARGET_TIMESPAN = 40;  // 2 minutes per difficulty cycle, on average.
    public static final int TARGET_SPACING = 40;  // 30 seconds per block.
    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING; // 4 blocks
    public static final int AVERAGING_INTERVAL = INTERVAL * 10; // 80 blocks
    public static final int AVERAGING_TARGET_TIMESPAN = AVERAGING_INTERVAL * TARGET_SPACING; // 40 minutes
    public static final int MAX_ADJUST_DOWN = 20; // 20% adjustment down
    public static final int MAX_ADJUST_UP = 2; // 1% adjustment up
    public static final int TARGET_TIMESPAM_ADJ_DOWN = TARGET_TIMESPAN * (100 + MAX_ADJUST_DOWN) / 100;
    public static final int MIN_ACTUAL_TIMESPAN = AVERAGING_TARGET_TIMESPAN * (100 - MAX_ADJUST_UP) / 100;
    public static final int MAX_ACTUAL_TIMESPAN = AVERAGING_TARGET_TIMESPAN * (100 + MAX_ADJUST_DOWN) / 100;

    /**
     * Blocks with a timestamp after this should enforce BIP 16, aka "Pay to script hash". This BIP changed the
     * network rules in a soft-forking manner, that is, blocks that don't follow the rules are accepted but not
     * mined upon and thus will be quickly re-orged out as long as the majority are enforcing the rule.
     */
    public static final int BIP16_ENFORCE_TIME = 1333238400;
    
    /**
     * The maximum money to be generated
     */
    public static final BigInteger MAX_MONEY = new BigInteger("2000000", 10).multiply(COIN);

    /** Alias for TestNet3Params.get(), use that instead. */
    @Deprecated
    public static NetworkParameters testNet() {
        return TestNet3Params.get();
    }

    /** Alias for TestNet2Params.get(), use that instead. */
    @Deprecated
    public static NetworkParameters testNet2() {
        return TestNet2Params.get();
    }

    /** Alias for TestNet3Params.get(), use that instead. */
    @Deprecated
    public static NetworkParameters testNet3() {
        return TestNet3Params.get();
    }

    /** Alias for MainNetParams.get(), use that instead */
    @Deprecated
    public static NetworkParameters prodNet() {
        return MainNetParams.get();
    }

    /** Returns a testnet params modified to allow any difficulty target. */
    @Deprecated
    public static NetworkParameters unitTests() {
        return UnitTestParams.get();
    }

    /** Returns a standard regression test params (similar to unitTests) */
    @Deprecated
    public static NetworkParameters regTests() {
        return RegTestParams.get();
    }

    /**
     * A Java package style string acting as unique ID for these parameters
     */
    public String getId() {
        return id;
    }

    public abstract String getPaymentProtocolId();

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NetworkParameters)) return false;
        NetworkParameters o = (NetworkParameters) other;
        return o.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    /** Returns the network parameters for the given string ID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromID(String id) {
        if (id.equals(ID_MAINNET)) {
            return MainNetParams.get();
        } else if (id.equals(ID_TESTNET)) {
            return TestNet3Params.get();
        } else if (id.equals(ID_UNITTESTNET)) {
            return UnitTestParams.get();
        } else {
            return null;
        }
    }

    /** Returns the network parameters for the given string paymentProtocolID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromPmtProtocolID(String pmtProtocolId) {
        if (pmtProtocolId.equals(PAYMENT_PROTOCOL_ID_MAINNET)) {
            return MainNetParams.get();
        } else if (pmtProtocolId.equals(PAYMENT_PROTOCOL_ID_TESTNET)) {
            return TestNet3Params.get();
        } else {
            return null;
        }
    }

    public int getSpendableCoinbaseDepth() {
        return spendableCoinbaseDepth;
    }

    /**
     * Returns true if the block height is either not a checkpoint, or is a checkpoint and the hash matches.
     */
    public boolean passesCheckpoint(int height, Sha256Hash hash) {
        Sha256Hash checkpointHash = checkpoints.get(height);
        return checkpointHash == null || checkpointHash.equals(hash);
    }

    /**
     * Returns true if the given height has a recorded checkpoint.
     */
    public boolean isCheckpoint(int height) {
        Sha256Hash checkpointHash = checkpoints.get(height);
        return checkpointHash != null;
    }

    public int getSubsidyDecreaseBlockCount() {
        return subsidyDecreaseBlockCount;
    }

    /** Returns DNS names that when resolved, give IP addresses of active peers. */
    public String[] getDnsSeeds() {
        return dnsSeeds;
    }

    /**
     * <p>Genesis block for this chain.</p>
     *
     * <p>The first block in every chain is a well known constant shared between all Bitcoin implemenetations. For a
     * block to be valid, it must be eventually possible to work backwards to the genesis block by following the
     * prevBlockHash pointers in the block headers.</p>
     *
     * <p>The genesis blocks for both test and prod networks contain the timestamp of when they were created,
     * and a message in the coinbase transaction. It says, <i>"The Times 03/Jan/2009 Chancellor on brink of second
     * bailout for banks"</i>.</p>
     */
    public Block getGenesisBlock() {
        return genesisBlock;
    }

    /** Default TCP port on which to connect to nodes. */
    public int getPort() {
        return port;
    }

    /** The header bytes that identify the start of a packet on this network. */
    public long getPacketMagic() {
        return packetMagic;
    }

    /**
     * First byte of a base58 encoded address. See {@link com.google.acoin.core.Address}. This is the same as acceptableAddressCodes[0] and
     * is the one used for "normal" addresses. Other types of address may be encountered with version codes found in
     * the acceptableAddressCodes array.
     */
    public int getAddressHeader() {
        return addressHeader;
    }

    /**
     * First byte of a base58 encoded P2SH address.  P2SH addresses are defined as part of BIP0013.
     */
    public int getP2SHHeader() {
        return p2shHeader;
    }

    /** First byte of a base58 encoded dumped private key. See {@link com.google.acoin.core.DumpedPrivateKey}. */
    public int getDumpedPrivateKeyHeader() {
        return dumpedPrivateKeyHeader;
    }

    /**
     * How much time in seconds is supposed to pass between "interval" blocks. If the actual elapsed time is
     * significantly different from this value, the network difficulty formula will produce a different value. Both
     * test and production Acoin networks use 2 minutes (120 seconds).
     */
    public int getTargetTimespan() {
        return targetTimespan;
    }

    /**
     * Documentation to be added. Used in difficulty transition calculation.
     */
    public int getMinActualTimespan() {
        return minActualTimespan;
    }

    /**
     * Documentation to be added. Used in difficulty transition calculation.
     */
    public int getMaxActualTimespan() {
        return maxActualTimespan;
    }

    /**
     * Documentation to be added. Used in difficulty transition calculation.
     */
    public int getAveratingTargetTimespan() {
        return averatingTargetTimespan;
    }

    /**
     * The version codes that prefix addresses which are acceptable on this network. Although Satoshi intended these to
     * be used for "versioning", in fact they are today used to discriminate what kind of data is contained in the
     * address and to prevent accidentally sending coins across chains which would destroy them.
     */
    public int[] getAcceptableAddressCodes() {
        return acceptableAddressCodes;
    }

    /**
     * If we are running in testnet-in-a-box mode, we allow connections to nodes with 0 non-genesis blocks.
     */
    public boolean allowEmptyPeerChain() {
        return true;
    }

    /** How many blocks pass between difficulty adjustment periods. Acoin standardises this to be 4. */
    public int getInterval() {
        return interval;
    }

    /** How many blocks whose average should the difficulty adjustment be based on. */
    public int getAveragingInterval() { return averagingInterval; }
	
	 /** Maximum target represents the easiest allowable proof of work. */
    public BigInteger getMaxTarget() {
        return maxTarget;
    }

    /** What the easiest allowable proof of work should be. */
    public BigInteger getProofOfWorkLimit() {
        return proofOfWorkLimit;
    }

    /**
     * The key used to sign {@link com.google.acoin.core.AlertMessage}s. You can use {@link com.google.acoin.core.ECKey#verify(byte[], byte[], byte[])} to verify
     * signatures using it.
     */
    public byte[] getAlertSigningKey() {
        return alertSigningKey;
    }
}
