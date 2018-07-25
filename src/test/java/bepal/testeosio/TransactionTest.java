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


import bepal.eosio.crypto.ec.EosPrivateKey;
import bepal.eosio.crypto.ec.EosPublicKey;
import bepal.eosio.transaction.AccountName;
import bepal.eosio.transaction.AccountPermission;
import bepal.eosio.transaction.Action;
import bepal.eosio.transaction.Transaction;
import bepal.eosio.transaction.message.*;
import bepal.eosio.util.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TransactionTest {

    public static EosPrivateKey privateKey;
    public static EosPublicKey publicKey;
    public static byte[] ChainID;

    @BeforeClass
    public static void testStart() {
        privateKey = new EosPrivateKey("5KQD4VwQknSMtb1pWSmhQYRfyH9KDXraPbbmHtYTfL6vMnJPK1s");
        publicKey = new EosPublicKey("EOS6wVRZDXaDG5hFbebHxgvJ62V9aVVuyaRnAVTHcnSSgKb3HG135");
        // rpc service localhost:8080/v1/chain/get_info  -> chain_id
        ChainID = StringUtils.fromHexString("aca376f206b8fc25a6ed44dbdc66547c36c6c33e3a119ffbeaef943642f0e906");
    }

    /// EOSIO transaction content packaging use cases
    @Test
    public void testTx() {
        int block_num = 0;// The most recent irreversible block # last_irreversible_block_num
        int ref_block_prefix = 0; // encode last_irreversible_block_id # last_irreversible_block_id[8-16]
        // ref https://github.com/EOSIO/eos/blob/master/libraries/chain/transaction.cpp set_reference_block function

        /// generate transaction
        // [1] make block base info
        Transaction transaction = new Transaction();
        transaction.ChainID = ChainID;
        transaction.BlockNum = block_num;
        transaction.BlockPrefix = ref_block_prefix;
        transaction.NetUsageWords = 0;
        transaction.KcpuUsage = 0;
        transaction.DelaySec = 0;
        transaction.Expiration = System.currentTimeMillis() / 1000 + 60 * 60;

        String sendfrom = "bepal1";
        String sendto = "bepal2";
        String runtoken = "eosio.token";
        String tokenfun = "transfer";

        // [2] determine the type of transaction
        Action message = new Action();
        message.Account = new AccountName(runtoken);
        message.Name = new AccountName(tokenfun);
        message.Authorization.add(new AccountPermission(sendfrom, "active"));
        transaction.Actions.add(message);

        // [3] content of the action of the account party
        TxMessageData mdata = new TxMessageData();
        mdata.From = new AccountName(sendfrom);
        mdata.To = new AccountName(sendto);
        mdata.Amount = new Asset("1.0000 SYS");
        message.Data = mdata;

        // [4] sign action
        transaction.Signature.add(privateKey.sign(transaction.getSignHash()).eosEncoding(true));

        // [5] print broadcast data
        try {
            System.out.println(transaction.toJson().toString(4));
        } catch (Exception ex) {
            assertTrue(false);
        }

        // [6] check sign
        assertTrue(publicKey.verify(transaction.getSignHash().getBytes(), transaction.Signature.get(0)));
    }

    @Test
    public void testBuyRam() {
        int block_num = 0;// The most recent irreversible block # last_irreversible_block_num
        int ref_block_prefix = 0; // encode last_irreversible_block_id # last_irreversible_block_id[8-16]
        // ref https://github.com/EOSIO/eos/blob/master/libraries/chain/transaction.cpp set_reference_block function


        // [1] make block base info
        Transaction transaction = new Transaction();
        transaction.ChainID = ChainID;
        transaction.BlockNum = block_num;
        transaction.BlockPrefix = ref_block_prefix;
        transaction.NetUsageWords = 0;
        transaction.KcpuUsage = 0;
        transaction.DelaySec = 0;
        transaction.Expiration = System.currentTimeMillis() / 1000 + 60 * 60;

        String sendfrom = "bepal1";
        String sendto = "bepal1";
        String runtoken = "eosio";
        String tokenfun = "buyram";//569164

        // [2] determine the type of transaction
        Action message = new Action();
        message.Account = new AccountName(runtoken);
        message.Name = new AccountName(tokenfun);
        message.Authorization.add(new AccountPermission(sendfrom, "active"));
        transaction.Actions.add(message);

        // [3] content of the action of the account party
        BuyRamMessageData mdata = new BuyRamMessageData();
        mdata.Payer = new AccountName(sendfrom);
        mdata.Receiver = new AccountName(sendto);
        mdata.Quant = new Asset("1.0000 SYS");
        message.Data = mdata;

        // [4] sign action
        transaction.Signature.add(privateKey.sign(transaction.getSignHash()).eosEncoding(true));

        // [5] print broadcast data
        try {
            System.out.println(transaction.toJson().toString(4));
        } catch (Exception ex) {
            assertTrue(false);
        }

        // [6] check sign
        assertTrue(publicKey.verify(transaction.getSignHash().getBytes(), transaction.Signature.get(0)));
    }

    @Test
    public void testSellRam() {
        int block_num = 0;// The most recent irreversible block # last_irreversible_block_num
        int ref_block_prefix = 0; // encode last_irreversible_block_id # last_irreversible_block_id[8-16]
        // ref https://github.com/EOSIO/eos/blob/master/libraries/chain/transaction.cpp set_reference_block function


        // [1] make block base info
        Transaction transaction = new Transaction();
        transaction.ChainID = ChainID;
        transaction.BlockNum = block_num;
        transaction.BlockPrefix = ref_block_prefix;
        transaction.NetUsageWords = 0;
        transaction.KcpuUsage = 0;
        transaction.DelaySec = 0;
        transaction.Expiration = System.currentTimeMillis() / 1000 + 60 * 60;

        String sendfrom = "bepal1";
        String runtoken = "eosio";
        String tokenfun = "sellram";//570351

        // [2] determine the type of transaction
        Action message = new Action();
        message.Account = new AccountName(runtoken);
        message.Name = new AccountName(tokenfun);
        message.Authorization.add(new AccountPermission(sendfrom, "active"));
        transaction.Actions.add(message);

        // [3] content of the action of the account party
        SellRamMessageData mdata = new SellRamMessageData();
        mdata.Account = new AccountName(sendfrom);
        mdata.Bytes = 1024;
        message.Data = mdata;

        // [4] sign action
        transaction.Signature.add(privateKey.sign(transaction.getSignHash()).eosEncoding(true));

        // [5] print broadcast data
        try {
            System.out.println(transaction.toJson().toString(4));
        } catch (Exception ex) {
            assertTrue(false);
        }

        // [6] check sign
        assertTrue(publicKey.verify(transaction.getSignHash().getBytes(), transaction.Signature.get(0)));
    }

    @Test
    public void testDelegatebw() {
        int block_num = 0;// The most recent irreversible block # last_irreversible_block_num
        int ref_block_prefix = 0; // encode last_irreversible_block_id # last_irreversible_block_id[8-16]
        // ref https://github.com/EOSIO/eos/blob/master/libraries/chain/transaction.cpp set_reference_block function


        // [1] make block base info
        Transaction transaction = new Transaction();
        transaction.ChainID = ChainID;
        transaction.BlockNum = block_num;
        transaction.BlockPrefix = ref_block_prefix;
        transaction.NetUsageWords = 0;
        transaction.KcpuUsage = 0;
        transaction.DelaySec = 0;
        transaction.Expiration = System.currentTimeMillis() / 1000 + 60 * 60;

        String sendfrom = "bepal1";
        String runtoken = "eosio";
        String tokenfun = "delegatebw";//570916

        // [2] determine the type of transaction
        Action message = new Action();
        message.Account = new AccountName(runtoken);
        message.Name = new AccountName(tokenfun);
        message.Authorization.add(new AccountPermission(sendfrom, "active"));
        transaction.Actions.add(message);

        // [3] content of the action of the account party
        DelegatebwMessageData mdata = new DelegatebwMessageData();
        mdata.From = new AccountName(sendfrom);
        mdata.Receiver = new AccountName(sendfrom);
        mdata.StakeNetQuantity = new Asset("1.0000 SYS");
        mdata.StakeCpuQuantity = new Asset("1.0000 SYS");
        // @notes: 0: the authorizer cannot undelegatebw.
        //         1: the authorizer can undelegatebw.
        //         It is suggested to fill in 1
        mdata.Transfer = 0;
        message.Data = mdata;

        // [4] sign action
        transaction.Signature.add(privateKey.sign(transaction.getSignHash()).eosEncoding(true));

        // [5] print broadcast data
        try {
            System.out.println(transaction.toJson().toString(4));
        } catch (Exception ex) {
            assertTrue(false);
        }

        // [6] check sign
        assertTrue(publicKey.verify(transaction.getSignHash().getBytes(), transaction.Signature.get(0)));
    }

    @Test
    public void testUnDelegatebw() {
        int block_num = 0;// The most recent irreversible block # last_irreversible_block_num
        int ref_block_prefix = 0; // encode last_irreversible_block_id # last_irreversible_block_id[8-16]
        // ref https://github.com/EOSIO/eos/blob/master/libraries/chain/transaction.cpp set_reference_block function


        // [1] make block base info
        Transaction transaction = new Transaction();
        transaction.ChainID = ChainID;
        transaction.BlockNum = block_num;
        transaction.BlockPrefix = ref_block_prefix;
        transaction.NetUsageWords = 0;
        transaction.KcpuUsage = 0;
        transaction.DelaySec = 0;
        transaction.Expiration = System.currentTimeMillis() / 1000 + 60 * 60;

        String sendfrom = "bepal1";
        String runtoken = "eosio";
        String tokenfun = "undelegatebw";//571165

        // [2] determine the type of transaction
        Action message = new Action();
        message.Account = new AccountName(runtoken);
        message.Name = new AccountName(tokenfun);
        message.Authorization.add(new AccountPermission(sendfrom, "active"));
        transaction.Actions.add(message);

        // [3] content of the action of the account party
        UnDelegatebwMessageData mdata = new UnDelegatebwMessageData();
        mdata.From = new AccountName(sendfrom);
        mdata.Receiver = new AccountName(sendfrom);
        mdata.StakeNetQuantity = new Asset("1.0000 SYS");
        mdata.StakeCpuQuantity = new Asset("1.0000 SYS");
        message.Data = mdata;

        // [4] sign action
        transaction.Signature.add(privateKey.sign(transaction.getSignHash()).eosEncoding(true));

        // [5] print broadcast data
        try {
            System.out.println(transaction.toJson().toString(4));
        } catch (Exception ex) {
            assertTrue(false);
        }

        // [6] check sign
        assertTrue(publicKey.verify(transaction.getSignHash().getBytes(), transaction.Signature.get(0)));
    }

    @Test
    public void testRegProxy() {
        int block_num = 0;// The most recent irreversible block # last_irreversible_block_num
        int ref_block_prefix = 0; // encode last_irreversible_block_id # last_irreversible_block_id[8-16]
        // ref https://github.com/EOSIO/eos/blob/master/libraries/chain/transaction.cpp set_reference_block function


        // [1] make block base info
        Transaction transaction = new Transaction();
        transaction.ChainID = ChainID;
        transaction.BlockNum = block_num;
        transaction.BlockPrefix = ref_block_prefix;
        transaction.NetUsageWords = 0;
        transaction.KcpuUsage = 0;
        transaction.DelaySec = 0;
        transaction.Expiration = System.currentTimeMillis() / 1000 + 60 * 60;

        String sendfrom = "bepal1";
        String runtoken = "eosio";
        String tokenfun = "regproxy";//571581

        // [2] determine the type of transaction
        Action message = new Action();
        message.Account = new AccountName(runtoken);
        message.Name = new AccountName(tokenfun);
        message.Authorization.add(new AccountPermission(sendfrom, "active"));
        transaction.Actions.add(message);

        // [3] content of the action of the account party
        RegProxyMessageData mdata = new RegProxyMessageData();
        mdata.Proxy = new AccountName(sendfrom);
        // set proxy  `isProxy = true`
        // off set proxy  `isProxy = false`
        mdata.isProxy = true;
        message.Data = mdata;

        // [4] sign action
        transaction.Signature.add(privateKey.sign(transaction.getSignHash()).eosEncoding(true));

        // [5] print broadcast data
        try {
            System.out.println(transaction.toJson().toString(4));
        } catch (Exception ex) {
            assertTrue(false);
        }

        // [6] check sign
        assertTrue(publicKey.verify(transaction.getSignHash().getBytes(), transaction.Signature.get(0)));
    }

    @Test
    public void testVote() {
        int block_num = 0;// The most recent irreversible block # last_irreversible_block_num
        int ref_block_prefix = 0; // encode last_irreversible_block_id # last_irreversible_block_id[8-16]
        // ref https://github.com/EOSIO/eos/blob/master/libraries/chain/transaction.cpp set_reference_block function


        // [1] make block base info
        Transaction transaction = new Transaction();
        transaction.ChainID = ChainID;
        transaction.BlockNum = block_num;
        transaction.BlockPrefix = ref_block_prefix;
        transaction.NetUsageWords = 0;
        transaction.KcpuUsage = 0;
        transaction.DelaySec = 0;
        transaction.Expiration = System.currentTimeMillis() / 1000 + 60 * 60;

        String sendfrom = "bepal1";
        String sendto = "bepal2";
        // If the voting is conducted on behalf of others,
        // please fill in the account name of the agent here.
        // If the voting is conducted on an individual,  proxy = ""
        String proxy = "bepal3";
        String runtoken = "eosio";
        String tokenfun = "voteproducer";//571581

        // [2] determine the type of transaction
        Action message = new Action();
        message.Account = new AccountName(runtoken);
        message.Name = new AccountName(tokenfun);
        message.Authorization.add(new AccountPermission(sendfrom, "active"));
        transaction.Actions.add(message);

        // [3] content of the action of the account party
        VoteProducerMessageData mdata = new VoteProducerMessageData();
        mdata.Voter = new AccountName(sendfrom);
        mdata.Proxy = new AccountName(proxy);
        mdata.Producers.add(new AccountName(sendto));
        message.Data = mdata;

        // [4] sign action
        transaction.Signature.add(privateKey.sign(transaction.getSignHash()).eosEncoding(true));

        // [5] print broadcast data
        try {
            System.out.println(transaction.toJson().toString(4));
        } catch (Exception ex) {
            assertTrue(false);
        }

        // [6] check sign
        assertTrue(publicKey.verify(transaction.getSignHash().getBytes(), transaction.Signature.get(0)));
    }
}
