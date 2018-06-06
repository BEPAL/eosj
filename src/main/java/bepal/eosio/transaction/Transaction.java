package bepal.eosio.transaction;

import bepal.eosio.crypto.ec.EosEcUtil;
import bepal.eosio.transaction.message.NewMessageData;
import bepal.eosio.transaction.message.TxMessageData;
import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;
import bepal.eosio.util.StringUtils;
import bepal.eosio.crypto.digest.Ripemd160;
import bepal.eosio.crypto.digest.Sha256;
import bepal.eosio.crypto.util.Base58;
import bepal.eosio.crypto.util.HexUtils;
import org.json.me.JSONArray;
import org.json.me.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class Transaction implements BaseTransaction {

    public byte[] ChainID = StringUtils.fromHexString("706a7ddd808de9fc2b8879904f3b392256c83104c1d544b38302cc07d9fca477");
    public long Expiration;
    public int BlockNum;//uint16
    public long BlockPrefix;//uint32
    public int NetUsageWords;//unsigned_int
    public int KcpuUsage;//uint8
    public int DelaySec;//unsigned_int

    public List<Action> ContextFreeActions = new ArrayList<>();
    public List<Action> Actions = new ArrayList<>();
    public TreeMap<Integer, String> ExtensionsType = new TreeMap<>(new DataComparator());//uint16_t,vector<char>
    public List<byte[]> Signature = new ArrayList<>();

    public class DataComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer s, Integer t1) {
            return s.compareTo(t1);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    public static void sortAccountName(List<AccountName> accountNames) {
        for (int i = accountNames.size() - 1; i > 0; --i) {
            for (int j = 0; j < i; ++j) {
                if (getSign(accountNames.get(j + 1).AccountValue, accountNames.get(j).AccountValue)) {
                    AccountName temp = accountNames.get(j);
                    accountNames.set(j, accountNames.get(j + 1));
                    accountNames.set(j + 1, temp);
                }
            }
        }
    }

    public static boolean getSign(long x, long y) {
        if (x > 0 && y > 0) {
            return x < y;
        }
        if (x < 0 && y < 0) {
            return x < y;
        }
        return x > y;
    }

    /**
     * @brief Obtaining complete byte stream data
     */
    @Override
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(IVarUtil.intToBytes(Expiration));
            stream.write(IVarUtil.shortToBytes(BlockNum));
            stream.write(IVarUtil.intToBytes(BlockPrefix));
            stream.write(AccountName.eosIVarToByte(NetUsageWords));
            stream.write(KcpuUsage);
            stream.write(AccountName.eosIVarToByte(DelaySec));

            stream.write(AccountName.eosIVarToByte(ContextFreeActions.size()));
            for (int i = 0; i < ContextFreeActions.size(); i++) {
                stream.write(ContextFreeActions.get(i).toByte());
            }
            stream.write(AccountName.eosIVarToByte(Actions.size()));
            for (int i = 0; i < Actions.size(); i++) {
                stream.write(Actions.get(i).toByte());
            }
            stream.write(AccountName.eosIVarToByte(ExtensionsType.size()));
            for (Integer key : ExtensionsType.keySet()) {
                int name = key;
                String value = ExtensionsType.get(key);
                stream.write(IVarUtil.shortToBytes(name));
                EByteUtil.toData(value, stream);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        Expiration = EByteUtil.getIntByLong(data, index);
        BlockNum = EByteUtil.getShort(data, index);
        BlockPrefix = EByteUtil.getIntByLong(data, index);
        NetUsageWords = (int) EByteUtil.getIVar(data, index);
        KcpuUsage = EByteUtil.getByte(data, index);
        DelaySec = (int) EByteUtil.getIVar(data, index);

        long count = AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            Action action = new Action();
            action.parse(data, index);
            ContextFreeActions.add(action);
        }
        count = AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            Action action = new Action();
            action.parse(data, index);
            Actions.add(action);
        }
        count = AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            int name = EByteUtil.getShort(data, index);
            String value = EByteUtil.getString(data, index);
            ExtensionsType.put(name, value);
        }
    }

    @Override
    public byte[] toSignData() {
        return toSignData(new byte[0]);
    }

    public byte[] toSignData(byte[] cfd) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            // pack chain_id
            stream.write(ChainID);
            // transaction
            stream.write(toByte());
            // sign data
            if (cfd.length > 0) {
                stream.write(Sha256.from(cfd).getBytes());
            } else {
                stream.write(StringUtils.fromHexString("0000000000000000000000000000000000000000000000000000000000000000"));
            }
        } catch (Exception ex) {
        }
        return stream.toByteArray();
    }


    /**
     * @brief sha256(self)
     * @note  get the whole deal, use SHA256 to get a digest
     *         such a hash is commonly used for signatures
     */
    public Sha256 getSignHash() {
        return Sha256.from(toSignData());
    }

    @Override
    public byte[] getCreatePubKey() {
        NewMessageData messageData = (NewMessageData) Actions.get(0).Data;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        EByteUtil.toData(messageData.Name.AccountData, stream);
        EByteUtil.toIVarData(messageData.Active.Keys.get(0).PubKey.Data, stream);
        return stream.toByteArray();
    }

    public JSONObject toJson() {
        JSONArray signature = new JSONArray();
        if (Signature.size() != 0) {
            for (int i = 0; i < Signature.size(); i++) {
                signature.put(toEOSSignature(Signature.get(i)));
            }
        }
        try {
            JSONObject endjson = new JSONObject();
            endjson.put("compression", "none");
            endjson.put("signatures", signature);
            endjson.put("packed_trx", HexUtils.toHex(toByte()));
            return endjson;
        } catch (Exception ex) {
        }
        return null;
    }

    public static String toEOSSignature(byte[] data) {
        try {
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            temp.write(data, 0, data.length);
            temp.write("K1".getBytes("utf-8"));

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(data, 0, data.length);
            stream.write(Ripemd160.from(temp.toByteArray()).bytes(), 0, 4);
            return EosEcUtil.EOS_Sign + Base58.encode(stream.toByteArray());
        } catch (Exception ex) {
        }
        return "";
    }

    @Override
    public String getActionType() {
        return Actions.get(0).Name.AccountName;
    }

    @Override
    public byte[] getTokenAccount() {
        return Actions.get(0).Account.AccountData;
    }

    @Override
    public byte[] getSendTo() {
        TxMessageData messageData = (TxMessageData) Actions.get(0).Data;
        return messageData.To.AccountData;
    }

    @Override
    public long getSendAmount() {
        TxMessageData messageData = (TxMessageData) Actions.get(0).Data;
        return messageData.Amount.Amount;
    }

    @Override
    public byte[] getMessageData() {
        return Actions.get(0).Data.toByte();
    }

    public byte[] getTxID() {
        return Sha256.from(toByte()).getBytes();
    }
}