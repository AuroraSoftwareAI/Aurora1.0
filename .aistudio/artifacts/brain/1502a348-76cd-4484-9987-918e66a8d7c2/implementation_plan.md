# Modernizacja Aurora AI: Tryb Przerywania Live, Dyktowanie STT w Czacie, Usunięcie Trybu Wsparcia, Pamięć Adaptacyjna i Naturalne Głosy

Kompleksowa transformacja interakcji głosowych i zarządzania kontekstem w Aurora AI: wprowadzenie natychmiastowego przerywania wypowiedzi w trybie Live, bezpośrednie dyktowanie mowy (STT) do pola wprowadzania wiadomości w oknie czatu, usunięcie sztucznego podziału na Tryb Wsparcia na rzecz jednolitego charakteru asystenta, wprowadzenie automatycznego profilu stylu adaptacyjnego w lokalnej pamięci dla nowych niezależnych rozmów oraz rozszerzenie palety głosów TTS o naturalne profile brzmienia i selektor silnika mowy.

## User Review & Critical Decisions

> [!IMPORTANT]
> Kluczowe decyzje potwierdzone przez użytkownika w procesie doprecyzowania wymagań:

- **Tryb przerywania w Live Mode**: Zawsze aktywny z natychmiastowym przerwaniem i wygaszeniem audio syntezatora mowy (TTS) w ułamku sekundy po wykryciu głosu użytkownika, natychmiast przełączając układ w stan aktywnego słuchania nowej wypowiedzi.
- **Zwykły mikrofon w oknie czatu**: Przycisk mikrofonu w wierszu wprowadzania wiadomości realizuje bezpośrednie dyktowanie STT do pola tekstowego (`TextField`), umożliwiając użytkownikowi swobodną korektę i wysłanie wiadomości zwykłym przyciskiem wyślij (zamiast otwierania pełnoekranowego widoku Live). Pełny tryb Live Voice pozostaje dostępny przez dedykowany przycisk FAB / ikonę Live.
- **Usunięcie Trybu Wsparcia**: Całkowita likwidacja przełącznika "Tryb Swobodny / Tryb Wsparcia", etykiet i związanych z nim mechanizmów zgód. Czat działa domyślnie wg autentycznych, elastycznych zasad asystenta (dopasowanie do stylu, slang, poczucie humoru bez konieczności nazywania go trybem swobodnym).
- **Pamięć adaptacyjna i niezależne rozmowy**: Każdy nowy czat to nowa, niezależna rozmowa (historia wiadomości w czacie działa w obrębie bieżącej sesji). Globalny kontekst dla nowych czatów tworzą: (1) wpisy oznaczone jako "do zapamiętania" w Bazie Wiedzy oraz (2) automatycznie generowany i aktualizowany na bieżąco oraz po zakończeniu rozmowy profil stylu adaptacyjnego asystenta.
- **Naturalne głosy TTS**: Rozszerzenie konfiguracji o predefiniowane profile brzmieniowe (np. *Aurora Naturalna*, *Ciepły i Spokojny*, *Ekspresyjny*, *Radiowy*) oraz dynamiczną listę zainstalowanych w systemie głosów TTS z możliwością odsłuchania próbki audio bezpośrednio w aplikacji.

---

## 1. Overview & Core Concept

Aurora AI staje się płynnym, wysoce responsywnym kompanem konwersacyjnym, który eliminuje bariery między tekstem a mową:
1. **Prawdziwie konwersacyjny Live Voice**: Użytkownik nie musi czekać, aż asystent skończy długą wypowiedź – rozpoczęcie mówienia natychmiast ucisza syntezator i przełącza strumień w tryb aktywnego nasłuchu.
2. **Szybkie dyktowanie wiadomości (Dictation STT)**: Mikrofon w polu tekstowym transkrybuje mowę na żywo do pola wpisywania, dając pełną kontrolę nad edycją tekstu przed wysłaniem.
3. **Spójny, jednolity charakter**: Rezygnacja ze sztywnych etykiet trybów; model naturalnie adoptuje pożądany ton, dowcip i bezpośredniość.
4. **Inteligentny kontekst bez zanieczyszczeń**: Czat zachowuje czyste granice pomiędzy kolejnymi rozmowami, jednocześnie niosąc ze sobą najważniejszą wiedzę użytkownika oraz wyuczony profil stylistyczny.
5. **Większa ekspresja głosu**: Naturalnie brzmiące profile modulacji i łatwy wybór spośród najlepszych głosów systemowych Android TTS.

---

## 2. User Experience & Visual Design

### Ścieżka użytkownika (User Flows)

1. **Dyktowanie STT w oknie czatu (`ChatScreen`)**:
   - Użytkownik widzi ikonę mikrofonu bezpośrednio przy polu wprowadzania tekstu.
   - Dotknięcie mikrofonu uruchamia dyktowanie (ikona pulsuje na turkusowo/czerwono, pole wyświetla rozpoznawany na żywo tekst).
   - Kolejne dotknięcie lub naturalna pauza zatwierdza tekst w polu `TextField`. Użytkownik może dopisać treść lub kliknąć przycisk "Wyślij".
   - Pełnoekranowy tryb Live uruchamia się przez przycisk czatu głosowego w prawym dolnym rogu okienka czatu nad wierszem wpisywania oraz przycisk LIVE w górnym pasku aplikacji (usunięto nakładający się pływający FAB).

2. **Interruption Mode w Live Voice (`LiveVoiceScreen`)**:
   - Gdy model mówi (pulsujący orb w stanie `MODEL_SPEAKING`), użytkownik po prostu zaczyna mówić.
   - Detekcja mowy (`SpeechRecognizer` / RMS detektor) w ułamku sekundy wysyła sygnał przerwania (`cancel`), wycisza TTS (`tts.stop()`) i przełącza orb w stan `USER_SPEAKING` z rejestracją nowych słów.
   - Na ekranie pojawia się subtelny indykator "Przerwano – słucham Cię...".

3. **Uproszczony interfejs bez Trybu Wsparcia**:
   - Usunięto z paska górnego czatu i ekranu Live chipy przełączania trybów.
   - W `SettingsScreen` usunięto kartę wyboru trybów oraz kartę "Pamięć trybu wsparcia". Pozostaje przejrzysty panel konfiguracji osobowości (humor, bezpośredniość, temperatura) oraz karta bazy wiedzy i pamięci.

4. **Baza Wiedzy & Profil Adaptacyjny (`KnowledgeScreen`)**:
   - Zakładka 1: **Baza Wiedzy** – źródła i fakty wyraźnie dodane przez użytkownika ("do zapamiętania").
   - Zakładka 2: **Styl Adaptacyjny** – wygenerowany przez model profil tonu, słownictwa i sposobu formułowania myśli, automatycznie doskonalony w trakcie i po rozmowach.

5. **Wybór Naturalnych Głosów w Ustawieniach (`SettingsScreen`)**:
   - Sekcja **Głos i Brzmienie Asystenta**:
     - Kafelki predefiniowanych profili: *Aurora Naturalna*, *Ciepły i Spokojny*, *Ekspresyjny i Nowoczesny*, *Radiowy / Głęboki*, *Neutralny*.
     - Rozwijany selektor głosów silnika TTS zainstalowanych na urządzeniu (z filtrowaniem języków i jakości).
     - Przycisk `Odsłuchaj próbkę`, odtwarzający krótkie, naturalne zdanie testowe.

---

## 3. Key Product Decisions & Trade-Offs

- **Decyzja 1: Mikrofon w wierszu wprowadzania jako czysty STT, a nie Live Launcher**:
  - *Wybrane podejście*: Przycisk w wierszu wprowadzania uruchamia lokalny `SpeechRecognizerManager` w trybie dyktowania i zapisuje tekst do stanu `inputText`. Live Voice jest dostępny z dedykowanego FAB.
  - *Dlaczego*: Użytkownicy oczekują, że mikrofon w pasku pisania służy do podyktowania wiadomości tekstowej, którą mogą przeczytać i zredagować przed wysłaniem.
- **Decyzja 2: Natychmiastowe wygaszanie TTS przy wczesnej detekcji mowy**:
  - *Wybrane podejście*: W trybie Live mikrofon nasłuchuje równolegle (lub reaguje na zdarzenia początkowej mowy / RMS). Gdy użytkownik zaczyna mówić w trakcie odtwarzania TTS, natychmiast wywoływana jest metoda `ttsManager.stop()`, a stan orba przechodzi do `USER_SPEAKING`.
  - *Dlaczego*: Gwarantuje płynność dialogu zbliżoną do rozmowy telefonicznej z człowiekiem bez konieczności czekania na koniec generowania.
- **Decyzja 3: Izolacja historii sesji a ciągłość profilu stylu**:
  - *Wybrane podejście*: Każdy `sessionId` posiada własną, odrębną historię wiadomości wysyłaną do Gemini. Pomiędzy sesjami przenoszony jest wyłącznie profil stylu adaptacyjnego oraz aktywne elementy Bazy Wiedzy.
  - *Dlaczego*: Zgodnie z wytycznymi użytkownika, nowy czat to całkowicie czysta rozmowa, nieobciążona wątkami starych dyskusji, ale zachowująca wyuczony charakter asystenta i wiedzę bazową.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                               UI LAYER                                 │
│  ChatScreen (STT Dictation)  │  LiveVoiceScreen (Interruption VAD)    │
│  SettingsScreen (Voices)     │  KnowledgeScreen (Adaptive Profile)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                             ChatViewModel                              │
│  • handleDictationInput()         • onSpeechInterruptionDetected()     │
│  • updateAdaptiveStyleProfile()   • applyVoicePreset()                 │
└─────────────┬─────────────────────┴────────────────────┬───────────────┘
              │                                          │
┌─────────────▼───────────────────────────┐  ┌───────────▼───────────────┐
│              Audio Services             │  │       Data & AI Layer     │
│  • TextToSpeechManager:                 │  │  • GeminiRepository:     │
│    - stop() [instant cancel]            │  │    - Clean system prompt   │
│    - availableVoices, setVoice()        │  │    - Session-scoped chats │
│    - previewVoice()                     │  │    - Style summarizer     │
│  • SpeechRecognizerManager:             │  │  • Room Database:         │
│    - dictation mode vs live loop        │  │    - knowledge_sources    │
│    - onBeginningOfSpeech interrupt      │  │    - learned_facts (style)│
└─────────────────────────────────────────┘  └───────────────────────────┘
```

### Szczegóły implementacji komponentów:

1. **`TextToSpeechManager.kt`**:
   - Dodanie metody `getAvailableVoices(): List<Voice>` filtrującej głosy dla `Locale("pl", "PL")` oraz `Locale.getDefault()`.
   - Dodanie metody `setVoiceByName(name: String)`.
   - Zdefiniowanie struktury `VoiceProfile` (nazwa, opis, pitch, rate, preferowany typ głosu).
   - Wdrożenie natychmiastowego przerwania `cancelUtterance()` z wyzerowaniem kolejki dźwięków `QUEUE_FLUSH`.
   - Metoda `playPreview(sampleText: String)`.

2. **`SpeechRecognizerManager.kt`**:
   - Obsługa trybu jednorazowego dyktowania (STT dla czatu tekstowego) vs trybu ciągłego dialogu Live.
   - Emisja zdarzenia `onSpeechDetectedWhileModelSpeaking` umożliwiającego natychmiastowe odcięcie strumienia audio w ułamku sekundy.

3. **`ChatViewModel.kt`**:
   - Zarządzanie stanem dyktowania w czacie (`isDictatingText: StateFlow<Boolean>`).
   - Wdrożenie logiki przerwania wypowiedzi w Live Mode (`handleLiveInterruption()`).
   - Usunięcie `AuroraMode` i powiązanych struktur Trybu Wsparcia.
   - Wdrożenie funkcji `analyzeAndUpdateAdaptiveStyle()` analizującej styl konwersacji (zapisującej do lokalnej bazy danych cechy stylu w kategorii `FactCategory.STYLE`).
   - Przekazywanie do Gemini promptu łączącego: historię tylko bieżącego czatu + aktywną Bazę Wiedzy + profil stylu adaptacyjnego.

4. **`ChatScreen.kt` & `LiveVoiceScreen.kt`**:
   - Zmiana zachowania przycisku mikrofonu w `ChatScreen`: bezpośrednie nagrywanie do tekstu w polu edycji ze wskaźnikiem nagrywania.
   - Usunięcie kontrolek Trybu Wsparcia z pasków aplikacji i dialogów.
   - Dodanie w `LiveVoiceScreen` natychmiastowej reakcji na głos przerywający asystenta.

5. **`SettingsScreen.kt`**:
   - Usunięcie sekcji Trybu Wsparcia i pamięci wsparcia.
   - Dodanie sekcji wyboru i odsłuchu naturalnych głosów (profile brzmieniowe + lista głosów TTS + przycisk "Odsłuchaj próbkę").

6. **Weryfikacja**:
   - Sprawdzenie poprawności kompilacji projektu za pomocą `compile_applet`.
   - Uruchomienie testów jednostkowych w celu weryfikacji braku regresji.
