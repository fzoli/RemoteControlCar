package org.dyndns.fzoli.rccar.host;

//import java.io.InputStream;
//import java.net.ConnectException;
//import java.net.HttpURLConnection;
//import java.net.URL;

import java.util.Timer;
import java.util.TimerTask;

import ioio.lib.util.android.IOIOService;

//import org.apache.commons.ssl.Base64;
import org.dyndns.fzoli.rccar.host.socket.ConnectionHelper;
import org.dyndns.fzoli.rccar.host.vehicle.Vehicle;
import org.dyndns.fzoli.rccar.host.vehicle.Vehicles;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
//import android.util.Log;
import android.util.Log;

/**
 * Az alkalmazás háttérben futó szolgáltatása.
 * A hídhoz való hálózati kapcsolódást és a jármű mikrovezérlőjéhez való kapcsolódás a fő feladatköre.
 * A felhasználót értesíti minden fontos eseményről (warning, error). Arról is, hogy a szolgáltatás fut.
 * @author zoli
 */
public class ConnectionService extends IOIOService {
	
	/**
	 * A fatális hibák (röviden hibák) beállításukkor bontják a kapcsolatot a híddal és nem távolíthatóak el a notification barról. A hibák a service leállásakor mind eltűnnek. A hibákra kattintva a főablak jelenik meg.
	 * A figyelmeztetések (nem hibák) beállításukkor reconnect ütemezést aktiválnak és a service leállásakor vagy sikeres kapcsolódás esetén mind eltűnnek. A figyelmeztetések eltávolíthatóak és rájuk kattintva azonnal eltűnnek és lefuttatják a reconnect metódust.
	 * A figyelmeztetések közül egyszerre csak egy látható.
	 * Az other figyelmeztetés ismeretlen hibát jelöl.
	 * A sikeres kapcsolatfelvételt a null referencia jelzi.
	 */
	public static enum ConnectionError {
		OTHER,
		CONNECTION_ERROR,
		CONNECTION_LOST, 
		CONNECTION_REFUSED,
		INVALID_CERTIFICATE,
		WEB_IPCAM_UNREACHABLE(true),
		WRONG_CLIENT_VERSION(true),
		WRONG_CERTIFICATE_SETTINGS(true);
		
		/**
		 * Megadja, hogy fatális hibáról van-e szó.
		 */
		private final boolean FATAL_ERROR;
		
		/**
		 * A felsorolás konstruktora.
		 * Alapértelmezésként nem fatális hibáról van szó.
		 */
		private ConnectionError() {
			this(false);
		}
		
		/**
		 * A felsorolás konstruktora.
		 * @param error true esetén fatális hiba
		 */
		private ConnectionError(boolean error) {
			FATAL_ERROR = error;
		}
		
		/**
		 * A hibához kapcsolódó figyelmeztetés azonosítóját adja meg.
		 * Generált érték a felsorolás index alapján.
		 * Pl. 101, 102 ... 107
		 */
		public int getNotificationId() {
			return 100 + ordinal();
		}
		
		/**
		 * Megadja, hogy a hibához tartozik-e felületi figyelmeztetés.
		 * @return true esetén látható, tehát tartozik hozzá figyelmeztetés
		 */
		public boolean isVisible() {
			return this != OTHER;
		}
		
		/**
		 * Megadja, hogy a hiba fatális-e.
		 * Fatális hiba esetén nem ismétlődik meg a kapcsolódás.
		 * @return true esetén fatális a hiba, egyébként nem
		 */
		public boolean isFatalError() {
			return FATAL_ERROR;
		}
		
	}
	
	/**
	 * Segédváltozó ahhoz, hogy a {@code R.string} osztály Integer változóihoz hozzá lehessen férni úgy,
	 * hogy két változó segítégével (String + Integer) lehessen megkapni a referenciát.
	 * Pl. "err_" + key , ahol key egy változó érték, de a String konstans.
	 */
	private final static R.string STRINGS = new R.string();
	
	/**
	 * Konstans azonosítók azokhoz a figyelmeztetésekhez, amik nem a kapcsolódás figyelmeztetései.
	 */
	private final static int ID_NOTIFY = 0,
						ID_NOTIFY_CONFIG = 1,
						ID_NOTIFY_NETWORK = 2,
						ID_NOTIFY_INST_CAM = 3,
						ID_NOTIFY_GPS_ENABLE = 4;
	
	/**
	 * A SharedPreference azon kulcsai, melyek kódban több helyen is használatban vannak.
	 * Tipikus eset amikor getterben és setter metódus is írva van egy paraméterhez.
	 */
	private final static String KEY_STARTED = "started";
	
	/**
	 * Az alkalmazásom az IP Webcam alkalmazásra támaszkodik.
	 * Az IP Webcam alkalmazás ingyenes és a Play áruházból letölthető és
	 * ha nincs még telepítve, figyelmeztetés jelenik meg.
	 * Erre a figyelmeztetésre kattintva megnyílik a Play áruház és behozza az alkalmazást.
	 * Ahhoz, hogy az alkalmazást be lehessen hozni, a pontos csomag útvonalára van szükség.
	 */
	private final static String PACKAGE_CAM = "com.pas.webcam";
	
	/**
	 * Az Android naplózójához használt teg.
	 */
	public final static String LOG_TAG = "mobilerc";
	
	/**
	 * A szolgáltatás az eseményeket ezzel a kulccsal várja.
	 */
	public final static String KEY_EVENT = "event";
	
	/**
	 * A szolgáltatás ezeket az eseményeket fogadja és dolgozza fel.
	 * A kód Android 2.1-től egészen a 4-es verzióig működőképes, ezért már elavult eseményeket is használ.
	 */
	@SuppressWarnings("deprecation")
	public final static String EVT_RECONNECT_NOW = "reconnect now",
						EVT_CONNECTIVITY_CHANGE = ConnectivityManager.CONNECTIVITY_ACTION,
						EVT_GPS_SENSOR_CHANGE = LocationManager.PROVIDERS_CHANGED_ACTION,
						EVT_SDCARD_MOUNTED = Intent.ACTION_MEDIA_MOUNTED,
						EVT_SHUTDOWN = Intent.ACTION_SHUTDOWN,
						EVT_APP_INSTALL = Intent.ACTION_PACKAGE_INSTALL,
						EVT_APP_ADDED = Intent.ACTION_PACKAGE_ADDED;
	
	/**
	 * Összeköttetés az Activity és a Service között.
	 */
	private final ConnectionBinder BINDER = new ConnectionBinder(this);
	
	/**
	 * A felhasználó által kiválasztott jármű vezérlője.
	 */
	private Vehicle vehicle;
	
	/**
	 * A hídhoz való kapcsolódáshoz szükséges beállítások.
	 */
	private Config config;
	
	/**
	 * A hídhoz való kapcsolódást megvalósító objektum.
	 */
	private ConnectionHelper conn;
	
	/**
	 * Az Android kapcsolódáskezelője.
	 * Arra kell, hogy a szolgáltatás értesüljön arról, ha a hálózati kapcsolat kialakult vagy megszünt.
	 * Pl. megszakadt a WiFi jel, vagy a felhasználó kiválasztott egy hálózatot és sikerült rá kapcsolódni
	 * Addig, míg nincs hálózat, fölösleges próbálkozni kapcsolódással, úgy se fog menni.
	 */
	private ConnectivityManager cm;
	
	/**
	 * Az Android helyzetmeghatározója.
	 * GPS jel fogadására kell, valamint arra, hogy megtudja a szolgáltatás, hogy elérhető-e a GPS szenzor.
	 * A hídhoz való kapcsolódásnak nem feltétele a GPS jel megléte. Ez esetben ismeretlen lesz a pozíció.
	 */
	private LocationManager lm;
	
	/**
	 * Az újrakapcsolódás időzítésére fenntartott időzítő.
	 * Ha a kapcsolat megszakadt a híddal vagy nem sikerült kapcsolódni rá,
	 * újra megpróbálja a szolgáltatás a kapcsolódást későbbre időzítve.
	 */
	private final Timer CONN_TIMER = new Timer();
	
	/**
	 * Az újrakapcsolódáshoz használt ütemezett feladat.
	 * Egy kapcsolódáshoz egy példány használható, ezért mindig az aktuális példányt tartalmazza a változó.
	 */
	private TimerTask connTask;
	
	/**
	 * Az Android rendszerikon kezelője.
	 * Arra kell, hogy figyelmeztetést tudjon megjeleníteni a szolgáltatás a felhasználónak.
	 */
	private NotificationManager nm;
	
	/**
	 * A fő rendszerikon.
	 * Akkor látható, amikor a szolgáltatás fut.
	 */
	private Notification notification;
	
	/**
	 * A fő rendszerikon értesítő szövege.
	 * Jelzi, hogy ki van-e alakítva a kapcsolat a járművel és a híddal.
	 * Ha rákattintanak a rendszerikonra, felhozza az alkalmazás főablakát.
	 */
	private PendingIntent contentIntent;
	
	/**
	 * A szolgáltatást felfüggesztése illetve aktiválása.
	 * Ha a szolgáltatás felfüggesztett, úgy viselkedik, mint ha nem futna.
	 * Erre azért van szükség, mert attól, hogy a szolgáltatás még le van állítva,
	 * az általa indított szálak még aktívak és bármikor jöhet olyan esemény, amely
	 * nem várt hatást eredményez. Pl. híd disconnect esetén figyelmeztetés és újrakapcsolódás
	 * Kezdetben a szolgáltatás aktív, tehát végzi a dolgát első indulástól kezdve.
	 */
	private static boolean suspended = false;
	
	public static boolean isSuspended() {
		Log.i(LOG_TAG, "suspended: " + suspended);
		return suspended;
	}
	
	public static void setSuspended(boolean suspended) {
		ConnectionService.suspended = suspended;
	}
	
	public Config getConfig() {
		return config;
	}
	
	public ConnectionBinder getBinder() {
		return BINDER;
	}
	
	@Override
	public ConnectionBinder onBind(Intent intent) {
		return BINDER;
	}
	
	@Override
	public Vehicle createIOIOLooper() {
		return vehicle = Vehicles.createVehicle(BINDER, Integer.parseInt(getSharedPreferences(this).getString("vehicle", "0")));
	}
	
	private ConnectionHelper createConnectionHelper() {
		config = createConfig(this);
		if (!config.isCorrect() && !isOfflineMode()) {
			setConfigNotificationVisible(true);
		}
		else {
			setConfigNotificationVisible(false);
		}
		if (isOfflineMode() || !config.isCorrect() || !isNetworkAvailable() || !isAppInstalled(PACKAGE_CAM)) {
			return null;
		}
		return conn = new ConnectionHelper(this);
	}
	
//	private void startIPWebcam() { //TODO: ez egyelőre csak teszt, nem itt a helye, hanem majd a kapcsolódáskor kell hasonlót futtatni
//		if (isAppInstalled(PACKAGE_CAM)) {
//			SharedPreferences pref = getSharedPreferences(this);
//			String port = pref.getString("cam_port", "8080");
//			String user = pref.getString("cam_user", "");
//			String password = pref.getString("cam_password", "");
//			Intent launcher = new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
//			Intent ipwebcam = 
//				new Intent()
//				.setClassName("com.pas.webcam", "com.pas.webcam.Rolling")
//				.putExtra("cheats", new String[] {
//						"set(Awake,true)", // ébrenlét fenntartása
//						"set(Notification,true)", // rendszerikon ne legyen rejtve
//						"set(Audio,1)", // audio stream kikapcsolása
//						"set(Video,320,240)", // videó felbontás 320x240
//						"set(DisableVideo,false)", // videó stream engedélyezése
//						"set(Quality,30)", // JPEQ képkockák minősége 30 százalék
//						"set(Port," + port + ")", // figyelés beállítása a 8080-as portra
//						"set(Login," + user + ")", // felhasználónév beállítása a konfig alapján
//						"set(Password," + password + ")" // jelszó beállítása a konfig alapján
//						})
//				.putExtra("hidebtn1", true) // Súgó gomb elrejtése
//				.putExtra("caption2", getString(R.string.run_in_background)) // jobb oldali gomb feliratának beállítása
//				.putExtra("intent2", launcher) // jobb oldali gomb eseménykezelőjének beállítása, hogy hozza fel az asztalt
//			    .putExtra("returnto", launcher) // ha a programból kilépnek, szintén az asztal jön fel
//			    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // activity indítása új folyamatként, hogy a service elindíthassa
//			startActivity(ipwebcam); // a program indítása a fenti konfigurációval
//			try {
//				HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/videofeed").openConnection(); //kapcsolat objektum létrehozása
//	            conn.setRequestMethod("GET"); // GET metódus beállítása
//	            conn.setRequestProperty("Authorization", Base64.encodeBase64String(new String(user + ':' + password).getBytes()));
//	            // most, hogy minden be van állítva, kapcsolódás
//	            conn.connect();
//	            InputStream in = conn.getInputStream(); // mjpeg stream megszerzése
//			}
//			catch (ConnectException ex) {
//				Log.i(LOG_TAG, "retry later", ex);
//				try {
//					Thread.sleep(2000);
//				}
//				catch (Exception e) {
//					;
//				}
//				finally {
//					startIPWebcam();
//				}
//			}
//			catch (Exception ex) {
//				Log.i(LOG_TAG, "error", ex);
//			}
//		}
//	}
//	
//	private void stopIPWebcam() { //TODO: hatástalan a broadcast intent küldése. Miért?
//		if (isAppInstalled(PACKAGE_CAM)) {
//			sendBroadcast(new Intent("com.pas.webcam.CONTROL").putExtra("action", "stop"));
//		}
//	}
	
	public void reconnectSchedule() {
		reconnectSchedule(false);
	}
	
	private void reconnectSchedule(boolean now) {
		if (now) {
			if (connTask != null) connTask.run();
			else reconnect();
			connTask = null;
		}
		else if (connTask == null) {
			connTask = new TimerTask() {
				
				@Override
				public void run() {
					reconnect();
					connTask = null;
				}
				
			};
			CONN_TIMER.schedule(connTask, getReconnectDelay());
		}
	}
	
	private void reconnect() {
		if (!isBridgeConnected()) connect(true);
	}
	
	private void connect(boolean reconnect) {
		Log.i(LOG_TAG, "connect calling");
		if (conn == null || reconnect) {
			disconnect(false);
			if ((isStarted(this) && !isSuspended()) && createConnectionHelper() != null) conn.connect();
		}
	}
	
	private void disconnect(boolean stop) {
		Log.i(LOG_TAG, "disconnect calling");
		if (connTask != null && stop) {
			connTask.cancel();
			connTask = null;
		}
		if (conn != null) {
			conn.disconnect();
			conn = null;
		}
		getBinder().fireConnectionStateChange(false);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (startId == 1) {
			initNotification();
			connect(false);
			updateNotificationText();
			setNotificationsVisible(true);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (intent.hasExtra(KEY_EVENT) && !isSuspended()) {
			String event = intent.getStringExtra(KEY_EVENT);
			if (event.equals(EVT_CONNECTIVITY_CHANGE)) {
				if (startId != 1) setNetworkNotificationVisible(true);
				if (isNetworkAvailable()) connect(true);
				else disconnect(false);
			}
			else if (event.equals(EVT_GPS_SENSOR_CHANGE)) {
				if (startId != 1) setGpsEnableNotificationVisible(true);
			}
			else if (event.equals(EVT_APP_ADDED) || event.equals(EVT_APP_INSTALL)) {
				if (startId != 1) {
					setCamInstallNotificationVisible(true);
					connect(false);
				}
			}
			else if (event.equals(EVT_SDCARD_MOUNTED)) {
				connect(false);
			}
			else if (event.equals(EVT_RECONNECT_NOW)) {
				reconnectSchedule(true);
			}
			else if (event.equals(EVT_SHUTDOWN)) {
				setSuspended(true);
				stopSelf();
			}
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		disconnect(true);
		setNotificationsVisible(false);
		setConnectionError(null, true);
		removeNotification();
		super.onDestroy();
	}
	
	public void onVehicleConnectionStateChanged() {
		updateNotificationText();
	}
	
	@SuppressWarnings("deprecation")
	private void initNotification() {
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_main, getString(R.string.app_name), System.currentTimeMillis());
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
	}
	
	@SuppressWarnings("deprecation")
	private void setNotificationText(String s) {
		if (nm != null && notification != null && contentIntent != null) {
			notification.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), s, contentIntent);
			nm.notify(ID_NOTIFY, notification);
		}
	}
	
	public void updateNotificationText() {
		if (!isSuspended()) setNotificationText(getString(R.string.vehicle) + ": " + getString(isVehicleConnected() ? R.string.exists : R.string.not_exists) + "; " + (isOfflineMode() ? getString(R.string.title_offline) : (getString(R.string.bridge_conn) + ": " + getString(isBridgeConnected() ? R.string.exists : R.string.not_exists))) + '.');
	}
	
	private void removeNotification() {
		nm.cancel(ID_NOTIFY);
		nm = null;
		notification = null;
		contentIntent = null;
	}
	
	private void addOnlineNotification(int resText, Intent intent, int key, boolean error) {
		if (!isOfflineMode()) {
			addNotification(resText, intent, key, false, error);
		}
	}
	
	private void addNotification(int resText, Intent intentActivity, int key, boolean removable, boolean error) {
		addNotification(resText, intentActivity, null, key, removable, error);
	}
	
	@SuppressWarnings("deprecation")
	private void addNotification(int resText, Intent intentActivity, Intent intentService, int key, boolean removable, boolean error) {
		removeNotification(key);
		if (isSuspended()) return;
		Log.i(LOG_TAG, "add notification request: " + key);
		Notification notification = new Notification(error ? R.drawable.ic_error : R.drawable.ic_warning, getString(resText), System.currentTimeMillis());
		notification.flags |= removable ? Notification.FLAG_AUTO_CANCEL : Notification.FLAG_NO_CLEAR;
		PendingIntent contentIntent;
		int flag = removable ? PendingIntent.FLAG_ONE_SHOT : PendingIntent.FLAG_UPDATE_CURRENT;
		if (intentActivity != null) contentIntent = PendingIntent.getActivity(this, 0, intentActivity, removable ? PendingIntent.FLAG_ONE_SHOT : flag);
		else contentIntent = PendingIntent.getService(this, 0, intentService, flag);
		notification.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), getString(resText), contentIntent);
		nm.notify(key, notification);
	}
	
	public void setConnectionError(ConnectionError error) {
		setConnectionError(error, false);
	}

	private void setConnectionError(ConnectionError error, boolean removeAll) {
		updateNotificationText();
		getBinder().fireConnectionStateChange(false);
		ConnectionError[] errors = ConnectionError.values();
		if (removeAll || isSuspended()) {
			Log.i(LOG_TAG, "connection msg remove");
			// service leáll, figyelmeztetések és hibák eltüntetése
			for (ConnectionError err : errors) {
				removeNotification(err.getNotificationId());
			}
		}
		else if (isStarted(this) && !isSuspended()) {
			if (error == null || !error.isFatalError()) {
				// sikeres kapcsolódás vagy figyelmeztető üzenet, figyelmeztetések eltüntetése
				Log.i(LOG_TAG, "connection warn remove");
				for (ConnectionError err : errors) {
					if (!err.isFatalError()) removeNotification(err.getNotificationId());
				}
			}
			if (error != null) {
				Integer id = null;
				if (error.isVisible()) {
					try {
						id = (Integer) R.string.class.getField("err_" + error.ordinal()).get(STRINGS);
					}
					catch (Exception ex) {
						Log.i(LOG_TAG, "no text");
					}
				}
				if (error.isFatalError()) {
					Log.i(LOG_TAG, "add error notify " + error);
					// kapcsolat bontása, üzenet megjelenítése, amire kattintva a főablak jelenik meg
					disconnect(true);
					if (id != null) addNotification(id, new Intent(this, MainActivity.class), error.getNotificationId(), false, true);
				}
				else {
					Log.i(LOG_TAG, "add warn notify " + error);
					// reconnect ütemezés, üzenet megjelenítése eltávolíthatóként, amire kattintva azonnali reconnect fut le
					reconnectSchedule();
					if (id != null) addNotification(id, null, new Intent(this, ConnectionService.class).putExtra(KEY_EVENT, EVT_RECONNECT_NOW), error.getNotificationId(), true, false);
				}
			}
		}
	}
	
	private void setNotificationsVisible(boolean visible) {
		setConfigNotificationVisible(visible);
		setNetworkNotificationVisible(visible);
		setGpsEnableNotificationVisible(visible);
		setCamInstallNotificationVisible(visible);
	}
	
	private void setConfigNotificationVisible(boolean visible) {
		if (config == null) return;
		if (visible && !config.isCorrect()) {
			if (isSDCardMounted()) addOnlineNotification(R.string.set_config, new Intent(this, MainActivity.class), ID_NOTIFY_CONFIG, true);
			else addOnlineNotification(R.string.sdcard_not_mounted, new Intent(Settings.ACTION_MEMORY_CARD_SETTINGS), ID_NOTIFY_CONFIG, false);
		}
		else {
			removeNotification(ID_NOTIFY_CONFIG);
		}
	}
	
	private void setNetworkNotificationVisible(boolean visible) {
		if (visible && !isNetworkAvailable() && !isNetworkConnecting()) addOnlineNotification(R.string.set_network, new Intent(Settings.ACTION_WIRELESS_SETTINGS), ID_NOTIFY_NETWORK, false);
		else removeNotification(ID_NOTIFY_NETWORK);
	}
	
	private void setCamInstallNotificationVisible(boolean visible) {
		if (visible && !isAppInstalled(PACKAGE_CAM)) addOnlineNotification(R.string.install_cam, new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + PACKAGE_CAM)), ID_NOTIFY_INST_CAM, true);
		else removeNotification(ID_NOTIFY_INST_CAM);
	}
	
	private void setGpsEnableNotificationVisible(boolean visible) {
		if (visible && !isGpsEnabled()) addOnlineNotification(R.string.set_gps, new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), ID_NOTIFY_GPS_ENABLE, false);
		else removeNotification(ID_NOTIFY_GPS_ENABLE);
	}
	
	private void removeNotification(int key) {
		if (nm == null) initNotification();
		nm.cancel(key);
	}
	
	public int getReconnectDelay() {
		return Integer.parseInt(getSharedPreferences(this).getString("reconnect_delay", "20000"));
	}
	
	public boolean isVehicleConnected() {
		return vehicle.isConnected();
	}
	
	public boolean isBridgeConnected() {
		return conn != null && conn.isConnected();
	}
	
	private boolean isOfflineMode() {
		return isOfflineMode(this);
	}
	
	private boolean isNetworkConnecting() {
		return isNetworkState(NetworkInfo.State.CONNECTING);
	}
	
	private boolean isNetworkAvailable() {
		return isNetworkState(NetworkInfo.State.CONNECTED);
	}
	
	private boolean isNetworkState(NetworkInfo.State state) {
		final NetworkInfo activeNetwork = getActiveNetworkInfo();
		return activeNetwork != null && state != null && activeNetwork.getState() == state;
	}
	
	private NetworkInfo getActiveNetworkInfo() {
		try {
			if (cm == null) cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			return cm.getActiveNetworkInfo();
		}
		catch (RuntimeException ex) {
			/* Null or Permission Denied */
			return null;
		}
	}
	
	private boolean isGpsEnabled() {
		if (lm == null) lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
	
	private boolean isAppInstalled(String packageName) {
	    PackageManager pm = getPackageManager();
	    boolean installed = false;
	    try {
	       pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
	       installed = true;
	    } catch (PackageManager.NameNotFoundException e) {
	       installed = false;
	    }
	    return installed;
	}
	
	private static SharedPreferences getSharedPreferences(Context context) {
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public static Config createConfig(Context context) {
		return new Config(getSharedPreferences(context));
	}
	
	private static boolean isSDCardMounted() {
		String state = Environment.getExternalStorageState();
	    return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
	}
	
	public static boolean isOfflineMode(Context context) {
		return getSharedPreferences(context).getBoolean("offline", false);
	}
	
	public static boolean isStarted(Context context) {
		return getSharedPreferences(context).getBoolean(KEY_STARTED, false);
	}
	
	public static void setStarted(Context context, boolean b) {
		getSharedPreferences(context).edit().putBoolean(KEY_STARTED, b).commit();
	}
	
}
