package org.dyndns.fzoli.rccar.model.bridge;

import java.util.ArrayList;
import java.util.List;
import org.dyndns.fzoli.rccar.model.controller.ChatMessage;
import org.dyndns.fzoli.rccar.model.controller.ControllerData;
import org.dyndns.fzoli.rccar.model.controller.HostState;
import org.dyndns.fzoli.rccar.model.host.HostData;

/**
 *
 * @author zoli
 */
public class HostStorage {
    
    //TODO: tárolni kell, ki az aktuális irányító és azt is, kik szeretnének irányítani
    
    private final String HOST_NAME;
    private final HostData HOST_DATA = new HostData();
    private final List<String> CONTROLLERS = new ArrayList<String>(); //TODO: String helyett saját osztály, amiben szerepel a név és az admin prioritás
    private final List<ChatMessage> CHAT_MESSAGES = new ArrayList<ChatMessage>();

    public HostStorage(String hostName) {
        HOST_NAME = hostName;
    }

    public String getHostName() {
        return HOST_NAME;
    }

    public List<String> getControllers() {
        synchronized(CONTROLLERS) {
            return new ArrayList<String>(CONTROLLERS);
        }
    }
    
    public void addController(String c) {
        if (c != null) synchronized(CONTROLLERS) {
            CONTROLLERS.add(c);
        }
    }
    
    public void removeController(String c) {
        if (c != null) synchronized(CONTROLLERS) {
            CONTROLLERS.remove(c);
        }
    }
    
    public List<ChatMessage> getChatMessages() {
        synchronized(CHAT_MESSAGES) {
            return new ArrayList<ChatMessage>(CHAT_MESSAGES);
        }
    }
    
    public void addChatMessage(ChatMessage m) {
        if (m != null) synchronized(CHAT_MESSAGES) {
            CHAT_MESSAGES.add(m);
        }
    }
    
    public ControllerData createControllerData() {
        ControllerData d = new ControllerData(getChatMessages());
        d.setHostName(HOST_NAME);
        d.setHostState(createHostState());
        d.setBatteryLevel(HOST_DATA.getBatteryLevel());
        return d;
    }
    
    private HostState createHostState() {
        return null; //TODO
    }
    
}