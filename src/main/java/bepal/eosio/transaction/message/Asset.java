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

package bepal.eosio.transaction.message;

import bepal.eosio.util.EByteUtil;
import bepal.eosio.util.IVarUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Asset {

    public long Amount;

    public int Decimal;

    public String Unit;

    public Asset() {

    }

    public Asset(String value) {
        String[] arr = value.split(" ");
        int index = arr[0].indexOf(".");
        Decimal = arr[0].length() - index - 1;
        Amount = (long) (Double.parseDouble(arr[0]) * Math.pow(10, Decimal));
        Unit = arr[1];
    }

    public Asset(long amount, int decimal, String unit) {
        Amount = amount;
        Decimal = decimal;
        Unit = unit;
    }

    public void toByte(ByteArrayOutputStream stream) throws IOException {
        stream.write(IVarUtil.longToBytes(Amount));
        stream.write(Decimal);
        stream.write(getStringToData(Unit));
    }

    public void parse(byte[] data, EByteUtil.ByteIndex index) {
        Amount = EByteUtil.getLong(data, index);
        Decimal = EByteUtil.getByte(data, index);
        Unit = new String(EByteUtil.getData(data, 7, index)).trim();
    }

    public byte[] getStringToData(String str) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] data = str.getBytes("utf-8");
            stream.write(data, 0, data.length >= 7 ? 7 : data.length);
            for (int i = data.length; i < 7; i++) {
                stream.write(0);
            }
        } catch (Exception ex) {
        }
        return stream.toByteArray();
    }

    @Override
    public String toString() {
        double value = Amount / Math.pow(10, Decimal);
        return String.format("%." + Decimal + "f %s", value, Unit);
    }

    public static Asset toAsset(byte[] data, EByteUtil.ByteIndex index) {
        Asset asset = new Asset();
        asset.parse(data,index);
        return asset;
    }
}
