package bepal.eosio.transaction.message;

import java.util.List;

public interface MessageData {
    byte[] toByte();

    void parse(byte[] data);
}
