package fun.xiantiao.maimaicontrol;

import com.fazecast.jSerialComm.SerialPort;
import fun.xiantiao.maimaicontrol.command.CommandService;
import fun.xiantiao.maimaicontrol.logger.Log4j2LoggerAdapter;
import fun.xiantiao.maimaicontrol.netty.SerialPortChannelWrapper;
import fun.xiantiao.maimaicontrol.parser.MaimaiTouchDataParser;
import fun.xiantiao.maimaicontrol.shutdown.ShutdownManager;
import fun.xiantiao.maimaicontrol.utils.PropertyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MaimaiTouchController {

    // 要接收到多少个数据包才进行一次记录
    private static final int MINIMUM_LOG_PACKET_AMOUNT = PropertyUtil.getProperty("fun.xiantiao.minimumLogPacketAmount", 2048);

    private static final Logger logger = LogManager.getLogger("MaimaiControl");

    private static SerialPort serialPort;
    private static Channel channel;
    private static CommandService commandService;

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("jSerialComm.library.randomizeNativeName", "true");
        commandService = new CommandService(new Log4j2LoggerAdapter(logger));
        commandService.start();

        serialPort = initSerialPort();
        serialPort.openPort();

        logger.info("Serial port connected.");

        channel = SerialPortChannelWrapper.wrapChannel(serialPort);

        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            private static int packetNum = 0;
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (packetNum++ == MINIMUM_LOG_PACKET_AMOUNT){
                    packetNum = 0;
                    logger.info("{} packets received", MINIMUM_LOG_PACKET_AMOUNT);
                }
                super.channelRead(ctx, msg);
            }
        }).addLast(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf data) {
                List<String> activeContacts = MaimaiTouchDataParser.parseMprContacts(MaimaiTouchDataParser.parseTouchBits(data));
                if (!activeContacts.isEmpty()) {
                    logger.info("Active Contacts: {}", activeContacts);
                }
            }
        });
        channel.writeAndFlush(Unpooled.copiedBuffer("{00A0}".getBytes()));

        ShutdownManager.addToShutdownList(() -> logger.info("Shutting down..."));
        ShutdownManager.addToShutdownList(serialPort::closePort);
        ShutdownManager.addToShutdownList(LogManager::shutdown);
        ShutdownManager.waitForShutdown();

    }

    private static SerialPort initSerialPort() {
        logger.info("请输入端口序号");
        SerialPort[] commPorts = SerialPort.getCommPorts();
        for (int i = 0; i < commPorts.length; i++) {
            logger.info("{}. {}", i, commPorts[i]);
        }

        CompletableFuture<SerialPort> future = new CompletableFuture<>();

        Consumer<String> originalProcessor = commandService.getCommandProcessor();
        commandService.setCommandProcessor((input) -> {

            if (input.equalsIgnoreCase("stop")) {
                System.exit(0);
            }

            try {
                int id = Integer.parseInt(input);
                if (id >= commPorts.length || id < 0){
                    throw new  IllegalArgumentException();
                }

                future.complete(commPorts[id]);
            } catch (Exception e) {
                logger.error("错误的端口序号");
            }
        });

        SerialPort serialPort = future.join();

        commandService.setCommandProcessor(originalProcessor);

        return serialPort;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static SerialPort getSerialPort() {
        return serialPort;
    }

    public static CommandService getCommandService() {
        return commandService;
    }

    public static Channel getChannel() {
        return channel;
    }

}
