/*
* Copyright (c) 2018, Bepal
* All rights reserved.
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, Berkeley nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

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
