package org.dyndns.fzoli.rccar.test;

import java.io.File;
import javax.net.ssl.SSLSocket;
import org.dyndns.fzoli.socket.SSLSocketUtil;
import org.dyndns.fzoli.socket.handler.AbstractSecureClientHandler;
import org.dyndns.fzoli.socket.process.AbstractSecureProcess;
import org.dyndns.fzoli.socket.process.impl.ClientDisconnectProcess;

/**
 * Teszt elindító kapcsolatmegszakadás detektálására kliens oldalon.
 * @author zoli
 */
public class ClientDisconnectTest {
    
    public static void main(String[] args) throws Exception {
        for (int i = 0; i <= 1; i++) { // két kapcsolatot fog kialakítani. élesben is hasonló lesz, annyi eltéréssel, hogy az első kapcsolat kiépítését be fogja várni és aztán épül ki a többi, ha az sikerült
            SSLSocket s = SSLSocketUtil.createClientSocket("192.168.20.5", 8443, new File("test-certs/ca.crt"), new File("test-certs/controller.crt"), new File("test-certs/controller.key"), new char[]{});
            new Thread(new AbstractSecureClientHandler(s, 5, i) {  // az eszközazonosító 5, a kapcsolatazonosító ciklusonként más

                @Override
                protected AbstractSecureProcess selectProcess() { // kliens oldali teszt feldolgozó használata
                    switch (getConnectionId()) {
                        case 1:
                            return new DummyProcess(this);
                        default:
                            return new ClientDisconnectProcess(this) {

                                @Override
                                protected void onDisconnect() {
                                    System.out.println("SERVER DISCONNECT");
                                    super.onDisconnect();
                                }

                            };
                    }
                }

            }).start(); // új szálban indítás
        }
    }
    
}
