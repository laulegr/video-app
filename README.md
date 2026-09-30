# Reel Cut

Native Android-App (Kotlin + Jetpack Compose) zum schnellen Schneiden und
Zusammenfügen von Kurzvideos für Instagram — Trimmen, Reihenfolge ändern,
Filter drauf, exportieren, teilen.

## Funktionsumfang (v0.1)

- **Videos auswählen** über den System-Photo-Picker (kein Storage-Permission-Dialog nötig)
- **Timeline**: Clips per Pfeil-Buttons neu anordnen, einzeln entfernen
- **Trimmen** pro Clip über einen Start/End-Range-Slider
- **Filter** (Original, Vibrant, Schwarz-Weiß, Warm, Moody) – als Media3-GL-Effekte beim Export angewendet
- **Übergänge**: Schnitt (aktiv) · Überblendung (UI vorhanden, als "bald verfügbar" markiert – Media3s Video-Compositing für Crossfades ist noch experimentell)
- **KI-Vorlagen (v1, regelbasiert)**: schlägt basierend auf Clip-Anzahl/-Länge ein Schnitttempo + Filter vor (`ai/TemplateEngine.kt`), hinter einem `TemplateEngine`-Interface, damit später ein echtes on-device/cloud-Modell eingesetzt werden kann, ohne UI/ViewModel anzufassen
- **Export**: Media3 Transformer trimmt, hängt Clips zusammen, rendert die Filter fest ein und speichert das MP4 direkt in die Galerie (`Movies/ReelCut`) – von dort aus normal zu Instagram teilbar

## Architektur

```
model/      VideoClip, FilterPreset, TransitionType – reine Datenklassen
editing/    VideoExporter (Media3 Transformer-Pipeline), FilterEffects (Preset -> Media3 Effects),
            ThumbnailLoader (MediaMetadataRetriever für Dauer + Vorschaubild)
ai/         TemplateEngine-Interface + HeuristicTemplateEngine (v1)
ui/home     Auswahl-Screen (Photo Picker)
ui/editor   Timeline, Filter/Übergang-Auswahl, KI-Vorlagen, Export-Flow (EditorViewModel + EditorScreen)
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
> Die Media3-Transformer-APIs (`Effects`, `Composition`, `EditedMediaItemSequence`)
> sind nach bestem Wissen für **media3 1.4.1** eingesetzt; falls Android Studio
> beim ersten Sync einzelne Imports/Signaturen bemängelt, sind das kleine,
> lokal schnell zu fixende Abweichungen zur tatsächlich aufgelösten Version –
> kein struktureller Umbau.

## Nächste Schritte / Ideen

- Echte Crossfade-/Musik-Übergänge (Media3 `VideoCompositorSettings` bzw. eigener GL-Effekt)
- Musik-Spur unterlegen, Lautstärke-Ducking
- Text-/Sticker-Overlays
- KI-Vorlagen v2: Inhaltsbasierte Vorschläge (z. B. on-device ML Kit Objekt-/Szenenerkennung
  oder ein Cloud-Call, der tatsächlich Frames/Audio analysiert statt nur Clip-Metadaten)
- Direkter "Share to Instagram"-Intent statt nur "in Galerie speichern"
