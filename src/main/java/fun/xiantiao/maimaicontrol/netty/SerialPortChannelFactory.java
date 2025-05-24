package fun.xiantiao.maimaicontrol.netty;

import com.fazecast.jSerialComm.SerialPort;
import io.netty.channel.ChannelFactory;

public class SerialPortChannelFactory implements ChannelFactory<SerialPortChannel> {

    private final SerialPort serialPort;

    public SerialPortChannelFactory(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    @Override
    public SerialPortChannel newChannel() {
        if (!serialPort.isOpen()) {
            throw new IllegalStateException("Serial port is not opened");
        }
        return new SerialPortChannel(serialPort);
    }
}

