package org.dyndns.fzoli.rccar.bridge;

import org.dyndns.fzoli.rccar.bridge.config.PermissionConfig;

/**
* A híd jogosultságok adatbázisa.
* A jogosultság konfiguráció naprakészen tartása a feladata.
* Az osztály betöltésekor inicializálódik a konfiguráció.
* Ez után elindul egy időzítő, ami 10 másodpercenként megnézi, hogy módosult-e a konfiuráció és ha módosult, újrainicializálja azt.
* Miután az új objektum életbe lépett, a már online vezérlőkön végigmegy és ha változott egy felhasználó jogosultsága, akkor elküldi az új
* jogosultságot a programnak, hogy frissítse azt a felületen. Ha egy felhasználótól megvonták a vezérlés jogát azon az autón, amit vezérel, a szerver azonnal elveszi tőle
* az autó vezérlését, mint ha lemondott volna róla a felhasználó, és ha a teljes jogot mevonják tőle, akkor visszakerül a járműválasztó menübe,
* mint ha a jármű kliens programja kilépett volna a szerverről (és a listájából kikerül a megvont jármű).
* Ha egy felhasználó jogot kapott egy jármű figyeléséhez, esetleg a vezérléséhez is és még nem választott járművet (tehát kap jelzést a lista módosulásáról),
* a listája frissítésre kerül, mint ha a jármű most csatlakozott volna a szerverhez.
* Ha a tiltólistára kerül fel egy online kliens, akkor a szerver természetesen bontja vele a kapcsolatot és legközelebbi kapcsolódáskor már nem fogja beenedni.
* @author zoli
*/
public class Permissions {
    
    private static PermissionConfig config = new PermissionConfig();

    private Permissions() {
    }

    public static PermissionConfig getConfig() {
        return config;
    }
    
}