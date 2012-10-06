package org.dyndns.fzoli.rccar;

/**
 * Kapcsolat- és eszközazonosítók.
 * @author zoli
 */
public interface ConnectionKeys {
    
    /**
     * Az autót vezérlő telefon eszközazonosítója.
     */
    int KEY_DEV_HOST = 0;
    
    /**
     * Az autót irányító számítógép eszközazonosítója.
     */
    int KEY_DEV_CONTROLLER = 1;
    
    /**
     * A kapcsolat megszakadását detektáló szál kapcsolatazonosítója.
     */
    int KEY_CONN_DISCONNECT = 0;
    
    /**
     * A tesztelés idejére készített DUMMY-feldolgozó kapcsolatazonosítója.
     */
    int KEY_CONN_DUMMY = 1;
    
}