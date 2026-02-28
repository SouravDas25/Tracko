import json
import typing


def to_str(v) -> str:
    if v is None:
        return ""
    return str(v)


def clip(s: str, n: int) -> str:
    if s is None:
        return ""
    s = str(s)
    if len(s) <= n:
        return s
    if n <= 1:
        return s[:n]
    return s[: n - 3] + "..."


def print_result(result: dict, *, raw: bool = False) -> None:
    status = result.get("status")
    elapsed_ms = result.get("elapsed_ms")
    print(f"HTTP {status} ({elapsed_ms}ms)")
    if raw:
        txt = result.get("text") or ""
        if txt:
            print(txt)
        return

    payload = result.get("json")
    if payload is not None:
        print(json.dumps(payload, indent=2, sort_keys=True))
        return

    txt = result.get("text")
    if txt:
        print(txt)


def print_table(
    rows: list[dict],
    columns: list[tuple[str, str]],
    *,
    max_widths: dict[str, int] | None = None,
    right_align: set[str] | None = None,
    formatters: dict[str, typing.Callable] | None = None,
) -> None:
    # columns: [(key, header)]
    max_widths = max_widths or {}
    right_align = right_align or set()
    formatters = formatters or {}

    rendered_rows: list[list[str]] = []
    for r in rows:
        row_cells: list[str] = []
        for k, _ in columns:
            v = r.get(k)
            if k in formatters:
                try:
                    v = formatters[k](v)
                except Exception:
                    v = r.get(k)
            cell = to_str(v)
            mw = int(max_widths.get(k, 32))
            row_cells.append(clip(cell, mw))
        rendered_rows.append(row_cells)

    headers = [h for _, h in columns]
    widths = [len(h) for h in headers]
    for r_row in rendered_rows:
        for i, cell in enumerate(r_row):
            widths[i] = max(widths[i], len(cell))

    def fmt_row(cells: list[str]) -> str:
        out: list[str] = []
        for i, cell in enumerate(cells):
            key = columns[i][0]
            if key in right_align:
                out.append(cell.rjust(widths[i]))
            else:
                out.append(cell.ljust(widths[i]))
        return " | ".join(out)

    print(fmt_row(headers))
    print("-+-".join("-" * w for w in widths))
    for r_row in rendered_rows:
        print(fmt_row(r_row))
