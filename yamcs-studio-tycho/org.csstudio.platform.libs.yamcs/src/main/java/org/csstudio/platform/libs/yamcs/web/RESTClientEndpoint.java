package org.csstudio.platform.libs.yamcs.web;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.yamcs.api.ws.YamcsConnectionProperties;
import org.yamcs.protobuf.Rest.RestDumpRawMdbRequest;
import org.yamcs.protobuf.Rest.RestDumpRawMdbResponse;
import org.yamcs.protobuf.Rest.RestExceptionMessage;
import org.yamcs.protobuf.Rest.RestListAvailableParametersRequest;
import org.yamcs.protobuf.Rest.RestListAvailableParametersResponse;
import org.yamcs.protobuf.Rest.RestReplayResponse;
import org.yamcs.protobuf.Rest.RestSendCommandRequest;
import org.yamcs.protobuf.Rest.RestSendCommandResponse;
import org.yamcs.protobuf.Rest.RestValidateCommandRequest;
import org.yamcs.protobuf.Rest.RestValidateCommandResponse;
import org.yamcs.protobuf.Yamcs.ReplayRequest;

import com.google.protobuf.MessageLite;

/**
 * Implements the client-side API of the rest web api. Sequences outgoing requests on a single
 * thread for simplicity.
 *
 * Instances should be shutdown() when no longer in use.
 */
public class RESTClientEndpoint {

    private static final String BINARY_MIME_TYPE = "application/octet-stream";

    private YamcsConnectionProperties yprops;
    private EventLoopGroup group = new NioEventLoopGroup(1);

    public RESTClientEndpoint(YamcsConnectionProperties yprops) {
        this.yprops = yprops;
    }

    public void replay(ReplayRequest request, ResponseHandler responseHandler) {
        doRequest(HttpMethod.GET, "/api/archive", request, RestReplayResponse.newBuilder(), responseHandler);
    }

    public void validateCommand(RestValidateCommandRequest request, ResponseHandler responseHandler) {
        doRequest(HttpMethod.GET, "/api/commanding/validate", request, RestValidateCommandResponse.newBuilder(), responseHandler);
    }

    public void sendCommand(RestSendCommandRequest request, ResponseHandler responseHandler) {
        doRequest(HttpMethod.POST, "/api/commanding/send", request, RestSendCommandResponse.newBuilder(), responseHandler);
    }

    public void listAvailableParameters(RestListAvailableParametersRequest request, ResponseHandler responseHandler) {
        doRequest(HttpMethod.GET, "/api/mdb/parameters", request, RestListAvailableParametersResponse.newBuilder(), responseHandler);
    }

    public void dumpRawMdb(RestDumpRawMdbRequest request, ResponseHandler responseHandler) {
        doRequest(HttpMethod.GET, "/api/mdb/dump", null, RestDumpRawMdbResponse.newBuilder(), responseHandler);
    }

    private <S extends MessageLite> void doRequest(HttpMethod method, String uri, MessageLite msg, MessageLite.Builder target, ResponseHandler handler) {
        URI resource = yprops.webResourceURI(uri);
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpClientCodec());
                            p.addLast(new HttpObjectAggregator(1048576));
                            p.addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                                @Override
                                public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
                                    if (HttpResponseStatus.OK.equals(response.getStatus())) {
                                        target.mergeFrom(new ByteBufInputStream(response.content()));
                                        ctx.close();
                                        handler.onMessage(target.build());
                                    } else {
                                        InputStream in = new ByteBufInputStream(response.content());
                                        RestExceptionMessage msg = RestExceptionMessage.newBuilder().mergeFrom(in).build();
                                        ctx.close();
                                        handler.onException(msg);
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    cause.printStackTrace();
                                    ctx.close();
                                    handler.onFault(cause);
                                }
                            });
                        }
                    });

            Channel ch = b.connect(resource.getHost(), resource.getPort()).sync().channel();
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, resource.getRawPath());
            request.headers().set(HttpHeaders.Names.HOST, resource.getHost());
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
            request.headers().set(HttpHeaders.Names.ACCEPT, BINARY_MIME_TYPE);
            if (msg != null) {
                msg.writeTo(new ByteBufOutputStream(request.content()));
                request.headers().set(HttpHeaders.Names.CONTENT_TYPE, BINARY_MIME_TYPE);
                request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());
            }
            ch.writeAndFlush(request);
            ch.closeFuture().sync();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs an orderly shutdown of this service
     */
    public void shutdown() {
        group.shutdownGracefully();
    }
}
