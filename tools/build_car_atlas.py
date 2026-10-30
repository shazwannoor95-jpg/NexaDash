#!/usr/bin/env python3
"""Pack eight transparent PNG views for the existing SagaAngleView, without changing the app."""
import argparse
from pathlib import Path
import os
import tempfile
import warnings

from PIL import Image, ImageOps


SIZE = (1774, 887)
# Exact coordinates used by SagaAngleView; this is not a uniform 4x2 grid.
BOUNDS = (
    (58, 127, 385, 396), (437, 129, 860, 398),
    (885, 171, 1337, 362), (1360, 136, 1752, 394),
    (59, 533, 376, 810), (425, 537, 857, 800),
    (876, 583, 1331, 773), (1337, 540, 1759, 804),
)
MAX_PIXELS = 16_777_216


def build(input_dir, output, force=False):
    sources = [input_dir / f"{index:02}.png" for index in range(1, 9)]
    missing = [path.name for path in sources if not path.is_file()]
    if missing:
        raise ValueError("Missing PNG views: " + ", ".join(missing))
    if output.suffix.lower() != ".png":
        raise ValueError("Output must end in .png")
    if output.resolve() in {path.resolve() for path in sources}:
        raise ValueError("Output must not replace a source image")
    if output.exists() and not force:
        raise ValueError("Output already exists; choose another path or pass --force")

    with Image.new("RGBA", SIZE, (0, 0, 0, 0)) as atlas:
        for path, (left, top, right, bottom) in zip(sources, BOUNDS):
            with Image.open(path) as source:
                if source.format != "PNG" or getattr(source, "n_frames", 1) != 1:
                    raise ValueError(f"{path.name}: choose a single-frame PNG")
                if source.width * source.height > MAX_PIXELS:
                    raise ValueError(f"{path.name}: limit each image to 16 megapixels")
                # EXIF orientation is applied before trimming; no background removal or AI here.
                with ImageOps.exif_transpose(source).convert("RGBA") as rgba:
                    with rgba.getchannel("A") as alpha:
                        bounds = alpha.getbbox()
                        if bounds is None:
                            raise ValueError(f"{path.name}: image is entirely transparent")
                        if alpha.getextrema()[0] == 255:
                            raise ValueError(f"{path.name}: remove the background and export transparent PNG")
                    with rgba.crop(bounds) as cropped:
                        cropped.thumbnail((right - left, bottom - top), Image.Resampling.LANCZOS)
                        x = left + (right - left - cropped.width) // 2
                        y = top + (bottom - top - cropped.height) // 2
                        atlas.alpha_composite(cropped, (x, y))

        output.parent.mkdir(parents=True, exist_ok=True)
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(dir=output.parent, suffix=".png", delete=False) as handle:
                temporary = Path(handle.name)
                atlas.save(handle, format="PNG", optimize=True)
            os.replace(temporary, output)
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True, help="Directory with 01.png through 08.png")
    parser.add_argument("--output", type=Path, default=Path("build/car-preview/saga_angles.png"))
    parser.add_argument("--force", action="store_true", help="Replace an existing output PNG")
    args = parser.parse_args()
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            build(args.input, args.output, args.force)
    except (OSError, ValueError, Image.DecompressionBombError, Image.DecompressionBombWarning) as error:
        parser.error(str(error))
    print(f"Created {args.output} — 1774x887 RGBA, 8 views; source PNGs unchanged.")
    print("Copy into res/drawable-nodpi/saga_angles.png in your own checkout, then rebuild the APK.")


if __name__ == "__main__":
    main()
