package bepal.eosio.transaction;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;

import java.io.ByteArrayOutputStream;

public class KeyPermissionWeight {
    public PublicKey PubKey;//33 byte
    public int Weight;

    public KeyPermissionWeight() {
        Weight = 1;
    }

    public KeyPermissionWeight(byte[] pubKey) {
        this();
        PubKey = new PublicKey(pubKey);
    }

    /**
     * @brief Obtaining complete byte stream data
     */
    public byte[] toByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(PubKey.toByte());
            stream.write(IVarUtil.shortToBytes(Weight));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parse(byte[] data, EByteUtil.ByteIndex index) {
        PubKey = new PublicKey();
        PubKey.parse(data, index);
        Weight = EByteUtil.getShort(data, index);
    }

    public class PublicKey {
        public int Type;
        public byte[] Data;

        public PublicKey() {

        }

        public PublicKey(byte[] data) {
            Type = 0;
            Data = data;
        }

        /**
         * @brief Obtaining complete byte stream data
         */
        public byte[] toByte() {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                stream.write(IVarUtil.ivarToByte(Type));
                stream.write(Data);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return stream.toByteArray();
        }

        public void parse(byte[] data, EByteUtil.ByteIndex index) {
            Type = (int) EByteUtil.getIVar(data, index);
            Data = EByteUtil.getData(data, 33, index);
        }
    }
}
