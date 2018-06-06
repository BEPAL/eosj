package bepal.eosio.transaction;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Authority {
    public long Threshold;
    public List<KeyPermissionWeight> Keys = new ArrayList<>();
    public List<AccountPermissionWeight> Accounts = new ArrayList<>();
    public List<WaitWeight> Waits = new ArrayList<>();

    public Authority() {
        Threshold = 1;
    }

    /**
     * @brief Obtaining complete byte stream data
     */
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(IVarUtil.intToBytes(Threshold));
            stream.write(AccountName.eosIVarToByte(Keys.size()));
            for (int i = 0; i < Keys.size(); i++) {
                stream.write(Keys.get(i).toByte());
            }
            stream.write(AccountName.eosIVarToByte(Accounts.size()));
            for (int i = 0; i < Accounts.size(); i++) {
                stream.write(Accounts.get(i).toByte());
            }
            stream.write(AccountName.eosIVarToByte(Waits.size()));
            for (int i = 0; i < Waits.size(); i++) {
                stream.write(Waits.get(i).toByte());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parse(byte[] data, EByteUtil.ByteIndex index) {
        Threshold = EByteUtil.getIntByLong(data, index);
        long count = AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            KeyPermissionWeight weight = new KeyPermissionWeight();
            weight.parse(data, index);
            Keys.add(weight);
        }
        count = AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            AccountPermissionWeight weight = new AccountPermissionWeight();
            weight.parse(data, index);
            Accounts.add(weight);
        }
        count = AccountName.eosByteToIVar(data, index);
        for (int i = 0; i < count; i++) {
            WaitWeight wait = new WaitWeight();
            wait.parse(data, index);
            Waits.add(wait);
        }
    }

    public void addKey(KeyPermissionWeight key) {
        Keys.add(key);
    }

    public void addAccount(AccountPermissionWeight account) {
        Accounts.add(account);
    }
}
