package org.wangpai.demo.im.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import java.util.List;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wangpai.demo.im.protocol.Message;
import org.wangpai.demo.im.protocol.Protocol;
import org.wangpai.demo.im.util.json.JsonUtil;
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
                // 最外层解码器。可解决粘包、半包问题
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0,
                        Protocol.HEAD_LENGTH, 0, Protocol.HEAD_LENGTH));
                // 将二进制数据解码成 String 数据
                ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));
                // 将 String 数据（JSON 数据）解码成 Java 对象
                ch.pipeline().addLast(new MessageToMessageDecoder<String>() {
                    @Override
                    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out)
                            throws JsonProcessingException {
                        out.add(JsonUtil.json2Pojo(msg, Message.class));
                    }
                });
                // 进行对转化后的最终的数据的处理
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object obj) {
                        mainFace.receive(((Message) obj).getMsg());
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