package org.dyndns.fzoli.rccar.host.socket;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;

import org.apache.commons.ssl.Base64;
import org.dyndns.fzoli.rccar.host.Config;
import org.dyndns.fzoli.rccar.host.ConnectionService;
import org.dyndns.fzoli.rccar.host.R;
import org.dyndns.fzoli.socket.handler.SecureHandler;
import org.dyndns.fzoli.socket.process.AbstractSecureProcess;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A kamerakép streamelése a hídnak MJPEG folyamként.
 * A Hídnak való streamelés megvalósítása az utód osztályok feladata.
 * @author zoli
 */
public abstract class AbstractHostVideoProcess extends AbstractSecureProcess {

	/**
	 * Az IP Webcam alkalmazás elindításához szükséges.
	 */
	protected final ConnectionService SERVICE;
	
	/**
	 * A handler objektumot létrehozó kapcsolódássegítő referenciája.
	 */
	protected final ConnectionHelper HELPER;
	
	/**
	 * A kiépített MJPEG stream HTTP kapcsolata.
	 */
	protected HttpURLConnection conn;
	
	/**
	 * Biztonságos MJPEG stream küldő inicializálása.
	 * @param handler Biztonságos kapcsolatfeldolgozó, ami létrehozza ezt az adatfeldolgozót.
	 * @throws NullPointerException ha handler null
	 */
	public AbstractHostVideoProcess(ConnectionService service, ConnectionHelper helper, SecureHandler handler) {
		super(handler);
		HELPER = helper;
		SERVICE = service;
	}

	/**
	 * Elindítja az IP Webcam alkalmazás szerverét a megadott paraméterekkel.
	 * Ha a szerver már fut, a paraméterben megadott beállítások nem érvényesülnek.
	 * Miután a szerver elindult, a kameraképet mutató Activity megjelenik egy elrejtő gombbal.
	 * @param port a szerver portja, amin figyel
	 * @param user a szerver eléréséhez szükséges felhasználónév (üres string esetén névtelen hozzáférés)
	 * @param password nem névtelen hozzáférés esetén a felhasználónévhez tartozó jelszó
	 */
	private void startIPWebcamActivity(String port, String user, String password) {
		Intent launcher = new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
		Intent ipwebcam = 
			new Intent()
			.setClassName("com.pas.webcam", "com.pas.webcam.Rolling")
			.putExtra("cheats", new String[] {
					"set(Awake,true)", // ébrenlét fenntartása
					"set(Notification,true)", // rendszerikon ne legyen rejtve
					"set(Audio,1)", // audio stream kikapcsolása
					"set(Video,320,240)", // videó felbontás 320x240
					"set(DisableVideo,false)", // videó stream engedélyezése
					"set(Quality,30)", // JPEQ képkockák minősége 30 százalék
					"set(Port," + port + ")", // figyelés beállítása a 8080-as portra
					"set(Login," + user + ")", // felhasználónév beállítása a konfig alapján
					"set(Password," + password + ")" // jelszó beállítása a konfig alapján
					})
			.putExtra("hidebtn1", true) // Súgó gomb elrejtése
			.putExtra("caption2", SERVICE.getString(R.string.run_in_background)) // jobb oldali gomb feliratának beállítása
			.putExtra("intent2", launcher) // jobb oldali gomb eseménykezelőjének beállítása, hogy hozza fel az asztalt
		    .putExtra("returnto", launcher) // ha a programból kilépnek, szintén az asztal jön fel
		    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // activity indítása új folyamatként, hogy a service elindíthassa
		SERVICE.startActivity(ipwebcam); // a program indítása a fenti konfigurációval
	}
	
	/**
	 * Leállítja az IP Webcam alkalmazást.
	 */
	private void stopIPWebcamActivity() {
		stopIPWebcamActivity(SERVICE);
	}
	
	/**
	 * Leállítja az IP Webcam alkalmazást.
	 * @param context Activity vagy Service
	 */
	public static void stopIPWebcamActivity(Context context) {
		context.sendBroadcast(new Intent("com.pas.webcam.CONTROL").putExtra("action", "stop"));
	}
	
	/**
	 * Kapcsolódik az IP Webcam alkalmazás MJPEG folyamához.
	 * Tíz alkalommal kísérli meg a kapcsolódást 2 másodperc szünetet tartva a két próbálkozás között.
	 * Ha a kapcsolódás nem sikerül, kiküldi az alkalmazást elindító utasítást az Androidnak.
	 * Az alkalmazást a felhasználó beállítása alapján indítja el és ez alapján kapcsolódik a helyi szerverhez.
	 * Ha sikerült a kapcsolódás, de olvasni nem lehet a folyamból, újraindítja az IP Webcam alkalmazást,
	 * hogy frissüljenek a konfigurációs beállításai, mert valószínűleg azzal van a gond.
	 * Ha a harmadik kapcsolódás sem sikerül, szintén újraindítja az IP Webcam alkalmazást.
	 * @return true, ha sikerült a kapcsolódás és a folyam olvasása, egyébként false
	 */
	protected boolean openIPWebcamConnection() {
		Config conf = SERVICE.getConfig();
		String port = conf.getCameraStreamPort(); // szerver port
		String user = conf.getCameraStreamUser(); // felhasználónév
		String password = conf.getCameraStreamPassword(); // jelszó
		
		String httpUrl = "http://127.0.0.1:" + port + "/videofeed"; // a szerver pontos címe
		String authProp = "Basic " + Base64.encodeBase64String(new String(user + ':' + password).getBytes()); // a HTTP felhasználóazonosítás Base64 alapú
		
		boolean stopped = false; // kezdetben még nem lett leállítva az IP Webcam
		for (int i = 1; i <= 10; i++) { // 10 próbálkozás a kapcsolat létrehozására
			try {
				conn = (HttpURLConnection) new URL(httpUrl).openConnection(); //kapcsolat objektum létrehozása
				conn.setRequestMethod("GET"); // GET metódus beállítása
				if (user != null && !user.equals("")) conn.setRequestProperty("Authorization", authProp); // ha van azonosítás, adat beállítása
				conn.connect(); // kapcsolódás
				if (i != 0) { // ellenőrzés csak akkor, ha a ciklusváltozó értéke nem 0
					try {
						conn.getInputStream().read(new byte[5120]); // olvashatóság tesztelése
					}
					catch (FileNotFoundException fnfe) {
						closeIPWebcamConnection(true);
						throw fnfe;
					}
					closeIPWebcamConnection(false); // kapcsolat lezárása, de program futva hagyása
					i = -1; // az olvasás teszt sikeres volt, a következő ciklus futáskor a változó értéke 0 lesz, hogy ne legyen újra olvasás
					continue; // következő ciklusra lépés
				}
				return true;
			}
			catch (ConnectException ex) { // ha a kapcsolódás nem sikerült
				Log.i(ConnectionService.LOG_TAG, "ip webcam open error", ex);
				if (i == 5 && !stopped) { // az 5. próbálkozásra leállítás, ha még nem volt
					i = 0; // újra 10 próbálkozás
					stopped = true; // több leállítás nem kell
					stopIPWebcamActivity(); // ha a harmadik kapcsolódás sem sikerült, alkalmazás leállítása
				}
				startIPWebcamActivity(port, user, password); // IP Webcam program indítása, hátha még nem fut
				sleep(2000); // 2 másodperc várakozás a program töltésére
			}
			catch (SocketException ex) { // valószínűleg eltérő konfigurációval fut az IP Webcam vagy éppen újraindul
				Log.i(ConnectionService.LOG_TAG, "ip webcam open error", ex);
				if (!stopped) { // leállítás, ha még nem volt
					i = 0; // újra 10 próbálkozás
					stopped = true; // több leállítás nem kell
					stopIPWebcamActivity(); // az alkalmazás leállítása
				}
				startIPWebcamActivity(port, user, password); // majd újra elindítása
				sleep(2000); // 2 másodperc várakozás a program töltésére
			}
			catch (Exception ex) { // egyéb ismeretlen hiba
				Log.i(ConnectionService.LOG_TAG, "ip webcam open error", ex);
				sleep(2000); // 2 másodperc várakozás a program töltésére
			}
		}
		
		closeIPWebcamConnection(true);
		
		return false;
	}
	
	/**
	 * Lezárja a kapcsolatot az IP Webcam alkalmazás szerverével és leállítja az IP Webcam programot.
	 * @param stop true esetén az IP Webcam program leáll, egyébként csak a kapcsolat zárul le
	 */
	protected void closeIPWebcamConnection(boolean stop) {
		if (conn != null) conn.disconnect();
		if (stop) stopIPWebcamActivity();
	}
	
	/**
	 * Elkezdi streamelni az IP Webcam által küldött MJPEG folyamot.
	 * Elsőként kialakítja a kapcsolatott az IP Webcam szerverével, aztán elkezdi a feltöltést.
	 * Ha nem sikerül az IP Webcam szerveréhez kapcsolódni vagy gond van a programmal, hibát jelez a szolgáltatásnak.
	 * Ha a kapcsolat létrejötte után a felhasználó leállítja az IP Webcam szerverét, újra lefut rekurzívan ez a metódus.
	 * Ha a hídnak való feltöltés közben hiba történik, az egész kapcsolatfeldolgozó újrapéldányosítódik új kapcsolattal.
	 */
	@Override
	public abstract void run();

}
