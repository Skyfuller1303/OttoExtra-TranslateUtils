# Changelog

## [0.1.2] – 2026-07-15

### Geändert

- Die Übersetzungs-API wurde von `translate.skyfuller.de` auf `translate.ottoextra.dev` umgestellt.
- Bereits gespeicherte Konfigurationen mit der alten Übersetzungs-Domain werden automatisch migriert.

## [0.1.1] – 2026-07-15

### Hinzugefügt

- Rechts neben der Chat-Eingabe wird das aktuelle Sprachpaar angezeigt, beispielsweise `[DE>SV]`.
- Ein Klick auf den Sprachindikator schaltet die Übersetzung direkt ein oder aus.
- Der Indikator zeigt den Zustand farblich an: grün für aktive Übersetzung, grau für den aktuellen Kanal inaktiv und rot für ausgeschaltet.
- Die Chat-Eingabe reserviert Platz für den Sprachindikator, sodass Text nicht mehr dahinter weiterläuft.

### Geändert

- Übersetzte RP-Nachrichten und das zugehörige Original werden kanalverträglich getrennt versendet.
- Originaltexte werden als eigene vollständige `((...))`-Nachricht gesendet, damit der Server sie zuverlässig als OOC erkennt.
- Mehrteilige Nachrichten aus OttoExtras LongChat-Versand werden vor der Übersetzung wieder zusammengeführt.
- Mindestversion von OttoExtra auf `0.1.13.2-a` angehoben.

### Behoben

- Bereits vorhandene `((...))`-Passagen werden nicht übersetzt und bleiben beim Senden unverändert.
- Emotes, Farbcodes, URLs, Tags und weitere geschützte Textteile bleiben auch bei längeren Nachrichten erhalten.
- Bei deaktivierter Übersetzung oder nicht unterstützten Kanälen wird die Originalnachricht zuverlässig unverändert gesendet.

## [0.1.0] – 2026-07-15

### Hinzugefügt

- Erstveröffentlichung als optionales OttoExtra-Addon für automatische RP-Chat-Übersetzungen über eine LibreTranslate-kompatible API.
- Konfiguration von Quell- und Zielsprache, API-Endpunkt, Timeout und geschützten Textbereichen.
- Asynchrone Übersetzung mit sicherem Fallback auf die Originalnachricht bei API-Fehlern.
- Eigener TranslateUtils-Reiter in den OttoExtra-Einstellungen und optionaler Hotkey zum Ein- und Ausschalten.
