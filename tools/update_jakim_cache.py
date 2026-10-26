#!/usr/bin/env python3
"""Refresh bundled annual official schedules atomically; no GPS/user data is sent."""
import datetime as dt
import hashlib
import json
from pathlib import Path
import urllib.request

ZONES = ("SGR01", "SGR02", "SGR03", "WLY01")
MONTHS = {"Jan": 1, "Feb": 2, "Mac": 3, "Mar": 3, "Apr": 4, "Mei": 5,
          "May": 5, "Jun": 6, "Jul": 7, "Ogos": 8, "Aug": 8, "Sep": 9,
          "Okt": 10, "Oct": 10, "Nov": 11, "Dis": 12, "Dec": 12}
KEYS = ("fajr", "syuruk", "dhuhr", "asr", "maghrib", "isha")
ROOT = Path(__file__).resolve().parents[1] / "assets" / "jakim"


def validate(zone, content):
    data = json.loads(content)
    if data.get("zone") != zone or not data.get("status", "").startswith("OK"):
        raise ValueError(f"Official response zone/status mismatch: {zone}")
    dates = []
    for row in data["prayerTime"]:
        day, month, year = row["date"].split("-")
        dates.append(dt.date(int(year), MONTHS[month], int(day)))
        times = [dt.time.fromisoformat(row[key]) for key in KEYS]
        if times != sorted(set(times)):
            raise ValueError(f"Invalid prayer time order: {zone} {row['date']}")
    if not dates or len(set(dates)) != len(dates):
        raise ValueError("Missing or duplicate dates")
    first, last = min(dates), max(dates)
    if first != dt.date(first.year, 1, 1) or last != dt.date(first.year, 12, 31):
        raise ValueError("Response is not a full calendar year")
    if len(dates) != (last - first).days + 1:
        raise ValueError("Missing days in annual timetable")
    return first, last, len(dates)


def main():
    metadata = {"source": "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=year&zone=<zone>",
                "retrieved_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "zones": {}}
    downloaded = {}
    for zone in ZONES:
        url = metadata["source"].replace("<zone>", zone)
        req = urllib.request.Request(url, headers={"User-Agent": "SagaDash/1.0"})
        with urllib.request.urlopen(req, timeout=30) as response:
            content = response.read(2_000_001)
        if len(content) > 2_000_000:
            raise ValueError("Response too large")
        first, last, count = validate(zone, content)
        downloaded[zone] = content
        metadata["zones"][zone] = {"days": count, "first": first.isoformat(), "last": last.isoformat(),
                                   "sha256": hashlib.sha256(content).hexdigest()}
        print(f"Validated {zone}: {count} days, {first} to {last}")
    ROOT.mkdir(parents=True, exist_ok=True)
    for zone, content in downloaded.items():
        tmp = ROOT / (zone + ".json.tmp")
        tmp.write_bytes(content)
        tmp.replace(ROOT / (zone + ".json"))
    (ROOT / "source.json").write_text(json.dumps(metadata, indent=2) + "\n")


if __name__ == "__main__":
    main()
