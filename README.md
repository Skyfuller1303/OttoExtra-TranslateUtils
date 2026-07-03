# OttoExtra-TranslateUtils

Optionales Addon für [OttoExtra](https://github.com/Skyfuller1303/OttoExtra):
automatische Übersetzung von RP-Chatnachrichten über eine
LibreTranslate-kompatible API — ohne Emotes, Farbcodes oder geschützte
Textteile zu verändern.

**Minecraft 1.21.11 · Fabric · Client-only · benötigt OttoExtra ≥ 0.1.11**

## Funktionen

- Neuer Reiter **TranslateUtils** in den OttoExtra-Einstellungen
  (Sprachpaar, Anzeige, geschützte Inhalte, API)
- Sprachpaare: de/en/da/sv/pl/fr/es (LibreTranslate-Kürzel)
- Hotkey zum schnellen Ein-/Ausschalten (Optionen → Steuerung, Standard unbelegt)
- Chat-Ausgabe: `[DE>SV] übersetzte Nachricht (Originalnachricht)`
- Emotes `*...*`, Minecraft-Farbcodes, URLs und `[Tags]` werden vor der
  Übersetzung per Platzhalter (`XKEEP0X`) maskiert und unverändert wiederhergestellt
- Übersetzung läuft asynchron; bei API-Fehler oder Timeout wird immer die
  Originalnachricht gesendet
- Config: `config/ottoextra-translateutils.json`

## Installation

`OttoExtra-TranslateUtils-<version>.jar` zusammen mit OttoExtra in den
`mods`-Ordner legen. Ohne OttoExtra lädt das Addon nicht.

## Bauen

```bash
./gradlew build
```

Fertige JAR liegt danach in `dist/`. Die Basis-Mod zum Kompilieren liegt als
`libs/ottoextra-<version>.jar` im Repository; bei neuer OttoExtra-Version JAR
tauschen und `ottoextra_version` in `gradle.properties` anpassen.

## Dev-Client

```bash
./gradlew runClient
```

Startet Minecraft mit OttoExtra + Addon aus dem Dev-Classpath.
