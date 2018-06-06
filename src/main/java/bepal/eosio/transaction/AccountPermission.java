package bepal.eosio.transaction;

import bepal.eosio.util.EByteUtil;

import java.io.ByteArrayOutputStream;

public class AccountPermission {
    public AccountName Account;
    public AccountName Permission;

    public AccountPermission() {

    }
    /**
     * @brief Initialization method
     */
    public AccountPermission(String account, String permission) {
        Account = new AccountName(account);
        Permission = new AccountName(permission);
    }

    /**
     * @brief Obtaining complete byte stream data
     */
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Account.AccountData);
            stream.write(Permission.AccountData);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parse(byte[] data, EByteUtil.ByteIndex index) {
        Account = new AccountName(EByteUtil.getData(data, 8, index));
        Permission = new AccountName(EByteUtil.getData(data, 8, index));
    }
}
