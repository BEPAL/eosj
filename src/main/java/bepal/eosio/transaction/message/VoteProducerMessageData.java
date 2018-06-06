package bepal.eosio.transaction.message;

import bepal.eosio.transaction.Transaction;
import bepal.eosio.util.EByteUtil;
import bepal.eosio.transaction.AccountName;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class VoteProducerMessageData implements MessageData {

    public AccountName Voter;
    public AccountName Proxy;
    public List<AccountName> Producers = new ArrayList<>();

    @Override
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Voter.AccountData);
            stream.write(Proxy.AccountData);
            stream.write(AccountName.eosIVarToByte(Producers.size()));
            Transaction.sortAccountName(Producers);
            for (int i = 0; i < Producers.size(); i++) {
                stream.write(Producers.get(i).AccountData);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public void parse(byte[] data) {
        EByteUtil.ByteIndex index = new EByteUtil.ByteIndex();
        Voter = new AccountName(EByteUtil.getData(data, 8, index));
        Proxy = new AccountName(EByteUtil.getData(data, 8, index));
        int count = (int) AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            AccountName temp = new AccountName(EByteUtil.getData(data, 8, index));
            Producers.add(temp);
        }
    }
}
