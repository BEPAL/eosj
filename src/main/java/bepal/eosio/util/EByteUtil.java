package bepal.eosio.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * Created by 10254 on 2017-11-09.
 */

public class EByteUtil {

    public static void toDataByte(int data, ByteArrayOutputStream outStrean) {
        outStrean.write(data);
    }

    public static void toData(int data, ByteArrayOutputStream outStrean) {
        byte[] ldata = IVarUtil.intToBytes(data);
        outStrean.write(ldata, 0, ldata.length);
    }

    public static void toData(long data, ByteArrayOutputStream outStrean) {
        byte[] ldata = IVarUtil.longToBytes(data);
        outStrean.write(ldata, 0, ldata.length);
    }

    public static void toData(String data, ByteArrayOutputStream outStrean) {
        try {
            toIVarData(data.getBytes("utf-8"), outStrean);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void toData(BigInteger data, ByteArrayOutputStream outStrean) {
        toIVarData(data.toByteArray(), outStrean);
    }

    public static void toData(byte[] data, ByteArrayOutputStream outStrean) {
        outStrean.write(data, 0, data.length);
    }

    public static void toIVar(long data, ByteArrayOutputStream outStrean) {
        byte[] ldata = IVarUtil.ivarToByte(data);
        outStrean.write(ldata, 0, ldata.length);
    }

    public static void toIVarData(byte[] data, ByteArrayOutputStream outStrean) {
        byte[] ldata = IVarUtil.ivarToByte(data.length);
        outStrean.write(ldata, 0, ldata.length);
        outStrean.write(data, 0, data.length);
    }

    public static byte getByte(byte[] data, ByteIndex offset) {
        byte bdata = data[offset.offset];
        offset.offset += 1;
        return bdata;
    }

    public static int getShort(byte[] data, ByteIndex offset) {
        int idata = IVarUtil.bytesToShort(data, offset.offset);
        offset.offset += 2;
        return idata;
    }

    public static int getInt(byte[] data, ByteIndex offset) {
        int idata = IVarUtil.bytesToInt(data, offset.offset);
        offset.offset += 4;
        return idata;
    }

    public static long getIntByLong(byte[] data, ByteIndex offset) {
        long idata = IVarUtil.bytesToIntByLong(data, offset.offset);
        offset.offset += 4;
        return idata;
    }

    public static long getLong(byte[] data, ByteIndex offset) {
        long ldata = IVarUtil.bytesToLong(data, offset.offset);
        offset.offset += 8;
        return ldata;
    }

    public static long getIVar(byte[] data, ByteIndex offset) {
        long length = IVarUtil.byteToIvar(data, offset.offset);
        offset.offset += IVarUtil.sizeOfIvar(length);
        return length;
    }

    public static String getString(byte[] data, ByteIndex offset) {
        try {
            return new String(getIVarData(data, offset), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BigInteger getBigInteger(byte[] data, ByteIndex offset) {
        return new BigInteger(getIVarData(data, offset));
    }

    public static byte[] getData(byte[] data, int length, ByteIndex offset) {
        byte[] nowdata = new byte[length];
        System.arraycopy(data, offset.offset, nowdata, 0, nowdata.length);
        offset.offset += length;
        return nowdata;
    }

    public static byte[] getIVarData(byte[] data, ByteIndex offset) {
        long length = IVarUtil.byteToIvar(data, offset.offset);
        offset.offset += IVarUtil.sizeOfIvar(length);
        byte[] nowdata = new byte[(int) length];
        System.arraycopy(data, offset.offset, nowdata, 0, nowdata.length);
        offset.offset += length;
        return nowdata;
    }

    public static class ByteIndex {
        public int offset;

        public ByteIndex() {
            offset = 0;
        }

        @Override
        public String toString() {
            return "offset = " + offset;
        }
    }
}
