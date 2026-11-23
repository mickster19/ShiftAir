# ShiftAir (Offline Android App)

App ya Android yenye:
- **Transfer**: Tuma na pokea faili bila intaneti kwa Wi-Fi Direct + TCP Socket.
- **Video Player**: Orodha ya video zote simuni + Media3 ExoPlayer.
- **Audio Player**: Orodha ya nyimbo + Mini-Player chini.
- **File Manager**: Vinjari folda zote simuni na fungua faili.

## Jinsi ya ku-build APK moja kwa moja kwa Termux + GitHub Actions

1. **Fungua Termux** na nenda kwenye folda uliyotoa (unzip) `ShiftAir`:
   ```bash
   cd ~/storage/downloads/ShiftAir
   git init
   git add .
   git commit -m "Initial ShiftAir source code"
   git branch -M main
   git remote add origin https://github.com/USERNAME_YAKO/ShiftAir.git
   git push -u origin main
   ```
2. **Nenda GitHub kwenye repo yako (`ShiftAir`)**:
   - Fungua tab ya **Actions**.
   - Workflow ya **Build ShiftAir Debug APK** itaanza yenyewe na kuunda APK.
   - Ikikamilika (alama ya kijani ✅), shuka chini kwenye **Artifacts** na pakua **ShiftAir-Debug-APK**.

## Jinsi ya kuijenga kwenye Android Studio (Kompyuta)
1. Fungua Android Studio (Hedgehog au zaidi) → **Open** → chagua folda ya `ShiftAir`.
2. Ruhusu **Gradle Sync** ikamilike.
3. Bofya **Run ▶** kusakinisha kwenye simu yako.
