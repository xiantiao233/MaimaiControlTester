package fun.xiantiao.maimaicontrol.parser;

import java.util.*;
import java.util.stream.IntStream;

public class MprOriginalDataParser {

    // 返回数据结构：包含4个MPR传感器的触发电极列表
    public static class ParsedResult {
        public final List<List<Integer>> triggeredPads; // [MPR0电极列表, MPR1电极列表, ...]
        public final boolean isValid;

        private ParsedResult(boolean isValid, List<List<Integer>> triggeredPads) {
            this.isValid = isValid;
            this.triggeredPads = triggeredPads;
        }
    }

    // 核心解析方法
    public static ParsedResult parse(byte[] rawData) {
        // 验证数据格式
        if (!isValidFormat(rawData)) {
            return new ParsedResult(false, Collections.emptyList());
        }

        // 提取4个MPR的触摸状态
        List<List<Integer>> result = new ArrayList<>(4);
        for (int mprId = 0; mprId < 4; mprId++) {
            int high = Byte.toUnsignedInt(rawData[1 + mprId * 2]);
            int low = Byte.toUnsignedInt(rawData[2 + mprId * 2]);
            int state = (high << 8) | low;
            result.add(getTriggeredPads(state));
        }

        return new ParsedResult(true, result);
    }

    // 数据格式验证
    private static boolean isValidFormat(byte[] data) {
        return data != null
                && data.length == 10
                && data[0] == '['
                && data[9] == ']';
    }

    // 从16位状态值获取触发电极列表
    private static List<Integer> getTriggeredPads(int state) {
        List<Integer> pads = new ArrayList<>();
        for (int bit = 0; bit < 12; bit++) { // 每个MPR最多12个电极
            if ((state & (1 << bit)) != 0) {
                pads.add(bit);
            }
        }
        return pads;
    }

    // 示例用法
    public static void main(String[] args) {
        // 示例数据：MPR0的0号电极被触发
        byte[] testData = {
                '[',
                0x00, 0x01, // MPR0: 0x0001 (二进制 00000000 00000001)
                0x00, 0x00, // MPR1: 无触发
                0x00, 0x00, // MPR2: 无触发
                0x00, 0x00, // MPR3: 无触发
                ']'
        };

        ParsedResult result = parse(testData);
        if (result.isValid) {
            System.out.println("MPR0触发电极: " + result.triggeredPads.get(0)); // 输出 [0]
        } else {
            System.out.println("数据无效");
        }
    }
}
