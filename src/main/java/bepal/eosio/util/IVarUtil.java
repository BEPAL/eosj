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


public class IVarUtil {

    public static byte[] ivarToByte(long value) {
        byte[] bytes;
        switch (sizeOfIvar(value)) {
            case 1:
                return new byte[]{(byte) value};
            case 3:
                return new byte[]{(byte) 253, (byte) (value), (byte) (value >> 8)};
            case 5:
                bytes = new byte[5];
                byte[] ibyte = intToBytes(value);
                bytes[0] = (byte) 254;
                bytes[1] = ibyte[0];
                bytes[2] = ibyte[1];
                bytes[3] = ibyte[2];
                bytes[4] = ibyte[3];
                return bytes;
            default:
                bytes = new byte[9];
                byte[] lbyte = longToBytes(value);
                bytes[0] = (byte) 255;
                bytes[1] = lbyte[0];
                bytes[2] = lbyte[1];
                bytes[3] = lbyte[2];
                bytes[4] = lbyte[3];
                bytes[5] = lbyte[4];
                bytes[6] = lbyte[5];
                bytes[7] = lbyte[6];
                bytes[8] = lbyte[7];
                return bytes;
        }
    }

    public static long byteToIvar(byte[] data, int offset) {
        long value = 0;
        int first = 0xFF & data[offset];
        if (first < 253) {
            value = first;
        } else if (first == 253) {
            value = (0xFF & data[offset + 1]) | ((0xFF & data[offset + 2]) << 8);
        } else if (first == 254) {
            value = readUint32(data, offset + 1);
        } else {
            value = readInt64(data, offset + 1);
        }
        return value;
    }

    public static int sizeOfIvar(long value) {
        if (value < 0) return 9;
        if (value < 253) return 1;
        if (value <= 0xFFFFL) return 3;
        if (value <= 0xFFFFFFFFL) return 5;
        return 9;
    }

    public static byte[] shortToBytes(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static int bytesToShort(byte[] src, int offset) {
        int value = (src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8);
        return value;
    }

    public static byte[] intToBytes(long value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static int bytesToInt(byte[] data, int offset) {
        int value = (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
        return value;
    }

    public static long bytesToIntByLong(byte[] src, int offset) {
        long value = ((long) src[offset] & 0xFF)
                | ((long) (src[offset + 1] & 0xFF) << 8)
                | ((long) (src[offset + 2] & 0xFF) << 16)
                | ((long) (src[offset + 3] & 0xFF) << 24);
        return value;
    }

    public static byte[] longToBytes(long value) {
        byte[] src = new byte[8];
        src[7] = (byte) ((value >> 56) & 0xFF);
        src[6] = (byte) ((value >> 48) & 0xFF);
        src[5] = (byte) ((value >> 40) & 0xFF);
        src[4] = (byte) ((value >> 32) & 0xFF);
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static long bytesToLong(byte[] data, int offset) {
        long value = ((long) data[offset] & 0xFF)
                | ((long) (data[offset + 1] & 0xFF) << 8)
                | ((long) (data[offset + 2] & 0xFF) << 16)
                | ((long) (data[offset + 3] & 0xFF) << 24)
                | ((long) (data[offset + 4] & 0xFF) << 32)
                | ((long) (data[offset + 5] & 0xFF) << 40)
                | ((long) (data[offset + 6] & 0xFF) << 48)
                | ((long) (data[offset + 7] & 0xFF) << 56);
        return value;
    }

    public static long readUint32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
                ((bytes[offset + 1] & 0xffl) << 8) |
                ((bytes[offset + 2] & 0xffl) << 16) |
                ((bytes[offset + 3] & 0xffl) << 24);
    }

    public static long readInt64(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
                ((bytes[offset + 1] & 0xffl) << 8) |
                ((bytes[offset + 2] & 0xffl) << 16) |
                ((bytes[offset + 3] & 0xffl) << 24) |
                ((bytes[offset + 4] & 0xffl) << 32) |
                ((bytes[offset + 5] & 0xffl) << 40) |
                ((bytes[offset + 6] & 0xffl) << 48) |
                ((bytes[offset + 7] & 0xffl) << 56);
    }
}
