# NeonLyrics (Fabric, Minecraft 1.21.4)

## Bauen
1. JDK 21 installieren. Gradle 8.12+ installieren (oder den Gradle-Wrapper aus https://github.com/FabricMC/fabric-example-mod hierher kopieren).
2. Im Projektordner: `gradle build`  (Windows mit Wrapper: `gradlew build`)
3. Die Mod liegt danach in `build/libs/neonlyrics-1.0.0.jar` (die `-sources.jar` NICHT verwenden).
4. Fabric Loader + Fabric API fuer 1.21.4 installieren und die .jar in `.minecraft/mods` legen.

## Spotify einrichten
1. https://developer.spotify.com/dashboard -> App erstellen, Redirect-URI `http://127.0.0.1:8898/callback` eintragen, "Web API" waehlen.
2. Client-ID kopieren, im Spiel mit Rechts-Shift das Menue oeffnen -> Spotify -> Client-ID einfuegen -> "Mit Spotify verbinden".
3. Lyrics kommen von LRCLIB (kostenlos, ohne Key).

Falls die Versionen in gradle.properties nicht aufloesen: aktuelle Werte auf https://fabricmc.net/develop eintragen.
