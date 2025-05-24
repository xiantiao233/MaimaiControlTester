package fun.xiantiao.maimaicontrol;

import com.fazecast.jSerialComm.SerialPort;
import fun.xiantiao.maimaicontrol.command.CommandService;
import fun.xiantiao.maimaicontrol.logger.Log4j2LoggerAdapter;
import fun.xiantiao.maimaicontrol.parser.MaimaiTouchDataParser;
import fun.xiantiao.maimaicontrol.parser.MprOriginalDataParser;
import fun.xiantiao.maimaicontrol.shutdown.ShutdownManager;
import fun.xiantiao.maimaicontrol.transfer.basic.Transfer;
import fun.xiantiao.maimaicontrol.transfer.proxies.MaimaiTouchPacketSpiltProxy;
import fun.xiantiao.maimaicontrol.utils.PropertyUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MaimaiTouchController {

    // 要接收到多少个数据包才进行一次记录
    private static final int MINIMUM_LOG_PACKET_AMOUNT = PropertyUtil.getProperty("fun.xiantiao.minimumLogPacketAmount", 2048);

    private static final Logger logger = LogManager.getLogger("MaimaiControl");

    private static SerialPort serialPort;
    private static Transfer<byte[]> serialPortTransfer;
    private static CommandService commandService;

    public static void main(String[] args) {
        System.setProperty("jSerialComm.library.randomizeNativeName", "true");
        commandService = new CommandService(new Log4j2LoggerAdapter(logger));
        commandService.start();

        serialPort = initSerialPort();
        serialPort.openPort();

        logger.info("Serial port connected.");

        serialPortTransfer = new MaimaiTouchPacketSpiltProxy(new SerialPortTransfer(serialPort));
        serialPortTransfer.send("{00A0}".getBytes(StandardCharsets.UTF_8));

        serialPortTransfer.addReceiver((data) -> {
            if (data[0] == '[') {
                MprOriginalDataParser.ParsedResult result = MprOriginalDataParser.parse(data);
                List<Integer> triggeredPads0 = result.triggeredPads.get(0);
                List<Integer> triggeredPads1 = result.triggeredPads.get(1);
                List<Integer> triggeredPads2 = result.triggeredPads.get(2);
                List<Integer> triggeredPads3 = result.triggeredPads.get(3);
                if (!triggeredPads0.isEmpty()) logger.info("MPR0触发电极: {}", triggeredPads0); // 输出 [0]
                if (!triggeredPads1.isEmpty()) logger.info("MPR1触发电极: {}", triggeredPads1); // 输出 [0]
                if (!triggeredPads2.isEmpty()) logger.info("MPR2触发电极: {}", triggeredPads2); // 输出 [0]
                if (!triggeredPads3.isEmpty()) logger.info("MPR3触发电极: {}", triggeredPads3); // 输出 [0]
            } else if (data[0] == '(') {
                List<String> activeContacts = MaimaiTouchDataParser.parseMprContacts(MaimaiTouchDataParser.parseTouchBits(data));
                if (!activeContacts.isEmpty()) {
                    logger.info("Active Contacts: {}", activeContacts);
                }
            }
        });

        serialPortTransfer.addReceiver(new Consumer<>() {

            private static int packetNum = 0;

            @Override
            public void accept(byte[] data) {
                if (packetNum++ == MINIMUM_LOG_PACKET_AMOUNT){
                    packetNum = 0;
                    logger.info("{} packets received", MINIMUM_LOG_PACKET_AMOUNT);
                }
            }
        });

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

    public static Transfer<byte[]> getSerialPortTransfer() {
        return serialPortTransfer;
    }

    public static CommandService getCommandService() {
        return commandService;
    }

}
