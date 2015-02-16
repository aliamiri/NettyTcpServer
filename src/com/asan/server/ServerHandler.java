package com.asan.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter { // (1)

    static int i = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
        ByteBuf in = (ByteBuf) msg;

        byte[] b = new byte[2];
        b[0] = in.readByte();
        b[1] = in.readByte();

        int len = Integer.parseInt(BCDtoString(b));

        byte[] fullMessage = new byte[len];

        int i = 0;
        fullMessage[i] = b[0];
        i++;
        fullMessage[i] = b[1];
        i++;

        while (in.isReadable()) {
            fullMessage[i] = in.readByte();
            i++;
        }

        fullMessage = swapbytes(fullMessage,3,5);
        fullMessage = swapbytes(fullMessage,4,6);

        ByteBuf out =ctx.alloc().buffer(len);

        out.writeBytes(fullMessage);
        ctx.writeAndFlush(out);
    }

    private byte[] swapbytes(byte[] in, int i,int j){
        byte temp = in[i];
        in[i] = in[j];
        in[j] = temp;
        return in;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }



    public static String BCDtoString(byte bcd) {
        StringBuffer sb = new StringBuffer();
        byte high = (byte) (bcd & 0xf0);
        high >>>= (byte) 4;
        high = (byte) (high & 0x0f);
        byte low = (byte) (bcd & 0x0f);
        sb.append(high);
        sb.append(low);
        return sb.toString();
    }


    public static String BCDtoString(byte[] bcd) {

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < bcd.length; i++) {
            sb.append(BCDtoString(bcd[i]));
        }

        return sb.toString();
    }
}