/*
* Copyright (c) 2018, Bepal
* All rights reserved.
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, Berkeley nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package bepal.testeosio;

import bepal.eosio.crypto.digest.Sha256;
import bepal.eosio.crypto.ec.EosPrivateKey;
import bepal.eosio.crypto.ec.EosPublicKey;
import bepal.eosio.hd.ChildNumber;
import bepal.eosio.hd.DeterministicKey;
import bepal.eosio.hd.MnemonicCode;
import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;

public class PrivateKeyTest {

    public static byte[] seed;
    public static String message = "test";

    @BeforeClass
    public static void testStart() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 12; ++i) {
            data.add("Bepal");
        }

        seed = MnemonicCode.toSeed(data, "");
        System.out.println("Seed : [" + Hex.toHexString(seed) + "]");

    }

    @Test
    public void testEos() {
        DeterministicKey rootKey = DeterministicKey.createMasterPrivateKey(seed);

        /// build key chain
        // 44'/194'/0'/0/0
        ImmutableList<ChildNumber> path = ImmutableList.of(new ChildNumber(44, true),
                new ChildNumber(194, true),
                new ChildNumber(0, true),
                new ChildNumber(0, false),
                new ChildNumber(0));
        // 44'/194'/0'
        ImmutableList<ChildNumber> path1 = ImmutableList.of(new ChildNumber(44, true),
                new ChildNumber(194, true),
                new ChildNumber(0, true));
        // 0/0
        ImmutableList<ChildNumber> path2 = ImmutableList.of(new ChildNumber(0, false),
                new ChildNumber(0, false));


        // this's standard bip 44
        String xpub = rootKey.get(path1).serializePubB58();
        DeterministicKey rootxpub = DeterministicKey.deserializeB58(xpub);

        // ec key
        EosPrivateKey privateKey = rootKey.get(path).toEosPrivateKey();
        EosPublicKey publicKey1 = privateKey.getPublicKey();
        EosPublicKey publicKey2 = rootxpub.get(path2).toEosPublicKey();

        // sign message
        assertArrayEquals(publicKey1.getBytes(), publicKey2.getBytes());
        Sha256 msg = Sha256.from(message.getBytes());
        assertTrue(publicKey2.verify(msg.getBytes(), privateKey.sign(msg)));

        // private key
        // 0e3c85f023ee52312d97132c8f84ea386baa3f918322d3f8003d956925a40f03
        // 5HvZEPkiAur49Tb4k9nYvagAYEmobsiFtmegzSeuWHWke2JCL9K
        System.out.println("EOS prvkey: [" + privateKey.toString() + "]");
        // address
        // EOS5eyq829Bi8Cmg99WGvVPNVWqq2Rc5kYRXxtxjTP2RAisom1GHa
        System.out.println("EOS address: [" + publicKey2.toString() + "]");
    }
}
