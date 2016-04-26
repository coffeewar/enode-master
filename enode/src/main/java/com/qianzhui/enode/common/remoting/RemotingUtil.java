package com.qianzhui.enode.common.remoting;

import com.qianzhui.enode.common.utilities.BitConverter;

/**
 * Created by junbo_xu on 2016/3/10.
 */
public class RemotingUtil {
    public static byte[] buildRequestMessage(RemotingRequest request) {
        byte[] sequenceBytes = BitConverter.getBytes(request.getSequence());
        byte[] codeBytes = BitConverter.getBytes(request.getCode());
        byte[] typeBytes = BitConverter.getBytes(request.getType());
        byte[] message = new byte[12 + request.getBody().length];

        System.arraycopy(sequenceBytes, 0, message, 0, 8);
        System.arraycopy(codeBytes, 0, message, 8, 2);
        System.arraycopy(typeBytes, 0, message, 10, 2);
        System.arraycopy(request.getBody(), 0, message, 12, request.getBody().length);

        return message;
    }

    public static RemotingRequest parseRequest(byte[] messageBuffer) {
        byte[] sequenceBytes = new byte[8];
        byte[] codeBytes = new byte[2];
        byte[] typeBytes = new byte[2];
        byte[] body = new byte[messageBuffer.length - 12];

        System.arraycopy(messageBuffer, 0, sequenceBytes, 0, 8);
        System.arraycopy(messageBuffer, 8, codeBytes, 0, 2);
        System.arraycopy(messageBuffer, 10, typeBytes, 0, 2);
        System.arraycopy(messageBuffer, 12, body, 0, body.length);

        long sequence = BitConverter.toLong(sequenceBytes);
        short code = BitConverter.toShort(codeBytes);
        short type = BitConverter.toShort(typeBytes);

        return new RemotingRequest(code, body, type, sequence);
    }

    public static byte[] buildResponseMessage(RemotingResponse response) {
        byte[] sequenceBytes = BitConverter.getBytes(response.getSequence());
        byte[] requestCodeBytes = BitConverter.getBytes(response.getRequestCode());
        byte[] responseCodeBytes = BitConverter.getBytes(response.getCode());
        byte[] requestTypeBytes = BitConverter.getBytes(response.getType());
        byte[] message = new byte[14 + response.getBody().length];

        System.arraycopy(sequenceBytes, 0, message, 0, 8);
        System.arraycopy(requestCodeBytes, 0, message, 8, 2);
        System.arraycopy(responseCodeBytes, 0, message, 10, 2);
        System.arraycopy(requestTypeBytes, 0, message, 12, 2);
        System.arraycopy(response.getBody(), 0, message, 14, response.getBody().length);

        return message;
    }

    public static RemotingResponse parseResponse(byte[] messageBuffer) {
        byte[] requestSequenceBytes = new byte[8];
        byte[] requestCodeBytes = new byte[2];
        byte[] responseCodeBytes = new byte[2];
        byte[] requestTypeBytes = new byte[2];
        byte[] responseBody = new byte[messageBuffer.length - 14];

        System.arraycopy(messageBuffer, 0, requestSequenceBytes, 0, 8);
        System.arraycopy(messageBuffer, 8, requestCodeBytes, 0, 2);
        System.arraycopy(messageBuffer, 10, responseCodeBytes, 0, 2);
        System.arraycopy(messageBuffer, 12, requestTypeBytes, 0, 2);
        System.arraycopy(messageBuffer, 14, responseBody, 0, responseBody.length);

        long requestSequence = BitConverter.toLong(requestSequenceBytes);
        short requestCode = BitConverter.toShort(requestCodeBytes);
        short responseCode = BitConverter.toShort(responseCodeBytes);
        short requestType = BitConverter.toShort(requestTypeBytes);

        return new RemotingResponse(requestCode, responseCode, requestType, responseBody, requestSequence);
    }
}
