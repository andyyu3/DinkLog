# GPS Location Auto-Fill Learnings

What we built, how it works, and the Android/Kotlin concepts behind every decision.

---

## What This Feature Adds

When a user opens the Add Match screen, the app automatically detects their location and pre-fills the Location field with a human-readable place name (e.g., "Wolfe Park, Columbus, Ohio"). If location permission is denied or GPS fails, a Snackbar tells the user to enter it manually.

```
User taps + to log a match
        ↓
AddMatch.onViewCreated() fires
        ↓
requestLocationPermissionAndFetch()
        ↓ (first launch)                    ↓ (permission already granted)
System permission dialog                   fetchLocation()
        ↓ user taps Allow                        ↓
fetchLocation()                     FusedLocationProviderClient
        ↓                               .getCurrentLocation()
FusedLocationProviderClient                  ↓ lat/lng
    .getCurrentLocation()            resolveAddress(lat, lng)
        ↓ lat/lng                            ↓
resolveAddress(lat, lng)            Geocoder → Address object
        ↓                                    ↓
Geocoder → Address object           formatAddress()
        ↓                                    ↓
formatAddress()                 locationInput.setText(name)
        ↓
locationInput.setText(name)
```

Nothing changes in the ViewModel or Repository — location is just a string that flows through the existing `saveMatch()` / `editMatch()` path.

---

## New Dependency: `play-services-location`

```kotlin
implementation("com.google.android.gms:play-services-location:21.2.0")
```

`FusedLocationProviderClient` is not part of the Android SDK. It lives in **Google Play Services** — a separate Google package installed on every real Android device. You have to declare it as a dependency or the import doesn't exist.

### Why FusedLocationProviderClient and Not `LocationManager`?

Android has a built-in `LocationManager` class. We don't use it because:

- `LocationManager` requires you to manage GPS, Wi-Fi, and cell tower sources manually — you pick one and live with its limitations.
- `FusedLocationProviderClient` **fuses** all three sources automatically, picks the best available signal, and is battery-aware. Google built it specifically so developers don't have to think about which sensor to use.
- `getCurrentLocation()` is a one-shot call that returns a single fix and stops. `LocationManager` requires listener management and manual cleanup.

---

## New Concept: Runtime Permissions

Android permissions fall into two categories:

| Type | Example | What You Need |
|---|---|---|
| Normal | `INTERNET` | Declare in manifest only |
| Dangerous | `ACCESS_FINE_LOCATION` | Declare in manifest **AND** request at runtime |

Location is "dangerous" because it's sensitive data. Declaring it in the manifest tells the OS your app *may* use it. The runtime request is a system dialog the user must respond to — your code can't bypass it.

### Two Permissions, Not One

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

- **`ACCESS_FINE_LOCATION`** — GPS hardware. High accuracy, higher battery cost.
- **`ACCESS_COARSE_LOCATION`** — Wi-Fi + cell towers. Less precise but works indoors.

Starting in Android 12, the user can downgrade your request to coarse even if you asked for fine. Declaring both means the app still works in that case.

### The Modern Permission Request Pattern

The old way was `requestPermissions()` + `onRequestPermissionsResult()`. That's deprecated. The modern replacement is `ActivityResultContracts`:

```kotlin
private val locationPermissionRequest = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
               || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) fetchLocation()
    else showLocationError(LocationMessages.PERMISSION_DENIED)
}
```

**`registerForActivityResult`** must be called before `onStart()`. Declaring it as a class property satisfies this. The lambda runs automatically after the user responds to the dialog — you don't wire it up anywhere else.

**`RequestMultiplePermissions`** returns a `Map<String, Boolean>` — each permission mapped to whether it was granted. We check either fine or coarse to handle the Android 12 downgrade case.

### Checking Before Asking

```kotlin
private fun requestLocationPermissionAndFetch() {
    val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
        fetchLocation()
    } else {
        locationPermissionRequest.launch(...)
    }
}
```

`ContextCompat.checkSelfPermission()` reads the current permission state without prompting the user. On every launch after the first, permission is already granted and we skip the dialog entirely.

---

## New Concept: `by lazy` for Context-Dependent Properties

```kotlin
private val fusedLocationClient by lazy {
    LocationServices.getFusedLocationProviderClient(requireContext())
}
```

`requireContext()` can't be called during class initialization — the Fragment isn't attached to an Activity yet at that point. `by lazy` creates the client on **first access**, which happens from `fetchLocation()` only after the view exists.

This is also why we can't use `lateinit var` here — the project rules prohibit `lateinit var` on non-injected fields. `val ... by lazy` is the correct alternative: it's immutable (`val`) and deferred.

---

## New Concept: `@SuppressLint("MissingPermission")`

```kotlin
@SuppressLint("MissingPermission")
private fun fetchLocation() {
    ...
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
    ...
}
```

`getCurrentLocation()` is annotated internally with `@RequiresPermission`. Android Studio's lint checker sees this and flags any call site that doesn't have a permission annotation, even if you've already checked at runtime.

`@SuppressLint("MissingPermission")` tells lint: "I know this requires permission — I've already verified it before this function is ever called." It suppresses the warning without changing any runtime behavior. This is correct to use here because `fetchLocation()` is only reachable through `requestLocationPermissionAndFetch()`, which guarantees permission is granted first.

---

## New Concept: Geocoder — Reverse Geocoding

**Geocoding** = address → coordinates.
**Reverse geocoding** = coordinates → address.

```kotlin
val geocoder = Geocoder(requireContext(), Locale.getDefault())
geocoder.getFromLocation(lat, lng, 1) { addresses -> ... }
```

`Geocoder` is built into Android. It uses Google's geocoding backend on real devices (via Google Play Services), but requires no API key — it runs through the device's own Google account.

The result is a `List<Address>` — each `Address` has many fields:

```
address.featureName   → "Wolfe Park"
address.thoroughfare  → "N High St"
address.locality      → "Columbus"
address.adminArea     → "Ohio"
address.getAddressLine(0) → "4100 N High St, Columbus, OH 43214"
```

We pass `maxResults = 1` — we only need the best match.

### The API 33 Split

`Geocoder` has two very different APIs depending on Android version. This is one of the more annoying version differences in Android development:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // API 33+: async callback, not deprecated
    geocoder.getFromLocation(lat, lng, 1) { addresses -> ... }
} else {
    // API 26-32: synchronous, deprecated in API 33
    val addresses = geocoder.getFromLocation(lat, lng, 1)
}
```

| | API 33+ | API 26–32 |
|---|---|---|
| Style | Async callback | Synchronous, blocking |
| Status | Current | Deprecated |
| Thread | Safe on main thread | **Must run on IO thread** |

The old version blocks while it waits for a network response. If called on the main thread it freezes the UI. We use `Dispatchers.IO` for it:

```kotlin
viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
    @Suppress("DEPRECATION")
    val addresses = geocoder.getFromLocation(lat, lng, 1)
    withContext(Dispatchers.Main) { _binding?.locationInput?.setText(name) }
}
```

`withContext(Dispatchers.Main)` switches back to the main thread before touching the UI. You can never update views from a background thread.

`@Suppress("DEPRECATION")` silences the compiler warning — this code intentionally uses the old API because this branch only runs on devices below API 33.

---

## New Concept: `_binding?.view?.post { }`

```kotlin
_binding?.locationInput?.post { _binding?.locationInput?.setText(name) }
```

Two separate things are happening here:

### Why `_binding?.` instead of `binding.`?

The GPS fetch and geocoding are asynchronous. By the time the result arrives, the user may have already navigated away. When a Fragment is destroyed, `_binding` is set to null in `onDestroyView()`. The safe accessor `binding` would throw an error if called after that.

Using `_binding?.locationInput` (nullable access) silently does nothing if the view is already gone — no crash.

### Why `.post { }`?

`TextInputLayout` manages a floating label — the hint text ("Location") that animates up when the field has content. When you call `setText()` programmatically (not from user typing), `TextInputLayout` sometimes doesn't detect the change in time to animate the label. The result: the hint overlaps the filled text.

`.post { }` schedules the `setText` call for the **next UI frame**, after the current layout pass finishes. This gives `TextInputLayout` a chance to measure and properly float the label.

---

## `formatAddress()` — Building a Readable Name

```kotlin
private fun formatAddress(address: Address): String {
    val namedPlace = address.featureName
        ?.takeIf { it.any(Char::isLetter) && it != address.thoroughfare }
    val street = listOfNotNull(address.subThoroughfare, address.thoroughfare).joinToString(" ")
    val parts = listOfNotNull(
        namedPlace ?: street.ifBlank { null },
        address.locality ?: address.subAdminArea,
        address.adminArea
    )
    return parts.joinToString(", ").ifBlank { address.getAddressLine(0) ?: LocationMessages.UNAVAILABLE }
}
```

The `Address` object splits a street address into two fields — `subThoroughfare` is the street number and `thoroughfare` is the street name. Joining them with `" "` produces `"2151 Neil Ave"`. Without this, `featureName` (which is just `"2151"` for a plain address) and `thoroughfare` (`"Neil Ave"`) would each become their own part, producing `"2151, Neil Ave"` with an unwanted comma.

`namedPlace` captures `featureName` only when it contains letters and differs from `thoroughfare`. This distinguishes a real named place like `"Wolfe Park"` from a street number like `"2151"`. If there's a named place, it's used instead of the street — for a pickleball app, `"Wolfe Park, Columbus, Ohio"` is more useful than `"3890 Olentangy River Rd, Columbus, Ohio"`.

`address.locality ?: address.subAdminArea` — `locality` is the city. Some geocoder results use `subAdminArea` instead depending on the region.

`ifBlank { address.getAddressLine(0) }` — last resort fallback to the full formatted address string if all other fields were null.

---

## Kotlin-Rules Compliance

| Rule | How We Applied It |
|---|---|
| **Function Size** | Every new function is under 10 lines. `resolveAddress` dispatches to `resolveGeocodeNew` / `resolveGeocodeCompat` to stay small. |
| **No Magic Strings** | Error messages live in `LocationMessages` object in `Constants.kt`. |
| **No `!!`** | Used `_binding?.locationInput` (safe call) everywhere instead of force-unwrapping. |
| **No `lateinit var`** | `fusedLocationClient` uses `by lazy` delegation instead. |
| **No `runBlocking`** | Old Geocoder path uses `lifecycleScope.launch(Dispatchers.IO)` + `withContext`. |
| **No Business Logic in Fragment** | Fragment handles system interaction (permissions, GPS, geocoding). Location is passed as a plain string to the ViewModel — same as if the user typed it. |
| **MVVM layers respected** | ViewModel and Repository are completely unchanged. The feature lives entirely in the Fragment because it's a UI/system concern, not a data concern. |

---

## Files Changed

```
Changed files:
├── app/build.gradle.kts          ← Added play-services-location:21.2.0
├── AndroidManifest.xml           ← Added ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION
├── utils/Constants.kt            ← Added LocationMessages object
└── pages/AddMatch.kt             ← All GPS + permission + geocoding logic
```