package fun.xiantiao.maimaicontrol;

import com.fazecast.jSerialComm.SerialPort;
import fun.xiantiao.maimaicontrol.command.CommandService;
import fun.xiantiao.maimaicontrol.logger.Log4j2LoggerAdapter;
import fun.xiantiao.maimaicontrol.parser.MaimaiTouchDataParser;
import fun.xiantiao.maimaicontrol.parser.MprOriginalDataParser;
import fun.xiantiao.maimaicontrol.shutdown.ShutdownManager;
import fun.xiantiao.maimaicontrol.transfer.basic.Transfer;
import fun.xiantiao.maimaicontrol.transfer.proxies.MaimaiTouchPacketSpiltProxy;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class MaimaiTouchController {

    private static final int MINIMUM_PACKET_AMOUNT_LOG = 512;

    private static boolean readingOriginal = false;
    private static SerialPort comPort;
    private static Transfer<byte[]> serialPortTransfer;

    private static final Logger logger = LogManager.getLogger("MaimaiControl");

    public static void main(String[] args) {
        System.setProperty("jSerialComm.library.randomizeNativeName", "true");
        new CommandService(new Log4j2LoggerAdapter(logger)).start();

        comPort = SerialPort.getCommPorts()[1];
        comPort.openPort();

        logger.info("Serial port connected.");

        serialPortTransfer = new MaimaiTouchPacketSpiltProxy(new SerialPortTransfer(comPort));
        serialPortTransfer.send("{00A0}".getBytes(StandardCharsets.UTF_8));

        serialPortTransfer.addReceiver((data) -> {
            if (readingOriginal) {
                // logger.info("Data: {}", dump(data));
                logger.info("Data: {}", MprOriginalDataParser.parse(data));
            }
        });

        serialPortTransfer.addReceiver((data) -> {
            List<String> activeContacts = MaimaiTouchDataParser.parseMprContacts(MaimaiTouchDataParser.parseTouchBits(data));
            if (!activeContacts.isEmpty()) {
                logger.info("Active Contacts: {}", activeContacts);
            }
        });

        serialPortTransfer.addReceiver(new Consumer<>() {

            private static int packetNum = 0;

            @Override
            public void accept(byte[] data) {
                if (packetNum++ == MINIMUM_PACKET_AMOUNT_LOG){
                    packetNum = 0;
                    logger.info("Received {} packets", MINIMUM_PACKET_AMOUNT_LOG);
                }
            }
        });

        ShutdownManager.addToShutdownList(() -> logger.info("Shutting down..."));
        ShutdownManager.addToShutdownList(comPort::closePort);
        ShutdownManager.waitForShutdown();

    }

    private static String dump(byte[] data) {
        StringBuilder builder = new  StringBuilder();
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            byte b = data[i];
            if ((i == 0 || i == 9) && (b == ')' || b == '(')) {
                builder.append((char) b);
                continue;
            }

            builder.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return builder.toString();
    }

    public static SerialPort getComPort() {
        return comPort;
    }

    public static Transfer<byte[]> getSerialPortTransfer() {
        return serialPortTransfer;
    }

    public static void setReadingOriginal(boolean readingOriginal) {
        MaimaiTouchController.readingOriginal = readingOriginal;
    }

    public static boolean isReadingOriginal() {
        return readingOriginal;
    }

    public static Logger getLogger() {
        return logger;
    }
}
