package fun.xiantiao.maimaicontrol.transfer.proxies;

import fun.xiantiao.maimaicontrol.transfer.basic.Transfer;
import fun.xiantiao.maimaicontrol.transfer.basic.TransferProxy;
import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 由于Maimai的触摸信息数据包可能一次性发送多个
 * 此类用于对触摸信息数据包进行切片
 */
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
        for (byte[] bytes : splitPacket(data)) {
            super.onReceive(bytes);
        }

    }

    public static List<byte[]> splitPacket(byte[] source) {
        List<byte[]> packets = new ArrayList<>();

        if (source.length < 9) {
            throw new IllegalArgumentException("Impossible packet length, data: " + new String(Hex.encodeHex(source)));
        }

        for (int i = 0, j; i < source.length; i += j) {
            if (source[i] == '[' && source[i + 9] == ']') {
                j = 10;
            } else if (source[i] == '(' && source[i + 8] == ')') {
                j = 9;
            } else {
                throw new IllegalArgumentException("Invalid data packet, data: " + new String(Hex.encodeHex(source)));
            }
            packets.add(Arrays.copyOfRange(source, i, i + j));

        }

        return packets;
    }

}


