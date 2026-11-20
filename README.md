# ShiftAir 🚀

App ya Android yenye features nyingi: **Transfer** (kama Xender, bila intaneti), **Video Player**, **Audio Player**, na **File Manager**.

## Features
- 📡 **Transfer** — tuma/pokea faili kati ya vifaa viwili kupitia **Wi-Fi Direct** (hakuna intaneti/data inayotumika kabisa)
- 🎬 **Video Player** — orodha ya video zote zilizopo simuni + player ya ExoPlayer
- 🎵 **Audio Player** — orodha ya nyimbo + mini-player chini
- 📁 **File Manager** — vinjari folda za simu na fungua faili yoyote

## Jinsi ya kuijenga (build)

1. Pakua/nakili folda hii yote (`ShiftAir/`) kwenye kompyuta yako
2. Fungua **Android Studio** (toleo la hivi karibuni linapendekezwa, Hedgehog au zaidi)
3. Chagua **Open** → chagua folda ya `ShiftAir`
4. Android Studio itaomba "Gradle Sync" — ruhusu ifanye kazi (itahitaji intaneti kupakua Gradle na maktaba mara ya kwanza tu)
5. Ikiwa itaomba "Gradle Wrapper" ambayo haipo kikamilifu, bofya **"OK" / "Generate Wrapper"** itakapoulizwa — Android Studio itaijenga yenyewe
6. Unganisha simu yako ya Android (au tumia emulator) kwa **USB debugging** ikiwashwa
7. Bofya **Run ▶** — app itasakinishwa na kuanza kwenye simu

## Muundo wa msimbo (source code)

```
app/src/main/java/com/shiftair/app/
├── MainActivity.kt              → shell kuu, bottom navigation
├── wifidirect/
│   ├── WifiDirectManager.kt      → discovery + connection ya Wi-Fi Direct
│   ├── WiFiDirectBroadcastReceiver.kt
│   └── TransferService.kt       → kutuma/kupokea faili kwa socket
├── ui/transfer/TransferFragment.kt
├── ui/video/VideoFragment.kt + VideoPlayerActivity.kt
├── ui/audio/AudioFragment.kt
├── ui/files/FilesFragment.kt
├── adapters/                    → RecyclerView adapters (Peer, Media, File)
├── model/                       → data classes
└── util/MediaStoreHelper.kt     → kusoma video/audio kutoka MediaStore
```

## Jinsi Transfer inavyofanya kazi (bila intaneti)

1. Vifaa vyote viwili vinafungua tab ya **Transfer** → app inatafuta vifaa jirani kwa Wi-Fi Direct (`discoverPeers`)
2. Mtumiaji A anachagua kifaa cha B kwenye orodha → simu mbili zinaunganishwa moja kwa moja (P2P)
3. A anabofya **Tuma** na kuchagua faili → B anabofya **Pokea**
4. Faili inapita kwa socket ya TCP moja kwa moja kati ya simu mbili (bila router, bila intaneti)
5. Faili zilizopokewa zinahifadhiwa kwenye: `Android/data/com.shiftair.app/files/Downloads/ShiftAir/`

## Vitu unavyoweza kuongeza baadaye
- Uhamisho wa folda nzima (si faili moja tu)
- QR code ya kuunganisha vifaa haraka zaidi
- Historia ya faili zilizotumwa/kupokewa (Room database)
- Equalizer kwenye Audio Player
- Dark mode kamili
- Kuonyesha thumbnail za video kwenye orodha (Glide/Coil)

## Ruhusa (Permissions) zinazotumika
Zote ni za **ndani ya simu** — hakuna `INTERNET` permission popote katika app hii:
- Wi-Fi state + Nearby Wi-Fi Devices (kwa Transfer)
- Media (Video/Audio/Images) za kusoma faili zilizopo
- Location (inahitajika na mfumo wa Android kwa Wi-Fi Direct scanning)
