package org.dyndns.fzoli.rccar.bridge.socket;

import java.io.Serializable;
import java.util.List;
import org.dyndns.fzoli.rccar.model.PartialBaseData;
import org.dyndns.fzoli.rccar.model.bridge.ControllerStorage;
import org.dyndns.fzoli.rccar.model.bridge.HostStorage;
import org.dyndns.fzoli.rccar.model.bridge.StorageList;
import org.dyndns.fzoli.rccar.model.controller.HostList;
import org.dyndns.fzoli.rccar.model.host.HostData;
import org.dyndns.fzoli.socket.handler.SecureHandler;

/**
 *
 * @author zoli
 */
public class HostSideMessageProcess extends BridgeMessageProcess {

    private HostStorage storage;
    
    private List<ControllerStorage> controllers = StorageList.getControllerStorageList();
    
    public HostSideMessageProcess(SecureHandler handler) {
        super(handler);
    }

    @Override
    protected void onStart() {
        // MJPEG streamelés aktiválása teszt céljából:
        sendMessage(new HostData.BooleanPartialHostData(Boolean.TRUE, HostData.BooleanPartialHostData.BooleanType.STREAMING));
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendConnectionMessage(false);
    }
    
    private void sendConnectionMessage(boolean connected) {
        if (storage == null) return;
        storage.setConnected(connected);
        HostList.PartialHostList msgLs = new HostList.PartialHostList(getRemoteCommonName(), connected ? HostList.PartialHostList.ChangeType.ADD : HostList.PartialHostList.ChangeType.REMOVE);
        for (ControllerStorage cs : controllers) {
            if (cs.getHostStorage() == null || !storage.isConnected()) cs.getMessageProcess().sendMessage(msgLs);
            if (cs.getHostStorage() == storage) cs.getMessageProcess().sendMessage(connected ? cs.createControllerData() : StorageList.createHostList(cs.getName()));
        }
    }
    
    @Override
    protected void onMessage(Serializable o) {
        if (o instanceof HostData) {
            storage = StorageList.createHostStorage(this, (HostData) o);
            sendConnectionMessage(true);
        }
        else if (o instanceof PartialBaseData) {
            PartialBaseData<HostData, ?> pd = (PartialBaseData) o;
            if (storage != null) storage.getReceiver().update(pd);
        }
    }
    
}
