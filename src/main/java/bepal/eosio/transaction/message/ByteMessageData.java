package bepal.eosio.transaction.message;

public class ByteMessageData implements MessageData {

    public byte[] Data;

    public ByteMessageData() {

    }

    public ByteMessageData(byte[] data) {
        Data = data;
    }

    @Override
    public byte[] toByte() {
        return Data;
    }

    @Override
    public void parse(byte[] data) {
        Data = data;
    }
}
