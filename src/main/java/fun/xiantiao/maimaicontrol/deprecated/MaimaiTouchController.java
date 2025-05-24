package fun.xiantiao.maimaicontrol.deprecated;

import com.fazecast.jSerialComm.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

@Deprecated
public class MaimaiTouchController {

    private SerialPort serialPort;
    private volatile boolean running = false;
    private MaimaiTouchController.TouchDataListener listener;

    // 协议常量
    private static final byte FRAME_START = '{';
    private static final byte FRAME_END = '}';
    private static final int BAUDRATE = 9600;
    private static final int CONNECT_TIMEOUT = 3000;

    // 主程序入口
    public static void main(String[] args) {
        MaimaiTouchController controller = new MaimaiTouchController();
        controller.setTouchDataListener(activePads ->
                System.out.println("当前触摸点: " + Arrays.toString(activePads))
        );

        // 获取可用端口列表
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.err.println("[错误] 未检测到任何串口设备");
            showConnectionTips();
            return;
        }

        // 交互式端口选择
        String selectedPort = selectPortInteractively(ports);
        if (selectedPort == null) return;

        // 建立连接
        if (controller.connect(selectedPort)) {
            Runtime.getRuntime().addShutdownHook(new Thread(controller::disconnect));
            keepProgramRunning();
        }
    }

    public boolean connect(String portName) {
        try {
            // 获取端口对象
            serialPort = SerialPort.getCommPort(portName);

            // 有效性检查：检查是否在系统可用端口列表中
            if (!isPortAvailable(portName)) {
                throw new IllegalArgumentException("端口 " + portName + " 不存在");
            }

            // 检查端口是否已被占用
            if (serialPort.isOpen()) {
                throw new IllegalStateException("端口已被其他程序占用");
            }

            // 配置串口参数
            serialPort.setBaudRate(BAUDRATE);
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    100,
                    100
            );

            // 尝试打开端口
            if (!serialPort.openPort(CONNECT_TIMEOUT)) {
                throw new IOException("端口打开失败");
            }

            // 启动数据监听线程
            running = true;
            new Thread(this::dataListenLoop).start();

            // 执行握手协议
            return performHandshake();

        } catch (Exception e) {
            handleConnectionError(e, portName);
            return false;
        }
    }

    // 验证端口是否存在于系统可用列表中
    private boolean isPortAvailable(String targetPort) {
        return Arrays.stream(SerialPort.getCommPorts())
                .anyMatch(p ->
                        p.getSystemPortName().equalsIgnoreCase(targetPort)
                );
    }

    // 数据监听循环
    private void dataListenLoop() {
        byte[] buffer = new byte[1024];
        while (running) {
            int bytesRead = serialPort.readBytes(buffer, buffer.length);
            if (bytesRead > 0) {
                processData(buffer, bytesRead);
            }
        }
    }

    // 协议解析
    private void processData(byte[] data, int length) {
        // 简化的协议处理逻辑
        if (data[0] == '(' && data[8] == ')') {
            long touchBits = 0;
            for (int i = 7; i >= 1; i--) {
                touchBits = (touchBits << 5) | (data[i] & 0x1F);
            }
            notifyTouchEvent(touchBits);
        }
    }

    // 触摸事件回调
    private void notifyTouchEvent(long touchBits) {
        int[] activePads = new int[34];
        int index = 0;
        for (int i = 0; i < 34; i++) {
            if ((touchBits & (1L << i)) != 0) {
                activePads[index++] = i + 1;
            }
        }
        if (listener != null && index > 0) {
            listener.onTouchData(Arrays.copyOf(activePads, index));
        }
    }

    // 交互式端口选择
    private static String selectPortInteractively(SerialPort[] ports) {
        Scanner scanner = new Scanner(System.in);
        printPortList(ports);

        while (true) {
            System.out.print("\n请输入端口名称（或输入 q 退出）: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("q")) {
                scanner.close();
                return null;
            }

            // 自动补全Linux路径
            input = input.replaceFirst("^tty", "/dev/tty");

            // 验证端口有效性
            for (SerialPort port : ports) {
                if (port.getSystemPortName().equalsIgnoreCase(input)) {
                    if (!port.isOpen()) {
                        scanner.close();
                        return port.getSystemPortName();
                    }
                    System.err.println("错误：该端口已被其他程序占用");
                }
            }
            System.err.println("无效的端口名称，请从以下列表选择：");
            printPortList(ports);
        }
    }

    // 打印端口列表
    private static void printPortList(SerialPort[] ports) {
        System.out.println("\n可用串口列表：");
        Arrays.stream(ports).forEach(p ->
                System.out.printf("  %-15s %s%s\n",
                        p.getSystemPortName(),
                        p.getPortDescription(),
                        p.isOpen() ? " (已占用)" : "")
        );
    }

    // 保持程序运行
    private static void keepProgramRunning() {
        System.out.println("\n服务运行中，按 Ctrl+C 退出...");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 错误处理
    private static void handleConnectionError(Exception e, String port) {
        System.err.println("\n连接失败: " + e.getMessage());
        System.err.println("目标端口: " + port);
        showConnectionTips();
    }

    // 连接帮助信息
    private static void showConnectionTips() {
        System.out.println("\n故障排查指南：");
        System.out.println("1. 检查设备是否通过USB正确连接");
        System.out.println("2. 确认已安装对应驱动程序");
        System.out.println("3. 尝试更换USB接口或数据线");
        System.out.println("4. Linux/Mac用户检查串口权限：");
        System.out.println("   sudo chmod 666 /dev/tty*");
        System.out.println("5. 关闭可能占用端口的其他软件");
    }

    // 关闭连接
    public void disconnect() {
        running = false;
        if (serialPort != null) {
            serialPort.closePort();
            System.out.println("已断开端口连接");
        }
    }

    // 握手协议
    private boolean performHandshake() {
        try {
            // 发送复位命令
            sendCommand(new byte[]{FRAME_START, 0x00, 0x00, 0x45, FRAME_END});
            Thread.sleep(500);

            // 发送启动命令
            sendCommand(new byte[]{FRAME_START, 0x00, 0x00, 0x41, FRAME_END});
            return true;
        } catch (Exception e) {
            System.err.println("握手协议失败: " + e.getMessage());
            return false;
        }
    }

    // 数据发送
    public void sendCommand(byte[] data) {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.writeBytes(data, data.length);
        }
    }

    // 监听器接口
    public interface TouchDataListener {
        void onTouchData(int[] activePads);
    }

    public void setTouchDataListener(MaimaiTouchController.TouchDataListener listener) {
        this.listener = listener;
    }
}