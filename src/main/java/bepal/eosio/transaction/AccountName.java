package bepal.eosio.transaction;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;

import java.io.ByteArrayOutputStream;

public class AccountName {

    static char[] charmap = ".12345abcdefghijklmnopqrstuvwxyz".toCharArray();

    public byte[] AccountData;
    public String AccountName;
    public long AccountValue;

    public AccountName(String name) {
        AccountName = name;
        AccountData = accountNameToHex(name);
        AccountValue = IVarUtil.bytesToLong(AccountData, 0);
    }

    public AccountName(byte[] name) {
        if (name.length < 8) {
            name = new byte[8];
        }
        AccountName = hexToAccountName(name);
        AccountData = name;
        AccountValue = IVarUtil.bytesToLong(AccountData, 0);
    }

    /**
     * @brief Conversion to each other
     *      Name and HEX
     * @note  Compress the name to HEX or
     *       decompress the compressed HEX to its name.
     */
    public byte[] accountNameToHex(String name) {
        int len = name.length();
        long value = 0;
        for (int i = 0; i <= 12; ++i) {
            long c = 0;
            if (i < len && i <= 12) c = charIndexOf(charmap, name.charAt(i));
            if (i < 12) {
                c &= 0x1f;
                c <<= 64 - 5 * (i + 1);
            } else {
                c &= 0x0f;
            }
            value |= c;
        }
        return IVarUtil.longToBytes(value);
    }

    public String hexToAccountName(byte[] hex) {
        long tmp = IVarUtil.bytesToLong(hex, 0);
        char[] str = new char[13];
        for (int i = 0; i <= 12; ++i) {
            char c = charmap[(int) (tmp & (i == 0 ? 0x0f : 0x1f))];
            str[12 - i] = c;
            tmp >>= (i == 0 ? 4 : 5);
        }
        int count = 0;
        for (int i = 12; i >= 0; i--) {
            if (str[i] != 46) {
                break;
            }
            count = i;
        }
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < count; i++) {
            name.append(str[i]);
        }
        return name.toString();
    }


    public int charIndexOf(char[] map, char data) {
        for (int i = 0; i < map.length; i++) {
            if (map[i] == data) {
                return i;
            }
        }
        return 0;
    }

    public static byte[] eosIVarToByte(long value) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            do {
                byte b = (byte) ((byte) value & 0x7F);
                value >>= 7;
                b |= ((value > 0 ? 1 : 0) << 7);
                stream.write(b);
            } while (value != 0);
        } catch (Exception ex) {

        }
        return stream.toByteArray();
    }

    public static long eosByteToIVar(byte[] value, EByteUtil.ByteIndex index) {
        long v = 0;
        int b = 0;
        int by = 0;
        do {
            b = value[index.offset];
            v |= ((long) b & 0x7f) << by;
            by += 7;
            index.offset++;
        } while ((b & 0x80) != 0);
        return v;
    }

    public static byte[] getData(String name) {
        return new AccountName(name).AccountData;
    }

    public static long getValue(String name) {
        return new AccountName(name).AccountValue;
    }
}
