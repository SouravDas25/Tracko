import importlib.util
import os
import sys
from types import ModuleType


def _load_legacy_module() -> ModuleType:
    """Load the legacy single-file CLI implementation (tracko-cli/tracko_cli.py).

    This lets us introduce a package entrypoint (`python -m tracko_cli`) without
    breaking existing behavior while we progressively split commands into modules.
    """

    pkg_dir = os.path.dirname(__file__)
    legacy_path = os.path.abspath(os.path.join(pkg_dir, os.pardir, "tracko_cli.py"))

    spec = importlib.util.spec_from_file_location("tracko_cli_legacy", legacy_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load legacy CLI module from: {legacy_path}")

    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def build_parser():
    legacy = _load_legacy_module()
    return legacy.build_parser()


def main(argv: list[str]) -> int:
    legacy = _load_legacy_module()
    return int(legacy.main(argv))


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
