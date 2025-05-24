package fun.xiantiao.maimaicontrol;

import com.fazecast.jSerialComm.SerialPort;
import fun.xiantiao.maimaicontrol.transfer.basic.Transfer;

public class SerialPortTransfer extends Transfer<byte[]> {

    private final OutputStreamReader reader = new OutputStreamReader();
    private final Thread thread = new Thread(reader);

    private final SerialPort serialPort;

    public SerialPortTransfer(SerialPort serialPort) {
        this.serialPort = serialPort;
        thread.start();
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
                while ((available = serialPort.bytesAvailable()) != 0) {

                    byte[] readBuffer = new byte[available];
                    serialPort.readBytes(readBuffer, readBuffer.length);

                    onReceive(readBuffer);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }

    }
}
