package com.asan.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private ByteBuf buf;

    static int currentLen = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (buf == null) {
            buf = ctx.alloc().buffer(100000);
        }
        ByteBuf m = (ByteBuf) msg;
        buf.writeBytes(m);
        m.release();

        while (true) {
            if (currentLen == 0 && buf.readableBytes() > 1) {
                byte[] b = new byte[2];
                b[0] = buf.readByte();
                b[1] = buf.readByte();
                currentLen = Integer.parseInt(BCDtoString(b));
            }
            if (buf.readableBytes() >= currentLen && currentLen > 0) {

                int i = 0;
                byte[] fullMessage = new byte[currentLen + 2];
                byte[] len = DecToBCDArray(currentLen, 2);

                fullMessage[i] = len[0];
                i++;
                fullMessage[i] = len[1];
                i++;

                while (i < fullMessage.length) {
                    fullMessage[i] = buf.readByte();
                    i++;
                }

                fullMessage = swapbytes(fullMessage, 3, 5);
                fullMessage = swapbytes(fullMessage, 4, 6);

                if (fullMessage[11] == 1) {
                    ByteBuf out = ctx.alloc().buffer(currentLen);

                    out.writeBytes(fullMessage);
                    ctx.writeAndFlush(out);
                }
                currentLen = 0;
            }
            if ((buf.readableBytes() < currentLen) || (currentLen == 0 && buf.readableBytes() < 1) )
                break;
        }
    }

    private byte[] swapbytes(byte[] in, int i, int j) {
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

    public static byte[] DecToBCDArray(long num, int byteLen) {
        int digits = 0;

        long temp = num;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }

        if (byteLen == -1)
            byteLen = digits % 2 == 0 ? digits / 2 : (digits + 1) / 2;
        boolean isOdd = digits % 2 != 0;

        byte bcd[] = new byte[byteLen];

        for (int i = 0; i < digits; i++) {
            byte tmp = (byte) (num % 10);

            if (i == digits - 1 && isOdd)
                bcd[i / 2] = tmp;
            else if (i % 2 == 0)
                bcd[i / 2] = tmp;
            else {
                byte foo = (byte) (tmp << 4);
                bcd[i / 2] |= foo;
            }

            num /= 10;
        }

        for (int i = 0; i < byteLen / 2; i++) {
            byte tmp = bcd[i];
            bcd[i] = bcd[byteLen - i - 1];
            bcd[byteLen - i - 1] = tmp;
        }

        return bcd;
    }
}