package fun.xiantiao.maimaicontrol.command;

import fun.xiantiao.maimaicontrol.MaimaiTouchController;
import fun.xiantiao.maimaicontrol.logger.UniversalLogger;
import fun.xiantiao.maimaicontrol.shutdown.ShutdownManager;
import net.minecrell.terminalconsole.SimpleTerminalConsole;

import java.nio.charset.StandardCharsets;

public class CommandService extends SimpleTerminalConsole {

    private final Thread thread = new Thread(super::start);

    private final UniversalLogger logger;

    public CommandService(UniversalLogger logger){
        this.logger = logger;
    }

    @Override
    public boolean isRunning() {
        return !ShutdownManager.isShutdown();
    }

    @Override
    public void runCommand(String command) {

        if (command.equalsIgnoreCase("stop")) {
            ShutdownManager.shutdown();
            return;
        }

        if (command.equalsIgnoreCase("p")) {
            boolean state = !MaimaiTouchController.isReadingOriginal();
            MaimaiTouchController.setReadingOriginal(state);
            logger.info("Read original data: " + state);
        }

        // E - 清楚触摸缓存
        // L - 暂停扫描触摸
        // A - 启动扫描触摸
        // O - 开启或者关闭发送mpr源数据
        String data = "{00%s0}".formatted(command);
        MaimaiTouchController.getSerialPortTransfer().send(data.getBytes(StandardCharsets.UTF_8));
        logger.info("Send: " + data);
    }

    @Override
    public void shutdown() {
        ShutdownManager.shutdown();
        thread.interrupt();
    }

    @Override
    public void start() {
        thread.start();
    }
}
