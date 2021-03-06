﻿Mi ez?
------

Ebben a könyvtárban egy Android virtuálisgép található, melyre telepítve van és be van állítva az alkalmazás.

Linuxon a könyvtár tartalmát a ~/.android/avd könyvtárba lehet másolni és az Eclipse IDE-n belül el lehet indítani a virtuális gépet.
(A Mobile-RC alkalmazás nem kerül további frissítésre, ezt az adb install -r RemoteControlCar.apk paranccsal lehet megtenni.)

Az Eclipse-re pluginként telepíthető az Android SDK, de elérhető előre konfigurált változat is az Android oldaláról:
http://developer.android.com/sdk/index.html

Az alkalmazás feltelepíthető már meglévő virtuális gépre, ehhez adok most útmutatást.

A rendszer nyelve:
------------------

Elsőként a nyelvet állítottam át magyarra, hogy a program is magyar nyelvű legyen és ne angol.
(Mivel németül, se más nyelven nem tudok, ez a két nyelv áll rendelkezésre.)
A billentyűzetet is a kínairól átállítottam az alapértelmezettre és magyar nyelvűre.

IP-Webcam:
----------

Ahhoz, hogy a program képes legyen a kameraképet streamelni, ez a program szükséges. Ha nincs telepítve, a Hídhoz való kapcsolódás előtt a program üzen a felhasználónak és rá kattintva be is tölt a Play áruház, de mivel a virtuális gépeken nincs Play áruház, nem történik semmi.
Maradt a kézi telepítés az adb parancs segítségével a projekt könyvtárából kiadva:
adb install ip-webcam/com.pas.webcam-1.apk

ADB:
----

Az adb az Android SDK része, a platform-tools könyvtáron belül található.

File-manager:
-------------

Ahhoz, hogy kapcsolódni tudjon a kliens program a szerverhez, meg kell adni neki a tanúsítványfájlok pontos helyét.
Erre lehetőség van a program beállításában a rendszerre telepített fájl-menedzser program egyikével.
Mivel a virtuális gépen még fájl-menedzser sincs, ezt is külön kell telepíteni az adb parancs segítségével.

Az IP-Webcam mellé felmásoltam a telefonom lévő alkalmazást, ami a CyanogenMod ROM része.

Telepítése a telefonra:
adb install ip-webcam/FileManager.apk

A tanúsítvány fájlok:
---------------------

A tanúsítványok felmásolhatóak a projekt könyvtárából a virtuális gépre az alábbi paranccsal:
adb push test-certs-passwd /mnt/sdcard/test-certs

(Bárhová tehető az SD-kártyán belül, de a parancs az alapértelmezett helyre másol, így nem kell kitallózni a beállításokban egyessével mindhárom fájlt.)

A parancs a test-certs-passwd könyvtár tartalmát másolja a futó virtuális gép (vagy telefon, ha USB-n össze van kötve a géppel és az adb nincs tiltva) SD-kártyájára, azon belül is a test-certs könyvtárba, ami a program alapértelmezett könyvtára.

Ha az alábbi hibaüzenet jelenik meg
failed to copy 'test-certs-passwd/host.crt' to '/mnt/sdcard/test-certs/host.crt': No such file or directory
akkor vagy nincs SD-kártya felcsattolva, vagy régebbi Android rendszer fut a telefonon.
Ha fel van csattolva az SD-kártya, /mnt/sdcard útvonal helyett csak simán /sdcard legyen a parancsban, tehát:
adb push test-certs-passwd /sdcard/test-certs

Az SD-kártya méretét elsőre 10 MB-ra állítottam, de az nem volt elég, hogy a program kibontsa a tanúsítványokat, így nem tudott kapcsolódni a Hídhoz.
30 MB-ra állítva az SD-kártya méretét az SD-kártya formázódott, így másolhattam újra rá a tanúsítványokat, de ennyi hely már elég volt ahhoz, hogy a program megfelelően működjön.

A program telepítése:
---------------------

Természetesen fel kell telepíteni magát az alkalmazást is:

adb install android/RemoteControlCar/bin/RemoteControlCar.apk

Kapcsolódás a Hídhoz:
---------------------

A fenti műveletek elvégzése után indítható a program.
(Előtte is, csak fölösleges, mert úgy se fog csinálni semmit, csak jelzi a felhasználónak ugyan azt, amit fentebb elmondtam kifejtve.)

A beállításokban be kell állítani a teszttanúsítvány jelszavát: asdfgh
Ha ez kimarad, a jelszóvédett tanúsítványok beolvasása sikertelen lesz és hibát jelez a program.

Ha a szerver ugyan azon a gépen fut, melyen a virtuális gép és nem lett elállítva az alapértelmezett konfig, akkor most már indítható az alkalmazás.

Az első kapcsolódás elég hosszú folyamat a tanúsítványok beolvasása és ellenőrzése miatt. Két percet is igénybevehet!
Miután a tanúsítványok a memóriába kerültek, a kapcsolódás pár másodpercen belül végbemegy.

