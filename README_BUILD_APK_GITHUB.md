# Build APK senza Android Studio

Questo progetto contiene una GitHub Action che compila automaticamente l'APK.

## Passaggi

1. Crea una repository GitHub nuova, per esempio `LyricSyncAndroid`.
2. Carica dentro GitHub **tutto il contenuto della cartella `LyricSyncAndroid`**, non lo zip.
3. Vai su GitHub nella repo.
4. Apri la scheda `Actions`.
5. Apri workflow `Build LyricSync Android APK`.
6. Premi `Run workflow`.
7. Aspetta la fine della build.
8. Apri il job completato.
9. Scarica l'artifact `LyricSyncAndroid-debug-apk`.
10. Dentro trovi `app-debug.apk`.

## Installazione sul telefono

1. Copia `app-debug.apk` sul telefono.
2. Aprilo.
3. Se Android lo chiede, abilita `Installa app sconosciute`.
4. Installa.
5. Apri LyricSync.
6. Premi `Abilita accesso notifiche/media`.
7. Attiva LyricSync.
8. Avvia una canzone da Spotify / Apple Music / YouTube Music.
