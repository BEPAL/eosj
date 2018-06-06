package bepal.eosio.transaction.message;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;
import bepal.eosio.transaction.AccountName;

import java.io.ByteArrayOutputStream;

public class SellRamMessageData implements MessageData {

    public AccountName Account;

    public long Bytes;

    @Override
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Account.AccountData);
            stream.write(IVarUtil.longToBytes(Bytes));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        Account = new AccountName(EByteUtil.getData(data, 8, index));
        Bytes = EByteUtil.getLong(data, index);
    }
}
