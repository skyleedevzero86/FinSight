package com.sleekydz86.finsight.core.notification.domain.dto;

import lombok.Data;

@Data
public class MessageSendResult {

    private boolean success;
    private String messageId;
    private MessageType messageType;
    private String from;
    private String to;
    private String errorMessage;
    private String errorCode;

    public static MessageSendResult success(String messageId, MessageType messageType, String from, String to) {
        MessageSendResult result = new MessageSendResult();
        result.success = true;
        result.messageId = messageId;
        result.messageType = messageType;
        result.from = from;
        result.to = to;
        return result;
    }

    public static MessageSendResult failure(String errorMessage, String errorCode) {
        MessageSendResult result = new MessageSendResult();
        result.success = false;
        result.errorMessage = errorMessage;
        result.errorCode = errorCode;
        return result;
    }
}