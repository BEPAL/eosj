package bepal.eosio.transaction.message;


import bepal.eosio.util.EByteUtil;
import bepal.eosio.transaction.AccountName;

import java.io.ByteArrayOutputStream;

public class TxMessageData implements MessageData {
    public AccountName From;
    public AccountName To;
    public Asset Amount;
    public byte[] Data;

    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(From.AccountData);
            stream.write(To.AccountData);
            Amount.toByte(stream);
            if (Data != null) {
                stream.write(AccountName.eosIVarToByte(Data.length));
                stream.write(Data);
            } else {
                stream.write(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        From = new AccountName(EByteUtil.getData(data, 8, index));
        To = new AccountName(EByteUtil.getData(data, 8, index));
        Amount = Asset.toAsset(data, index);
        if (data.length > index.offset) {
            Data = EByteUtil.getData(data, (int) AccountName.eosByteToIVar(data, index), index);
        } else {
            Data = new byte[0];
        }
    }
}
