package org.wangpai.demo.im.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import java.util.List;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wangpai.demo.im.protocol.Message;
import org.wangpai.demo.im.protocol.Protocol;
import org.wangpai.demo.im.util.json.JsonUtil;

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

    private boolean isStarted;

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
                // 最外层编码器。为了帮助接收端解决粘包、半包问题
                ch.pipeline().addLast(new LengthFieldPrepender(Protocol.HEAD_LENGTH));
                // 将 String 数据转化为二进制数据
                ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
                // 将 Java 对象转化为 String 数据（JSON 数据）
                ch.pipeline().addLast(new MessageToMessageEncoder<Message>() {
                    @Override
                    protected void encode(ChannelHandlerContext ctx, Message message, List<Object> out)
                            throws JsonProcessingException {
                        out.add(JsonUtil.pojo2Json(message));
                    }
                });
            }
        });

        ChannelFuture future = bootstrap.connect();
        future.addListener((ChannelFuture futureListener) -> {
            if (futureListener.isSuccess()) {
                System.out.println("服务端连接成功"); // FIXME：日志
            } else {
                System.out.println("服务端连接失败"); // FIXME：日志
            }
        });
        try {
            future.sync();
        } catch (Exception exception) {
            exception.printStackTrace(); // FIXME：日志
        }
        this.channel = future.channel();
        this.isStarted = true;

        return this;
    }

    public void send(Message message) {
        channel.writeAndFlush(message);
    }

    public void send(String msg) {
        if (!isStarted) {
            this.start();
            isStarted = true;
        }

        var message = new Message();
        message.setMsg(msg);

        this.send(message);
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