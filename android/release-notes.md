**Catgirl Downloader for Android 1.0.1** (Android 12+). Installs over 1.0.0.

### Fixed
- Sometimes the next image took a long time to appear, even after staying on the previous one for a
  while. This happened when the server was slow or the background download stalled. Now, if the
  preloaded image isn't ready within 1.5 s of tapping, a fresh download starts in parallel and
  whichever finishes first is shown; the other one becomes the next preloaded image.
- A failed background preload is retried right away instead of waiting for the next tap.
- Stalled connections are detected after 15 s without data instead of 30 s.

---

**Catgirl Downloader na Androida 1.0.1** (Android 12+). Instaluje się na wersję 1.0.0.

### Poprawki
- Czasem trzeba było długo czekać na następny obraz, nawet po dłuższym oglądaniu poprzedniego.
  Działo się tak, gdy serwer odpowiadał wolno albo pobieranie w tle się zawiesiło. Teraz, jeśli obraz
  z tła nie jest gotowy w ciągu 1,5 s od tapnięcia, równolegle startuje nowe pobieranie i pokazany
  zostaje ten, który przyjdzie pierwszy; drugi staje się kolejnym obrazem z wyprzedzeniem.
- Nieudane pobieranie w tle jest od razu ponawiane, zamiast czekać na tapnięcie.
- Zawieszone połączenie jest wykrywane po 15 s bez danych zamiast 30 s.
