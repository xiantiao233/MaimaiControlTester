package fun.xiantiao.maimaicontrol.netty;

import com.fazecast.jSerialComm.SerialPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class SerialPortChannelUtil {

    public static SerialPortChannel toChannel(SerialPort serialPort) {
        EventLoopGroup group = new DefaultEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channelFactory(new SerialPortChannelFactory(serialPort));

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
