package fun.xiantiao.maimaicontrol;

import com.fazecast.jSerialComm.SerialPort;
import fun.xiantiao.maimaicontrol.transfer.basic.Transfer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SerialPortTransfer extends Transfer<byte[]> {

    private final SerialPort serialPort;

    private static final Logger logger = LogManager.getLogger("MaimaiControl");

    public SerialPortTransfer(SerialPort serialPort) {
        this.serialPort = serialPort;
        new Thread(new OutputStreamReader()).start();
    }

    public void send(byte[] data) {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.writeBytes(data, data.length);
        }
    }

    private class OutputStreamReader implements Runnable {

        @Override
        public void run() {

            while (serialPort.isOpen()) try {
                int available;
                while ((available = serialPort.bytesAvailable()) > 0) {
                    byte[] readBuffer = new byte[available];
                    serialPort.readBytes(readBuffer, readBuffer.length);

                    onReceive(readBuffer);
                }
            } catch (Exception e) {
                logger.error(e);
            }

        }
    }
}
