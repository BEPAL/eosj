package bepal.eosio.transaction;

import bepal.eosio.transaction.message.*;
import bepal.eosio.util.EByteUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Action {
    /**
     * contract account
     */
    public AccountName Account;
    /**
     * contract function name
     */
    public AccountName Name;
    public List<AccountPermission> Authorization = new ArrayList<>();//permission_level
    public MessageData Data;

    /**
     * @brief Obtaining complete byte stream data
     */
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Account.AccountData);
            stream.write(Name.AccountData);

            stream.write(AccountName.eosIVarToByte(Authorization.size()));
            for (int i = 0; i < Authorization.size(); i++) {
                stream.write(Authorization.get(i).toByte());
            }
            byte[] data = Data.toByte();
            stream.write(AccountName.eosIVarToByte(data.length));
            stream.write(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parse(byte[] data, EByteUtil.ByteIndex index) {
        Account = new AccountName(EByteUtil.getData(data, 8, index));
        Name = new AccountName(EByteUtil.getData(data, 8, index));
        long count = AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            AccountPermission permission = new AccountPermission();
            permission.parse(data, index);
            Authorization.add(permission);
        }
        if (Name.AccountName.equals("transfer")) {
            Data = new TxMessageData();
        } else if (Name.AccountName.equals("newaccount")) {
            Data = new NewMessageData();
        } else if (Name.AccountName.equals("buyram")) {
            Data = new BuyRamMessageData();
        } else if (Name.AccountName.equals("sellram")) {
            Data = new SellRamMessageData();
        } else if (Name.AccountName.equals("delegatebw")) {
            Data = new DelegatebwMessageData();
        } else if (Name.AccountName.equals("undelegatebw")) {
            Data = new UnDelegatebwMessageData();
        } else if (Name.AccountName.equals("voteproducer")) {
            Data = new VoteProducerMessageData();
        } else if (Name.AccountName.equals("regproxy")) {
            Data = new RegProxyMessageData();
        } else {
            Data = new ByteMessageData();
        }

        Data.parse(EByteUtil.getData(data, (int) AccountName.eosByteToIVar(data, index), index));
    }
}