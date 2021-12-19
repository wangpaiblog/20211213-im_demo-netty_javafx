package org.wangpai.demo.im.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.nio.charset.StandardCharsets;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wangpai.demo.im.view.MainFace;

/**
 * @since 2021-12-1
 */
@Accessors(chain = true)
public class Server {
    @Setter
    private int port;

    @Setter
    private MainFace mainFace;

    private EventLoopGroup bossLoopGroup = new NioEventLoopGroup(1);

    private EventLoopGroup workerLoopGroup = new NioEventLoopGroup();

    public Server start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossLoopGroup, this.workerLoopGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.localAddress(port);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object obj) {
                        ByteBuf msgBuffer = (ByteBuf) obj;
                        int length = msgBuffer.readableBytes();
                        byte[] msgBytes = new byte[length];
                        msgBuffer.getBytes(0, msgBytes);

                        mainFace.receive(new String(msgBytes, StandardCharsets.UTF_8));

                        try {
                            super.channelRead(ctx, obj);
                        } catch (Exception exception) {
                            exception.printStackTrace(); // TODO：日志
                        }
                    }
                });
            }
        });

        try {
            ChannelFuture channelFuture = bootstrap.bind().sync();
            ChannelFuture closeFuture = channelFuture.channel().closeFuture();
            closeFuture.sync();
        } catch (Exception exception) {
            exception.printStackTrace(); // FIXME：日志
        } finally {
            this.workerLoopGroup.shutdownGracefully();
            this.bossLoopGroup.shutdownGracefully();
        }

        return this;
    }

    public void destroy() {
        this.workerLoopGroup.shutdownGracefully();
        this.bossLoopGroup.shutdownGracefully();
    }

    private Server() {
        super();
    }

    public static Server getInstance() {
        return new Server();
    }
}