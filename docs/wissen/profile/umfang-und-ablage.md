# Profil: Umfang, Ablage, Bedienung

Stand: 2026-11-19 · Quelle: Vorgabe des Nutzers (Brainstorming). Technische Machbarkeit je Punkt noch **ungeprüft**.

## Was ein Profil enthält
Konfiguration der **Toolfenster** (Seitenleisten), je Toolfenster:
- **Anker/Seite:** left / right / bottom / top.
- **Split-Position:** z. B. left/top vs. left/bottom (oberer oder unterer Bereich einer Seitenleiste).
- **Sichtbarkeit:** offen oder zu.
- **ViewMode:** Dock Pinned, Dock Unpinned, Undock, Float, Window.
- **Position/Größe des Fensters**, falls abgedockt (Undock/Float/Window).

Beispiel: „Explorer“ left/top und „Commit“ left/bottom müssen beim Laden genau so wiederhergestellt werden, inkl. offen/zu.

Nicht im Umfang (nicht genannt): Editor-Splits, offene Dateien, Hauptfenster-Größe.

## Ablage
- **Global** (nicht pro Projekt), Format **JSON**.

## Fehlende Toolfenster
- Toolfenster, die im Profil stehen, aber im aktuellen Projekt nicht existieren, werden beim Laden **still übersprungen**.

## Bedienung
- Untermenü **`Window > Profile`** (bestehendes Menü): Liste der Profile zum Wechseln, „Speichern als…“, „Verwalten…“ (Dialog: umbenennen, löschen).

## Offen
- Shortcuts (zunächst nicht nötig).
