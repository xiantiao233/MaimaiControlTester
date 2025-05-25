package fun.xiantiao.maimaicontrol.netty;

import com.fazecast.jSerialComm.SerialPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class SerialPortChannelWrapper {

    public static SerialPortChannel wrapChannel(SerialPort serialPort) {
        return wrapChannel(serialPort, new ChannelHandler[0]);
    }

    public static SerialPortChannel wrapChannel(SerialPort serialPort, ChannelHandler... handlers) {
        EventLoopGroup group = new DefaultEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channelFactory(new SerialPortChannelFactory(serialPort)).handler(new ChannelInitializer<SerialPortChannel>() {
                    @Override
                    protected void initChannel(SerialPortChannel channel) {
                        if (handlers.length > 0) channel.pipeline().addLast(handlers);
                    }

                });

        ChannelFuture channelFuture = bootstrap.connect(new SocketAddress() {});
        try {
            channelFuture.sync();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }

        SerialPortChannel channel = (SerialPortChannel) channelFuture.channel();

        CompletableFuture.runAsync(() -> {
            try {
                channel.closeFuture().sync();
                group.shutdownGracefully();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return channel;
    }
}
