/*
 * Copyright 2013 Matija Mazi.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2015 - 2018 Bepal
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

package bepal.eosio.hd;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.math.ec.FixedPointCombMultiplier;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * A deterministic key is a node in a {@link }. As per
 * <a href="https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki">the BIP 32 specification</a> it is a pair
 * (key, chaincode). If you know its path in the tree and its chain code you can derive more keys from this. To obtain
 * one of these, you can call {@link (byte[])}.
 */
public class DeterministicKey extends ECKey {

    private final DeterministicKey parent;
    private final ImmutableList<ChildNumber> childNumberPath;
    private final int depth;
    private int parentFingerprint; // 0 if this key is root node of key hierarchy
    /**
     * 32 bytes
     */
    private final byte[] chainCode;

    /**
     * Constructs a key from its components. This is not normally something you should use.
     */
    public DeterministicKey(ImmutableList<ChildNumber> childNumberPath, byte[] chainCode, ECPoint publicAsPoint, @Nullable BigInteger priv, @Nullable DeterministicKey parent) {
        super(priv, compressPoint(checkNotNull(publicAsPoint)));
        checkArgument(chainCode.length == 32);
        this.parent = parent;
        this.childNumberPath = checkNotNull(childNumberPath);
        this.chainCode = Arrays.copyOf(chainCode, chainCode.length);
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.parentFingerprint = (parent != null) ? parent.getFingerprint() : 0;
    }

    /**
     * Constructs a key from its components. This is not normally something you should use.
     */
    public DeterministicKey(ImmutableList<ChildNumber> childNumberPath, byte[] chainCode, BigInteger priv, @Nullable DeterministicKey parent) {
        super(priv, compressPoint(CURVE.getG().multiply(priv)));
        checkArgument(chainCode.length == 32);
        this.parent = parent;
        this.childNumberPath = checkNotNull(childNumberPath);
        this.chainCode = Arrays.copyOf(chainCode, chainCode.length);
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.parentFingerprint = (parent != null) ? parent.getFingerprint() : 0;
    }


    /**
     * Returns the path through some {@link } which reaches this keys position in the tree.
     * A path can be written as 1/2/1 which means the first child of the root, the second child of that node, then
     * the first child of that node.
     */
    public ImmutableList<ChildNumber> getPath() {
        return childNumberPath;
    }

    /**
     * Return this key's depth in the hierarchy, where the root node is at depth zero.
     * This may be different than the number of segments in the path if this key was
     * deserialized without access to its parent.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns the last element of the path returned by {@link DeterministicKey#getPath()}
     */
    public ChildNumber getChildNumber() {
        return childNumberPath.size() == 0 ? ChildNumber.ZERO : childNumberPath.get(childNumberPath.size() - 1);
    }

    /**
     * Returns the chain code associated with this key. See the specification to learn more about chain codes.
     */
    public byte[] getChainCode() {
        return chainCode;
    }

    @Nullable
    public DeterministicKey getParent() {
        return parent;
    }

    /**
     * Returns private key bytes, padded with zeros to 33 bytes.
     *
     * @throws IllegalStateException if the private key bytes are missing.
     */
    public byte[] getPrivKeyBytes33() {
        byte[] bytes33 = new byte[33];
        byte[] priv = getPrivKeyBytes();
        System.arraycopy(priv, 0, bytes33, 33 - priv.length, priv.length);
        return bytes33;
    }

    /**
     * Returns the same key with the private bytes removed. May return the same instance. The purpose of this is to save
     * memory: the private key can always be very efficiently rederived from a parent that a private key, so storing
     * all the private keys in RAM is a poor tradeoff especially on constrained devices. This means that the returned
     * key may still be usable for signing and so on, so don't expect it to be a true pubkey-only object! If you want
     * that then you should follow this call with a call to {@link #()}.
     */
    public DeterministicKey dropPrivateBytes() {
        if (isPubKeyOnly())
            return this;
        else
            return new DeterministicKey(getPath(), getChainCode(), pub, null, parent);
    }


    public static byte[] addChecksum(byte[] input) {
        int inputLength = input.length;
        byte[] checksummed = new byte[inputLength + 4];
        System.arraycopy(input, 0, checksummed, 0, inputLength);
        byte[] checksum = Sha256Hash.hashTwice(input);
        System.arraycopy(checksum, 0, checksummed, inputLength, 4);
        return checksummed;
    }

    /**
     * A deterministic key is considered to be 'public key only' if it hasn't got a private key part and it cannot be
     * rederived. If the hierarchy is encrypted this returns true.
     */
    @Override
    public boolean isPubKeyOnly() {
        return super.isPubKeyOnly() && (parent == null || parent.isPubKeyOnly());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPrivKey() {
        return privKey != null;
    }

    /**
     * Returns the first 32 bits of the result of {@link #getIdentifier()}.
     */
    public int getFingerprint() {
        // TODO: why is this different than armory's fingerprint? BIP 32: "The first 32 bits of the identifier are called the fingerprint."
        return ByteBuffer.wrap(Arrays.copyOfRange(getIdentifier(), 0, 4)).getInt();
    }

    /**
     * Returns RIPE-MD160(SHA256(pub key bytes)).
     */
    public byte[] getIdentifier() {
        return Sha256Hash.sha256hash160(getPubKey());
    }

    /**
     * Return the fingerprint of the key from which this key was derived, if this is a
     * child key, or else an array of four zero-value bytes.
     */
    public int getParentFingerprint() {
        return parentFingerprint;
    }

    public byte[] serializePublic() {
        return serialize(true);
    }

    public byte[] serializePrivate() {
        return serialize(false);
    }

    private byte[] serialize(boolean pub) {
        ByteBuffer ser = ByteBuffer.allocate(78);
        ser.putInt(pub ? 0x0488B21E : 0x0488ADE4);
        ser.put((byte) getDepth());
        ser.putInt(getParentFingerprint());
        ser.putInt(getChildNumber().i());
        ser.put(getChainCode());
        ser.put(pub ? getPubKey() : getPrivKeyBytes33());
        checkState(ser.position() == 78);
        return ser.array();
    }

    public String serializePubB58() {
        return toBase58(serialize(true));
    }

    public String serializePrivB58() {
        return toBase58(serialize(false));
    }

    static String toBase58(byte[] ser) {
        return Base58.encode(addChecksum(ser));
    }

    /**
     * Deserialize a base-58-encoded HD Key with no parent
     */
    public static DeterministicKey deserializeB58(String base58) {
        return deserializeB58(null, base58);
    }

    /**
     * Deserialize a base-58-encoded HD Key.
     *
     * @param parent The parent node in the given key's deterministic hierarchy.
     * @throws IllegalArgumentException if the base58 encoded key could not be parsed.
     */
    public static DeterministicKey deserializeB58(@Nullable DeterministicKey parent, String base58) {
        return deserialize(Base58.decodeChecked(base58), parent);
    }

    /**
     * Deserialize an HD Key with no parent
     */
    public static DeterministicKey deserialize(byte[] serializedKey) {
        return deserialize(serializedKey, null);
    }

    /**
     * Append a derivation level to an existing path
     */
    public static ImmutableList<ChildNumber> append(List<ChildNumber> path, ChildNumber childNumber) {
        return ImmutableList.<ChildNumber>builder().addAll(path).add(childNumber).build();
    }

    /**
     * Deserialize an HD Key.
     *
     * @param parent The parent node in the given key's deterministic hierarchy.
     */
    public static DeterministicKey deserialize(byte[] serializedKey, @Nullable DeterministicKey parent) {
        ByteBuffer buffer = ByteBuffer.wrap(serializedKey);
        int header = buffer.getInt();
        int xpub = 0x0488B21E;
        int xpri = 0x0488ADE4;
        if (header != xpri && header != xpub)
            throw new IllegalArgumentException("Unknown header bytes: " + toBase58(serializedKey).substring(0, 4));
        boolean pub = header == xpub;
        int depth = buffer.get() & 0xFF; // convert signed byte to positive int since depth cannot be negative
        final int parentFingerprint = buffer.getInt();
        final int i = buffer.getInt();
        final ChildNumber childNumber = new ChildNumber(i);
        ImmutableList<ChildNumber> path;
        if (parent != null) {
            if (parentFingerprint == 0)
                throw new IllegalArgumentException("Parent was provided but this key doesn't have one");
            if (parent.getFingerprint() != parentFingerprint)
                throw new IllegalArgumentException("Parent fingerprints don't match");
            path = append(parent.getPath(), childNumber);
            if (path.size() != depth)
                throw new IllegalArgumentException("Depth does not match");
        } else {
            if (depth >= 1)
                // We have been given a key that is not a root key, yet we lack the object representing the parent.
                // This can happen when deserializing an account key for a watching wallet.  In this case, we assume that
                // the client wants to conceal the key's position in the hierarchy.  The path is truncated at the
                // parent's node.
                path = ImmutableList.of(childNumber);
            else path = ImmutableList.of();
        }
        byte[] chainCode = new byte[32];
        buffer.get(chainCode);
        byte[] data = new byte[33];
        buffer.get(data);
        checkArgument(!buffer.hasRemaining(), "Found unexpected data in key");
        if (pub) {
            return new DeterministicKey(path, chainCode, ECKey.CURVE.getCurve().decodePoint(data), null, parent);
        } else {
            return new DeterministicKey(path, chainCode, new BigInteger(1, data), parent);
        }
    }

    /**
     * Verifies equality of all fields but NOT the parent pointer (thus the same key derived in two separate heirarchy
     * objects will equal each other.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeterministicKey other = (DeterministicKey) o;
        return super.equals(other)
                && Arrays.equals(this.chainCode, other.chainCode)
                && Objects.equal(this.childNumberPath, other.childNumberPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), Arrays.hashCode(chainCode), childNumberPath);
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this).omitNullValues();
        helper.add("pub", Hex.toHexString(pub.getEncoded()));
        helper.add("chainCode", Hex.toHexString(chainCode));
        helper.add("path", getPath());
        helper.add("isPubKeyOnly", isPubKeyOnly());
        return helper.toString();
    }

    /**
     * Returns a key for the given path, optionally creating it.
     *
     * @param path the path to the key
     * @return next newly created key using the child derivation function
     * @throws IllegalArgumentException if create is false and the path was not found.
     */
    public DeterministicKey get(List<ChildNumber> path) {
        DeterministicKey temp = this;
        for (int i = 0; i < path.size(); i++) {
            temp = temp.deriveChildKey(path.get(i));
        }
        return temp;
    }

    /**
     * @throws if private derivation is attempted for a public-only parent key, or
     *            if the resulting derived key is invalid (eg. private key == 0).
     */
    public DeterministicKey deriveChildKey(ChildNumber childNumber) {
        if (!hasPrivKey()) {
            return getPublic(childNumber);
        } else {
            return getPrivate(childNumber);
        }
    }

    public DeterministicKey getPrivate(ChildNumber childNumber) {
        checkArgument(hasPrivKey(), "Parent key must have private key bytes for this method.");
        byte[] parentPublicKey = getPubKeyPoint().getEncoded(true);
        checkState(parentPublicKey.length == 33, "Parent pubkey must be 33 bytes, but is " + parentPublicKey.length);
        ByteBuffer data = ByteBuffer.allocate(37);
        if (childNumber.isHardened()) {
            data.put(getPrivKeyBytes33());
        } else {
            data.put(parentPublicKey);
        }
        data.putInt(childNumber.i());
        byte[] i = Sha256Hash.hmacSha512(getChainCode(), data.array());
        checkState(i.length == 64, i.length);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] chainCode = Arrays.copyOfRange(i, 32, 64);
        BigInteger ilInt = new BigInteger(1, il);

        final BigInteger priv = getPrivKey();
        BigInteger ki = priv.add(ilInt).mod(ECKey.CURVE.getN());

        return new DeterministicKey(append(getPath(), childNumber), chainCode, ki, this);
    }

    public DeterministicKey getPublic(ChildNumber childNumber) {
        byte[] parentPublicKey = getPubKeyPoint().getEncoded(true);
        checkState(parentPublicKey.length == 33, "Parent pubkey must be 33 bytes, but is " + parentPublicKey.length);
        ByteBuffer data = ByteBuffer.allocate(37);
        data.put(parentPublicKey);
        data.putInt(childNumber.i());
        byte[] i = Sha256Hash.hmacSha512(getChainCode(), data.array());
        checkState(i.length == 64, i.length);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] chainCode = Arrays.copyOfRange(i, 32, 64);
        BigInteger ilInt = new BigInteger(1, il);
        ECPoint Ki = publicPointFromPrivate(ilInt).add(getPubKeyPoint());
        return new DeterministicKey(append(getPath(), childNumber), chainCode, Ki, null, this);
    }

    /**
     * Returns public key point from the given private key. To convert a byte array into a BigInteger, use <tt>
     * new BigInteger(1, bytes);</tt>
     */
    public static ECPoint publicPointFromPrivate(BigInteger privKey) {
        /*
         * TODO: FixedPointCombMultiplier currently doesn't support scalars longer than the group order,
         * but that could change in future versions.
         */
        if (privKey.bitLength() > CURVE.getN().bitLength()) {
            privKey = privKey.mod(CURVE.getN());
        }
        String s = privKey.toString(16);
        return new FixedPointCombMultiplier().multiply(CURVE.getG(), privKey);
    }


    /**
     * Generates a new deterministic key from the given seed, which can be any arbitrary byte array. However resist
     * the temptation to use a string as the seed - any key derived from a password is likely to be weak and easily
     * broken by attackers (this is not theoretical, people have had money stolen that way). This method checks
     * that the given seed is at least 64 bits long.
     *
     * @throws if                       generated master key is invalid (private key 0 or >= n).
     * @throws IllegalArgumentException if the seed is less than 8 bytes and could be brute forced.
     */
    public static DeterministicKey createMasterPrivateKey(byte[] seed) {
        checkArgument(seed.length > 8, "Seed is too short and could be brute forced");
        // Calculate I = HMAC-SHA512(key="Bitcoin seed", msg=S)
        byte[] i = Sha256Hash.hmacSha512(Sha256Hash.createHmacSha512Digest("Bitcoin seed".getBytes()), seed);
        // Split I into two 32-byte sequences, Il and Ir.
        // Use Il as master secret key, and Ir as master chain code.
        checkState(i.length == 64, i.length);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] ir = Arrays.copyOfRange(i, 32, 64);
        Arrays.fill(i, (byte) 0);
        DeterministicKey masterPrivKey = createMasterPrivKeyFromBytes(il, ir);
        Arrays.fill(il, (byte) 0);
        Arrays.fill(ir, (byte) 0);
        return masterPrivKey;
    }

    /**
     * @throws if privKeyBytes is invalid (0 or >= n).
     */
    public static DeterministicKey createMasterPrivKeyFromBytes(byte[] privKeyBytes, byte[] chainCode) {
        BigInteger priv = new BigInteger(1, privKeyBytes);
        return new DeterministicKey(ImmutableList.<ChildNumber>of(), chainCode, priv, null);
    }
}
