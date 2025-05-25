package fun.xiantiao.maimaicontrol.netty;

import com.fazecast.jSerialComm.SerialPort;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;

import java.net.SocketAddress;
import java.util.Optional;

public class SerialPortChannel extends AbstractChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);

    private final ChannelConfig config = new DefaultChannelConfig(this);
    private final SerialPortInputReader inputReader = new SerialPortInputReader();

    private final SerialPort serialPort;

    SerialPortChannel(SerialPort serialPort) {
        super(null);
        this.serialPort = serialPort;
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return serialPort.isOpen();
    }

    @Override
    public boolean isActive() {
        return serialPort.isOpen() && inputReader.isAlive();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new SerialPortUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    protected SocketAddress localAddress0() {
        throw new UnsupportedOperationException("localAddress not supported");
    }

    @Override
    protected SocketAddress remoteAddress0() {
        throw new UnsupportedOperationException("remoteAddress not supported");
    }

    @Override
    protected void doBind(SocketAddress socketAddress) {
        throw new UnsupportedOperationException("bind not supported");
    }

    @Override
    protected void doDisconnect() {
        close0();
    }

    @Override
    protected void doClose() {
        close0();
    }

    private void close0() {
        if (serialPort.isOpen()) {
            serialPort.closePort();
        }
        pipeline().fireChannelInactive();
    }

    @Override
    protected void doBeginRead() {
        inputReader.startIfNeeded();
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) {
        Object msg;
        while ((msg = in.current()) != null) {
            // NOTE: `in::remove` is overloaded: remove() and remove(Throwable)
            write0(msg).ifPresentOrElse(in::remove, in::remove);
        }
    }

    private Optional<Exception> write0(Object msg) {
        if (!(msg instanceof ByteBuf buf)) {
            String type = (msg != null) ? msg.getClass().getName() : "null";
            return Optional.of(new UnsupportedOperationException("Unsupported message type: " + type));
        }

        int length = buf.readableBytes();
        if (length <= 0) {
            return Optional.of(new UnsupportedOperationException("Zero-length ByteBuf message"));
        }

        try {
            byte[] data = new byte[length];
            buf.readBytes(data);
            serialPort.writeBytes(data, data.length);
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.of(ex);
        }

    }

    private class SerialPortUnsafe extends AbstractUnsafe {
        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            if (!isOpen()) {
                promise.setFailure(new IllegalStateException("Serial port is not opened"));
            }

            promise.setSuccess();
        }
    }

    private class SerialPortInputReader extends Thread {
        private boolean started = false;

        void startIfNeeded() {
            if (!started) {
                started = true;
                setName("SerialPortInputReader");
                setDaemon(true);
                start();
            }
        }

        @Override
        public void run() {

            try {
                while (isOpen()) doRead();
            } catch (Exception e) {
                pipeline().fireExceptionCaught(e);
            } finally {
                unsafe().close(unsafe().voidPromise());
            }
        }

        private void doRead() {
            int available;
            while ((available = serialPort.bytesAvailable()) > 0) {

                byte[] buffer = new byte[available];
                int bytesRead = serialPort.readBytes(buffer, buffer.length);

                ByteBuf byteBuf = alloc().buffer(bytesRead);
                try {
                    byteBuf.writeBytes(buffer, 0, bytesRead);
                    pipeline().fireChannelRead(byteBuf);
                    pipeline().fireChannelReadComplete();
                } catch (Throwable t) {
                    ReferenceCountUtil.release(byteBuf);
                    pipeline().fireExceptionCaught(t);
                    break;
                }

            }
        }
    }

    @Override
    public String toString() {
        return "SerialPortChannel[" + serialPort.getSystemPortName() + "]";
    }

}
