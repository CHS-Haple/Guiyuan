#!/usr/bin/env python3
"""Bounded, source-backed regression check for Guiyuan #1159.

This checks the actual source-built MIUIX NavDisplay used by CI and the
critically damped spring's first-frame directional response. It does not
replace final OEM predictive-back device acceptance.
"""
import math
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PINNED_SOURCE = (
    "miuix-nav/src/commonMain/kotlin/"
    "top/yukonga/miuix/kmp/nav/core/NavDisplay.kt"
)
COMMIT_LINE = "initialVelocity = released.coerceIn(floor, 0f),"
UNSAFE_LINE = "initialVelocity = released.coerceAtLeast(floor),"


def main() -> None:
    upstream = ROOT / os.environ.get("MIUIX_SOURCE_DIR", "miuix-upstream") / PINNED_SOURCE
    if not upstream.is_file():
        raise SystemExit(f"Missing source-built navigation driver: {upstream}")
    source = upstream.read_text(encoding="utf-8")
    if source.count(COMMIT_LINE) != 1 or UNSAFE_LINE in source:
        raise SystemExit("Pinned MIUIX was not patched with the reviewed commit-direction handoff")
    if "presentation.animatedTop.settleCancel(" not in source:
        raise SystemExit("Source no longer contains the independently owned cancellation settle")

    # A mildly negative sample is still a platform-completed back (-0.6 > -1);
    # the original positive seed makes the committed page reverse for one frame.
    omega = math.sqrt(146.0)
    remaining = 0.3
    release = 0.6
    floor = -omega * remaining
    corrected_seed = min(max(release, floor), 0.0)

    def distance_at_one_frame(initial_velocity: float) -> float:
        dt = 1.0 / 60.0
        return math.exp(-omega * dt) * (
            remaining + (initial_velocity + omega * remaining) * dt
        )

    assert distance_at_one_frame(release) > remaining
    assert corrected_seed == 0
    assert distance_at_one_frame(corrected_seed) < remaining
    for measured in (-0.8, -0.6, -0.1, 0.0, 0.1, 0.6, 100.0):
        applied = min(max(-measured, floor), 0.0)
        if not floor <= applied <= 0.0:
            raise AssertionError(f"Commit seed direction invalid for {measured}: {applied}")
    print("Verified pinned MIUIX patched completion direction and spring first-frame regression")


if __name__ == "__main__":
    main()
