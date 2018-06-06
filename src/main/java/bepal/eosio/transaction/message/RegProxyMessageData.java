package bepal.eosio.transaction.message;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.transaction.AccountName;

import java.io.ByteArrayOutputStream;

public class RegProxyMessageData implements MessageData {

    public AccountName Proxy;

    // Whether or not to register proxy
    public boolean isProxy;

    @Override
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Proxy.AccountData);
            stream.write(isProxy ? 1 : 0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        Proxy = new AccountName(EByteUtil.getData(data, 8, index));
        isProxy = EByteUtil.getByte(data, index) == 1;
    }
}
