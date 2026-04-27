# MovieDB Lab 3 — Full Study Notes
### Based on your actual code — corrections and expansions included

---

## ⚠️ CORRECTIONS TO YOUR ORIGINAL NOTES

Before reading everything, fix these mistakes first:

| What you wrote | What is actually true |
|---|---|
| `object MovieRepository {}` — it is a singleton | **WRONG.** `MovieRepository` is a `class`, not an `object`. It uses a `companion object` inside it for the `from()` factory method. A new instance is created each time via `MovieRepository.from(context)`. |
| `local.properties: TMDB_READ_TOKEN=...` | **WRONG key name.** The build.gradle.kts looks for `TMDB_API_KEY` with `findProperty("TMDB_API_KEY")`. Your local.properties should say `TMDB_API_KEY=your_key_here`. |
| `MovieListUiState(isConnected = true)` (original code) | **Fixed to `false`.** Starting as `true` was a bug — the list never loaded on first launch. Now it starts `false` so the connectivity observer triggers a sync automatically. |
| `MovieRepository.refreshList()` clears cache first, then fetches | **Fixed order.** Now fetches from network first. Only clears cache if the network call succeeds. Prevents data loss on network failure. |
| Database `version = 1` | **Now version = 2** because `sortOrder` column was added to `MovieEntity`. |
| `NetworkMovieDetail.homepage` and `imdbId` are nullable (`String?`) | **WRONG.** They are non-nullable `String` in the data class. The `.orEmpty()` calls in repository are just defensive. |
| Notes do not mention `sortOrder` field | `MovieEntity` now has `val sortOrder: Int = 0`. It stores the position from the API response so the list keeps TMDB's ranking order instead of alphabetical. |
| Notes do not mention `fallbackToDestructiveMigration()` | `DatabaseProvider` now calls `.fallbackToDestructiveMigration()` on the builder. This prevents crashes when the Room schema changes. |
| Notes do not mention `BackoffPolicy.EXPONENTIAL` | `SyncScheduler` now sets exponential backoff — WorkManager waits 15 seconds before retrying, doubling each time. |
| `MovieReviewsVideosViewModel` says it extends `AndroidViewModel` | **WRONG.** It extends plain `ViewModel()`. Only `MovieListViewModel` and `MovieDetailViewModel` extend `AndroidViewModel` because they need the `Application` context. |

---

## PART 1 — ANDROID CONCEPTS YOU MUST KNOW

### What is a Composable?
A function marked `@Composable` draws UI. It is not a normal function — Compose reruns it whenever its state changes. You do not call it like a regular function; Compose calls it.

```kotlin
@Composable
fun MovieListScreen(...) {
    // This entire function is a UI description, not imperative code
}
```

### What is a ViewModel?
A class that holds and manages screen state. It survives screen rotations (configuration changes). The UI reads from it but does not own the data.

- `ViewModel()` — basic. No access to Application context.
- `AndroidViewModel(application)` — gives you `application` context for things like database and connectivity.

### What is a Coroutine?
A way to do work (like network calls or database reads) without blocking the UI thread. `suspend fun` means the function can pause and resume. `viewModelScope.launch { }` starts a coroutine tied to the ViewModel's lifetime — when the ViewModel is destroyed, the coroutine is automatically cancelled.

### What is a Flow?
A stream of values over time. Room emits a new value whenever the database changes. The ViewModel collects that stream with `collectLatest { }`. `collectLatest` cancels the previous collection if a new value arrives before it finishes.

### What is a sealed class?
A class where all possible subclasses are defined in the same file. Used in `Screen.kt` so all navigation routes are in one place and you cannot accidentally use a route that does not exist.

### What is a companion object?
A block inside a class that acts like static members in Java. You call its functions on the class name, not on an instance. In `MovieRepository`, the `companion object` holds the `from()` factory and `buildPosterUrl()` helper.

### What is an object (singleton)?
A class with exactly one instance, created automatically. You never write `val x = MyObject()`. Used for: `TmdbApi`, `ApiRepository`, `SyncScheduler`, `DatabaseProvider`.

### What is an enum class?
A fixed set of named constants. `CachedViewType` has exactly three values: `POPULAR`, `TOP_RATED`, `FAVORITES`. Safer than raw strings — the compiler checks you used a valid value.

### Kotlin notation quick reference

| Syntax | Meaning |
|---|---|
| `val` | Read-only variable (cannot reassign) |
| `var` | Mutable variable (can reassign) |
| `?.` | Safe call — only runs if not null |
| `?: default` | Elvis operator — use default if null |
| `!!` | Force non-null (crash if null) |
| `it` | Default name for single lambda parameter |
| `{ param -> body }` | Lambda (anonymous function) |
| `by mutableStateOf(...)` | Compose state — Compose observes this value |
| `private set` | Can be read from outside, but only written from inside the class |
| `init { }` | Runs when the class is first created |
| `suspend fun` | Can be called from a coroutine only |
| `withContext(Dispatchers.IO)` | Switch this coroutine to a background thread |
| `runCatching { }` | Try-catch that returns a Result — used when you want to ignore errors silently |
| `?.let { }` | Run the block only if the value is not null; `it` = the non-null value |
| `?.takeIf { condition }` | Return the value if condition is true, null otherwise |
| `mapIndexed { index, item -> }` | Like `map` but also gives you the position number |
| `emptyList()` | Returns a list with zero items |
| `.toSet()` | Converts a list to a set (no duplicates, fast lookup) |
| `@SerializedName("json_key")` | Tells Gson which JSON field maps to this Kotlin property |
| `@Volatile` | Makes a variable safe to read/write from multiple threads |
| `synchronized(this) { }` | Only one thread can run this block at a time |

---

## PART 2 — THE THREE TYPES OF DATA MODELS

Your app has **three separate layers** of data models. This is intentional. Each serves a different purpose.

### Layer 1 — Network models (what the API sends)
These match the JSON that TMDb returns. Gson converts JSON into these.

```kotlin
// NetworkMovie.kt — matches one item in TMDb's "results" array
data class NetworkMovie(
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,  // JSON key is "poster_path"
    val overview: String
)

// NetworkMovieDetail.kt — matches the detail endpoint response
data class NetworkMovieDetail(
    val id: Int,
    val genres: List<NetworkGenre>,
    val homepage: String,           // non-nullable String
    @SerializedName("imdb_id") val imdbId: String   // non-nullable String
)

// NetworkGenre.kt — nested object inside NetworkMovieDetail
data class NetworkGenre(val id: Int, val name: String)

// MovieListResponse.kt — the top-level wrapper the API returns
data class MovieListResponse(val results: List<NetworkMovie>)
```

Why `@SerializedName`? Because Kotlin uses camelCase (`posterPath`) but JSON uses snake_case (`poster_path`). This annotation tells Gson to map one to the other.

### Layer 2 — Room entities (what the database stores)
These are database table rows. Each field is a column.

```kotlin
// MovieEntity.kt — one row in the "movies" table
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,            // unique ID, no duplicates
    val title: String,
    val posterUrl: String,              // already the full URL, built by repository
    val overview: String,
    val cachedViewType: String?,        // "POPULAR", "TOP_RATED", or NULL (for favorites-only rows)
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0              // position from API (0 = first result from TMDB)
)
```

`cachedViewType` is the key to the caching strategy:
- A movie fetched as Popular has `cachedViewType = "POPULAR"`
- When you switch to Top Rated, `clearCurrentCachedList()` sets ALL `cachedViewType` to NULL
- Then `deleteNonFavoriteRowsWithoutCache()` deletes rows where `cachedViewType IS NULL AND isFavorite = 0`
- Favorites survive because `isFavorite = 1` protects them from deletion

```kotlin
// MovieDetailEntity.kt — one row in the "movie_details" table
@Entity(tableName = "movie_details")
data class MovieDetailEntity(
    @PrimaryKey val movieId: Int,
    val genresJson: String,    // genres stored as JSON string: ["Action","Drama"]
    val homepage: String,
    val imdbId: String
)
```

Why `genresJson` instead of a proper list? Room cannot store `List<String>` directly. The repository uses `Gson` to convert the list to a JSON string before storing, and back to a list when reading.

```kotlin
// AppStateEntity.kt — one row in the "app_state" table
@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 0,              // always ID=0, there is only ever one row
    val selectedViewType: String = CachedViewType.POPULAR.name
)
```

This table stores which tab is currently selected. It survives app restarts. The background worker reads it to know which list to refresh.

### Layer 3 — UI/domain models (what screens use)
Clean, simple objects the UI works with. No Room or network details.

```kotlin
// Movie.kt — what screens display
data class Movie(
    val id: Int,
    val title: String,
    val posterUrl: String,
    val overview: String,
    val isFavorite: Boolean = false
)

// MovieDetail.kt — extra detail for the detail screen
data class MovieDetail(
    val movieId: Int,
    val genres: List<String>,   // already converted from JSON string
    val homepage: String,
    val imdbId: String
)
```

**Why three layers?** Separation of concerns. The network layer can change (different JSON keys) without touching Room or UI. The database schema can change without touching the UI. The UI models are exactly what the screen needs — no more, no less.

---

## PART 3 — THE DATABASE LAYER (ROOM)

### How Room works
Room is Android's local database library. You define:
1. **Entities** — the table structure (`@Entity`)
2. **DAOs** — the SQL queries (`@Dao`)
3. **Database** — the container that ties them together (`@Database`)

Room generates all the actual SQL and Java/Kotlin code at compile time (via KSP).

### MovieDao.kt — queries for the movies table

```kotlin
@Dao
interface MovieDao {

    // Returns a Flow — Room emits new values automatically when the table changes
    @Query("SELECT * FROM movies WHERE cachedViewType = :viewType ORDER BY sortOrder ASC")
    fun observeMoviesByViewType(viewType: String): Flow<List<MovieEntity>>
    // :viewType is a parameter — Room substitutes the actual value at runtime

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY title")
    fun observeFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId LIMIT 1")
    fun observeMovieById(movieId: Int): Flow<MovieEntity?>

    @Query("SELECT id FROM movies WHERE isFavorite = 1")
    suspend fun getFavoriteMovieIds(): List<Int>  // suspend = called from coroutine only

    @Upsert  // insert if new, update if ID already exists
    suspend fun upsertMovies(movies: List<MovieEntity>)

    // Step 1 of cache replacement: mark all cached rows as uncached
    @Query("UPDATE movies SET cachedViewType = NULL WHERE cachedViewType IS NOT NULL")
    suspend fun clearCurrentCachedList()

    // Step 2 of cache replacement: delete rows that are uncached AND not favorites
    @Query("DELETE FROM movies WHERE cachedViewType IS NULL AND isFavorite = 0")
    suspend fun deleteNonFavoriteRowsWithoutCache()

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun setFavorite(movieId: Int, isFavorite: Boolean)

    @Query("SELECT isFavorite FROM movies WHERE id = :movieId LIMIT 1")
    suspend fun isFavorite(movieId: Int): Boolean?
}
```

**Flow vs suspend in DAO:**
- `fun` returning `Flow<...>` — the UI observes this continuously. Room pushes new data whenever the table changes.
- `suspend fun` returning a value — called once from a coroutine. Used for one-shot operations like insert, delete, update.

### AppStateDao.kt — reads/writes the selected view type

```kotlin
@Dao
interface AppStateDao {
    // Flow version — ViewModel observes this to react when selection changes
    @Query("SELECT * FROM app_state WHERE id = 0 LIMIT 1")
    fun observeAppState(): Flow<AppStateEntity?>

    // One-shot version — Worker uses this to read the current selection once
    @Query("SELECT selectedViewType FROM app_state WHERE id = 0 LIMIT 1")
    suspend fun getSelectedViewTypeOnce(): String?

    @Upsert
    suspend fun upsertAppState(state: AppStateEntity)
}
```

### MovieDbDatabase.kt — the database container

```kotlin
@Database(
    entities = [MovieEntity::class, MovieDetailEntity::class, AppStateEntity::class],
    version = 2,            // bumped from 1 to 2 when sortOrder column was added
    exportSchema = false
)
abstract class MovieDbDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun movieDetailDao(): MovieDetailDao
    abstract fun appStateDao(): AppStateDao
}
```

`version = 2` because a new column (`sortOrder`) was added to `MovieEntity`. Room requires the version number to increase whenever the schema changes.

### DatabaseProvider.kt — singleton pattern

```kotlin
object DatabaseProvider {
    @Volatile                          // @Volatile = visible to all threads immediately
    private var INSTANCE: MovieDbDatabase? = null

    fun getDatabase(context: Context): MovieDbDatabase {
        return INSTANCE ?: synchronized(this) {   // only one thread can enter this block
            Room.databaseBuilder(
                context.applicationContext,        // use app context, not activity context
                MovieDbDatabase::class.java,
                "movie_db"
            )
            .fallbackToDestructiveMigration()      // wipe and rebuild if schema version changes
            .build()
            .also { INSTANCE = it }               // save the instance for next time
        }
    }
}
```

**Why `applicationContext` instead of `context`?** Activity contexts get destroyed when the activity is destroyed. `applicationContext` lives as long as the app runs. The database should live that long.

**Why `@Volatile`?** Multiple threads might call `getDatabase()` at the same time. `@Volatile` ensures that when one thread sets `INSTANCE`, all other threads see the updated value immediately.

**Why `synchronized(this)`?** After checking `INSTANCE == null`, two threads could both enter the block simultaneously and create two databases. `synchronized` makes only one thread run the block at a time.

**Why `fallbackToDestructiveMigration()`?** When you bump `version` without writing a migration, Room needs to know what to do. This tells it: delete the old database and create a fresh one. Safe for this app because all data is either re-fetched from the internet or re-added by the user.

---

## PART 4 — THE NETWORK LAYER

### TmdbApiService.kt — the API interface and Retrofit setup

```kotlin
// The interface — describes what API calls exist
interface TmdbApiService {
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,          // appended as ?api_key=...
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): MovieListResponse                           // Gson converts JSON into this

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(...): MovieListResponse

    @GET("movie/{movie_id}")                       // {movie_id} = URL template
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,            // substituted into the URL
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): NetworkMovieDetail

    @GET("movie/{movie_id}/reviews")
    suspend fun getMovieReviews(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): ReviewResponse

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("language") language: String = "en-US"
    ): VideoResponse
}
```

The URL Retrofit builds for `getMovieReviews(550, ...)`:
`https://api.themoviedb.org/3/movie/550/reviews?api_key=YOUR_KEY&language=en-US&page=1`

```kotlin
// The singleton that creates and holds the Retrofit instance
object TmdbApi {
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OKHTTP", message)      // logs every request/response in Logcat
    }.apply { level = HttpLoggingInterceptor.Level.BODY }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)      // fail if can't connect in 15s
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    // "by lazy" = only created the first time TmdbApi.retrofitService is accessed
    val retrofitService: TmdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())  // JSON → data classes
            .build()
            .create(TmdbApiService::class.java)   // generates the implementation
    }
}
```

**Why `by lazy`?** Creating a Retrofit instance is expensive. `by lazy` defers creation until the first use and then reuses the same instance forever.

### ApiRepository.kt — for reviews and videos only

```kotlin
object ApiRepository {
    suspend fun fetchReviews(movieId: Int): List<Review> = withContext(Dispatchers.IO) {
        // withContext(Dispatchers.IO) switches to a background thread for network work
        TmdbApi.retrofitService.getMovieReviews(movieId, BuildConfig.TMDB_API_KEY).results
    }

    suspend fun fetchVideos(movieId: Int): List<Video> = withContext(Dispatchers.IO) {
        TmdbApi.retrofitService.getMovieVideos(movieId, BuildConfig.TMDB_API_KEY).results
    }
}
```

**Why `withContext(Dispatchers.IO)`?** `Dispatchers.IO` is a thread pool designed for slow blocking operations like network calls. Switching to it keeps the main (UI) thread free. Note: Retrofit with coroutines already handles thread switching internally, so this is an extra safety layer.

---

## PART 5 — THE REPOSITORY (MovieRepository.kt)

`MovieRepository` is a **class** (not an `object`/singleton). A new instance is created via the `companion object`'s `from()` factory.

```kotlin
class MovieRepository(
    private val movieDao: MovieDao,
    private val detailDao: MovieDetailDao,
    private val appStateDao: AppStateDao
) {
    private val gson = Gson()
    // ...
    companion object {
        // Factory method — creates a MovieRepository with all its dependencies
        fun from(context: Context): MovieRepository {
            val db = DatabaseProvider.getDatabase(context)
            return MovieRepository(
                movieDao = db.movieDao(),
                detailDao = db.movieDetailDao(),
                appStateDao = db.appStateDao()
            )
        }

        // Builds the full image URL from the path segment TMDb gives us
        private fun buildPosterUrl(posterPath: String?): String {
            return if (posterPath.isNullOrBlank()) ""
            else "https://image.tmdb.org/t/p/w500$posterPath"
            // example: /abc123.jpg → https://image.tmdb.org/t/p/w500/abc123.jpg
        }
    }
}
```

### Key functions and what they do

**`observeSelectedViewType()`** — watches Room for the current tab selection, emits a new `CachedViewType` whenever it changes:
```kotlin
fun observeSelectedViewType(): Flow<CachedViewType> {
    return appStateDao.observeAppState().map { state ->
        state?.selectedViewType?.let {
            runCatching { CachedViewType.valueOf(it) }.getOrDefault(CachedViewType.POPULAR)
        } ?: CachedViewType.POPULAR
    }
}
// .map transforms each emitted AppStateEntity into a CachedViewType enum
// runCatching handles the case where the stored string is not a valid enum name
// .getOrDefault = if conversion failed, use POPULAR as the safe fallback
// ?: CachedViewType.POPULAR = if state is null (first launch), default to POPULAR
```

**`observeMoviesFor(viewType)`** — returns the correct movie list from Room as UI models:
```kotlin
fun observeMoviesFor(viewType: CachedViewType): Flow<List<Movie>> {
    return when (viewType) {
        CachedViewType.FAVORITES -> movieDao.observeFavoriteMovies()
        CachedViewType.POPULAR,
        CachedViewType.TOP_RATED -> movieDao.observeMoviesByViewType(viewType.name)
        // viewType.name = the string "POPULAR" or "TOP_RATED"
    }.map { entities ->
        entities.map { entity ->   // convert each MovieEntity to a Movie (UI model)
            Movie(id = entity.id, title = entity.title, ...)
        }
    }
}
```

**`refreshList(viewType)`** — the main sync function. Fetches from network, then updates Room:
```kotlin
suspend fun refreshList(viewType: CachedViewType) {
    when (viewType) {
        CachedViewType.FAVORITES -> setSelectedViewType(CachedViewType.FAVORITES)

        CachedViewType.POPULAR, CachedViewType.TOP_RATED -> {
            setSelectedViewType(viewType)

            // STEP 1: Network call first — if this throws, we stop here.
            // The existing cache is UNTOUCHED. This is the safe order.
            val networkMovies = when (viewType) {
                CachedViewType.POPULAR -> TmdbApi.retrofitService.getPopularMovies(...).results
                CachedViewType.TOP_RATED -> TmdbApi.retrofitService.getTopRatedMovies(...).results
                CachedViewType.FAVORITES -> emptyList()
            }

            // STEP 2: Only after successful network fetch, read favorites and clear old cache
            val favoriteIds = movieDao.getFavoriteMovieIds().toSet()
            movieDao.clearCurrentCachedList()    // sets cachedViewType = NULL for all rows

            // STEP 3: Build new entities with sortOrder preserving TMDB's ranking
            val entities = networkMovies.mapIndexed { index, networkMovie ->
                MovieEntity(
                    id = networkMovie.id,
                    title = networkMovie.title,
                    posterUrl = buildPosterUrl(networkMovie.posterPath),
                    overview = networkMovie.overview,
                    cachedViewType = viewType.name,
                    sortOrder = index,               // 0 = most popular/top rated
                    isFavorite = favoriteIds.contains(networkMovie.id)  // preserve favorites
                )
            }

            movieDao.upsertMovies(entities)                  // insert or update
            movieDao.deleteNonFavoriteRowsWithoutCache()      // cleanup old non-favorites
        }
    }
}
```

**Why `mapIndexed`?** Normal `map` gives you only the item. `mapIndexed` gives you `index` (0, 1, 2...) as well. That index becomes `sortOrder`, so the list stays in TMDB's original popularity/rating order.

**`toggleFavorite(movieId)`** — flips the favorite flag:
```kotlin
suspend fun toggleFavorite(movieId: Int) {
    val current = movieDao.isFavorite(movieId) ?: false  // read current state, default false if null
    movieDao.setFavorite(movieId, !current)              // flip it
}
```

**`refreshMovieDetail(movieId)`** — fetches detail from network, stores genres as JSON:
```kotlin
suspend fun refreshMovieDetail(movieId: Int) {
    val networkDetail = TmdbApi.retrofitService.getMovieDetails(movieId, ...)
    val detailEntity = MovieDetailEntity(
        movieId = networkDetail.id,
        genresJson = gson.toJson(networkDetail.genres.map { it.name }),
        // networkDetail.genres is List<NetworkGenre>, we map to List<String> (just names)
        // then Gson converts that list to a JSON string: ["Action","Drama"]
        homepage = networkDetail.homepage.orEmpty(),
        imdbId = networkDetail.imdbId.orEmpty()
    )
    detailDao.upsertMovieDetail(detailEntity)
}
```

---

## PART 6 — CONNECTIVITY OBSERVER

```kotlin
class ConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observeIsConnected(): Flow<Boolean> = callbackFlow {
        // callbackFlow converts callback-based Android APIs into a Kotlin Flow

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            // Network became available → emit true

            override fun onLost(network: Network) { trySend(hasInternetConnection()) }
            // A network was lost but there might be others → recheck

            override fun onUnavailable() { trySend(false) }
            // Network request timed out → emit false

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
            // Network exists but may not have actual internet → check capability
        }

        trySend(hasInternetConnection())  // emit current state immediately when observer starts

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        // Now Android will call our callback whenever connectivity changes

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
            // When the Flow is cancelled (ViewModel destroyed), clean up the callback
        }
    }

    private fun hasInternetConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

**`trySend()`** vs `send()`: `trySend()` does not suspend — it either sends or drops the value if the channel is full. Safe to call from a callback that is not a coroutine.

**`callbackFlow { awaitClose { } }`**: The `awaitClose` block keeps the Flow alive until the collector cancels. It also runs cleanup (unregister callback) when that happens.

---

## PART 7 — WORKMANAGER

### Why WorkManager?
When internet returns, you cannot just call the repository function from the ViewModel directly in all cases — the app might be in the background. WorkManager schedules background work that runs even if the app is not in the foreground, and it respects constraints (like "only run when connected").

### SyncSelectedMoviesWorker.kt

```kotlin
class SyncSelectedMoviesWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    // CoroutineWorker = WorkManager worker that supports coroutines

    override suspend fun doWork(): Result {
        return try {
            val repository = MovieRepository.from(applicationContext)
            // Build the repository fresh — the worker has no ViewModel context
            repository.refreshCurrentSelectedList()
            // Reads selected type from Room, then calls refreshList()
            Result.success()
        } catch (e: Exception) {
            Result.retry()  // WorkManager will retry this work later
        }
    }
}
```

`refreshCurrentSelectedList()` reads `AppStateEntity` from Room to find the current tab, then calls `refreshList()` with that type. The worker does not need to know which tab is selected — it reads it from the database.

### SyncScheduler.kt

```kotlin
object SyncScheduler {
    private const val UNIQUE_WORK_NAME = "sync_selected_movies"

    fun enqueueSyncSelectedMovies(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // only run when internet is available
            .build()

        val request = OneTimeWorkRequestBuilder<SyncSelectedMoviesWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,   // first retry: 15s, then 30s, 60s, ...
                15,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,  // if a sync is already queued, replace it with this one
            request
        )
    }
}
```

**`enqueueUniqueWork`** with `REPLACE`: If the user taps Popular, then immediately taps Top Rated, two sync jobs would be queued. `REPLACE` cancels the first and keeps the second, so only the latest selection gets synced.

---

## PART 8 — UI STATE CLASSES

Each screen has a dedicated state data class. The ViewModel produces one state object and the screen reads it.

```kotlin
// MovieListUiState.kt
data class MovieListUiState(
    val selectedViewType: CachedViewType = CachedViewType.POPULAR,
    val movies: List<Movie> = emptyList(),
    val isConnected: Boolean = false,      // starts false so sync fires on first connection
    val isLoading: Boolean = false,
    val showNoConnectionImage: Boolean = false
)

// MovieDetailUiState.kt
data class MovieDetailUiState(
    val movie: Movie? = null,
    val detail: MovieDetail? = null,
    val isConnected: Boolean = false,      // starts false — same reason
    val isLoading: Boolean = false
)

// MovieReviewsVideosUiState.kt
data class MovieReviewsVideosUiState(
    val reviews: List<Review> = emptyList(),
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

**Why `isConnected = false` (not `true`)?** When the ViewModel starts, `observeConnectivity()` begins collecting the `ConnectivityObserver` Flow. The observer immediately emits the current state. If the device has internet, it emits `true`. The ViewModel checks `wasDisconnected = !uiState.isConnected = !false = true`. Since `connected = true` AND `wasDisconnected = true`, the condition `if (connected && wasDisconnected)` fires and the sync is scheduled. If it started as `true`, `wasDisconnected` would be `false`, the condition would never be true, and the list would never load on first launch.

---

## PART 9 — THE VIEWMODELS

### MovieListViewModel.kt (extends AndroidViewModel)

```kotlin
class MovieListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MovieRepository.from(application)
    private val connectivityObserver = ConnectivityObserver(application)

    var uiState by mutableStateOf(MovieListUiState(isLoading = true))
        private set
    // "by mutableStateOf" = Compose observes this. When it changes, screen recomposes.
    // "private set" = only this ViewModel can change it

    private var moviesJob: Job? = null

    init {
        observeSelectedViewType()   // runs immediately when ViewModel is created
        observeConnectivity()
    }
```

**`observeSelectedViewType()`** — watches Room for tab changes:
```kotlin
private fun observeSelectedViewType() {
    viewModelScope.launch {
        repository.observeSelectedViewType().collectLatest { selected ->
            // collectLatest: if a new value arrives before the block finishes, cancel the block and restart
            uiState = uiState.copy(
                selectedViewType = selected,
                isLoading = selected != CachedViewType.FAVORITES
                // if switching to FAVORITES, no loading spinner needed (data is already in Room)
            )
            observeMoviesForSelectedType(selected)
        }
    }
}
```

**`observeMoviesForSelectedType()`** — watches Room for movie list changes:
```kotlin
private fun observeMoviesForSelectedType(viewType: CachedViewType) {
    moviesJob?.cancel()    // cancel any previous movie observation
    moviesJob = viewModelScope.launch {
        repository.observeMoviesFor(viewType).collectLatest { movies ->
            uiState = uiState.copy(
                movies = movies,
                isLoading = false,
                showNoConnectionImage = !uiState.isConnected
                    && viewType != CachedViewType.FAVORITES
                    && movies.isEmpty()
                // show "no connection" image only if: offline AND not favorites AND list is empty
            )
        }
    }
}
```

**`observeConnectivity()`** — the auto-reload mechanism:
```kotlin
private fun observeConnectivity() {
    viewModelScope.launch {
        connectivityObserver.observeIsConnected().collectLatest { connected ->
            val wasDisconnected = !uiState.isConnected      // were we offline before?
            uiState = uiState.copy(
                isConnected = connected,
                showNoConnectionImage = !connected
                    && uiState.selectedViewType != CachedViewType.FAVORITES
                    && uiState.movies.isEmpty()
            )
            // AUTO-RELOAD: if we just regained connection and weren't connected before → sync
            if (connected && wasDisconnected && uiState.selectedViewType != CachedViewType.FAVORITES) {
                SyncScheduler.enqueueSyncSelectedMovies(getApplication())
            }
        }
    }
}
```

**`onSelectViewType(viewType)`** — called when user taps Popular / Top Rated / Favorites:
```kotlin
fun onSelectViewType(viewType: CachedViewType) {
    viewModelScope.launch {
        repository.setSelectedViewType(viewType)   // save to Room (worker reads this)

        when {
            viewType == CachedViewType.FAVORITES -> uiState = uiState.copy(isLoading = false)
            uiState.isConnected -> {
                uiState = uiState.copy(isLoading = true)
                SyncScheduler.enqueueSyncSelectedMovies(getApplication())  // fetch new list
            }
            else -> uiState = uiState.copy(isLoading = false, showNoConnectionImage = true)
        }
    }
}
```

### MovieDetailViewModel.kt (extends AndroidViewModel)

```kotlin
fun loadMovie(movieId: Int) {
    if (currentMovieId == movieId) return   // already loaded — do nothing
    currentMovieId = movieId

    movieJob?.cancel()      // cancel previous observations
    detailJob?.cancel()

    movieJob = viewModelScope.launch {
        repository.observeMovie(movieId).collectLatest { movie ->
            uiState = uiState.copy(movie = movie)
        }
    }

    detailJob = viewModelScope.launch {
        repository.observeMovieDetail(movieId).collectLatest { detail ->
            uiState = uiState.copy(detail = detail, isLoading = false)
        }
    }

    if (uiState.isConnected) refreshDetail()    // fetch fresh detail if we have internet
}

private fun refreshDetail() {
    val movieId = currentMovieId ?: return     // ?: return = if null, stop here
    viewModelScope.launch {
        uiState = uiState.copy(isLoading = true)
        runCatching {
            repository.refreshMovieDetail(movieId)
            // runCatching: if this throws, the error is swallowed silently
            // the cached detail is still shown if the refresh fails
        }
        uiState = uiState.copy(isLoading = false)
    }
}
```

### MovieReviewsVideosViewModel.kt (extends plain ViewModel)

```kotlin
class MovieReviewsVideosViewModel : ViewModel() {
    // Not AndroidViewModel because it uses ApiRepository (object), not Room with application context

    fun loadMovieData(movieId: Int) {
        val alreadyLoadedSameMovie =
            lastLoadedMovieId == movieId &&
                    (uiState.reviews.isNotEmpty() || uiState.videos.isNotEmpty())
        if (alreadyLoadedSameMovie) return    // don't reload if already have data

        lastLoadedMovieId = movieId
        uiState = MovieReviewsVideosUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val reviews = ApiRepository.fetchReviews(movieId)
                val videos = ApiRepository.fetchVideos(movieId)
                    .filter { it.site == "YouTube" && it.type == "Trailer" }
                    .take(5)                   // max 5 trailers
                uiState = MovieReviewsVideosUiState(reviews = reviews, videos = videos, isLoading = false)
            } catch (e: IOException) {
                uiState = MovieReviewsVideosUiState(isLoading = false, errorMessage = "Network error: ${e.message}")
            } catch (e: HttpException) {
                uiState = MovieReviewsVideosUiState(isLoading = false, errorMessage = "HTTP error: ${e.code()}")
            } catch (e: Exception) {
                uiState = MovieReviewsVideosUiState(isLoading = false, errorMessage = "Unexpected error: ${e.message}")
            }
        }
    }
}
```

**`IOException`** = no network / timeout. **`HttpException`** = server responded with 4xx/5xx error.

---

## PART 10 — NAVIGATION (Screen.kt + MovieApp.kt)

```kotlin
// Screen.kt — sealed class as a navigation route dictionary
sealed class Screen(val route: String) {
    object MovieList : Screen("movie_list")
    object MovieDetail : Screen("movie_detail/{movieId}") {
        // {movieId} is a URL template parameter
        fun createRoute(movieId: Int) = "movie_detail/$movieId"
        // e.g. createRoute(550) = "movie_detail/550"
    }
    object ReviewsVideos : Screen("reviews_videos/{movieId}") {
        fun createRoute(movieId: Int) = "reviews_videos/$movieId"
    }
    object MovieGrid : Screen("movie_grid")
}
```

```kotlin
// MovieApp.kt — the navigation container
@Composable
fun MovieApp() {
    val navController = rememberNavController()
    // rememberNavController = creates a controller that survives recompositions

    NavHost(navController = navController, startDestination = Screen.MovieList.route) {
        composable(Screen.MovieList.route) {
            MovieListScreen(
                onMovieClick = { movieId ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieId))
                },
                onGoToGridScreen = { navController.navigate(Screen.MovieGrid.route) }
            )
        }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
            // declares that "movieId" in the route is an Int
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            // backStackEntry holds route arguments — extract movieId
            MovieDetailScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() },
                onGoToReviewsVideos = { navController.navigate(Screen.ReviewsVideos.createRoute(movieId)) }
            )
        }
        // ... ReviewsVideos and MovieGrid similarly
    }
}
```

**Why does MovieListScreen not know about navController?** Screens should not control navigation directly. Instead, they receive lambda functions (`onMovieClick`, `onGoToGridScreen`). `MovieApp` defines what those lambdas actually do. This makes screens reusable and testable.

---

## PART 11 — THE SCREENS

### MovieListScreen.kt

```kotlin
@Composable
fun MovieListScreen(
    onMovieClick: (Int) -> Unit,      // lambda: takes movieId, returns nothing
    onGoToGridScreen: () -> Unit,     // lambda: no args, returns nothing
    viewModel: MovieListViewModel = viewModel()   // default-inject ViewModel
) {
    val uiState = viewModel.uiState   // read state (Compose observes this)

    // when {} block chooses what to show based on state
    when {
        uiState.isLoading -> CircularProgressIndicator()
        uiState.showNoConnectionImage -> { Image(...); Text("No internet...") }
        else -> LazyColumn { items(uiState.movies) { movie -> MovieListCard(...) } }
    }
}

// Each movie card in the list
@Composable
private fun MovieListCard(movie: Movie, onMovieClick: (Int) -> Unit, onToggleFavorite: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onMovieClick(movie.id) }) {
        // Poster image using Coil
        if (movie.posterUrl.isNotBlank()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = "Poster for ${movie.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
        // Text and favorite button below
        Text(movie.title, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onToggleFavorite) {
            Text(if (movie.isFavorite) "Remove Favorite" else "Add Favorite")
        }
    }
}
```

### MovieDetailScreen.kt

```kotlin
@Composable
fun MovieDetailScreen(movieId: Int, onBack: () -> Unit, onGoToReviewsVideos: () -> Unit, viewModel: MovieDetailViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    // LaunchedEffect runs its block when movieId changes (or screen first appears)
    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
    }

    val imdbUrl = uiState.detail?.imdbId?.takeIf { it.isNotBlank() }?.let {
        "https://www.imdb.com/title/$it/"
    }
    // ?.takeIf = only continue if imdbId is not blank
    // ?.let { } = transform imdbId into full URL

    // Opening external apps with implicit intents:
    Button(onClick = {
        val homepage = detail?.homepage.orEmpty()
        if (homepage.isNotBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(homepage))
            context.startActivity(intent)   // Android finds the right browser app
        }
    }) { Text("Open Movie Homepage") }
}
```

**`LaunchedEffect(movieId)`** — runs the block when `movieId` changes. If the screen recomposes for another reason, the block does NOT re-run (only re-runs if `movieId` itself changes). This is how you trigger a one-time load when a screen opens.

### MovieGridScreen.kt

Uses `LazyVerticalGrid` with the same `MovieListViewModel` instance as `MovieListScreen`. Because Compose's `viewModel()` returns the same ViewModel instance for the same scope, the grid automatically shows the same data and reacts to the same state.

```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),   // 2 columns
    ...
) {
    items(uiState.movies) { movie ->
        AsyncImage(model = movie.posterUrl, ...)
    }
}
```

### VideoPlayerComposable.kt

```kotlin
@Composable
fun VideoPlayerComposable() {
    val context = LocalContext.current

    val exoPlayer = ExoPlayer.Builder(context).build().apply {
        val mediaItem = MediaItem.fromUri("https://...BigBuckBunny.mp4")
        setMediaItem(mediaItem)
        prepare()           // prepares the player to play
        playWhenReady = false   // start paused (user presses play)
    }

    // DisposableEffect = runs cleanup when the composable leaves the screen
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()  // release media player resources — prevents memory leaks
        }
    }

    AndroidView(
        factory = { PlayerView(it).apply { player = exoPlayer } },
        // AndroidView embeds a traditional Android View (PlayerView) inside Compose
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}
```

**`DisposableEffect`** vs `LaunchedEffect`: `LaunchedEffect` runs a coroutine. `DisposableEffect` registers cleanup code that runs when the composable is removed from the screen.

---

## PART 12 — THE FULL DATA FLOW (what to say when professor asks)

### Flow 1: App first opens
```
MainActivity.onCreate()
  → setContent { MovieApp() }
  → NavHost starts at MovieList route
  → MovieListScreen is composed
  → viewModel() creates MovieListViewModel
  → MovieListViewModel.init fires:
      → observeSelectedViewType() starts collecting from Room
           Room has no AppState yet → emits POPULAR as default
           observeMoviesForSelectedType(POPULAR) starts
           Room has no movies yet → emits empty list
      → observeConnectivity() starts collecting from ConnectivityObserver
           ConnectivityObserver immediately emits current state (true = connected)
           wasDisconnected = !false = true    ← because isConnected started as false
           connected=true AND wasDisconnected=true → SyncScheduler.enqueueSyncSelectedMovies()
  → WorkManager runs SyncSelectedMoviesWorker
  → Worker calls repository.refreshCurrentSelectedList()
  → repository reads Room → gets POPULAR
  → repository calls TmdbApi.retrofitService.getPopularMovies()
  → Gson converts JSON → List<NetworkMovie>
  → repository saves List<MovieEntity> to Room
  → Room emits new value to observeMoviesByViewType Flow
  → ViewModel's collectLatest receives movies
  → uiState.movies updated
  → Compose recomposes MovieListScreen → list is displayed
```

### Flow 2: User taps "Top Rated"
```
User taps "Top Rated" button
  → viewModel.onSelectViewType(CachedViewType.TOP_RATED)
  → repository.setSelectedViewType(TOP_RATED) → saves to Room AppState table
  → isConnected=true → isLoading=true + SyncScheduler.enqueueSyncSelectedMovies()
  → observeSelectedViewType() in ViewModel receives TOP_RATED from Room
  → observeMoviesForSelectedType(TOP_RATED) starts (cancels old job)
  → Room has no TOP_RATED movies yet → emits empty list → spinner shows
  → WorkManager runs worker → repository.refreshList(TOP_RATED)
  → Fetches top rated from API → saves to Room
  → clearCurrentCachedList() sets all cachedViewType = NULL
  → (old Popular rows are cleared)
  → new TOP_RATED rows inserted
  → deleteNonFavoriteRowsWithoutCache() removes old Popular rows
  → Room emits new list → ViewModel updates uiState.movies
  → Compose shows top rated movies
```

### Flow 3: Internet disconnects while viewing Popular
```
User disconnects WiFi/cellular
  → ConnectivityObserver.onLost() fires → trySend(false)
  → ViewModel.observeConnectivity() receives false
  → isConnected = false
  → movies list is still showing (it came from Room cache)
  → showNoConnectionImage = false because movies.isNotEmpty()
  → User switches to Top Rated (no cache for it)
  → onSelectViewType(TOP_RATED) → isConnected=false → showNoConnectionImage=true
  → Screen shows the no-connection alert image
```

### Flow 4: Internet comes back
```
ConnectivityObserver.onAvailable() fires → trySend(true)
  → ViewModel.observeConnectivity() receives true
  → wasDisconnected = !false = true (we were offline)
  → connected=true AND wasDisconnected=true
  → SyncScheduler.enqueueSyncSelectedMovies()
  → Worker waits for CONNECTED constraint (already met)
  → Worker calls refreshCurrentSelectedList()
  → Fetches TOP_RATED from API → saves to Room
  → Room Flow emits new list → ViewModel updates
  → Screen automatically shows the list WITHOUT any user action
```

### Flow 5: User opens movie detail
```
User taps a movie card
  → onMovieClick(movie.id) lambda in MovieListScreen
  → MovieApp's navController.navigate("movie_detail/550")
  → MovieDetailScreen is composed with movieId=550
  → LaunchedEffect(550) fires → viewModel.loadMovie(550)
  → observeMovie(550) starts → Room has it → emits Movie
  → observeMovieDetail(550) starts → Room may or may not have it yet
  → isConnected=true → refreshDetail() called
  → TmdbApi.retrofitService.getMovieDetails(550) → Gson converts
  → detailDao.upsertMovieDetail() → Room emits to observeMovieDetail Flow
  → uiState.detail updated → screen shows genres, homepage, IMDb
```

---

## PART 13 — MANIFEST & BUILD

### AndroidManifest.xml — two permissions are required:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<!-- Required to make any HTTP request -->

<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!-- Required for ConnectivityManager to check and observe network state -->
```

### build.gradle.kts — how the API key gets into the app:
```kotlin
buildConfigField(
    "String",
    "TMDB_API_KEY",
    "\"${project.findProperty("TMDB_API_KEY") ?: ""}\""
)
// findProperty("TMDB_API_KEY") reads from local.properties
// local.properties should have: TMDB_API_KEY=your_actual_key_here
// This generates BuildConfig.TMDB_API_KEY at build time
// BuildConfig is auto-generated — you never edit it manually
```

local.properties (not committed to git — keeps the key private):
```
TMDB_API_KEY=your_actual_tmdb_api_key_here
```

---

## PART 14 — LIBRARIES SUMMARY

| Library | What it does | Where used in this project |
|---|---|---|
| Jetpack Compose | Kotlin-first UI framework | All screen files (`@Composable` functions) |
| Navigation Compose | Screen routing | `MovieApp.kt`, `Screen.kt` |
| Room | Local SQLite database | All `data/local/` files |
| Retrofit | HTTP client for API calls | `TmdbApiService.kt`, network calls in repository |
| Gson (converter) | JSON ↔ Kotlin data class conversion | Retrofit converter, genre JSON in repository |
| Coil | Loads images from URLs | `AsyncImage` in MovieListScreen, MovieGridScreen |
| WorkManager | Background task scheduling | `SyncSelectedMoviesWorker`, `SyncScheduler` |
| Media3 ExoPlayer | Video playback | `VideoPlayerComposable.kt` |
| OkHttp (logging) | HTTP logging for debugging | `TmdbApiService.kt` `HttpLoggingInterceptor` |
| KSP | Compile-time code generation for Room | Generates `MovieDao_Impl.kt` etc. |
| ViewModel Compose | `viewModel()` function in Compose | All screen files |
| Lifecycle Runtime | `viewModelScope`, `AndroidViewModel` | ViewModels |

---

## PART 15 — QUESTIONS YOUR PROFESSOR MIGHT ASK

**Q: Why does the list automatically reload when internet comes back?**
A: `MovieListViewModel.observeConnectivity()` collects a `Flow<Boolean>` from `ConnectivityObserver`. When it changes from `false` to `true`, the condition `connected && wasDisconnected` is true, so `SyncScheduler.enqueueSyncSelectedMovies()` is called. WorkManager runs the worker when the `CONNECTED` constraint is met, the worker calls `repository.refreshCurrentSelectedList()`, which fetches from API and saves to Room. Room's Flow emits the new list, and Compose recomposes the screen automatically.

**Q: How does the app still show data when there is no internet?**
A: The app uses an offline-first pattern. Screens observe Room database Flows, not the network directly. When online, data is fetched from the API and saved to Room. When offline, the screens still observe Room and see the previously saved data. Room does not care whether the internet is available.

**Q: How does only one list type get cached at a time?**
A: Each `MovieEntity` has a `cachedViewType` column. When switching list types, `clearCurrentCachedList()` sets all `cachedViewType` to NULL. Then new movies are inserted with the new `cachedViewType`. Then `deleteNonFavoriteRowsWithoutCache()` deletes all rows where `cachedViewType IS NULL AND isFavorite = 0`. This removes the old list while keeping favorites.

**Q: Why is `MovieRepository` a class and not an object?**
A: It needs to hold references to three DAOs (`movieDao`, `detailDao`, `appStateDao`). Those DAOs come from the Room database which requires an `Application` context to build. The `companion object`'s `from(context)` factory method handles building all three and wrapping them in the class. Using a class instead of an object also makes it easier to test, because you could inject different DAOs.

**Q: What is the difference between `Flow` and `suspend fun` in a DAO?**
A: `Flow` functions return a stream that keeps emitting new values whenever the database changes — the UI observes it continuously. `suspend fun` is a one-time operation that runs in a coroutine, completes, and returns a single value. Reads that need real-time reactivity use `Flow`. One-shot writes (insert, update, delete) and one-time reads (like the worker reading the current selection) use `suspend fun`.

**Q: What does `uiState.copy(...)` do?**
A: `data class` in Kotlin automatically provides a `copy()` function. It creates a new instance with all the same field values, except for the ones you specify. This is how the ViewModel updates state immutably — it never directly modifies fields, it always creates a new state object. Compose detects the new object and recomposes.

**Q: How does `LaunchedEffect` work?**
A: `LaunchedEffect(key)` starts a coroutine when the composable first enters the screen. If `key` changes later, it cancels the previous coroutine and starts a new one. When the composable leaves the screen, the coroutine is cancelled. Used in `MovieDetailScreen` to call `viewModel.loadMovie(movieId)` when the screen opens.
