# LyricSync Android

Versione Android di LyricSync.

## Funzioni

- legge la canzone in riproduzione da Spotify, Apple Music, YouTube Music ecc. tramite MediaSession;
- richiede accesso notifiche/media;
- cerca il testo su LRCLIB;
- usa testo sincronizzato se disponibile;
- se trova solo testo normale crea una sync approssimativa;
- mostra copertina sopra, lyrics sotto;
- crea una notifica Android con riga attuale e riga successiva.

## Come aprirlo

1. Estrai lo zip.
2. Apri Android Studio.
3. File → Open.
4. Seleziona la cartella `LyricSyncAndroid`.
5. Aspetta il Gradle Sync.
6. Premi Run.

## Primo avvio sul telefono

1. Apri LyricSync.
2. Premi `Abilita accesso notifiche/media`.
3. Attiva LyricSync nella schermata Android.
4. Torna nell'app.
5. Avvia una canzone da Spotify / Apple Music / YouTube Music.
6. LyricSync mostrerà copertina, titolo, artista e testo.

## Nota Android Auto

Android Auto non permette liberamente layout custom come CarPlay con qualunque app sideloadata.
Questa versione mostra la notifica lyrics Android; su alcuni sistemi auto può apparire come card/notifica, ma non è garantito come una vera app Android Auto ufficiale.
