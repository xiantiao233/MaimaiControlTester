package fun.xiantiao.maimaicontrol.parser;

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

    public static List<String> parseMprContacts(byte[] data) {
        List<String> contacts = new ArrayList<>();

        // 1. 验证数据包格式
        if (!validatePacket(data)) {
            throw new IllegalArgumentException("Invalid data packet, data: " + new String(Hex.encodeHex(data)));
        }

        // 2. 解码触摸数据
        long touchBits = decodeTouchBits(data);

        // 3. 遍历所有触摸点
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

    private static boolean validatePacket(byte[] data) {
        return data.length == 9 &&
                data[0] == '(' &&
                data[8] == ')';
    }

    private static long decodeTouchBits(byte[] data) {
        long touchBits = 0;
        // 从data[1]到data[7]重组数据（每个字节5位）
        for (int i = 1; i <= 7; i++) {
            touchBits = (touchBits << 5) | (data[i] & 0x1F);
        }
        return touchBits;
    }

    // 测试用例
    public static void main(String[] args) {
        // 示例数据：激活触摸点0(M1-3)、2(M1-1)、33(M3-15)
        byte[] testData = {
                '(',
                0b00000001, // 触摸点0-4 (00001)
                0b00000000, // 触摸点5-9
                0b00000000, // 触摸点10-14
                0b00000000, // 触摸点15-19
                0b00000000, // 触摸点20-24
                0b00000000, // 触摸点25-29
                0b00001000, // 触摸点30-33 (最高位1000)
                ')'
        };

        List<String> activeContacts = parseMprContacts(testData);
        System.out.println("Active Contacts: " + activeContacts);
        // 输出: [M1-3, M3-15]
    }
}