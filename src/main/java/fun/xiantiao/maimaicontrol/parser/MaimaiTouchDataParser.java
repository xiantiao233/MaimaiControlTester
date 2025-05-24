package fun.xiantiao.maimaicontrol.parser;

import fun.xiantiao.maimaicontrol.utils.ByteBufHexDump;
import io.netty.buffer.ByteBuf;
import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.List;

public class MaimaiTouchDataParser {
    // MPR传感器映射表（示例数据，需与实际硬件一致）
    private static final int[][] TOUCH_MAP = {
            // Group A
            {0, 0},
            {0, 1},
            {0, 2},
            {0, 3},
            {0, 4},
            {0, 5},
            {0, 6},
            {0, 7},
            {0, 8},
            {0, 9},
            {0, 10},
            {0, 11},

            {1, 12},
            {1, 0},
            {1, 1},
            {1, 2},
            {1, 3},
            {1, 4},
            {1, 5},
            {1, 6},
            {1, 7},
            {1, 8},
            {1, 9},
            {1, 10},
            {1, 11},
            {1, 12},

            {2, 0},
            {2, 1},
            {2, 2},
            {2, 3},
            {2, 4},
            {2, 5},
            {2, 6},
            {2, 7},
            {2, 8},
            {2, 9},
            {2, 10},
            {2, 11},
            {2, 12},

            {3, 0},
            {3, 1},
            {3, 2},
            {3, 3},
            {3, 4},
            {3, 5},
            {3, 6},
            {3, 7},
            {3, 8},
            {3, 9},
            {3, 10},
            {3, 11},
            {3, 12}
    };

    public static List<String> parseMprContacts(long touchBits) {
        List<String> contacts = new ArrayList<>();

        // 遍历所有触摸点
        for (int touchPoint = 0; touchPoint < 34; touchPoint++) {
            // 检查该触摸点是否激活（bit从高位开始对应触摸点0）
            boolean isActive = ((touchBits >> (33 - touchPoint)) & 0x1) == 1;

            if (isActive) {
                // 获取对应的MPR和端口
                int[] mapping = TOUCH_MAP[touchPoint];
                String contact = String.format("M%d-%d", mapping[0], mapping[1]);
                contacts.add(contact);
            }
        }

        return contacts;
    }

    public static long parseTouchBits(ByteBuf data) {
        if (!validatePacket(data)) {
            throw new IllegalArgumentException("Invalid data packet, data: " + ByteBufHexDump.copyAndToHex(data));
        }

        return decodeTouchBits(data);
    }



    public static boolean validatePacket(ByteBuf buf) {
        int readerIndex = buf.readerIndex();

        if (buf.readableBytes() < 9) {
            return false;
        }

        byte start = buf.getByte(readerIndex);
        byte end   = buf.getByte(readerIndex + 8);

        return start == '(' && end == ')';
    }

    public static long decodeTouchBits(ByteBuf buf) {

        buf.readByte();
        long touchBits = 0;

        for (int i = 0; i < 7; i++) {
            byte b = buf.readByte();
            touchBits = (touchBits << 5) | (b & 0x1F);
        }

        return touchBits;
    }
}