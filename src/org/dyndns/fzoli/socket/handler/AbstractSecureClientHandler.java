package org.dyndns.fzoli.socket.handler;

import java.net.Socket;
import javax.net.ssl.SSLSocket;
import org.dyndns.fzoli.socket.process.SecureProcess;

/**
 * Biztonságos kapcsolatkezelő kliens oldalra.
 * A socket feldolgozása előtt az adatok alapján kiválasztja, melyik feldolgozót kell indítani.
 * @author zoli
 */
public abstract class AbstractSecureClientHandler extends AbstractClientHandler implements SecureHandler {

    private String localCommonName, remoteCommonName;
    
    /**
     * A biztonságos kliens oldali kapcsolatkezelő konstruktora.
     * @param socket Socket, amin keresztül folyik a kommunikáció.
     * @param deviceId eszközazonosító, ami alapján a szerver tudja, mivel kommunikál
     * @throws IllegalArgumentException ha az eszközazonosító mérete nagyobb egy bájtnál vagy negatív
     */
    public AbstractSecureClientHandler(Socket socket, int deviceId) {
        super(socket, deviceId);
    }

    /**
     * Megszerzi a helyi és távoli tanúsítvány Common Name mezőjét.
     */
    @Override
    protected void init() {
        localCommonName = SecureUtil.getLocalCommonName(getSocket());
        remoteCommonName = SecureUtil.getRemoteCommonName(getSocket());
    }

    /**
     * Kiválasztja a biztonságos kapcsolatfeldolgozó objektumot az adatok alapján és elindítja.
     * A metódus csak akkor hívható meg, amikor már ismert a kapcsolatazonosító és eszközazonosító,
     */
    @Override
    protected abstract SecureProcess selectProcess();
    
    /**
     * @return SSLSocket, amin keresztül folyik a titkosított kommunikáció.
     */
    @Override
    public SSLSocket getSocket() {
        return (SSLSocket) super.getSocket();
    }

    /**
     * A titkosított kommunikáció ezen oldalán álló kliens tanúsítványának CN mezőjét adja vissza.
     */
    @Override
    public String getLocalCommonName() {
        return localCommonName;
    }

    /**
     * A titkosított kommunikáció másik oldalán álló szerver tanúsítványának CN mezőjét adja vissza.
     */
    @Override
    public String getRemoteCommonName() {
        return remoteCommonName;
    }
    
}