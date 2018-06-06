package bepal.eosio.transaction;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;

import java.io.ByteArrayOutputStream;

public class AccountPermissionWeight {

    public AccountPermission Permission;
    public int Weight;

    public AccountPermissionWeight() {
        Weight = 1;
    }

    public AccountPermissionWeight(String account, String permission) {
        this();
        Permission = new AccountPermission(account, permission);
    }

    /**
     * @brief Obtaining complete byte stream data
     */
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Permission.toByte());
            stream.write(IVarUtil.shortToBytes(Weight));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parse(byte[] data, EByteUtil.ByteIndex index) {
        Permission = new AccountPermission();
        Permission.parse(data, index);
        Weight = EByteUtil.getShort(data, index);
    }
}
