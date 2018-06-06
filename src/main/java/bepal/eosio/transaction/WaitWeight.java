package bepal.eosio.transaction;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;

import java.io.ByteArrayOutputStream;

public class WaitWeight {
    public long WaitSec;//uint32_t
    public int Weight;//uint16_t

    /**
     * @brief Obtaining complete byte stream data
     */
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(IVarUtil.intToBytes(WaitSec));
            stream.write(IVarUtil.shortToBytes(Weight));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parse(byte[] data, EByteUtil.ByteIndex index) {
        WaitSec = EByteUtil.getInt(data, index);
        Weight = EByteUtil.getShort(data, index);
    }
}
