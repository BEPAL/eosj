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
