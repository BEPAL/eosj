package bepal.eosio.transaction;

public interface BaseTransaction {
    byte[] toByte();

    void parse(byte[] data);

    String getActionType();

    byte[] getTokenAccount();

    byte[] toSignData();

    byte[] getCreatePubKey();

    byte[] getSendTo();

    long getSendAmount();

    byte[] getMessageData();
}
