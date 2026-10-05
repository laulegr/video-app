# Reel Cut

Native Android-App (Kotlin + Jetpack Compose) zum schnellen Schneiden und
Zusammenfügen von Kurzvideos für Instagram — Trimmen, Tempo, Filter, Text,
Musik, exportieren, teilen.

Getestet auf einem echten Gerät (Samsung Galaxy S25, Android 16) per adb/uiautomator —
nicht nur Code-Review. Build + Deploy + Export wurden end-to-end verifiziert.

## Funktionsumfang (v0.3)

- **Videos auswählen** über den System-Photo-Picker (kein Storage-Permission-Dialog nötig)
- **Timeline**: Clips per Pfeil-Buttons neu anordnen, einzeln entfernen, komplette Editor-Seite
  scrollbar (Filter/Übergang/Musik/Export-Button passen bei vielen Clips sonst nicht auf den Screen)
- **Trimmen** pro Clip über einen Start/End-Range-Slider
- **Geschwindigkeit** pro Clip (0.25x–3x), Video (`SpeedChangeEffect`) und Audio (`SonicAudioProcessor`) bleiben synchron
- **Text-Overlay** pro Clip (unteres Drittel, `TextOverlay`/`OverlayEffect`)
- **10 Filter** (Original, Vibrant, Noir, Sepia, Warm, Cool, Moody, Neon, Dreamy, Vintage) – als Media3-GL-Effekte beim Export angewendet
- **Übergänge**: Schnitt · Fade to Black (eigener zeitbasierter `RgbMatrix`-Effekt – ein echter Video-Crossfade/Dissolve bräuchte Media3s noch experimentelles Multi-Sequence-Compositing, daher diese robuste Alternative)
- **Musik**: eigener Titel unterlegbar, Lautstärke regelbar (`GainAudioProcessor`, eigener `BaseAudioProcessor` für PCM16-Gain), automatisch auf Videolänge gekürzt. *(v1: konstante Lautstärke, kein automatisches Sprach-Ducking – siehe Nächste Schritte)*
- **KI-Vorlagen** (6 Stück + bis zu 1 inhaltsbasierte extra):
  - v1, regelbasiert (`HeuristicTemplateEngine`): Quick Cuts, Highlight, Cinematic (Fade-to-Black), Retro Vibes, Hype Reel, Clean Cut – aus Clip-Anzahl/-Länge
  - v2, inhaltsbasiert (`ContentAwareTemplateEngine` + `ContentLabeler`): ML Kit Image Labeling (on-device, offline, kein API-Key) auf dem ersten Frame jedes Clips, erkennt Action/Nature/Portrait/Food und schlägt passend Tempo+Filter+Übergang vor – läuft asynchron nach, blockiert die UI nicht
- **Export**: Media3 Transformer trimmt, hängt Clips zusammen, rendert Filter/Text/Fades/Speed fest ein, mischt optional Musik dazu und speichert das MP4 direkt in die Galerie (`Movies/ReelCut`)
- **Teilen**: nach dem Export direkt per System-Share-Sheet (Instagram erscheint dort automatisch als Ziel)

## Architektur

```
model/      VideoClip (inkl. speed, captionText), FilterPreset (10 Presets), TransitionType
editing/    VideoExporter (Media3 Transformer-Pipeline: HDR-Modus, Trim, Speed, Fade, Text, Filter, Musik-Mix)
            FilterEffects, ColorMatrixEffects (Grayscale/Sepia), SpeedEffects, FadeEffects (eigene RgbMatrix),
            TextOverlayEffects, AudioGain (eigener BaseAudioProcessor), ShareIntent, ThumbnailLoader
ai/         TemplateEngine-Interface + HeuristicTemplateEngine (v1),
            ContentLabeler (ML Kit) + ContentAwareTemplateEngine (v2)
ui/home     Auswahl-Screen (Photo Picker)
ui/editor   Scrollbare Timeline (Trim/Speed/Text), Filter/Übergang/Musik, KI-Vorlagen,
            fixierter Export-Button (Scaffold bottomBar, navigationBarsPadding)
ui/navigation  Zwei Screens über eine gemeinsame, activity-scoped EditorViewModel-Instanz
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

### Bekannte, bereits gefixte Stolperfallen (für's nächste Mal)

Zwei nicht offensichtliche Bugs sind beim echten On-Device-Testing aufgefallen –
beide Codestellen tragen ausführliche Kommentare, hier die Kurzfassung:

1. **HDR-Aufnahmen (HLG/10-bit, z. B. Samsung-Standardkamera) wurden beim Export
   falsch eingefärbt**, unabhängig von Filtern. Media3s "richtige" Tonemapping-Modi
   (`..._USING_OPEN_GL`, `..._USING_MEDIACODEC`) produzierten auf dem Testgerät
   beide Falschfarben. Fix: `Composition.HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR`
   (siehe `VideoExporter.kt`) – laut Doku "likely washed out", sah im Test aber
   korrekt aus.
2. **Eigene `RgbMatrix`-Effekte (Grayscale/Sepia) waren transponiert.** Media3
   erwartet die 16 Floats **column-major**; row-major geschrieben ergab
   `out_r=0.299·(r+g+b)`, `out_g=0.587·(r+g+b)`, `out_b=0.114·(r+g+b)` statt dreier
   gleicher Grauwerte – sichtbar als starker Grün/Gelb-Farbstich. Siehe
   `ColorMatrixEffects.kt`.

## Nächste Schritte / Ideen

- Echter Video-Crossfade/Dissolve (Media3 `VideoCompositorSettings`, noch experimentell) statt Fade-to-Black
- Automatisches Lautstärke-Ducking der Musik bei Sprache (aktuell nur konstante Lautstärke)
- Sticker/Bild-Overlays zusätzlich zu Text
- KI-Vorlagen v3: Cloud-Analyse (Audio/Bewegung) für noch genauere Vorschläge
- Direktes Teilen als Instagram-Story (eigener Intent-Contract) statt generischem Share-Sheet
