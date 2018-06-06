package bepal.eosio.transaction.message;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.transaction.AccountName;

import java.io.ByteArrayOutputStream;

public class DelegatebwMessageData implements MessageData {

    public AccountName From;
    public AccountName Receiver;
    public Asset StakeNetQuantity;
    public Asset StakeCpuQuantity;
    public long Transfer;

    @Override
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(From.AccountData);
            stream.write(Receiver.AccountData);
            StakeNetQuantity.toByte(stream);
            StakeCpuQuantity.toByte(stream);
            stream.write(AccountName.eosIVarToByte(Transfer));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        From = new AccountName(EByteUtil.getData(data, 8, index));
        Receiver = new AccountName(EByteUtil.getData(data, 8, index));
        StakeNetQuantity = Asset.toAsset(data, index);
        StakeCpuQuantity = Asset.toAsset(data, index);
        Transfer = AccountName.eosByteToIVar(data, index);
    }
}
