# Reel Cut

Native Android-App (Kotlin + Jetpack Compose) zum schnellen Schneiden und
Zusammenfügen von Kurzvideos für Instagram — Trimmen, Tempo, Filter, Text,
Musik, exportieren, teilen.

## Funktionsumfang (v0.2)

- **Videos auswählen** über den System-Photo-Picker (kein Storage-Permission-Dialog nötig)
- **Timeline**: Clips per Pfeil-Buttons neu anordnen, einzeln entfernen
- **Trimmen** pro Clip über einen Start/End-Range-Slider
- **Geschwindigkeit** pro Clip (0.25x–3x), Video (`SpeedChangeEffect`) und Audio (`SonicAudioProcessor`) bleiben synchron
- **Text-Overlay** pro Clip (unteres Drittel, `TextOverlay`/`OverlayEffect`)
- **Filter** (Original, Vibrant, Schwarz-Weiß, Warm, Moody) – als Media3-GL-Effekte beim Export angewendet
- **Übergänge**: Schnitt · Fade to Black (eigener zeitbasierter `RgbMatrix`-Effekt – ein echter Video-Crossfade/Dissolve bräuchte Media3s noch experimentelles Multi-Sequence-Compositing, daher diese robuste Alternative)
- **Musik**: eigener Titel unterlegbar, Lautstärke regelbar (`GainAudioProcessor`, eigener `BaseAudioProcessor` für PCM16-Gain), automatisch auf Videolänge gekürzt. *(v1: konstante Lautstärke, kein automatisches Sprach-Ducking – siehe Nächste Schritte)*
- **KI-Vorlagen**:
  - v1, regelbasiert (`HeuristicTemplateEngine`): Schnitttempo + Filter aus Clip-Anzahl/-Länge
  - v2, inhaltsbasiert (`ContentAwareTemplateEngine` + `ContentLabeler`): ML Kit Image Labeling (on-device, offline, kein API-Key) auf dem ersten Frame jedes Clips, erkennt z. B. Personen/Sport/Essen und schlägt passend Tempo+Filter+Speed vor – läuft asynchron nach, blockiert die UI nicht
- **Export**: Media3 Transformer trimmt, hängt Clips zusammen, rendert Filter/Text/Fades/Speed fest ein, mischt optional Musik dazu und speichert das MP4 direkt in die Galerie (`Movies/ReelCut`)
- **Teilen**: nach dem Export direkt per System-Share-Sheet (Instagram erscheint dort automatisch als Ziel)

## Architektur

```
model/      VideoClip (inkl. speed, captionText), FilterPreset, TransitionType
editing/    VideoExporter (Media3 Transformer-Pipeline: Trim, Speed, Fade, Text, Filter, Musik-Mix)
            FilterEffects, SpeedEffects, FadeEffects (eigene RgbMatrix), TextOverlayEffects,
            AudioGain (eigener BaseAudioProcessor), ShareIntent, ThumbnailLoader
ai/         TemplateEngine-Interface + HeuristicTemplateEngine (v1),
            ContentLabeler (ML Kit) + ContentAwareTemplateEngine (v2)
ui/home     Auswahl-Screen (Photo Picker)
ui/editor   Timeline (Trim/Speed/Text), Filter/Übergang/Musik, KI-Vorlagen, Export+Teilen
ui/navigation  Zwei Screens über eine gemeinsame, activity-scoped EditorViewModel-Instanz
```

## Setup

Voraussetzungen: **Android Studio** (aktuelle Version), JDK 17, ein Android-Gerät
oder Emulator mit **API 26+**.

1. Projekt in Android Studio öffnen (`Open` → diesen Ordner wählen).
2. Beim ersten Öffnen fehlt `gradle-wrapper.jar` (Binärdatei, hier bewusst nicht
   eingecheckt, da auf dieser Maschine kein Gradle/JDK installiert war, um sie
   zu erzeugen). Android Studio bietet dafür beim Sync einen
   **"Create Gradle Wrapper"**-Fix an – einfach bestätigen. Alternativ per
   Terminal, falls lokal ein Gradle installiert ist:
   ```bash
   gradle wrapper --gradle-version 8.9
   ```
3. Gradle-Sync abwarten, dann per USB (USB-Debugging aktiviert) oder WLAN-Debugging
   auf das Android-Handy deployen (Run ▶).

> **Hinweis:** Der Code wurde auf dieser Maschine geschrieben, aber **nicht
> kompiliert** – es ist weder JDK noch Android SDK noch Gradle installiert.
> Alle Media3-Transformer/Effect-APIs (`Effects`, `Composition`, `EditedMediaItemSequence`,
> `SpeedChangeEffect`, `RgbMatrix`, `OverlayEffect`, `TextOverlay`, `BaseAudioProcessor`, …)
> wurden gegen den echten Quellcode von **media3 1.6.1** (github.com/androidx/media,
> Tag `1.6.1`) geprüft, nicht nur aus dem Gedächtnis geschrieben. Die App selbst lief
> nie auf einem Gerät – beim ersten Sync/Run in Android Studio können trotzdem noch
> kleinere Korrekturen nötig sein.

## Nächste Schritte / Ideen

- Echter Video-Crossfade/Dissolve (Media3 `VideoCompositorSettings`, noch experimentell) statt Fade-to-Black
- Automatisches Lautstärke-Ducking der Musik bei Sprache (aktuell nur konstante Lautstärke)
- Sticker/Bild-Overlays zusätzlich zu Text
- KI-Vorlagen v3: Cloud-Analyse (Audio/Bewegung) für noch genauere Vorschläge
- Direktes Teilen als Instagram-Story (eigener Intent-Contract) statt generischem Share-Sheet
