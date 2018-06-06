/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2014 the libsecp256k1 contributors
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

import bepal.eosio.crypto.ec.EosPrivateKey;
import bepal.eosio.crypto.ec.EosPublicKey;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.primitives.UnsignedBytes;
import org.spongycastle.asn1.*;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.asn1.x9.X9IntegerConverter;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.math.ec.FixedPointCombMultiplier;
import org.spongycastle.math.ec.FixedPointUtil;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// TODO: Move this class to tracking compression state itself.
// The Bouncy Castle developers are deprecating their own tracking of the compression state.

/**
 * <p>Represents an elliptic curve public and (optionally) private key, usable for digital signatures but not encryption.
 * Creating a new ECKey with the empty constructor will generate a new random keypair. Other static methods can be used
 * when you already have the public or private parts. If you create a key with only the public part, you can check
 * signatures but not create them.</p>
 *
 * <p>ECKey also provides access to Bitcoin Core compatible text message signing, as accessible via the UI or JSON-RPC.
 * This is slightly different to signing raw bytes - if you want to sign your own data and it won't be exposed as
 * text to people, you don't want to use this. If in doubt, ask on the mailing list.</p>
 *
 * <p>The ECDSA algorithm supports <i>key recovery</i> in which a signature plus a couple of discriminator bits can
 * be reversed to find the public key used to calculate it. This can be convenient when you have a message and a
 * signature and want to find out who signed it, rather than requiring the user to provide the expected identity.</p>
 *
 * <p>This class supports a variety of serialization forms. The methods that accept/return byte arrays serialize
 * private keys as raw byte arrays and public keys using the SEC standard byte encoding for public keys. Signatures
 * are encoded using ASN.1/DER inside the Bitcoin protocol.</p>
 *
 * <p>A key can be <i>compressed</i> or <i>uncompressed</i>. This refers to whether the public key is represented
 * when encoded into bytes as an (x, y) coordinate on the elliptic curve, or whether it's represented as just an X
 * co-ordinate and an extra byte that carries a sign bit. With the latter form the Y coordinate can be calculated
 * dynamically, however, <b>because the binary serialization is different the address of a key changes if its
 * compression status is changed</b>. If you deviate from the defaults it's important to understand this: money sent
 * to a compressed version of the key will have a different address to the same key in uncompressed form. Whether
 * a public key is compressed or not is recorded in the SEC binary serialisation format, and preserved in a flag in
 * this class so round-tripping preserves state. Unless you're working with old software or doing unusual things, you
 * can usually ignore the compressed/uncompressed distinction.</p>
 */
public class ECKey {

    /**
     * Sorts oldest keys first, newest last.
     */
    public static final Comparator<ECKey> AGE_COMPARATOR = new Comparator<ECKey>() {

        @Override
        public int compare(ECKey k1, ECKey k2) {
            if (k1.creationTimeSeconds == k2.creationTimeSeconds)
                return 0;
            else
                return k1.creationTimeSeconds > k2.creationTimeSeconds ? 1 : -1;
        }
    };

    /**
     * Compares pub key bytes using {@link UnsignedBytes#lexicographicalComparator()}
     */
    public static final Comparator<ECKey> PUBKEY_COMPARATOR = new Comparator<ECKey>() {
        private Comparator<byte[]> comparator = UnsignedBytes.lexicographicalComparator();

        @Override
        public int compare(ECKey k1, ECKey k2) {
            return comparator.compare(k1.getPubKey(), k2.getPubKey());
        }
    };

    // The parameters of the secp256k1 curve that Bitcoin uses.
    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");

    /**
     * The parameters of the secp256k1 curve that Bitcoin uses.
     */
    public static final ECDomainParameters CURVE;

    /**
     * Equal to CURVE.getN().shiftRight(1), used for canonicalising the S value of a signature. If you aren't
     * sure what this is about, you can ignore it.
     */
    public static final BigInteger HALF_CURVE_ORDER;

    private static final SecureRandom secureRandom;

    static {
        // Tell Bouncy Castle to precompute data that's needed during secp256k1 calculations. Increasing the width
        // number makes calculations faster, but at a cost of extra memory usage and with decreasing returns. 12 was
        // picked after consulting with the BC team.
        FixedPointUtil.precompute(CURVE_PARAMS.getG(), 12);
        CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(),
                CURVE_PARAMS.getH());
        HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);
        secureRandom = new SecureRandom();
    }

    // The two parts of the key. If "priv" is set, "pub" can always be calculated. If "pub" is set but not "priv", we
    // can only verify signatures not make them.
    protected BigInteger privKey;  // A field element.
    protected ECPoint pub;

    // Creation time of the key in seconds since the epoch, or zero if the key was deserialized from a version that did
    // not have this field.
    protected long creationTimeSeconds;

    private byte[] pubKeyHash;


    protected ECKey(@Nullable BigInteger priv, ECPoint pub) {
        this.privKey = priv;
        this.pub = checkNotNull(pub);
    }

    /**
     * Utility for compressing an elliptic curve point. Returns the same point if it's already compressed.
     * See the ECKey class docs for a discussion of point compression.
     */
    public static ECPoint compressPoint(ECPoint point) {
        return getPointWithCompression(point, true);
    }

    /**
     * Utility for decompressing an elliptic curve point. Returns the same point if it's already compressed.
     * See the ECKey class docs for a discussion of point compression.
     */
    public static ECPoint decompressPoint(ECPoint point) {
        return getPointWithCompression(point, false);
    }

    private static ECPoint getPointWithCompression(ECPoint point, boolean compressed) {
        if (point.isCompressed() == compressed)
            return point;
        point = point.normalize();
        BigInteger x = point.getAffineXCoord().toBigInteger();
        BigInteger y = point.getAffineYCoord().toBigInteger();
        return CURVE.getCurve().createPoint(x, y, compressed);
    }

    /**
     * Construct an ECKey from an ASN.1 encoded private key. These are produced by OpenSSL and stored by Bitcoin
     * Core in its wallet. Note that this is slow because it requires an EC point multiply.
     */
    public static ECKey fromASN1(byte[] asn1privkey) {
        return extractKeyFromASN1(asn1privkey);
    }

    /**
     * Creates an ECKey given the private key only. The public key is calculated from it (this is slow). The resulting
     * public key is compressed.
     */
    public static ECKey fromPrivate(BigInteger privKey) {
        return fromPrivate(privKey, true);
    }

    /**
     * Creates an ECKey given the private key only. The public key is calculated from it (this is slow), either
     * compressed or not.
     */
    public static ECKey fromPrivate(BigInteger privKey, boolean compressed) {
        ECPoint point = publicPointFromPrivate(privKey);
        return new ECKey(privKey, getPointWithCompression(point, compressed));
    }

    /**
     * Creates an ECKey given the private key only. The public key is calculated from it (this is slow). The resulting
     * public key is compressed.
     */
    public static ECKey fromPrivate(byte[] privKeyBytes) {
        return fromPrivate(new BigInteger(1, privKeyBytes));
    }

    /**
     * Creates an ECKey given the private key only. The public key is calculated from it (this is slow), either
     * compressed or not.
     */
    public static ECKey fromPrivate(byte[] privKeyBytes, boolean compressed) {
        return fromPrivate(new BigInteger(1, privKeyBytes), compressed);
    }

    /**
     * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
     * generator point by the private key. This is used to speed things up when you know you have the right values
     * already. The compression state of pub will be preserved.
     */
    public static ECKey fromPrivateAndPrecalculatedPublic(BigInteger priv, ECPoint pub) {
        return new ECKey(priv, pub);
    }

    /**
     * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
     * generator point by the private key. This is used to speed things up when you know you have the right values
     * already. The compression state of the point will be preserved.
     */
    public static ECKey fromPrivateAndPrecalculatedPublic(byte[] priv, byte[] pub) {
        checkNotNull(priv);
        checkNotNull(pub);
        return new ECKey(new BigInteger(1, priv), CURVE.getCurve().decodePoint(pub));
    }

    /**
     * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given point. The
     * compression state of pub will be preserved.
     */
    public static ECKey fromPublicOnly(ECPoint pub) {
        return new ECKey(null, pub);
    }

    /**
     * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given encoded point.
     * The compression state of pub will be preserved.
     */
    public static ECKey fromPublicOnly(byte[] pub) {
        return new ECKey(null, CURVE.getCurve().decodePoint(pub));
    }

    /**
     * Returns a copy of this key, but with the public point represented in uncompressed form. Normally you would
     * never need this: it's for specialised scenarios or when backwards compatibility in encoded form is necessary.
     */
    public ECKey decompress() {
        if (!pub.isCompressed())
            return this;
        else
            return new ECKey(privKey, pub);
    }

    /**
     * Creates an ECKey given only the private key bytes. This is the same as using the BigInteger constructor, but
     * is more convenient if you are importing a key from elsewhere. The public key will be automatically derived
     * from the private key.
     */
    @Deprecated
    public ECKey(@Nullable byte[] privKeyBytes, @Nullable byte[] pubKey) {
        this(privKeyBytes == null ? null : new BigInteger(1, privKeyBytes), pubKey);
    }


    /**
     * Creates an ECKey given either the private key only, the public key only, or both. If only the private key
     * is supplied, the public key will be calculated from it (this is slow). If both are supplied, it's assumed
     * the public key already correctly matches the private key. If only the public key is supplied, this ECKey cannot
     * be used for signing.
     *
     * @param compressed If set to true and pubKey is null, the derived public key will be in compressed form.
     */
    @Deprecated
    public ECKey(@Nullable BigInteger privKey, @Nullable byte[] pubKey, boolean compressed) {
        if (privKey == null && pubKey == null)
            throw new IllegalArgumentException("ECKey requires at least private or public key");
        this.privKey = privKey;
        // Derive public from private.
        ECPoint point = publicPointFromPrivate(privKey);
        point = getPointWithCompression(point, compressed);
        this.pub = point;
    }

    /**
     * Creates an ECKey given either the private key only, the public key only, or both. If only the private key
     * is supplied, the public key will be calculated from it (this is slow). If both are supplied, it's assumed
     * the public key already correctly matches the public key. If only the public key is supplied, this ECKey cannot
     * be used for signing.
     */
    @Deprecated
    private ECKey(@Nullable BigInteger privKey, @Nullable byte[] pubKey) {
        this(privKey, pubKey, false);
    }

    /**
     * Returns true if this key doesn't have unencrypted access to private key bytes. This may be because it was never
     * given any private key bytes to begin with (a watching key), or because the key is encrypted. You can use
     * {@link } to tell the cases apart.
     */
    public boolean isPubKeyOnly() {
        return privKey == null;
    }

    /**
     * Returns true if this key has unencrypted access to private key bytes. Does the opposite of
     * {@link #isPubKeyOnly()}.
     */
    public boolean hasPrivKey() {
        return privKey != null;
    }

    /**
     * Output this ECKey as an ASN.1 encoded private key, as understood by OpenSSL or used by Bitcoin Core
     * in its wallet storage format.
     *
     * @throws if the private key is missing or encrypted.
     */
    public byte[] toASN1() {
        try {
            byte[] privKeyBytes = getPrivKeyBytes();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(400);

            // ASN1_SEQUENCE(EC_PRIVATEKEY) = {
            //   ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
            //   ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
            //   ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
            //   ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
            // } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
            DERSequenceGenerator seq = new DERSequenceGenerator(baos);
            seq.addObject(new ASN1Integer(1)); // version
            seq.addObject(new DEROctetString(privKeyBytes));
            seq.addObject(new DERTaggedObject(0, CURVE_PARAMS.toASN1Primitive()));
            seq.addObject(new DERTaggedObject(1, new DERBitString(getPubKey())));
            seq.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen, writing to memory stream.
        }
    }

    /**
     * Returns public key bytes from the given private key. To convert a byte array into a BigInteger, use <tt>
     * new BigInteger(1, bytes);</tt>
     */
    public static byte[] publicKeyFromPrivate(BigInteger privKey, boolean compressed) {
        ECPoint point = publicPointFromPrivate(privKey);
        return point.getEncoded(compressed);
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
        return new FixedPointCombMultiplier().multiply(CURVE.getG(), privKey);
    }

    /**
     * Gets the raw public key value. This appears in transaction scriptSigs. Note that this is <b>not</b> the same
     * as the pubKeyHash/address.
     */
    public byte[] getPubKey() {
        return pub.getEncoded();
    }

    /**
     * Gets the public key in the form of an elliptic curve point object from Bouncy Castle.
     */
    public ECPoint getPubKeyPoint() {
        return pub;
    }

    /**
     * Gets the private key in the form of an integer field element. The public key is derived by performing EC
     * point addition this number of times (i.e. point multiplying).
     *
     * @throws IllegalStateException if the private key bytes are not available.
     */
    public BigInteger getPrivKey() {
        if (privKey == null)
            throw new RuntimeException();
        return privKey;
    }

    /**
     * Returns whether this key is using the compressed form or not. Compressed pubkeys are only 33 bytes, not 64.
     */
    public boolean isCompressed() {
        return pub.isCompressed();
    }

    /**
     * Groups the two components that make up a signature, and provides a way to encode to DER form, which is
     * how ECDSA signatures are represented when embedded in other data structures in the Bitcoin protocol. The raw
     * components can be useful for doing further EC maths on them.
     */
    public static class ECDSASignature {
        /**
         * The two components of the signature.
         */
        public final BigInteger r, s;

        /**
         * Constructs a signature with the given components. Does NOT automatically canonicalise the signature.
         */
        public ECDSASignature(BigInteger r, BigInteger s) {
            this.r = r;
            this.s = s;
        }

        /**
         * Returns true if the S component is "low", that means it is below {@link ECKey#HALF_CURVE_ORDER}. See <a
         * href="https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#Low_S_values_in_signatures">BIP62</a>.
         */
        public boolean isCanonical() {
            return s.compareTo(HALF_CURVE_ORDER) <= 0;
        }

        /**
         * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
         * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
         * the same message. However, we dislike the ability to modify the bits of a Bitcoin transaction after it's
         * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
         * considered legal and the other will be banned.
         */
        public ECDSASignature toCanonicalised() {
            if (!isCanonical()) {
                // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
                // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
                //    N = 10
                //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
                //    10 - 8 == 2, giving us always the latter solution, which is canonical.
                return new ECDSASignature(r, CURVE.getN().subtract(s));
            } else {
                return this;
            }
        }

        /**
         * DER is an international standard for serializing data structures which is widely used in cryptography.
         * It's somewhat like protocol buffers but less convenient. This method returns a standard DER encoding
         * of the signature, as recognized by OpenSSL and other libraries.
         */
        public byte[] encodeToDER() {
            try {
                return derByteStream().toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);  // Cannot happen.
            }
        }

        public static ECDSASignature decodeFromDER(byte[] bytes) throws IllegalArgumentException {
            ASN1InputStream decoder = null;
            try {
                decoder = new ASN1InputStream(bytes);
                final ASN1Primitive seqObj = decoder.readObject();
                if (seqObj == null)
                    throw new IllegalArgumentException("Reached past end of ASN.1 stream.");
                if (!(seqObj instanceof DLSequence))
                    throw new IllegalArgumentException("Read unexpected class: " + seqObj.getClass().getName());
                final DLSequence seq = (DLSequence) seqObj;
                ASN1Integer r, s;
                try {
                    r = (ASN1Integer) seq.getObjectAt(0);
                    s = (ASN1Integer) seq.getObjectAt(1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(e);
                }
                // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
                // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
                return new ECDSASignature(r.getPositiveValue(), s.getPositiveValue());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            } finally {
                if (decoder != null)
                    try {
                        decoder.close();
                    } catch (IOException x) {
                    }
            }
        }

        protected ByteArrayOutputStream derByteStream() throws IOException {
            // Usually 70-72 bytes.
            ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
            DERSequenceGenerator seq = new DERSequenceGenerator(bos);
            seq.addObject(new ASN1Integer(r));
            seq.addObject(new ASN1Integer(s));
            seq.close();
            return bos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ECDSASignature other = (ECDSASignature) o;
            return r.equals(other.r) && s.equals(other.s);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(r, s);
        }
    }

    /**
     * If this global variable is set to true, sign() creates a dummy signature and verify() always returns true.
     * This is intended to help accelerate unit tests that do a lot of signing/verifying, which in the debugger
     * can be painfully slow.
     */
    @VisibleForTesting
    public static boolean FAKE_SIGNATURES = false;

    /**
     * Returns true if the given pubkey is canonical, i.e. the correct length taking into account compression.
     */
    public static boolean isPubKeyCanonical(byte[] pubkey) {
        if (pubkey.length < 33)
            return false;
        if (pubkey[0] == 0x04) {
            // Uncompressed pubkey
            if (pubkey.length != 65)
                return false;
        } else if (pubkey[0] == 0x02 || pubkey[0] == 0x03) {
            // Compressed pubkey
            if (pubkey.length != 33)
                return false;
        } else
            return false;
        return true;
    }

    private static ECKey extractKeyFromASN1(byte[] asn1privkey) {
        // To understand this code, see the definition of the ASN.1 format for EC private keys in the OpenSSL source
        // code in ec_asn1.c:
        //
        // ASN1_SEQUENCE(EC_PRIVATEKEY) = {
        //   ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
        //   ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
        //   ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
        //   ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
        // } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
        //
        try {
            ASN1InputStream decoder = new ASN1InputStream(asn1privkey);
            DLSequence seq = (DLSequence) decoder.readObject();
            checkArgument(decoder.readObject() == null, "Input contains extra bytes");
            decoder.close();

            checkArgument(seq.size() == 4, "Input does not appear to be an ASN.1 OpenSSL EC private key");

            checkArgument(((ASN1Integer) seq.getObjectAt(0)).getValue().equals(BigInteger.ONE),
                    "Input is of wrong version");

            byte[] privbits = ((ASN1OctetString) seq.getObjectAt(1)).getOctets();
            BigInteger privkey = new BigInteger(1, privbits);

            ASN1TaggedObject pubkey = (ASN1TaggedObject) seq.getObjectAt(3);
            checkArgument(pubkey.getTagNo() == 1, "Input has 'publicKey' with bad tag number");
            byte[] pubbits = ((DERBitString) pubkey.getObject()).getBytes();
            checkArgument(pubbits.length == 33 || pubbits.length == 65, "Input has 'publicKey' with invalid length");
            int encoding = pubbits[0] & 0xFF;
            // Only allow compressed(2,3) and uncompressed(4), not infinity(0) or hybrid(6,7)
            checkArgument(encoding >= 2 && encoding <= 4, "Input has 'publicKey' with invalid encoding");

            // Now sanity check to ensure the pubkey bytes match the privkey.
            boolean compressed = (pubbits.length == 33);
            ECKey key = new ECKey(privkey, null, compressed);
            if (!Arrays.equals(key.getPubKey(), pubbits))
                throw new IllegalArgumentException("Public key in ASN.1 structure does not match private key.");
            return key;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen, reading from memory stream.
        }
    }

    /**
     * Decompress a compressed public key (x co-ord and low-bit of y-coord).
     */
    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }

    /**
     * Returns a 32 byte array containing the private key.
     *
     * @throws if the private key bytes are missing/encrypted.
     */
    public byte[] getPrivKeyBytes() {
        return bigIntegerToBytes(getPrivKey(), 32);
    }

    public long getCreationTimeSeconds() {
        return creationTimeSeconds;
    }

    /**
     * Sets the creation time of this key. Zero is a convention to mean "unavailable". This method can be useful when
     * you have a raw key you are importing from somewhere else.
     */
    public void setCreationTimeSeconds(long newCreationTimeSeconds) {
        if (newCreationTimeSeconds < 0)
            throw new IllegalArgumentException("Cannot set creation time to negative value: " + newCreationTimeSeconds);
        creationTimeSeconds = newCreationTimeSeconds;
    }

    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        checkArgument(b.signum() >= 0, "b must be positive or zero");
        checkArgument(numBytes > 0, "numBytes must be positive");
        byte[] src = b.toByteArray();
        byte[] dest = new byte[numBytes];
        boolean isFirstByteOnlyForSign = src[0] == 0;
        int length = isFirstByteOnlyForSign ? src.length - 1 : src.length;
        checkArgument(length <= numBytes, "The given number does not fit in " + numBytes);
        int srcPos = isFirstByteOnlyForSign ? 1 : 0;
        int destPos = numBytes - length;
        System.arraycopy(src, srcPos, dest, destPos, length);
        return dest;
    }

    public EosPrivateKey toEosPrivateKey() {
        return new EosPrivateKey(getPrivKeyBytes());
    }

    public EosPublicKey toEosPublicKey() {
        return new EosPublicKey(getPubKey());
    }
}
