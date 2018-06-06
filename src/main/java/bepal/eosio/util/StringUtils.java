/*
 * Copyright (c) 2017 Mithril coin.
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package bepal.eosio.util;

public class StringUtils {
    public static boolean isEmpty(CharSequence data) {
        return (null == data) || (data.length() <= 0);
    }

    public static byte[] fromHexString(String value) {
        value = value.toUpperCase();
        byte[] arr = new byte[value.length() / 2];
        char[] carr = value.toCharArray();
        String strTemp = "0123456789ABCDEF";
        for (int i = 0; i < carr.length; i += 2) {
            byte one = (byte) strTemp.indexOf(carr[i]);
            byte two = (byte) strTemp.indexOf(carr[i + 1]);
            arr[i / 2] = (byte) (one << 4 | two);
        }
        return arr;
    }

    public static String toHexString(String value) {
        try {
            byte[] arr = value.getBytes("UTF-8");
            return toHexString(arr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String toHexString(byte[] value) {
        StringBuilder stringBuilder = new StringBuilder();
        byte[] arr = value;
        for (byte item : arr) {
            String now = Integer.toHexString(item & 0xFF);
            if (now.length() == 1) {
                stringBuilder.append(0);
            }
            stringBuilder.append(now);
        }
        return stringBuilder.toString();
    }
}
