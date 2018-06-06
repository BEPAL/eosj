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
