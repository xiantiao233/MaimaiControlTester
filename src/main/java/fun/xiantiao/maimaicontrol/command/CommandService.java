package fun.xiantiao.maimaicontrol.command;

import fun.xiantiao.maimaicontrol.MaimaiTouchController;
import fun.xiantiao.maimaicontrol.logger.UniversalLogger;
import fun.xiantiao.maimaicontrol.shutdown.ShutdownManager;
import fun.xiantiao.maimaicontrol.utils.ByteBufHexDump;
import io.netty.buffer.Unpooled;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.commons.codec.DecoderException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Consumer;

public class CommandService extends SimpleTerminalConsole {

    private final Thread thread = new Thread(super::start);
    private final UniversalLogger logger;
    
    private Consumer<String> commandProcessor = this::runCommand0;

    public CommandService(UniversalLogger logger){
        this.logger = logger;
    }

    @Override
    public boolean isRunning() {
        return !ShutdownManager.isShutdown();
    }

    @Override
    public void runCommand(String command) {
        commandProcessor.accept(command);
    }

    private void runCommand0(String command) {
        if (command.equalsIgnoreCase("stop")) {
            ShutdownManager.shutdown();
            return;
        }

        if (command.equalsIgnoreCase("status")) {
            logger.info("串口状态: " + (MaimaiTouchController.getSerialPort().isOpen() ? "开启" : "关闭"));
            return;
        }

        if (command.toLowerCase(Locale.ROOT).startsWith("send string ")) {
            String dataString = command.substring(12);
            MaimaiTouchController.getChannel().writeAndFlush(Unpooled.wrappedBuffer(dataString.getBytes(StandardCharsets.UTF_8)));
            logger.warn("发送字符串: " + dataString);
            return;
        }

        if (command.toLowerCase(Locale.ROOT).startsWith("send hex ")) {
            String dataString = command.substring(9);
            try {
                MaimaiTouchController.getChannel().writeAndFlush(ByteBufHexDump.hexToByteBuf(dataString));
                logger.warn("发送数据: " + dataString);
            } catch (DecoderException e) {
                logger.warn("发送失败: " + dataString);
            }
            return;
        }

        // E - 清楚触摸缓存
        // L - 暂停扫描触摸
        // A - 启动扫描触摸
        // O - 开启或者关闭发送mpr源数据
        String data = "{00%s0}".formatted(command);
        MaimaiTouchController.getChannel().writeAndFlush(Unpooled.copiedBuffer(data.getBytes(StandardCharsets.UTF_8)));
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

    public void setCommandProcessor(Consumer<String> commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    public Consumer<String> getCommandProcessor() {
        return commandProcessor;
    }
}
