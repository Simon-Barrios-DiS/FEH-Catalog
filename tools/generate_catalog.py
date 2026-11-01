#!/usr/bin/env python3
"""
Genera el catálogo base de FEH Catalog a partir de la lista de héroes de la wiki.

La página List_of_Heroes se renderiza con Module:HeroList y contiene una fila
por héroe con atributos data-* que ya traen movimiento, arma, juego y
disponibilidad. Se usa la API action=parse para evitar el reto de Cloudflare de
la página HTML normal.

Uso:
    python tools/generate_catalog.py
    python tools/generate_catalog.py --html archivo.html
    python tools/generate_catalog.py --out app/src/main/assets/heroes.json
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import html
import json
import re
import sys
import unicodedata
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from collections import Counter
from datetime import date
from pathlib import Path

API_URL = (
    "https://feheroes.fandom.com/api.php"
    "?action=parse&page=List_of_Heroes&prop=text&format=json&formatversion=2"
)
THEMES_API_URL = (
    "https://feheroes.fandom.com/api.php"
    "?action=parse&page=List_of_Special_Heroes_by_theme&prop=text"
    "&format=json&formatversion=2"
)
NEW_HEROES_PAGE = "Summoning_event_archive/New_Heroes"
LME_PAGE = "Summoning_event_archive/Legendary_&_Mythic_&_Emblem_Heroes"
CARGO_URL = "https://feheroes.fandom.com/api.php"
USER_AGENT = "FEHCatalog/0.5 (catálogo personal; generate_catalog.py)"

# Tipos que identifican a un héroe LME. Se usan para reconocer al héroe nuevo
# de cada banner Legendary/Mythic/Emblem, no a todos los destacados.
LME_UNIT_TYPES = {"LEGENDARY", "MYTHIC", "EMBLEM"}

# Namespaces internos de un archivo .xlsx (Excel moderno).
VERSIONS_NS = {
    "main": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "pkg": "http://schemas.openxmlformats.org/package/2006/relationships",
}

# Secciones de List_of_Special_Heroes_by_theme y etiqueta corta que se muestra
# en la ficha. La lista es la fuente oficial para considerar a un héroe
# "seasonal"; las etiquetas se mantienen breves para que quepan en la ficha.
BANNER_BY_THEME = {
    "New Year": "Año Nuevo",
    "Desert Festivals (January)": "Desierto",
    "Day of Devotion (February)": "San Valentín",
    "Spring (March)": "Pascua",
    "Children (April)": "Niños",
    "Bridal Festival (May)": "Boda",
    "Summer (June,July)": "Verano",
    "Harvest Festival (October)": "Halloween",
    "Ninjas (November)": "Ninjas",
    "Winter (December)": "Navidad",
    "Hot Spring": "Termas",
    "Hoshidan Summer Festival (August)": "Hoshido",
    "Pirate Festival (August)": "Piratas",
    "Thief Festival (August)": "Ladrones",
    "Tea Party (August)": "Té",
    "Dance Festival (September)": "Danza",
    "Local Festival (September)": "Local",
    # La sección contiene por ahora la tabla del banner de pícnic de 2019.
    "Unique themes": "Pícnic",
}

SEASONAL_BANNERS = set(BANNER_BY_THEME.values())

SPECIAL_RATE_EXCLUDED_BANNERS = {"Legendary", "Mythic", "Emblem"}
SPECIAL_RATE_EXCLUDED_UNIT_TYPES = {
    "LEGENDARY",
    "MYTHIC",
    "EMBLEM",
    "GHB",
    "TEMPEST",
    "STORY",
    "REARMED",
    "ATTUNED",
    "AIDED",
    "ENTWINED",
    "VISTA",
    "REGULAR_4",
}

# Lista local con los héroes que la wiki ofrece como 4★–5★ Special Rate. Se
# convierten en el tipo `SPECIAL_4_5`, que usa el icono de rareza Regular 1-4.
SPECIAL_RATE_LIST_DEFAULT = "special_rate_heroes.csv"
SPECIAL_RATE_LIST_TYPE = "SPECIAL_4_5"


GAME_BY_SORT = {
    0: "HEROES",
    1: "SHADOW_DRAGON_NEW_MYSTERY",
    2: "ECHOES",
    3: "GENEALOGY_OF_THE_HOLY_WAR",
    4: "THRACIA_776",
    5: "BINDING_BLADE",
    6: "BLAZING_BLADE",
    7: "SACRED_STONES",
    8: "PATH_OF_RADIANCE",
    9: "RADIANT_DAWN",
    10: "AWAKENING",
    11: "FATES",
    12: "THREE_HOUSES",
    13: "TOKYO_MIRAGE_SESSIONS",
    14: "ENGAGE",
    15: "SHADOWS",
    16: "FORTUNES_WEAVE",
}

# Algunas filas nuevas de la wiki llegan sin data-titles. En ese caso se usa
# el data-sort-value de la celda de juego y los enlaces de esa misma celda.
GAME_SORT_BY_WIKI_TITLE = {
    "Fire Emblem Heroes": 0,
    "Fire Emblem: Shadow Dragon & the Blade of Light": 1,
    "Fire Emblem: Mystery of the Emblem": 1,
    "Fire Emblem Echoes: Shadows of Valentia": 2,
    "Fire Emblem: Genealogy of the Holy War": 3,
    "Fire Emblem: Thracia 776": 4,
    "Fire Emblem: The Binding Blade": 5,
    "Fire Emblem: The Blazing Blade": 6,
    "Fire Emblem: The Sacred Stones": 7,
    "Fire Emblem: Path of Radiance": 8,
    "Fire Emblem: Radiant Dawn": 9,
    "Fire Emblem Awakening": 10,
    "Fire Emblem Fates": 11,
    "Fire Emblem: Three Houses": 12,
    "Tokyo Mirage Sessions ♯FE": 13,
    "Fire Emblem Engage": 14,
    "Fire Emblem: Three Hopes": 12,
    "The Blazing Blade": 6,
    "Engage": 14,
}

MOVE_BY_WIKI = {
    "Infantry": "INFANTRY",
    "Armored": "ARMORED",
    "Cavalry": "CAVALRY",
    "Flying": "FLYING",
}

COLOR_BY_WIKI = {
    "Red": "RED",
    "Blue": "BLUE",
    "Green": "GREEN",
    "Colorless": "COLORLESS",
}

WEAPON_BY_WIKI = {
    "Sword": "SWORD",
    "Lance": "LANCE",
    "Axe": "AXE",
    "Bow": "BOW",
    "Dagger": "DAGGER",
    "Tome": "TOME",
    "Staff": "STAFF",
    "Breath": "BREATH",
    "Beast": "BEAST",
}

# Clases de disponibilidad que expone Module:HeroUtil.getUnitAvailabilityClasses.
UNIT_TYPE_BY_CLASS = {
    "special": "SPECIAL",
    "legendary": "LEGENDARY",
    "mythic": "MYTHIC",
    "duo": "DUO",
    "harmonized": "HARMONIZED",
    "rearmed": "REARMED",
    "attuned": "ATTUNED",
    "emblem": "EMBLEM",
    "aided": "AIDED",
    "entwined": "ENTWINED",
    "chosen": "CHOSEN",
    "vista": "VISTA",
    "story": "STORY",
    "ghb": "GHB",
    "tempest": "TEMPEST",
    "regular_5": "REGULAR_5",
    "regular_4_3_2_1": "REGULAR_4",
    "ascended": "ASCENDED",
    "brave": "BRAVE",
}

# Propiedades que pueden aparecer en el HTML pero no son tipos de unidad.
IGNORED_AVAILABILITY_CLASSES = {"fallen", "resplendent", "refresher"}

# Estas categorías existen en Cargo Properties pero no en las clases de
# disponibilidad de la lista. Se consultan aparte para no perder información.
EXTRA_UNIT_TYPES = ("ASCENDED", "BRAVE")
PROPERTY_TO_EXTRA_TYPE = {
    "ascended": "ASCENDED",
    "brave": "BRAVE",
}


def fetch_html() -> str:
    request = urllib.request.Request(API_URL, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        payload = json.load(response)
    return payload["parse"]["text"]


def fetch_extra_unit_types() -> dict[str, set[str]]:
    """Obtiene Ascended/Brave desde Cargo.

    La tabla renderizada no incluye estas dos propiedades en
    data-availability-classes, así que se complementan desde la misma fuente
    que usa la wiki. La consulta devuelve pocas filas (no llega al límite de la
    API), pero Fandom puede aplicar rate limit si se ejecuta muchas veces.
    """
    params = {
        "action": "cargoquery",
        "tables": "Units",
        "fields": "_pageName=Page,Name,Title,Properties",
        "where": (
            "(Properties__full LIKE '%ascended%' "
            "OR Properties__full LIKE '%brave%')"
        ),
        "limit": "500",
        "format": "json",
        "formatversion": "2",
    }
    url = f"{CARGO_URL}?{urllib.parse.urlencode(params)}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        payload = json.load(response)
    if "error" in payload:
        raise RuntimeError(
            f"Cargo devolvió un error: {payload['error'].get('info', payload['error'])}"
        )

    result: dict[str, set[str]] = {}
    for row in payload.get("cargoquery", []):
        title = row["title"]
        name = title.get("Name", "").strip()
        title_text = title.get("Title", "").strip()
        page = f"{name}: {title_text}" if title_text else name
        properties = {
            item.strip()
            for item in title.get("Properties", "").split(",")
            if item.strip()
        }
        tags = {
            PROPERTY_TO_EXTRA_TYPE[prop]
            for prop in properties
            if prop in PROPERTY_TO_EXTRA_TYPE
        }
        if tags:
            result[page] = tags
    return result


def fetch_theme_html() -> str:
    request = urllib.request.Request(
        THEMES_API_URL, headers={"User-Agent": USER_AGENT}
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        payload = json.load(response)
    if "error" in payload:
        raise RuntimeError(
            f"Fandom devolvió un error: {payload['error'].get('info', payload['error'])}"
        )
    return payload["parse"]["text"]


def fetch_parse_page(page: str) -> str:
    """Descarga el HTML renderizado de una página concreta de la wiki."""
    params = {
        "action": "parse",
        "page": page,
        "prop": "text",
        "format": "json",
        "formatversion": "2",
    }
    url = f"{CARGO_URL}?{urllib.parse.urlencode(params)}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        payload = json.load(response)
    if "error" in payload:
        raise RuntimeError(
            f"Fandom devolvió un error para {page}: "
            f"{payload['error'].get('info', payload['error'])}"
        )
    return payload["parse"]["text"]


def parse_theme_banners(
    html_text: str,
) -> tuple[dict[str, str], Counter[str], list[str], list[tuple[str, str, str]]]:
    """Extrae el banner de cada héroe desde List_of_Special_Heroes_by_theme.

    La página agrupa a los héroes seasonal en tablas bajo encabezados h3. Cada
    tabla puede incluir iconos (Duo/Harmonized, Heroic Grail) que también son
    enlaces; los héroes se distinguen porque su título de página siempre tiene
    el formato "Nombre: Título".
    """
    headings: list[tuple[int, int, int, str]] = []
    for match in re.finditer(r"<h([23])[^>]*>(.*?)</h\1>", html_text, re.S):
        title = re.sub("<[^>]+>", "", html.unescape(match.group(2))).strip()
        headings.append((match.start(), match.end(), int(match.group(1)), title))

    banners: dict[str, str] = {}
    counts: Counter[str] = Counter()
    unknown_headings: list[str] = []
    conflicts: list[tuple[str, str, str]] = []

    for index, (_, end, level, title) in enumerate(headings):
        if level != 3:
            continue
        banner = BANNER_BY_THEME.get(title)
        if not banner:
            unknown_headings.append(title)
            continue

        next_start = len(html_text)
        for start, _, next_level, _ in headings[index + 1:]:
            if next_level <= 3:
                next_start = start
                break
        section = html_text[end:next_start]

        pages: set[str] = set()
        for anchor in re.findall(r"<a\b[^>]*>", section, re.I):
            attrs = dict(
                re.findall(r'([a-zA-Z0-9:_-]+)="([^"]*)"', anchor)
            )
            href = html.unescape(attrs.get("href", ""))
            page = html.unescape(attrs.get("title", "")).strip()
            if not href.startswith("/wiki/") or ": " not in page:
                continue
            pages.add(page)

        for page in pages:
            previous = banners.get(page)
            if previous and previous != banner:
                conflicts.append((page, previous, banner))
            else:
                banners[page] = banner
        counts[banner] = len(pages)

    return banners, counts, unknown_headings, conflicts


def apply_theme_banners(
    heroes: list[dict], banners: dict[str, str]
) -> tuple[int, list[str]]:
    """Asigna banner y marca como Special a los héroes de la lista de temas.

    Los héroes de Tempest Trials seasonal solo traen la clase ``tempest`` en la
    lista principal. La página de temas confirma que también son Special Heroes,
    así que se añade ``SPECIAL`` sin quitar ``TEMPEST``.
    """
    catalog_pages = {hero["page"] for hero in heroes}
    applied = 0
    for hero in heroes:
        banner = banners.get(hero["page"])
        if not banner:
            continue
        hero["bannerType"] = banner
        if "SPECIAL" not in hero["unitTypes"]:
            hero["unitTypes"].insert(0, "SPECIAL")
        applied += 1
    return applied, sorted(set(banners) - catalog_pages)


def normalize_match(value: str) -> str:
    """Normaliza un texto para comparar nombres sin tildes ni mayúsculas."""
    value = unicodedata.normalize("NFKD", value)
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    return value.casefold()


def version_key(value: str) -> str:
    """Clave tolerante para emparejar nombres entre el catálogo y el Excel.

    Además de tildes y mayúsculas, elimina comillas. El Excel exporta algunos
    títulos con comillas duplicadas (por ejemplo, ``Rennac: Rich Merchant""``)
    mientras que la wiki usa ``Rennac: Rich "Merchant"``.
    """
    return (
        normalize_match(value)
        .replace('"', "")
        .replace("“", "")
        .replace("”", "")
    )


def xlsx_sheet_path(workbook: zipfile.ZipFile, sheet_name: str) -> str:
    """Devuelve la ruta interna de una hoja de Excel por su nombre."""
    workbook_root = ET.fromstring(workbook.read("xl/workbook.xml"))
    rels_root = ET.fromstring(workbook.read("xl/_rels/workbook.xml.rels"))
    rels = {
        rel.attrib["Id"]: rel.attrib["Target"]
        for rel in rels_root.findall("pkg:Relationship", VERSIONS_NS)
    }
    sheets = workbook_root.find("main:sheets", VERSIONS_NS)
    if sheets is None:
        raise ValueError("el Excel no contiene hojas")

    first_path = None
    for sheet in sheets:
        rel_id = sheet.attrib.get(
            "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
        )
        target = rels.get(rel_id)
        if not target:
            continue
        target = target.lstrip("/")
        if not target.startswith("xl/"):
            target = "xl/" + target
        if first_path is None:
            first_path = target
        if sheet.attrib.get("name") == sheet_name:
            return target
    if first_path:
        return first_path
    raise ValueError(f"no se encontró la hoja {sheet_name!r} en el Excel")


def read_versions_xlsx(path: Path) -> dict[str, str]:
    """Lee la hoja de versiones y devuelve ``{héroe: versión}``.

    Solo se usan las columnas A (héroe) y B (versión). La columna C (URL) se
    ignora deliberadamente. El lector es autónomo para no agregar dependencias
    de Python al generador.
    """
    with zipfile.ZipFile(path) as workbook:
        shared_strings: list[str] = []
        if "xl/sharedStrings.xml" in workbook.namelist():
            root = ET.fromstring(workbook.read("xl/sharedStrings.xml"))
            for item in root.findall("main:si", VERSIONS_NS):
                shared_strings.append(
                    "".join(
                        text.text or ""
                        for text in item.iter(
                            "{http://schemas.openxmlformats.org/"
                            "spreadsheetml/2006/main}t"
                        )
                    )
                )

        sheet_path = xlsx_sheet_path(workbook, "feh_versions_final")
        sheet = ET.fromstring(workbook.read(sheet_path))
        versions: dict[str, str] = {}
        for row in sheet.findall("main:sheetData/main:row", VERSIONS_NS):
            values: dict[str, str] = {}
            for cell in row.findall("main:c", VERSIONS_NS):
                ref = cell.attrib.get("r", "")
                column_match = re.match(r"([A-Z]+)", ref)
                if not column_match:
                    continue
                column = column_match.group(1)
                cell_type = cell.attrib.get("t")
                value = cell.find("main:v", VERSIONS_NS)
                if cell_type == "inlineStr":
                    inline = cell.find("main:is", VERSIONS_NS)
                    text = (
                        "".join(
                            part.text or ""
                            for part in inline.iter(
                                "{http://schemas.openxmlformats.org/"
                                "spreadsheetml/2006/main}t"
                            )
                        )
                        if inline is not None
                        else ""
                    )
                elif value is None:
                    text = ""
                elif cell_type == "s":
                    text = shared_strings[int(value.text)]
                else:
                    text = value.text or ""
                values[column] = text.strip()

            hero = values.get("A", "")
            version = values.get("B", "")
            if not hero or not version:
                continue
            if hero in {"Column1", "Heroe"}:
                continue
            versions[hero] = version
        return versions


def apply_versions(
    heroes: list[dict], versions: dict[str, str]
) -> tuple[int, list[str], list[tuple[str, list[str]]], list[str]]:
    """Asigna ``gameVersion`` a cada héroe usando el nombre de la página.

    Devuelve cuántos héroes quedaron enlazados, las filas del Excel que no
    existen en el catálogo, los nombres ambiguos y los héroes del catálogo que
    no tienen fila en el Excel.
    """
    by_key: dict[str, list[dict]] = {}
    for hero in heroes:
        by_key.setdefault(version_key(hero["page"]), []).append(hero)

    assigned_pages: set[str] = set()
    unmatched: list[str] = []
    ambiguous: list[tuple[str, list[str]]] = []
    for page, version in versions.items():
        matches = by_key.get(version_key(page), [])
        if not matches:
            unmatched.append(page)
            continue
        if len(matches) > 1:
            ambiguous.append((page, [hero["page"] for hero in matches]))
            continue
        hero = matches[0]
        hero["gameVersion"] = version
        assigned_pages.add(hero["page"])

    missing = [
        hero["page"] for hero in heroes if hero["page"] not in assigned_pages
    ]
    return len(assigned_pages), unmatched, ambiguous, missing


def read_special_rate_csv(path: Path) -> list[str]:
    """Lee ``special_rate_heroes.csv`` y devuelve los nombres de héroe.

    El archivo usa las columnas ``Heroe`` y ``Rareza``; el nombre coincide con
    la página de la wiki ("Alfonse: Sunlit Spirit").
    """
    with path.open(encoding="utf-8", newline="") as file:
        return [
            row["Heroe"].strip()
            for row in csv.DictReader(file)
            if row.get("Heroe", "").strip()
        ]


def apply_special_rate_list(
    heroes: list[dict], names: list[str]
) -> tuple[int, list[str]]:
    """Agrega el tipo ``SPECIAL_4_5`` a los héroes de la lista local.

    Devuelve cuántos héroes quedaron marcados y los nombres de la lista que no
    coinciden con una única página del catálogo.
    """
    by_key: dict[str, list[dict]] = {}
    for hero in heroes:
        by_key.setdefault(version_key(hero["page"]), []).append(hero)

    applied = 0
    unmatched: list[str] = []
    for name in names:
        matches = by_key.get(version_key(name), [])
        if len(matches) != 1:
            unmatched.append(name)
            continue
        unit_types = matches[0]["unitTypes"]
        if SPECIAL_RATE_LIST_TYPE not in unit_types:
            unit_types.append(SPECIAL_RATE_LIST_TYPE)
        applied += 1
    return applied, unmatched


def version_tuple(value: str | None) -> tuple[int, ...]:
    """Convierte ``8.7`` o ``10.10`` en una tupla comparable numéricamente."""
    if not value:
        return (999,)
    try:
        return tuple(int(part) for part in value.split("."))
    except ValueError:
        return (999,)


def apply_special_rate(heroes: list[dict]) -> int:
    """Marca la etiqueta Special Rate (campo histórico ``revival``).

    Los héroes no seasonal hasta 8.7 y los seasonal hasta 7.1 inclusive.
    Se excluyen Legendary/Mythic/Emblem y las categorías especiales de
    disponibilidad indicadas en ``SPECIAL_RATE_EXCLUDED_UNIT_TYPES``.
    """
    for hero in heroes:
        unit_types = set(hero.get("unitTypes", []))
        excluded = (
            hero.get("bannerType") in SPECIAL_RATE_EXCLUDED_BANNERS
            or bool(unit_types & SPECIAL_RATE_EXCLUDED_UNIT_TYPES)
        )
        if excluded:
            hero["revival"] = False
            continue
        limit = (7, 1) if hero.get("bannerType") in SEASONAL_BANNERS else (8, 7)
        hero["revival"] = version_tuple(hero.get("gameVersion")) <= limit
    return sum(1 for hero in heroes if hero["revival"])


def parse_summoning_archive(
    html_text: str, known_pages: set[str]
) -> list[tuple[str, list[str]]]:
    """Extrae (título del banner, páginas de héroes) de un archivo de invocación.

    Cada banner es una tabla ``wikitable``. La primera fila contiene el título
    y las celdas siguientes enlazan a los héroes destacados. Los enlaces se
    filtran contra el catálogo para no confundirlos con la página del banner.
    """
    tables: list[tuple[str, list[str]]] = []
    starts = [
        match.start()
        for match in re.finditer(
            r'<table\b[^>]*class="[^"]*wikitable[^"]*"[^>]*>', html_text
        )
    ]
    for index, start in enumerate(starts):
        end = starts[index + 1] if index + 1 < len(starts) else len(html_text)
        table = html_text[start:end]
        title_match = re.search(
            r'<th[^>]*colspan="3"[^>]*>(.*?)</th>', table, re.S
        )
        if not title_match:
            continue
        title = re.sub("<[^>]+>", "", html.unescape(title_match.group(1))).strip()
        pages: list[str] = []
        for anchor in re.findall(r"<a\b[^>]*>", table, re.I):
            attrs = dict(
                re.findall(r'([a-zA-Z0-9:_-]+)="([^"]*)"', anchor)
            )
            page = html.unescape(attrs.get("title", "")).strip()
            if page in known_pages and page not in pages:
                pages.append(page)
        if title and pages:
            tables.append((title, pages))
    return tables


def lme_category(hero: dict) -> str:
    """Etiqueta corta según el tipo LME real del héroe."""
    types = set(hero["unitTypes"])
    if "EMBLEM" in types:
        return "Emblem"
    if "MYTHIC" in types:
        return "Mythic"
    return "Legendary"


def select_lme_new_heroes(
    title: str, pages: list[str], by_page: dict[str, dict]
) -> tuple[list[str], list[str]]:
    """Selecciona los héroes LME nuevos de una tabla del archivo LME.

    El archivo incluye muchos héroes antiguos como destacados. El héroe nuevo
    se reconoce porque su nombre aparece en el título del banner. Los banners
    genéricos "Focus: Double Mythic Heroes" no nombran a nadie: en ese caso los
    dos primeros héroes LME de la tabla son los nuevos.
    """
    candidates = [
        page
        for page in pages
        if set(by_page[page]["unitTypes"]) & LME_UNIT_TYPES
    ]
    if not candidates:
        return [], []

    normalized_title = normalize_match(title)
    if "double mythic" in normalized_title:
        return candidates[:2], candidates

    selected: list[str] = []
    used_names: set[str] = set()
    for page in candidates:
        name = by_page[page]["name"]
        if name in used_names:
            continue
        pattern = r"(?<!\w)" + re.escape(normalize_match(name)) + r"(?!\w)"
        if re.search(pattern, normalized_title):
            selected.append(page)
            used_names.add(name)
    if not selected:
        # Último recurso: el primer héroe LME de la tabla. No se ha dado en el
        # archivo actual, pero evita perder una asignación si cambia el título.
        selected = candidates[:1]
    return selected, candidates


def apply_new_hero_banners(
    heroes: list[dict], tables: list[tuple[str, list[str]]]
) -> tuple[int, set[str]]:
    """Marca como New Heroes a los destacados de los banners New Heroes.

    Se aplica antes que LME para que un héroe que debutó en un banner New
    Heroes conserve esa etiqueta aunque después aparezca como destacado en un
    banner Legendary/Mythic/Emblem. No se pisa un banner seasonal ya asignado.
    """
    by_page = {hero["page"]: hero for hero in heroes}
    new_heroes_pages: set[str] = set()
    applied = 0
    for _, table_pages in tables:
        for page in table_pages:
            hero = by_page.get(page)
            if not hero:
                continue
            if hero["bannerType"] is None:
                hero["bannerType"] = "New Heroes"
                applied += 1
            if hero["bannerType"] == "New Heroes":
                new_heroes_pages.add(page)
    return applied, new_heroes_pages


def apply_lme_banners(
    heroes: list[dict],
    tables: list[tuple[str, list[str]]],
    new_heroes_pages: set[str],
) -> tuple[int, set[str], list[str]]:
    """Asigna Legendary/Mythic/Emblem a los héroes nuevos de cada banner LME.

    No se asignan todos los destacados: solo el héroe o héroes que dan nombre
    al banner (o los dos primeros LME en los banners genéricos). Si un héroe ya
    es New Heroes, se respeta esa etiqueta.
    """
    by_page = {hero["page"]: hero for hero in heroes}
    assigned: set[str] = set()
    applied = 0
    without_candidates: list[str] = []
    for title, table_pages in tables:
        selected, candidates = select_lme_new_heroes(title, table_pages, by_page)
        if not candidates:
            without_candidates.append(title)
            continue
        categories = {lme_category(by_page[page]) for page in selected}
        for page in selected:
            hero = by_page[page]
            if page in new_heroes_pages or hero["bannerType"] is not None:
                continue
            hero["bannerType"] = lme_category(hero)
            assigned.add(page)
            applied += 1

        # Algunos banners LME incluyen un héroe Duo no LME en el título, por
        # ejemplo "Duo Marth & Mythic Elm". Se le asigna la categoría del banner
        # (Mythic en ese caso) para que también tenga un origen.
        if len(categories) != 1:
            continue
        category = next(iter(categories))
        normalized_title = normalize_match(title)
        for page in table_pages:
            if page in selected or page in new_heroes_pages:
                continue
            hero = by_page[page]
            if hero["bannerType"] is not None or "DUO" not in hero["unitTypes"]:
                continue
            pattern = (
                r"(?<!\w)duo\s+"
                + re.escape(normalize_match(hero["name"]))
                + r"(?!\w)"
            )
            if re.search(pattern, normalized_title):
                hero["bannerType"] = category
                assigned.add(page)
                applied += 1
    return applied, assigned, without_candidates


def apply_lme_type_fallbacks(heroes: list[dict]) -> list[tuple[str, str]]:
    """Completa héroes LME que no aparecen en ninguno de los dos archivos.

    El archivo LME actual omite el banner del 2026-01-29 (Byleth Emblem y Elise
    Legendary). Eir Mythic tampoco está en el archivo New Heroes. Para no
    dejarlos sin origen se usa el tipo LME que ya trae la lista principal.
    """
    fallback: list[tuple[str, str]] = []
    for hero in heroes:
        if hero["bannerType"] is not None:
            continue
        if not (set(hero["unitTypes"]) & LME_UNIT_TYPES):
            continue
        category = lme_category(hero)
        hero["bannerType"] = category
        fallback.append((hero["page"], category))
    return fallback


def apply_regular_fallbacks(heroes: list[dict]) -> list[str]:
    """Marca como New Heroes el pool regular que falta en el archivo.

    El archivo New Heroes no lista a los 4★ demote ni a los héroes del pool
    inicial. Ambos son héroes regulares, así que se etiquetan New Heroes. No se
    tocan GHB, Tempest, Story ni los seasonal ya asignados.
    """
    fallback: list[str] = []
    for hero in heroes:
        if hero["bannerType"] is not None:
            continue
        if not (set(hero["unitTypes"]) & {"REGULAR_4", "REGULAR_5"}):
            continue
        hero["bannerType"] = "New Heroes"
        fallback.append(hero["page"])
    return fallback


def stable_id(page: str) -> int:
    """ID numérico estable a partir del título de la página de la wiki.

    Se usa un hash de 63 bits para no depender de IntID (que no está en la
    tabla renderizada) y para que los IDs sobrevivan a regeneraciones del
    catálogo. La probabilidad de colisión con 1.400 héroes es despreciable.
    """
    digest = hashlib.sha256(page.encode("utf-8")).digest()
    value = int.from_bytes(digest[:8], "big") & 0x7FFFFFFFFFFFFFFF
    return value or 1


def parse_weapon(value: str) -> tuple[str, str]:
    color, _, weapon = value.partition(" ")
    if color not in COLOR_BY_WIKI or weapon not in WEAPON_BY_WIKI:
        raise ValueError(f"tipo de arma desconocido: {value!r}")
    return COLOR_BY_WIKI[color], WEAPON_BY_WIKI[weapon]


def parse_rows(html_text: str) -> list[dict]:
    rows = re.findall(r'<tr class="hero-filter-element".*?</tr>', html_text, re.S)
    if not rows:
        raise RuntimeError("No se encontraron filas de héroes en el HTML")

    heroes: list[dict] = []
    seen_pages: set[str] = set()
    seen_ids: set[int] = set()

    for row in rows:
        attrs = dict(re.findall(r'data-([a-z0-9-]+)="([^"]*)"', row))
        move = attrs.get("move-type", "")
        weapon = attrs.get("weapon-type", "")
        titles = attrs.get("titles", "")
        availability_raw = html.unescape(attrs.get("availability-classes", ""))

        if not titles:
            # La fila de un héroe recién publicado puede llegar sin data-titles.
            # La celda de juego sí trae su data-sort-value y, a veces, más de un
            # juego enlazado (por ejemplo, Blazing Blade + Engage).
            game_cell = re.search(
                r'<td data-sort-value="(\d+)">\s*<i>(.*?)</i></td>', row, re.S
            )
            if game_cell:
                game_sorts = [int(game_cell.group(1))]
                for anchor_title in re.findall(
                    r'<a\b[^>]*title="([^"]+)"', game_cell.group(2)
                ):
                    sort = GAME_SORT_BY_WIKI_TITLE.get(
                        html.unescape(anchor_title)
                    )
                    if sort is not None and sort not in game_sorts:
                        game_sorts.append(sort)
                titles = ";".join(str(sort) for sort in game_sorts)

        if not move or not weapon or not titles:
            raise ValueError(f"fila incompleta: {attrs}")

        # El nombre visible es el título de la página ("Abel: The Panther").
        link = re.search(
            r'<td><a href="/wiki/[^"]+" title="([^"]+)">([^<]+)</a></td>', row
        )
        if not link:
            raise ValueError("no se encontró el enlace del héroe")
        page = html.unescape(link.group(1)).strip()
        visible_name = html.unescape(link.group(2)).strip()
        if page in seen_pages:
            raise ValueError(f"página duplicada: {page}")
        seen_pages.add(page)

        name, separator, title = visible_name.partition(": ")
        if not separator:
            name, title = visible_name, ""
        name = name.strip()
        title = title.strip()
        if not name:
            raise ValueError(f"nombre vacío en {page}")

        # La primera imagen de la fila es el retrato. Algunas son lazy-load y
        # guardan la URL real en data-src; otras ya tienen src.
        first_cell = re.search(r"<td>(.*?)</td>", row, re.S)
        if not first_cell:
            raise ValueError(f"sin celda de retrato: {page}")
        image_url = None
        for img_tag in re.findall(r"<img[^>]+>", first_cell.group(1)):
            img_attrs = dict(re.findall(r'([a-z0-9-]+)="([^"]*)"', img_tag))
            image_key = img_attrs.get("data-image-key", "")
            if image_key.endswith("_Face_FC.webp"):
                image_url = html.unescape(
                    img_attrs.get("data-src") or img_attrs.get("src") or ""
                )
                break
        if not image_url:
            raise ValueError(f"sin retrato: {page}")

        games = []
        for raw_sort in titles.split(";"):
            raw_sort = raw_sort.strip()
            if not raw_sort:
                continue
            try:
                game = GAME_BY_SORT[int(raw_sort)]
            except (ValueError, KeyError) as exc:
                raise ValueError(f"juego desconocido {raw_sort!r} en {page}") from exc
            if game not in games:
                games.append(game)
        if not games:
            raise ValueError(f"sin juego: {page}")

        classes = [item for item in availability_raw.split(";") if item]
        unit_types = []
        unknown_classes = []
        for item in classes:
            if item == "specialRate":
                # Es la tasa de 4★ especial, no un tipo de unidad. Si el héroe
                # solo tiene esta clase, pertenece al pool regular de 5★.
                continue
            mapped = UNIT_TYPE_BY_CLASS.get(item)
            if mapped:
                if mapped not in unit_types:
                    unit_types.append(mapped)
            elif item in IGNORED_AVAILABILITY_CLASSES:
                continue
            else:
                unknown_classes.append(item)
        if not unit_types and "specialRate" in classes:
            unit_types.append("REGULAR_5")
        if not unit_types:
            raise ValueError(f"sin tipo de unidad: {page} ({classes})")
        if unknown_classes:
            print(
                f"aviso: clases desconocidas en {page}: {sorted(set(unknown_classes))}",
                file=sys.stderr,
            )

        color, weapon_type = parse_weapon(weapon)
        move_type = MOVE_BY_WIKI.get(move)
        if not move_type:
            raise ValueError(f"movimiento desconocido {move!r} en {page}")

        release_match = re.search(r"<td>(\d{4}-\d{2}-\d{2})</td>\s*</tr>", row)
        release_date = release_match.group(1) if release_match else None

        hero_id = stable_id(page)
        if hero_id in seen_ids:
            raise ValueError(f"colisión de ID para {page}: {hero_id}")
        seen_ids.add(hero_id)

        heroes.append(
            {
                "id": hero_id,
                "page": page,
                "name": name,
                "title": title,
                "imageUrl": image_url,
                "movement": move_type,
                "weaponColor": color,
                "weaponType": weapon_type,
                "originGames": games,
                "gameSort": int(min(int(x) for x in titles.split(";") if x.strip())),
                "unitTypes": unit_types,
                "bannerType": None,
                "releaseDate": release_date,
                "gameVersion": None,
                "revival": False,
            }
        )

    return heroes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--html", help="HTML ya descargado (evita consultar la API)")
    parser.add_argument(
        "--skip-cargo",
        action="store_true",
        help="no consultar Ascended/Brave en Cargo (catálogo sin esos tipos)",
    )
    parser.add_argument(
        "--themes-html",
        help="HTML ya descargado de List_of_Special_Heroes_by_theme",
    )
    parser.add_argument(
        "--skip-themes",
        action="store_true",
        help="no asignar banners seasonal (catálogo sin bannerType)",
    )
    parser.add_argument(
        "--new-heroes-html",
        help="HTML ya descargado de Summoning_event_archive/New_Heroes",
    )
    parser.add_argument(
        "--lme-html",
        help=(
            "HTML ya descargado de Summoning_event_archive/"
            "Legendary_&_Mythic_&_Emblem_Heroes"
        ),
    )
    parser.add_argument(
        "--skip-origins",
        action="store_true",
        help="no asignar orígenes New Heroes/Legendary/Mythic/Emblem",
    )
    parser.add_argument(
        "--versions-xlsx",
        default="Versiones.xlsx",
        help="Excel con la versión de cada héroe (por defecto Versiones.xlsx)",
    )
    parser.add_argument(
        "--skip-versions",
        action="store_true",
        help="no leer el Excel de versiones",
    )
    parser.add_argument(
        "--special-rate-csv",
        default=SPECIAL_RATE_LIST_DEFAULT,
        help=(
            "CSV con los héroes 4★–5★ Special Rate "
            f"(por defecto {SPECIAL_RATE_LIST_DEFAULT})"
        ),
    )
    parser.add_argument(
        "--skip-special-rate-list",
        action="store_true",
        help="no marcar el tipo 4-5★ de la lista local",
    )
    parser.add_argument(
        "--out",
        default="app/src/main/assets/heroes.json",
        help="ruta del JSON de salida",
    )
    args = parser.parse_args()

    if args.html:
        html_text = Path(args.html).read_text(encoding="utf-8")
    else:
        print("Descargando List_of_Heroes desde la API de Fandom…", file=sys.stderr)
        html_text = fetch_html()

    heroes = parse_rows(html_text)

    if not args.skip_cargo:
        print("Consultando Ascended/Brave en Cargo…", file=sys.stderr)
        try:
            extra_types = fetch_extra_unit_types()
        except Exception as error:  # noqa: BLE001 - mensaje claro para el usuario
            print(f"Error al consultar Cargo: {error}", file=sys.stderr)
            print(
                "Se puede generar un catálogo sin Ascended/Brave con --skip-cargo.",
                file=sys.stderr,
            )
            return 2

        known_pages = {hero["page"] for hero in heroes}
        applied = 0
        for hero in heroes:
            tags = extra_types.get(hero["page"])
            if not tags:
                continue
            applied += 1
            for tag in EXTRA_UNIT_TYPES:
                if tag in tags and tag not in hero["unitTypes"]:
                    hero["unitTypes"].append(tag)
        unmatched = sorted(set(extra_types) - known_pages)
        print(
            f"Enriquecidos {applied}/{len(extra_types)} héroes con Ascended/Brave",
            file=sys.stderr,
        )
        if unmatched:
            print(
                f"aviso: {len(unmatched)} páginas de Cargo sin coincidencia: "
                f"{unmatched[:10]}",
                file=sys.stderr,
            )

    banner_counts: Counter[str] = Counter()
    if args.skip_themes:
        print(
            "Aviso: catálogo generado sin banners seasonal (--skip-themes).",
            file=sys.stderr,
        )
    else:
        try:
            if args.themes_html:
                themes_html = Path(args.themes_html).read_text(encoding="utf-8")
            else:
                print(
                    "Descargando List_of_Special_Heroes_by_theme desde la API de Fandom…",
                    file=sys.stderr,
                )
                themes_html = fetch_theme_html()
            banners, banner_counts, unknown_themes, conflicts = parse_theme_banners(
                themes_html
            )
            banner_applied, unmatched_banners = apply_theme_banners(heroes, banners)
        except Exception as error:  # noqa: BLE001 - mensaje claro para el usuario
            print(f"Error al consultar los temas seasonal: {error}", file=sys.stderr)
            print(
                "Se puede generar un catálogo sin banners con --skip-themes.",
                file=sys.stderr,
            )
            return 2

        print(
            f"Banners asignados a {banner_applied}/{len(banners)} héroes seasonal",
            file=sys.stderr,
        )
        if unknown_themes:
            print(
                f"aviso: secciones de tema sin etiqueta: {unknown_themes}",
                file=sys.stderr,
            )
        if conflicts:
            print(
                f"aviso: héroes en más de un tema: {conflicts[:10]}",
                file=sys.stderr,
            )
        if unmatched_banners:
            print(
                f"aviso: {len(unmatched_banners)} páginas de temas sin coincidencia: "
                f"{unmatched_banners[:10]}",
                file=sys.stderr,
            )

    if args.skip_origins:
        print(
            "Aviso: catálogo generado sin orígenes New Heroes/LME (--skip-origins).",
            file=sys.stderr,
        )
    else:
        try:
            if args.new_heroes_html:
                new_heroes_html = Path(args.new_heroes_html).read_text(
                    encoding="utf-8"
                )
            else:
                print(
                    "Descargando Summoning_event_archive/New_Heroes desde la API "
                    "de Fandom…",
                    file=sys.stderr,
                )
                new_heroes_html = fetch_parse_page(NEW_HEROES_PAGE)
            if args.lme_html:
                lme_html = Path(args.lme_html).read_text(encoding="utf-8")
            else:
                print(
                    "Descargando Summoning_event_archive/Legendary & Mythic & "
                    "Emblem Heroes desde la API de Fandom…",
                    file=sys.stderr,
                )
                lme_html = fetch_parse_page(LME_PAGE)

            known_pages = {hero["page"] for hero in heroes}
            new_heroes_tables = parse_summoning_archive(
                new_heroes_html, known_pages
            )
            lme_tables = parse_summoning_archive(lme_html, known_pages)
            new_applied, new_heroes_pages = apply_new_hero_banners(
                heroes, new_heroes_tables
            )
            lme_applied, _, lme_without_candidates = apply_lme_banners(
                heroes, lme_tables, new_heroes_pages
            )
            lme_fallback = apply_lme_type_fallbacks(heroes)
            regular_fallback = apply_regular_fallbacks(heroes)
        except Exception as error:  # noqa: BLE001 - mensaje claro para el usuario
            print(
                f"Error al consultar los archivos de invocación: {error}",
                file=sys.stderr,
            )
            print(
                "Se puede generar un catálogo sin orígenes con --skip-origins.",
                file=sys.stderr,
            )
            return 2

        print(
            f"Origen New Heroes asignado a {new_applied} héroes "
            f"({len(new_heroes_tables)} banners)",
            file=sys.stderr,
        )
        print(
            f"Origen LME asignado a {lme_applied} héroes "
            f"({len(lme_tables)} banners)",
            file=sys.stderr,
        )
        if lme_without_candidates:
            print(
                f"aviso: banners LME sin héroes candidatos: "
                f"{lme_without_candidates[:10]}",
                file=sys.stderr,
            )
        if lme_fallback:
            print(
                f"aviso: {len(lme_fallback)} héroes LME fuera de los archivos, "
                f"asignados por tipo: {lme_fallback}",
                file=sys.stderr,
            )
        if regular_fallback:
            print(
                f"aviso: {len(regular_fallback)} héroes del pool regular sin "
                f"banner en el archivo, asignados a New Heroes: "
                f"{regular_fallback[:10]}…",
                file=sys.stderr,
            )

    if args.skip_versions:
        print(
            "Aviso: catálogo generado sin versiones de héroe (--skip-versions).",
            file=sys.stderr,
        )
    else:
        versions_path = Path(args.versions_xlsx)
        if not versions_path.exists():
            print(
                f"Error: no se encontró {versions_path}. Usa --versions-xlsx "
                "o --skip-versions.",
                file=sys.stderr,
            )
            return 2
        try:
            versions = read_versions_xlsx(versions_path)
        except Exception as error:  # noqa: BLE001 - mensaje claro para el usuario
            print(
                f"Error al leer {versions_path}: {error}",
                file=sys.stderr,
            )
            return 2

        (
            versions_applied,
            versions_unmatched,
            versions_ambiguous,
            versions_missing,
        ) = apply_versions(heroes, versions)
        print(
            f"Versiones asignadas a {versions_applied}/{len(heroes)} héroes "
            f"({len(versions)} filas en {versions_path})",
            file=sys.stderr,
        )
        if versions_unmatched:
            print(
                f"aviso: filas del Excel sin héroe en el catálogo: "
                f"{versions_unmatched}",
                file=sys.stderr,
            )
        if versions_ambiguous:
            print(
                f"aviso: nombres ambiguos en el Excel: {versions_ambiguous}",
                file=sys.stderr,
            )
        if versions_missing:
            print(
                f"aviso: héroes sin versión en el Excel: "
                f"{versions_missing[:10]}…",
                file=sys.stderr,
            )

    if args.skip_special_rate_list:
        print(
            "Aviso: catálogo generado sin el tipo 4-5★ "
            "(--skip-special-rate-list).",
            file=sys.stderr,
        )
    else:
        special_csv = Path(args.special_rate_csv)
        if not special_csv.exists():
            print(
                f"Error: no se encontró {special_csv}. Usa --special-rate-csv "
                "o --skip-special-rate-list.",
                file=sys.stderr,
            )
            return 2
        try:
            special_names = read_special_rate_csv(special_csv)
        except Exception as error:  # noqa: BLE001 - mensaje claro para el usuario
            print(
                f"Error al leer {special_csv}: {error}",
                file=sys.stderr,
            )
            return 2

        special_applied, special_unmatched = apply_special_rate_list(
            heroes, special_names
        )
        print(
            f"4-5★: {special_applied}/{len(special_names)} héroes de "
            f"{special_csv}",
            file=sys.stderr,
        )
        if special_unmatched:
            print(
                f"aviso: filas de {special_csv} sin héroe único en el "
                f"catálogo: {special_unmatched}",
                file=sys.stderr,
            )

    special_rate = apply_special_rate(heroes)
    print(
        f"Special Rate: {special_rate}/{len(heroes)} héroes",
        file=sys.stderr,
    )
    heroes.sort(key=lambda item: (item["name"].casefold(), item["title"].casefold(), item["page"]))

    canonical = json.dumps(heroes, ensure_ascii=False, sort_keys=True).encode("utf-8")
    version = hashlib.sha256(canonical).hexdigest()[:16]

    output = {
        "version": version,
        "generatedAt": date.today().isoformat(),
        "source": "https://feheroes.fandom.com/wiki/List_of_Heroes",
        "bannerSource": (
            "https://feheroes.fandom.com/wiki/List_of_Special_Heroes_by_theme"
        ),
        "originSources": [
            "https://feheroes.fandom.com/wiki/Summoning_event_archive/New_Heroes",
            (
                "https://feheroes.fandom.com/wiki/Summoning_event_archive/"
                "Legendary_%26_Mythic_%26_Emblem_Heroes"
            ),
        ],
        "versionSource": "Versiones.xlsx (feh_versions_final)",
        "heroes": heroes,
    }

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(
        json.dumps(output, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )

    games = Counter(game for hero in heroes for game in hero["originGames"])
    types = Counter(unit_type for hero in heroes for unit_type in hero["unitTypes"])
    seasonal_labels = set(BANNER_BY_THEME.values())
    seasonal = sum(
        1 for hero in heroes if hero["bannerType"] in seasonal_labels
    )
    banner_labels = Counter(
        hero["bannerType"] for hero in heroes if hero["bannerType"]
    )
    origin_labels = Counter(
        hero["bannerType"]
        for hero in heroes
        if hero["bannerType"] and hero["bannerType"] not in seasonal_labels
    )
    without_origin = sum(1 for hero in heroes if not hero["bannerType"])
    print(f"Generado {out_path} ({out_path.stat().st_size:,} bytes)")
    print(f"Versión: {version} · héroes: {len(heroes)}")
    print("Juegos:", ", ".join(f"{game}={games[game]}" for game in GAME_BY_SORT.values()))
    print("Tipos:", ", ".join(f"{unit_type}={count}" for unit_type, count in types.most_common()))
    print(
        f"Seasonal: {seasonal} · banners: "
        + ", ".join(
            f"{label}={count}"
            for label, count in banner_labels.most_common()
            if label in seasonal_labels
        )
    )
    print(
        "Orígenes no seasonal: "
        + ", ".join(
            f"{label}={count}" for label, count in origin_labels.most_common()
        )
        + f", sin origen={without_origin}"
    )
    if not args.skip_themes and seasonal != sum(banner_counts.values()):
        print(
            "aviso: el número de héroes con banner no coincide con el total "
            f"de la página de temas ({seasonal} frente a "
            f"{sum(banner_counts.values())}).",
            file=sys.stderr,
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
