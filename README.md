# Reel Cut

Native Android-App (Kotlin + Jetpack Compose) zum schnellen Schneiden und
Zusammenfügen von Kurzvideos für Instagram — Trimmen, Tempo, Filter, Text,
Übergänge, Musik, exportieren, teilen.

Getestet auf einem echten Gerät (Samsung Galaxy S25, Android 16) per adb/uiautomator —
nicht nur Code-Review. Build + Deploy + Export wurden end-to-end verifiziert, jeder
Übergangstyp einzeln per Frame-Analyse der exportierten Videos.

## Funktionsumfang (v0.4)

- **Videos auswählen** über den System-Photo-Picker (kein Storage-Permission-Dialog nötig)
- **Timeline**: Clips per Pfeil-Buttons neu anordnen, einzeln entfernen, komplette Editor-Seite scrollbar
- **Trimmen** pro Clip über einen Start/End-Range-Slider
- **Geschwindigkeit** pro Clip (0.25x–3x), Video (`SpeedChangeEffect`) und Audio (`SonicAudioProcessor`) bleiben synchron
- **Text-Overlay** pro Clip (unteres Drittel, `TextOverlay`/`OverlayEffect`)
- **Format** (CapCut-artig): 9:16 Reel (Standard, 1080×1920), 4:5 Post, 1:1, Original – jeder Clip wird
  mittig auf das Format zugeschnitten, gemischte Hoch-/Querformat-Clips ergeben ein einheitliches Video
- **10 Filter** (Normal, Vibrant, Noir, Sepia, Warm, Cool, Moody, Neon, Dreamy, Vintage)
- **11 Übergänge pro Schnitt** (CapCut-artig): runder Knopf zwischen zwei Clips öffnet ein Sheet mit
  Keiner, Schwarz, Blitz, Zoom rein, Zoom raus, Drehen, Wisch links/rechts/hoch, Wackeln, Glitch –
  Dauer 0,1–1,5 s (Standard 0,3 s), "Auf alle anwenden". Alles "Edge"-Übergänge: der ausgehende Clip
  animiert in seiner letzten Hälfte, der eingehende in seiner ersten – die Videolänge ändert sich nicht.
- **Musik**: eigener Titel unterlegbar, Lautstärke regelbar (`GainAudioProcessor`), automatisch auf Videolänge gekürzt
- **KI-Vorlagen** setzen Tempo, Filter, Trim und Übergang an allen Schnitten:
  - regelbasiert: Quick Cuts (Zoom-Punch), Highlight (Wischer), Cinematic (kurze Schwarzblende),
    Retro Vibes (Blitz), Hype Reel (Glitch, 1.25x), Spin Edit (Drehen), Clean Cut
  - inhaltsbasiert per ML Kit Image Labeling (on-device): Action (Wackeln), Nature (Wischer),
    Portrait (Blitz), Food (Zoom)
- **Export** direkt in die Galerie (`Movies/ReelCut`), **Teilen** per System-Share-Sheet

## Architektur

```
model/      VideoClip (speed, captionText, transitionOut), Transition/TransitionType,
            CanvasFormat, FilterPreset
editing/    VideoExporter (Media3 Transformer-Pipeline), TransitionEffects (Geometrie + Farbe je Clip),
            FilterEffects, ColorMatrixEffects, SpeedEffects, TextOverlayEffects, AudioGain,
            ShareIntent, ThumbnailLoader
ai/         HeuristicTemplateEngine, ContentLabeler (ML Kit) + ContentAwareTemplateEngine
ui/editor   EditorScreen (Timeline, Vorlagen, Format, Filter, Musik), TransitionPicker
            (Knopf zwischen Clips + Auswahl-Sheet), EditorViewModel
```

## Setup

Voraussetzungen: **Android Studio** (aktuelle Version), JDK 17, ein Android-Gerät
oder Emulator mit **API 26+**.

1. Projekt in Android Studio öffnen (`Open` → diesen Ordner wählen).
2. Falls `gradle-wrapper.jar` fehlt: Android Studio bietet beim Sync einen
   **"Create Gradle Wrapper"**-Fix an – einfach bestätigen. Alternativ:
   ```bash
   gradle wrapper --gradle-version 8.9
   ```
3. Gradle-Sync abwarten, dann per USB (USB-Debugging aktiviert) oder WLAN-Debugging
   auf das Android-Handy deployen (Run ▶).

Media3 **1.9.4**. Alle verwendeten Transformer/Effect-APIs wurden gegen den echten
Quellcode auf GitHub geprüft, nicht nur aus dem Gedächtnis geschrieben.

### Stolperfallen, die beim On-Device-Testing aufgefallen sind (alle gefixt)

Die Codestellen tragen ausführliche Kommentare, hier die Kurzfassung:

1. **HDR-Aufnahmen (HLG/10-bit) wurden falsch eingefärbt.** Beide "richtigen"
   Tonemapping-Modi lieferten auf dem Testgerät Falschfarben; Fix:
   `HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR` (`VideoExporter.kt`).
2. **Eigene `RgbMatrix`-Effekte müssen column-major sein** – row-major ergab einen
   Grün/Gelb-Stich statt Graustufen (`ColorMatrixEffects.kt`).
3. **Effekte bekommen Zeitstempel relativ zum gesamten Video, nicht zum Clip**
   (Media3 addiert den Clip-Offset vor der Verarbeitung). Der alte Fade rechnete pro
   Clip ab 0 und machte dadurch ab Clip 2 den kompletten Rest schwarz. Die Übergänge
   kalibrieren sich jetzt auf den ersten Frame, den sie sehen (`TransitionEffects.kt`).
4. **`EditedMediaItem.setSpeed` brachte Bild und Ton auseinander** (Clip 2 startete
   ~130 ms zu früh); stattdessen `SpeedChangeEffect` + `SonicAudioProcessor` (`SpeedEffects.kt`).

**Bekannte Einschränkung:** Getrimmte Clips aus Quellen *mit B-Frames* (z. B. manche
heruntergeladenen oder bereits bearbeiteten Videos) verlieren die letzten ~3 Frames vor
dem Trim-Ende – das zeigt sich als ~0,1 s Standbild, Bild und Ton bleiben synchron.
Videos aus der Samsung-Kamera haben keine B-Frames und sind nicht betroffen.

## Nächste Schritte / Ideen (CapCut-Richtung)

- Echte Überblendung ("Mix") und Schiebe-Übergänge mit beiden Clips gleichzeitig im Bild –
  braucht Media3s Multi-Sequence-Compositing (zwei Video-Spuren mit Lücken + Alpha pro Frame)
- Tempo-Kurven (Montage, Hero, Bullet, Flash in/out) über einen variablen `SpeedProvider`
- Text-Animationen und -Stile, automatische Untertitel
- Beat-Sync: Schnitte automatisch auf die Musik legen
- Unscharfer Hintergrund statt Zuschnitt bei Format-Wechsel (CapCut "Canvas Blur")
- Automatisches Lautstärke-Ducking der Musik bei Sprache
- Vorschau-Player im Editor, damit man Übergänge vor dem Export sieht
