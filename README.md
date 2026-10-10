# World Time

An Android app for comparing the same moment across cities. It uses Material 3 dynamic colors, matching the system based light and dark theme used in the sibling Batman app.

The selected time starts at **Now** and updates while the app is open. Tap an hour in the shared timeline, tap the large time to open the system time picker, or tap the date to open the system date picker. A rounded vertical marker spans every city at the selected hour. Each city's day boundary has a gap and date label; each day is rounded at its ends while night, morning, day, and evening have different shades. The shared timeline handles daylight saving changes. Tap **Now** to return to the current time.

Use **Add city** to browse or search all available IANA time zones. Hold and drag a city row to reorder it. Each row's menu can set the home time zone or remove the city. Cities, order, and home time zone are saved on the device. The share button sends a concise list of local times and a link that opens the same view in the app.

Add the **World Time** home screen widget from your launcher's widget picker. The dark card shows live city clocks, UTC offsets, relative day labels, and a shared hourly timeline with a highlighted **Now** column. It uses the cities, order, and home time zone saved in the app and respects the device's 12/24-hour setting. Tap the widget to open the app and edit cities. Resize it to show more hours or cities; smaller sizes show a count of additional cities.

New widgets request a **4 × 3** cell size, with minimum resize bounds of **180 × 110dp** (approximately **3 × 2** cells; the actual cell sizes depend on the launcher). Existing widgets keep their size until you resize them or add them again.

City clocks update each minute through Android's native `TextClock`. The timeline, offsets, and day labels refresh periodically and whenever you edit cities, resize the widget, or change the system time or time zone. Android may delay chart refreshes to save battery, particularly while the device sleeps.

Build with `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.
