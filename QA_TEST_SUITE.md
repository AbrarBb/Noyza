# Noyza — QA Test Case Suite

Comprehensive quality assurance test cases for **Noyza**, covering all audio measurement pipelines, psychoacoustic suitability algorithms, activity profiles, predictive forecasting, GPS/places, history/analytics, widget, monetization, onboarding, accessibility, privacy, and cross-cutting regression.

Format: **ID | Title | Preconditions | Steps | Expected Result**

---

## 1. Audio Capture & Live Measurement

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| AUD-01 | Mic permission granted → live reading starts | App installed, permission not yet granted | Launch app → tap Start Measure | Permission dialog appears; on Allow, live dB gauge begins updating within 1s |
| AUD-02 | Mic permission denied | Fresh install | Deny mic permission | App shows a clear rationale/blocked state, no crash, no silent freeze |
| AUD-03 | Live gauge updates smoothly | Mic active | Speak loudly then go silent | Gauge reflects change via EMA (α=0.15) — no jarring jumps, no flicker |
| AUD-04 | RMS → dB conversion sanity | Quiet room (~30–40 dB actual, measured with reference meter) | Start measurement | Displayed dB within reasonable range of reference meter reading |
| AUD-05 | Calibration slider adjusts output | Live measurement running | Move calibration slider from 0 to +10 dB | Displayed dB increases by ~10 accordingly, in real time |
| AUD-06 | Calibration presets apply correctly | Settings/Calibration screen | Select each of 5 presets (Quiet Room, Modern Office, Smartphone Mic, Headset, Factory Default) | Correct offset value is applied and persisted after app restart |
| AUD-07 | Mic interrupted by phone call | Measurement in progress | Receive/answer a phone call | Measurement pauses gracefully, resumes or ends cleanly after call, no crash |
| AUD-08 | Backgrounding during measurement | Active session running | Press Home, wait 30s, return | Foreground service keeps measuring; data isn't lost; notification is visible while backgrounded |
| AUD-09 | Bluetooth headset mic switch | Active session, BT headset with mic connected | Start session using BT mic | App either uses BT mic input correctly or falls back to device mic without crashing |

---

## 2. Frequency Analysis (FFT / Psychoacoustic)

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| FFT-01 | Speech-heavy classification | Play recorded human speech near device | Run Quick Measure | Frequency profile classifies as SPEECH_HEAVY |
| FFT-02 | Low rumble classification | Play low-frequency hum (e.g., fan/AC noise) | Run Quick Measure | Classifies as LOW_RUMBLE |
| FFT-03 | Sharp clatter classification | Simulate clattering/clinking sounds | Run Quick Measure | Classifies as SHARP_CLATTER |
| FFT-04 | Balanced/mixed environment | Play mixed ambient noise (cafe recording) | Run Quick Measure | Classifies as BALANCED (or closest dominant category), no crash on mixed spectra |
| FFT-05 | Psychoacoustic penalty applied | Two rooms at equal dB: one speech-heavy, one low-hum | Measure both, same activity (e.g. Deep Work) | Speech-heavy room gets a lower suitability score than the low-hum room despite equal dB |
| FFT-06 | FrequencyProfileBar renders correctly | Any active session | Observe FrequencyProfileBar during session | Bar updates live, values sum/scale correctly, no visual overflow |
| FFT-07 | FFT performance/CPU load | Long session (30+ min) | Run continuous measurement for 30 minutes | No excessive battery drain/thermal throttling; app remains responsive |
| FFT-08 | Silence / near-zero input | Near-silent room (<20 dB) | Run FFT analysis | No divide-by-zero or NaN crash; returns a stable low-noise classification |

---

## 3. Suitability Scoring Engine

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| SUIT-01 | Ideal range → high score | Simulated steady 40 dB input, activity = Study (ideal 35–55) | Run measurement | Suitability score in high range (e.g. 85–100) |
| SUIT-02 | Above acceptable max → low score | Simulated 85 dB input, activity = Study (max 70) | Run measurement | Score drops sharply, environment flagged unsuitable |
| SUIT-03 | Stability component | Two sessions: one steady 45dB, one oscillating 30–60dB, same average | Compare scores | Steady session scores higher (25% stability weighting) |
| SUIT-04 | Peak spike penalty | Session with one sharp 90dB spike, otherwise quiet | Run session | Score reduced relative to spike-free session at same average (20% peak weighting) |
| SUIT-05 | Sustained exposure penalty | Session with prolonged (5+ min) loud noise vs brief loud burst | Compare scores | Sustained version scores lower (15% sustained-exposure weighting) |
| SUIT-06 | Weighted formula totals 100% | Code/unit level | Inspect scoring output components | Sum of weighted components matches final score for known test vectors |
| SUIT-07 | Custom activity profile scoring | Custom activity created with dB range 20–35, max 45 | Measure a 30 dB environment | Score reflects custom thresholds, not any built-in profile |
| SUIT-08 | Edge case: activity swap mid-session | Session running under "Study" | Switch activity to "Sleep" mid-session | Score recalculates against new profile without crashing or losing session data |

---

## 4. Activity Profiles (Built-in + Custom)

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| ACT-01 | All 10 built-in profiles selectable | Home screen | Tap through all 10 activity chips | Each loads correct icon, ideal range, and acceptable max per spec table |
| ACT-02 | Create custom activity | Custom activity sheet | Set name, icon, ideal min/max dB, acceptable max, spike sensitivity → Save | New activity appears in ActivitySelectorRow with "(+) Custom" origin, persists after restart |
| ACT-03 | Custom activity validation | Create custom activity sheet | Enter ideal min > ideal max | Form blocks save / shows validation error |
| ACT-04 | Custom activity validation — extreme values | Create custom activity sheet | Enter negative dB or >140 dB | Form rejects or clamps to realistic bounds |
| ACT-05 | Edit/delete custom activity | Existing custom activity | Edit thresholds, then delete | Changes persist correctly; deleted activity removed from selector and no longer referenced in old sessions (or gracefully handled if referenced) |
| ACT-06 | Custom activity in Explore compatibility matrix | Saved place, custom activity exists | Open Place Detail | Compatibility matrix includes custom activity alongside 10 built-ins |

---

## 5. Quick Measure & Active Session

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| SESS-01 | Quick Measure duration options | Home screen | Trigger Quick Measure, select 10s / 30s | Measurement runs for exact selected duration, then shows instant recommendation |
| SESS-02 | Active Session full flow | Home screen | Start Active Session, let run 2 min, stop | Foreground service starts/stops correctly; live graph renders; session saved to history |
| SESS-03 | Live noise graph accuracy | Active session running | Generate varying noise levels | Cubic-bezier graph reflects changes; spike markers appear at appropriate points; 65dB reference line visible |
| SESS-04 | High-noise alert trigger | Active session, activity with low acceptable max | Sustain noise above threshold for 3+ minutes | Push notification fires once threshold+duration condition met, not repeatedly spammed |
| SESS-05 | Session interrupted by app kill | Active session running | Force-stop app via OS task manager | On relaunch, session is either recovered/logged partially or cleanly discarded — no corrupted DB state |
| SESS-06 | Session Summary accuracy | Completed session with known synthetic data | View Session Summary | Avg/peak/min dB, stability score, and Quiet/Moderate/Loud/Very Loud percentage breakdown match expected calculated values |
| SESS-07 | Save Place from summary | Session Summary screen | Tap Save Place, enter name, tag GPS | Place saved with correct session-derived score and coordinates |

---

## 6. GPS / Location & Saved Places

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| GPS-01 | Location permission flow | Save Place sheet, no location permission yet | Tap "Tag GPS" | Permission prompt appears; on Allow, coordinates captured |
| GPS-02 | Location permission denied | Save Place sheet | Deny location permission | Place can still be saved without coordinates; no crash |
| GPS-03 | Haversine distance accuracy | Two places with known coordinates and known real-world distance | View Explore distance badges | Displayed distance matches expected value within reasonable rounding |
| GPS-04 | "Nearby" filter/sort | Multiple saved places, device location known | Apply Nearby filter in Explore | Places sorted ascending by distance from current location |
| GPS-05 | GPS unavailable indoors | Location services on but poor signal | Attempt GPS tag indoors | Graceful timeout/fallback message, doesn't hang indefinitely |
| GPS-06 | Duplicate place same coordinates | Existing saved place | Save a new place at (near-)identical coordinates | App allows it (or optionally warns of possible duplicate) without data corruption |

---

## 7. Explore, Ranking & Comparison

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| EXP-01 | Ranking badges correctness | 3+ saved places with different scores | Open Explore, "Best Overall" filter | #1/#2/#3 badges assigned in correct descending score order |
| EXP-02 | Quietest filter | Multiple places | Apply "Quietest" filter | Sorted by lowest average dB, not overall suitability score |
| EXP-03 | Most Stable filter | Multiple places with varying stability scores | Apply "Most Stable" filter | Sorted by stability metric correctly |
| EXP-04 | Category filter | Places tagged with categories | Apply Category filter | Only matching-category places shown |
| EXP-05 | Compatibility matrix full render | Place Detail screen | Open a saved place | All 10 built-in + any custom activities show a % compatibility value, none blank/NaN |
| EXP-06 | Place comparison (2–3 places) | 2–3 saved places selected | Open Compare view | Side-by-side metrics align correctly per place, no mismatched data |
| EXP-07 | Best Time to Visit card | Place with enough historical session data across different hours | Open Place Detail | Card shows quietest window, peak distraction window, and 24-hr heat bar consistent with underlying data |
| EXP-08 | Forecast with insufficient data | New place, 0–1 sessions logged | Open Place Detail | Forecast card shows a reasonable fallback (e.g., diurnal prior only) rather than crashing or showing empty/garbage data |

---

## 8. History & Analytics

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| HIST-01 | Session grouping by date | Sessions logged today, yesterday, and older | Open History | Correctly grouped under Today / Yesterday / date headers |
| HIST-02 | 7-day analytics accuracy | 7+ days of session data | Open Analytics | Weekly avg dB, suitability trend, best/noisiest day match underlying session data |
| HIST-03 | Analytics with sparse data | Only 1–2 sessions logged ever | Open Analytics | No crash; shows partial/limited-data state instead of broken charts |
| HIST-04 | CSV export | Existing session history | Trigger Export via FileProvider | Valid CSV generated with correct columns/rows, opens correctly in a spreadsheet app |
| HIST-05 | Full data wipe | Existing sessions, places, custom activities | Trigger "Delete all data" | All local data removed (Room DB cleared); app returns to fresh/empty state; no dangling references cause crashes |
| HIST-06 | Data wipe confirmation | Data wipe entry point | Tap wipe button | Confirmation dialog required before destructive action executes |

---

## 9. Home Screen Widget (Jetpack Glance)

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| WID-01 | Widget install | Widget picker | Add Noyza widget to home screen | Widget renders with current/last noise level and suitability status |
| WID-02 | Widget quick launch | Widget on home screen | Tap widget | Deep-links directly into measurement mode |
| WID-03 | Widget updates after app state change | Widget added, then a new session recorded in-app | Return to home screen | Widget reflects updated status without requiring manual refresh |
| WID-04 | Widget with no permission granted | Mic permission not yet granted, widget added | Tap widget | Routes to permission flow instead of crashing |

---

## 10. Monetization (AdMob + Billing)

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| MON-01 | Banner ad display (free tier) | Free/non-premium user | Navigate through main screens | Banner ads appear in designated ad slots only |
| MON-02 | No ads during active measurement | Active Session or Quick Measure running | Observe during measurement | Zero ads shown/triggered during live measurement, per spec |
| MON-03 | Interstitial frequency capping | Free user, repeated navigation | Trigger multiple screen transitions rapidly | Interstitials are capped (not shown on every transition) |
| MON-04 | Monthly subscription purchase | Paywall screen, sandbox/test billing account | Purchase monthly plan | Billing flow completes, premium features unlock, ads removed |
| MON-05 | Annual subscription purchase | Paywall screen | Purchase annual ("Best Value") plan | Correctly flagged/priced, unlocks same premium tier as monthly |
| MON-06 | Lifetime purchase | Paywall screen | Purchase lifetime option | One-time purchase unlocks premium permanently, no recurring billing |
| MON-07 | Remove Ads one-time purchase | Paywall or settings | Purchase Remove Ads only | Ads disabled but non-ad premium features (if any are gated separately) remain unaffected per design |
| MON-08 | Purchase restoration | Fresh install, previously purchased account signed in | Tap Restore Purchases | Correct entitlement restored instantly without re-purchase |
| MON-09 | Billing failure handling | Purchase flow | Simulate declined/cancelled payment | App shows clear error/cancellation state, no partial unlock, no crash |

---

## 11. Onboarding

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| ONB-01 | Full onboarding flow (5 steps) | Fresh install | Complete Welcome → Permission → Calibration → Activity → Notifications | Each step completes in order; progress indicator accurate (0..4 of 5); lands on Home after finishing |
| ONB-02 | Guided calibration accuracy | Onboarding Calibration step | Run 3-second ambient room check in a known-quiet room | Suggests an appropriate preset (e.g., Quiet Library ~35dB) close to actual conditions |
| ONB-03 | Calibration preset quick-select | Onboarding Calibration step | Tap each preset (Quiet Library, Normal Room, Active Space, Standard 0dB) | Correct offset applied; offset slider reflects chosen preset value |
| ONB-04 | Skip/back navigation | Any onboarding step | Attempt back navigation or skip (if available) | Behaves predictably — no orphaned state, no being stuck unable to proceed |
| ONB-05 | Permission denial during onboarding | Onboarding Permission step | Deny mic and/or location and/or notification permission | Onboarding still completes; app clearly explains reduced functionality rather than blocking entirely |
| ONB-06 | Re-triggering onboarding | Onboarding already completed once | Reinstall or clear app data | Onboarding runs again from Welcome screen |

---

## 12. Accessibility & Sensory Features

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| ACC-01 | Haptic spike alert toggle | Profile → Accessibility settings | Enable Haptic Spike Alerts, trigger a loud spike during session | Device vibrates with double-pulse pattern on spike detection |
| ACC-02 | Haptic alert disabled by default state respected | Toggle off | Trigger spike with haptics disabled | No vibration occurs |
| ACC-03 | Sensory-Friendly Mode | Profile settings | Enable Sensory-Friendly Mode | UI reduces motion/animation intensity app-wide |
| ACC-04 | TalkBack on LiveNoiseGaugeCard | TalkBack enabled | Focus on gauge with screen reader | Announces current dB/status via semantic description, not just visual |
| ACC-05 | TalkBack on FrequencyProfileBar | TalkBack enabled | Focus on frequency bar | Announces frequency classification meaningfully |
| ACC-06 | Font scaling | System font size set to largest | Navigate through all major screens | Text scales without clipping, overlap, or broken layouts |
| ACC-07 | Color contrast for gauge states | Any theme | Inspect gauge colors across Quiet/Moderate/Loud/Very Loud states | Sufficient contrast against background per WCAG baseline |

---

## 13. Privacy & Data Handling

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| PRIV-01 | No raw audio persisted | Active measurement | Inspect local storage / DB after session | Only numeric dB/statistical values stored; no raw audio files or waveforms saved |
| PRIV-02 | No raw audio transmitted | Active measurement, network monitoring enabled | Run session with network traffic inspected | No audio payloads sent over network (ad SDK/billing calls only) |
| PRIV-03 | CSV export contains no unexpected PII | Export flow | Inspect exported CSV | Only session/location/score data as documented; no unintended personal data leakage |
| PRIV-04 | Data wipe is complete | After full wipe | Inspect DB file / re-check all screens | No residual session, place, or custom activity records remain anywhere in the app |

---

## 14. Cross-Cutting / Regression

| ID | Title | Preconditions | Steps | Expected Result |
|---|---|---|---|---|
| REG-01 | Rotation/config change during session | Active session running | Rotate device | Session continues uninterrupted, no data loss, UI state preserved |
| REG-02 | Low battery / battery saver mode | Battery saver enabled | Run active session | Foreground service still functions (or degrades gracefully with a user-visible notice) |
| REG-03 | Android version matrix | Devices/emulators on API 26 (min) and API 35 (target) | Run full core flow (measure → save place → view history) on both | Consistent behavior across min and target SDK |
| REG-04 | Cold start performance | App fully closed | Launch app, time to interactive Home screen | Reasonable load time (SLA <2s on mid-range device) |
| REG-05 | Upgrade path from pre-feature build | Existing install with old schema (pre-GPS/custom-activity/forecast) | Upgrade app | Room DB migration runs cleanly; old sessions/places remain intact and readable |
| REG-06 | Notification channel behavior (Android 13+) | API 33+ device | Fresh install | Notification permission requested appropriately per Android 13+ rules; alerts still deliver once granted |

---

## Execution Priority for Pre-Launch Regression
1. **AUD, SUIT, SESS** (Core measurement loop — must be flawless)
2. **ONB, GPS** (First-run experience — highest drop-off risk if broken)
3. **PRIV** (Play Store policy risk if mishandled)
4. **MON** (Revenue-critical, only blocks if broken for paying users)
5. **ACC, WID, HIST/EXP** (Polish and secondary flows)
6. **REG-05** (DB migration — critical for existing user upgrade path)
