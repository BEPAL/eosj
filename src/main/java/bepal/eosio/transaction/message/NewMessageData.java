package bepal.eosio.transaction.message;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.transaction.AccountName;
import bepal.eosio.transaction.Authority;

import java.io.ByteArrayOutputStream;

public class NewMessageData implements MessageData {

    public AccountName Creator;
    public AccountName Name;
    public Authority Owner;
    public Authority Active;

    @Override
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Creator.AccountData);
            stream.write(Name.AccountData);
            stream.write(Owner.toByte());
            stream.write(Active.toByte());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        Creator = new AccountName(EByteUtil.getData(data, 8, index));
        Name = new AccountName(EByteUtil.getData(data, 8, index));
        Owner = new Authority();
        Owner.parse(data, index);
        Active = new Authority();
        Active.parse(data, index);
    }
}
