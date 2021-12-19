package org.wangpai.demo.im.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.nio.charset.StandardCharsets;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @since 2021-12-1
 */
@Accessors(chain = true)
public class Client {
    @Setter
    private String ip;

    @Setter
    private int port;

    private Channel channel;

    private EventLoopGroup workerLoopGroup = new NioEventLoopGroup();

    public Client start() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        // 设置接收端的 IP 和端口号，但实际上，自己作为发送端也会为自己自动生成一个端口号
        bootstrap.remoteAddress(ip, port);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        // TODO：如果需要服务器反馈信息，可在此添加业务

                        try {
                            super.channelRead(ctx, msg);
                        } catch (Exception exception) {
                            exception.printStackTrace(); // TODO：日志
                        }
                    }
                });
            }
        });

        ChannelFuture future = bootstrap.connect();
        future.addListener((ChannelFuture futureListener) -> {
            if (futureListener.isSuccess()) {
                System.out.println("客户端连接成功"); // FIXME：日志
            } else {
                System.out.println("客户端连接失败"); // FIXME：日志
            }
        });
        try {
            future.sync();
        } catch (Exception exception) {
            exception.printStackTrace(); // FIXME：日志
        }
        this.channel = future.channel();

        return this;
    }

    public void send(String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        var buffer = channel.alloc().buffer();
        buffer.writeBytes(bytes);
        channel.writeAndFlush(buffer);
    }

    public void destroy() {
        this.workerLoopGroup.shutdownGracefully();
    }

    private Client() {
        super();
    }

    public static Client getInstance() {
        return new Client();
    }
}