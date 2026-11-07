# FEH Catalog

Aplicación Android en Kotlin para llevar un catálogo personal de héroes de
**Fire Emblem Heroes**. El catálogo base se importa desde la wiki y el usuario
solo registra lo que tiene: posesión, copias, si conserva alguna copia, merges,
favoritos y notas.

La versión actual es **1.0** (base de datos Room versión 7).

## Cómo funciona

- El catálogo de héroes es de solo lectura dentro de la app. No hay alta,
  edición ni eliminación libre de héroes.
- Al abrir la app se importa `app/src/main/assets/heroes.json` en Room. El JSON
  se genera desde la wiki y contiene **1.428 héroes** (versión de contenido
  `d114f5e75733a855`, generada el 2026-10-31).
- La importación es un *upsert*: si el catálogo cambia, se actualizan los datos
  base; la colección personal no se toca.
- Los datos personales viven en una tabla separada y solo se crea una fila
  cuando el usuario interactúa con un héroe. Si no existe, se asume todo en
  falso/cero.

## Funcionalidades

- Catálogo completo con retrato, nombre, título, arma, color, movimiento,
  juegos de origen y tipos de unidad.
- Iconos de armas, movimiento y juegos en las tarjetas, la ficha y los filtros.
  El texto de esas categorías se reemplaza por el icono correspondiente.
- Iconos de rareza/unidad:
  - En el catálogo se muestra solo el símbolo.
  - En la ficha y en los filtros se muestra el símbolo y el nombre.
  - `Special`, `Ascended`, `Brave` y `Chosen` ya tienen icono propio.
  - `GHB`, `Tempest` y `Story` usan el símbolo de Heroic Grail.
- Origen de cada héroe:
  - Los seasonal muestran una etiqueta corta de su tema: `Año Nuevo`,
    `Desierto`, `San Valentín`, `Pascua`, `Niños`, `Boda`, `Verano`,
    `Halloween`, `Ninjas`, `Navidad`, `Termas`, `Hoshido`, `Piratas`,
    `Ladrones`, `Té`, `Danza`, `Local` y `Pícnic`.
  - Los héroes de banners New Heroes muestran `New Heroes`.
  - Los héroes nuevos de banners Legendary, Mythic y Emblem muestran
    `Legendary`, `Mythic` o `Emblem`.
  - GHB, Tempest y Story sin tema seasonal muestran `No aplica`, porque no
    pertenecen a un banner de invocación.
- Búsqueda por nombre o título, ignorando mayúsculas y tildes.
- Filtros por:
  - Colección: todos, tengo, conservo, me faltan, ya no, favoritos
    (cualquier tipo).
  - Juego de origen.
  - Tipo de unidad (se pueden combinar varios).
  - Movimiento.
  - Color de arma.
  - `Special Rate`.
- Tarjeta con retrato y casilla para marcar rápidamente si el héroe está en la
  colección. El fondo de la tarjeta se tiñe con el color de arma del héroe.
- Los favoritos se marcan con una línea del color del tipo de favorito
  alrededor de la tarjeta.
- Etiqueta `Special Rate` (abreviada `SR` en las tarjetas) para unidades con
  condiciones especiales de obtención. Incluye los héroes no seasonal hasta la
  versión `8.7` y los seasonal hasta la versión `7.1`, ambas inclusive.
  Quedan fuera Legendary, Mythic, Emblem, GHB, Tempest, Story, Rearmed,
  Attuned, Aided, Entwined, Vista y Regular 3-4.
- Etiqueta `4-5★`, con el icono de rareza Regular 1-4, para los héroes que la
  wiki ofrece como `4★ – 5★ Special Rate`. Se toma de la lista local
  `special_rate_heroes.csv` y aparece junto al resto de tipos de unidad, tanto
  en la tarjeta y la ficha como en los filtros.
- Ficha de detalle con:
  - Posesión (`Lo tengo`).
  - Copias actuales (0..99).
  - Conservación (`Lo conservo`).
  - Merges (+0 a +10).
  - Favorito con 8 tipos numerados (1 a 8) y corazones de colores distintos,
    como en FEH. El tipo 1 es rosa claro y el 8 es rosa intenso. El color
    solo cambia el borde de la tarjeta y el corazón del selector; no altera
    la colección ni los filtros.
  - Notas libres.
  - Origen: tema seasonal o categoría New Heroes/Legendary/Mythic/Emblem
    (solo lectura).
  - Etiqueta `Special Rate` cuando corresponda (solo lectura).
  - Versión de FEH en la que debutó el héroe (por ejemplo, `1.0`, `9.11` o
    `10.10`), leída de `Versiones.xlsx` (solo lectura).
- Persistencia local con Room.
- Tema claro/oscuro con paleta inspirada en FEH.

### Semántica de la colección

- `owned`: el jugador obtuvo el héroe, aunque ya no lo conserve.
- `copies`: copias actuales; `0` significa que ya no conserva ninguna.
- `kept`: todavía conserva al menos una copia.
- `merges`: de +0 a +10.
- `favoriteSlot`: `0` = sin favorito; `1..8` = tipo de favorito. Es
  independiente de la posesión y solo cambia el borde de la tarjeta y el
  color del corazón del selector.
- `notes`: texto libre.

## Fuente del catálogo

- Lista de héroes:
  `https://feheroes.fandom.com/wiki/List_of_Heroes`
- Lista de temas seasonal:
  `https://feheroes.fandom.com/wiki/List_of_Special_Heroes_by_theme`.
  El generador agrupa a los héroes por el encabezado de su tema y guarda una
  etiqueta corta en `bannerType`. Es la fuente que decide qué héroes son
  seasonal.
- La lista de temas también confirma que los héroes de Tempest Trials seasonal
  son Special Heroes. Por eso el generador les añade `Special` sin quitar
  `Tempest`.
- Archivos de invocación para el origen de los no seasonal:
  - New Heroes:
    `https://feheroes.fandom.com/wiki/Summoning_event_archive/New_Heroes`
  - Legendary, Mythic y Emblem:
    `https://feheroes.fandom.com/wiki/Summoning_event_archive/Legendary_%26_Mythic_%26_Emblem_Heroes`
  El generador procesa primero New Heroes y después LME. En los banners LME
  solo asigna el héroe nuevo que da nombre al banner; en los banners genéricos
  `Focus: Double Mythic Heroes` toma los dos primeros héroes LME. Si un héroe
  ya debutó en un banner New Heroes, conserva `New Heroes` aunque aparezca
  como destacado en un banner LME.
- El archivo New Heroes no incluye a los 4★ demote ni a los héroes del pool
  inicial. Esos héroes regulares se etiquetan `New Heroes`. Los cuatro héroes
  LME que faltan en los archivos (Byleth Emblem, Elise Legendary, Eir Mythic y
  Hector Emblem) se completan con su tipo. El Duo Marth del banner
  `Duo Marth & Mythic Elm: Year 35!` recibe `Mythic` como categoría del banner.
- GHB, Tempest y Story no pertenecen a un banner de invocación y quedan sin
  origen (`No aplica`).
- Resultado actual: 550 seasonal; 620 `New Heroes`; 59 `Legendary`; 52
  `Mythic`; 14 `Emblem`; 133 sin origen (115 GHB, 15 Tempest y 3 Story).
- La lista principal se actualizó el 2026-10-31 e incorpora a
  **Hector: Of Strength** (`Emblem`, versión `10.10`). Su fila llega sin
  `data-titles`; el generador usa el `data-sort-value` y los enlaces de la celda
  de juego (Blazing Blade + Engage) como respaldo.
- Versiones de héroe: `Versiones.xlsx`, hoja `feh_versions_final`, columnas
  `Heroe` y `Version`. La columna `URL` se ignora. El generador empareja por
  nombre de página y tolera tildes, mayúsculas y comillas duplicadas del Excel.
  Resultado actual: **1.428/1.428 héroes con versión**, 116 versiones distintas
  desde `1.0` hasta `10.10`.
- La etiqueta `Special Rate` se guarda como el booleano histórico `revival`:
  los héroes no seasonal hasta `8.7` inclusive y los seasonal hasta `7.1`
  inclusive quedan en `true`. Los demás quedan en `false`.
- Resultado actual: **555 héroes con Special Rate** (292 no seasonal y 263
  seasonal). Quedan excluidos 650 héroes por banner o por tipo de unidad.
- La etiqueta `4-5★` se toma de `special_rate_heroes.csv` (columnas `Heroe` y
  `Rareza`) y se guarda como el tipo de unidad `SPECIAL_4_5`. Se empareja por
  nombre de página tolerando tildes, mayúsculas y comillas. Resultado actual:
  **82 héroes**, todos también de tipo `Special`. Usa el icono de rareza
  Regular 1-4 (`ic_rarity_regular_1_4`).
- La generación descarga el HTML renderizado de la lista mediante
  `action=parse`, porque la consulta Cargo normal tiene un límite de 500 filas y
  puede aplicar *rate limit*.
- Los tipos `Ascended` y `Brave` no aparecen en `data-availability-classes` del
  HTML; se complementan con una consulta Cargo aparte sobre `Units.Properties`.
- Los retratos son URLs de `static.wikia.nocookie.net`. Esa CDN exige un
  `Referer` de `https://feheroes.fandom.com/`; por eso `FehApplication`
  configura Coil con un interceptor de OkHttp que lo añade.
- Los IDs de héroe son numéricos de 63 bits derivados de SHA-256 del título de
  la página de la wiki. El campo `page` también es único.
- La versión del seed es un hash del contenido del JSON: si cambia, la app
  vuelve a importar el catálogo en la siguiente apertura.

### Regenerar el catálogo

Requiere Python 3.10 o superior.

```powershell
python tools\generate_catalog.py
```

El comando descarga la lista principal, consulta Ascended/Brave, descarga la
lista de temas seasonal y los dos archivos de invocación, lee `Versiones.xlsx`
y `special_rate_heroes.csv`, y escribe `app/src/main/assets/heroes.json`.

Opciones útiles:

```powershell
# Usar HTML ya descargado (evita volver a pedir las listas)
python tools\generate_catalog.py --html "$env:TEMP\feh_list_parse.html" --themes-html "$env:TEMP\feh_themes_parse.html" --new-heroes-html "$env:TEMP\feh_new_heroes.html" --lme-html "$env:TEMP\feh_lme.html"

# Generar solo con el HTML, sin consultar Cargo
python tools\generate_catalog.py --html "$env:TEMP\feh_list_parse.html" --themes-html "$env:TEMP\feh_themes_parse.html" --new-heroes-html "$env:TEMP\feh_new_heroes.html" --lme-html "$env:TEMP\feh_lme.html" --skip-cargo

# Generar sin orígenes New Heroes/LME (no recomendado)
python tools\generate_catalog.py --skip-origins

# Generar sin versiones de héroe (no recomendado)
python tools\generate_catalog.py --skip-versions

# Usar otro CSV de héroes 4-5★ (por defecto special_rate_heroes.csv)
python tools\generate_catalog.py --special-rate-csv ruta\special_rate_heroes.csv

# Generar sin la etiqueta 4-5★ (no recomendado)
python tools\generate_catalog.py --skip-special-rate-list

# Usar otro Excel de versiones
python tools\generate_catalog.py --versions-xlsx ruta\Versiones.xlsx

# Generar sin banners seasonal (no recomendado)
python tools\generate_catalog.py --skip-themes

# Escribir en otra ruta
python tools\generate_catalog.py --out ruta\heroes.json
```

La consulta a Fandom puede devolver `ratelimited`; si pasa, hay que esperar y
volver a intentar. No conviene ejecutar el generador en bucle.

## Stack

- Kotlin 1.9.24
- Jetpack Compose (BOM 2024.06.00) + Material 3
- Navigation Compose
- Room 2.6.1 con KSP
- Coil 2.6.0 + OkHttp 4.12.0 para los retratos
- ViewModel + StateFlow
- Inyección de dependencias manual (`AppContainer`), sin Hilt

## Requisitos

- Android Studio Koala (2024.1) o superior.
- JDK 17. Android Studio incluye uno en `jbr`.
- Android SDK 34 instalado.
- Dispositivo o emulador con Android 8.0 (API 26) o superior.
- Para regenerar el catálogo: Python 3.10 o superior.

> El wrapper usa Gradle 8.7. Esa versión de Gradle no arranca con Java 24, así
> que para compilar desde la terminal hay que apuntar `JAVA_HOME` a un JDK 17:
>
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
> .\gradlew.bat :app:assembleDebug
> ```

## Cómo ejecutar

1. Abrir la carpeta del proyecto con Android Studio.
2. Esperar la sincronización de Gradle.
3. Seleccionar un dispositivo/emulador y ejecutar `app`.

APK de debug generado en `app/build/outputs/apk/debug/app-debug.apk`.

También se incluye una copia compilada lista para instalar en
`dist/FEHCatalog-1.0-debug.apk`:

```powershell
adb install -r dist\FEHCatalog-1.0-debug.apk
```

## Estructura

```
app/src/main/
├── assets/
│   └── heroes.json            # Catálogo base generado desde la wiki
├── res/drawable-nodpi/        # Iconos de armas, movimiento, juegos y rarezas
└── java/com/simon/fehcatalog/
    ├── FehApplication.kt      # Application + Coil con Referer
    ├── MainActivity.kt
    ├── data/
    │   ├── AppContainer.kt    # DI manual
    │   ├── CatalogHero.kt     # Entidad del catálogo inmutable
    │   ├── CatalogSeeder.kt   # Importa y actualiza heroes.json
    │   ├── CollectionEntry.kt # Datos personales + modelo combinado
    │   ├── Converters.kt      # Enums/sets <-> texto para Room
    │   ├── FehDatabase.kt     # Base Room v7 y migraciones 1 -> 2, 2 -> 3, 3 -> 4, 4 -> 5, 5 -> 7, 6 -> 7
    │   ├── HeroDao.kt
    │   └── HeroRepository.kt  # Catálogo + colección
    ├── model/
    │   ├── Enums.kt           # Juegos, tipos de unidad, armas, merges
    │   └── Search.kt          # Normalización para búsqueda
    └── ui/
        ├── FehCatalogApp.kt   # Navegación catálogo -> detalle
        ├── catalog/           # Lista, filtros y tarjeta
        ├── components/        # Retrato y etiquetas
        ├── detail/            # Ficha y controles de colección
        └── theme/
```

`tools/generate_catalog.py` es el generador reproducible del catálogo.

Los iconos originales están en `iconos_armas/`, `iconos_movimiento/`,
`iconos_juegos/` e `iconos_rarezas/`; Android usa las copias normalizadas de
`app/src/main/res/drawable-nodpi/`.

## Modelo de datos

### `catalog_heroes` (base inmutable)

| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long | ID estable derivado de la página de la wiki |
| `page` | String | Título de la página; índice único |
| `name` | String | Nombre del héroe |
| `title` | String | Título/epíteto |
| `imageUrl` | String? | Retrato en la CDN de Fandom |
| `movement` | MovementType | Infantería, acorazado, jinete, volador |
| `weaponColor` | WeaponColor | Rojo, azul, verde, incoloro |
| `weaponType` | WeaponType | Espada, lanza, hacha, arco, daga, tomo, bastón, aliento, garra |
| `originGames` | Set<FireEmblemGame> | Uno o varios juegos |
| `gameSort` | Int | Orden estándar de FEH |
| `unitTypes` | Set<UnitType> | Categorías de disponibilidad; no son excluyentes |
| `bannerType` | String? | Origen: tema seasonal o `New Heroes`/`Legendary`/`Mythic`/`Emblem`; `null` si no aplica |
| `releaseDate` | String? | Fecha de lanzamiento si la wiki la tiene |
| `gameVersion` | String? | Versión de FEH en la que debutó (`1.0`, `9.11`, `10.10`, etc.) |
| `revival` | Boolean | Etiqueta `Special Rate`; nombre de columna histórico |

### `collection_entries` (datos personales)

| Campo | Tipo | Notas |
|---|---|---|
| `heroId` | Long | PK y FK a `catalog_heroes.id` con `ON DELETE CASCADE` |
| `owned` | Boolean | El jugador lo obtuvo |
| `copies` | Int | Copias actuales (0..99) |
| `kept` | Boolean | Aún conserva al menos una copia |
| `merges` | Int | +0 a +10 |
| `favoriteSlot` | Int | `0` = sin favorito; `1..8` = tipo de favorito |
| `notes` | String | Texto libre |
| `updatedAt` | Long | Marca de tiempo |

### Juegos estándar FEH

`Heroes`, `Shadow Dragon / (New) Mystery`, `Echoes`,
`Genealogy of the Holy War`, `Thracia 776`, `The Binding Blade`,
`The Blazing Blade`, `The Sacred Stones`, `Path of Radiance`, `Radiant Dawn`,
`Awakening`, `Fates`, `Three Houses` (incluye Three Hopes),
`Tokyo Mirage Sessions #FE Encore`, `Engage`, `Fire Emblem Shadows` y
`Fortune's Weave`.

### Tipos de unidad

`Regular 5★`, `Regular 3-4★`, `Special`, `Legendary`, `Mythic`, `Duo`,
`Harmonized`, `Ascended`, `Rearmed`, `Attuned`, `Emblem`, `Aided`, `Entwined`,
`Chosen`, `Vista`, `Brave` (CYL), `Story`, `GHB` y `Tempest`.

Un héroe puede tener varios tipos a la vez; por ejemplo, un héroe Duo suele
mostrar `Special · Duo`, un Ascended del pool regular muestra
`Regular 5★ · Ascended`, y un héroe de Tempest Trials seasonal muestra
`Special · Tempest`.

### Migración

La base pasa por estas migraciones:

1. **1 → 2**: elimina la tabla antigua `heroes` y crea `catalog_heroes` y
   `collection_entries`.
2. **2 → 3**: agrega la columna `gameVersion` a `catalog_heroes`.
3. **3 → 4**: reemplaza el favorito booleano por `favoriteSlot`. Los favoritos
   existentes se convierten en el tipo 1 (rosa); el resto queda en `0`.
4. **4 → 5**: agrega la columna `revival` a `catalog_heroes`, con `0` por
   defecto. El nombre interno se mantiene por compatibilidad; en la interfaz
   se muestra como `Special Rate`.
5. **5 → 7**: puente para bases v5; su esquema ya coincide con v7.
6. **6 → 7**: recrea `catalog_heroes` para eliminar una columna temporal.

El esquema exportado está en `app/schemas/` y se usa para validar las
migraciones. No se usa `fallbackToDestructiveMigration` para no perder la
colección en actualizaciones futuras.

## Pruebas y compilación

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain --no-daemon
```

Los tests unitarios cubren normalización de búsqueda, copias, merges y
conversores de Room.

## Notas de compilación

- Las versiones están fijadas a AGP 8.5.2, Gradle 8.7, Kotlin 1.9.24 y SDK 34
  para coincidir con Android Studio Koala y el SDK instalado. Actualizar a
  versiones más nuevas (AGP 9, Kotlin 2.x, SDK 35+) requiere actualizar el
  entorno de Android Studio/Gradle a la vez.
- Si el proyecto se copia a una carpeta sincronizada en la nube (OneDrive,
  Dropbox, etc.), Gradle puede fallar con `AccessDeniedException` o
  `Unable to delete directory` en `app/build` por bloqueos de archivos de
  Windows. Lo ideal es mantenerlo en una ruta local como
  `C:\Users\<usuario>\proyectos`.

## Instalación en el teléfono

La APK publicada está firmada con el keystore de depuración de este PC
(`~/.android/debug.keystore`). Para actualizar sobre una instalación existente,
la nueva APK debe estar firmada con la misma clave y tener un `versionCode`
igual o mayor; de lo contrario Android rechaza la actualización por conflicto
de firma. Una actualización conserva la base de datos y la colección: no hay
que desinstalar.

En el Samsung SM-A315G (Android 12) la instalación desde My Files se quedaba en
`Installing…` porque el verificador de Play Protect
(`com.google.android.verifier`) era terminado por `lmkd` al no haber memoria
suficiente. Con la verificación de paquetes desactivada la instalación terminó
en unos 10 segundos:

```powershell
# Desactivar temporalmente la verificación (equivale a Play Protect →
# Analizar apps con Play Protect → desactivado)
adb shell settings put global package_verifier_enable 0

# Restaurar la verificación
adb shell settings put global package_verifier_enable 1
```

Si se prefiere mantener Play Protect activo, antes de instalar conviene cerrar
apps en segundo plano para liberar RAM y repetir la instalación; como
alternativa, `adb install -r <apk>` (o `pm install -r`) no pasa por el
verificador y funciona aunque la verificación esté activa.

## Próximos pasos sugeridos

1. Agregar un filtro por origen/banner.
2. Agregar estadísticas base (HP, Atk, Spd, Def, Res), rareza, nivel, IVs y
   dragonflowers.
3. Relacionar habilidades (arma, asistencia, especial, A/B/C/S) y sellos.
4. Importar/exportar la colección (CSV o JSON) y hacer copias de seguridad.
5. Añadir ordenamientos y estadísticas de colección.
6. Mover los textos de la interfaz a `strings.xml`.
