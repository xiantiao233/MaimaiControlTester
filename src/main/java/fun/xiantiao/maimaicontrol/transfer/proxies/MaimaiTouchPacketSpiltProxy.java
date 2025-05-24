package fun.xiantiao.maimaicontrol.transfer.proxies;

import fun.xiantiao.maimaicontrol.transfer.basic.Transfer;
import fun.xiantiao.maimaicontrol.transfer.basic.TransferProxy;

import java.util.Arrays;

public class MaimaiTouchPacketSpiltProxy extends TransferProxy<byte[], byte[]> {

    public MaimaiTouchPacketSpiltProxy(Transfer<byte[]> transfer) {
        super(transfer);
    }

    @Override
    public byte[] decode(byte[] data) {
        return data;
    }

    @Override
    protected byte[] encode(byte[] data) {
        return data;
    }

    @Override
    protected void onReceive(byte[] data) {
        for (byte[] bytes : splitToByteArray2D(data, 9)) {
            super.onReceive(bytes);
        }

    }

    public static byte[][] splitToByteArray2D(byte[] source, int chunkSize) {
        int numOfChunks = (source.length + chunkSize - 1) / chunkSize;
        byte[][] result = new byte[numOfChunks][];

        for (int i = 0; i < numOfChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(source.length, start + chunkSize);
            result[i] = Arrays.copyOfRange(source, start, end);
        }

        return result;
    }


}


