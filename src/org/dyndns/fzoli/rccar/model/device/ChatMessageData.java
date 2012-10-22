package org.dyndns.fzoli.rccar.model.device;

import java.util.Date;

/**
 *
 * @author zoli
 */
public class ChatMessageData extends ChatMessage {
    
    private final String sender;
    private final Date date;

    public ChatMessageData(String sender, String text) {
        super(text);
        this.sender = sender;
        this.date = new Date();
    }

    public String getSender() {
        return sender;
    }

    public Date getDate() {
        return date;
    }
    
}
