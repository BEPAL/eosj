package bepal.eosio.transaction.message;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.transaction.AccountName;

import java.io.ByteArrayOutputStream;

public class BuyRamMessageData implements MessageData {

    public AccountName Payer;
    public AccountName Receiver;
    public Asset Quant;

    @Override
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Payer.AccountData);
            stream.write(Receiver.AccountData);
            Quant.toByte(stream);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        Payer = new AccountName(EByteUtil.getData(data, 8, index));
        Receiver = new AccountName(EByteUtil.getData(data, 8, index));
        Quant = Asset.toAsset(data, index);
    }
}
