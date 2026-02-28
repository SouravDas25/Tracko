import datetime
import time

try:
    from dateutil import parser as date_parser
except Exception:
    date_parser = None


def parse_date_to_epoch_ms(date_str: str | None) -> int:
    if date_str is None or not str(date_str).strip():
        return int(time.time() * 1000)

    s = str(date_str).strip()
    if s.isdigit():
        return int(s)

    if "date_parser" in globals() and date_parser is not None:
        try:
            dt = date_parser.parse(s)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=datetime.timezone.utc)
            return int(dt.timestamp() * 1000)
        except Exception:
            pass

    try:
        s2 = s.replace("Z", "+00:00")
        dt = datetime.datetime.fromisoformat(s2)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=datetime.timezone.utc)
        return int(dt.timestamp() * 1000)
    except Exception:
        pass

    patterns = [
        "%Y-%m-%dT%H:%M:%S%z",
        "%Y-%m-%dT%H:%M:%SZ",
        "%Y-%m-%dT%H:%M:%S",
        "%Y-%m-%d",
    ]
    for p in patterns:
        try:
            dt = datetime.datetime.strptime(s, p)
            if p.endswith("%z") or p.endswith("Z"):
                if dt.tzinfo is None:
                    dt = dt.replace(tzinfo=datetime.timezone.utc)
            else:
                dt = dt.replace(tzinfo=datetime.timezone.utc)
            return int(dt.timestamp() * 1000)
        except Exception:
            continue

    return int(time.time() * 1000)
