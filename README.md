# Brunei Azan — F-Droid test repo

This orphan branch hosts a self-signed F-Droid repository for test builds.
It is not part of the app source (see `main`).

Add this repo in the F-Droid client (Settings → Repositories → +):

```
https://raw.githubusercontent.com/Qoyyuum/brunei-azan/fdroid/repo?fingerprint=B49BDFF6D2749F8F07ED4D6B280156C43443A943B46CB7537C010C8DE8A25D8D
```

| Package                         | versionCode | Device      |
| ------------------------------- | ----------- | ----------- |
| `it.anak.bruneiazan`            | 1           | Phone       |
| `it.anak.bruneiazan`            | 10002       | Wear OS     |
| `it.anak.bruneiazan`            | 20001       | Android TV  |
| `it.anak.bruneiazan.watchface`  | 20001       | Wear OS     |

F-Droid picks the right build per device from the `uses-feature`
declarations (`android.hardware.type.watch`, `android.software.leanback`).
