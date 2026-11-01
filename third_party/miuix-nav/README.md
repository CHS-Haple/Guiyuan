# Pinned MIUIX navigation completion-direction overlay

This directory stores a temporary, minimal, **source-pinned** patch for the
accepted `compose-miuix-ui/miuix` revision
`b9f70838509fbc3f63f1a972777a7e6ce07f578f`. Upstream MIUIX
navigation source is Apache-2.0 licensed; the patch only changes one velocity
handoff in `NavDisplay.kt`. Guiyuan's existing MIUIX third-party notices
remain applicable.

## Verified problem boundary

The platform can complete predictive back even when the final two reported
progress events have a *mild* negative velocity. At that moment the
`NavDisplay` completion accepts `velocity > -1` and retains
`released = -velocity` as the spring seed. An unbounded positive depth
velocity initially moves *away* from the committed exit target. For a
critically damped spring with frequency sqrt(146), remaining depth 0.3 and
sample velocity -0.6, the unbounded +0.6 seed moves the page farther from its
exit target during the first 1/60 s. A zero seed prevents that reversal.
See Guiyuan issue #1159. The actual B1703 device velocity trace has not been
captured, so this is a demonstrated code-level cause, not yet a verified match
to the user's individual gesture.

## Why this implementation

- Preferred long-term: a reviewed upstream MIUIX change and a new tested pin.
  The pinned upstream revision currently does not contain such a change.
- This **interim** solution is narrowly bounded to the upstream-owned velocity
  handoff and only after the pinned checkout passes both exact revision and
  original file-blob checks. The build fails closed if upstream source differs.
  No second gesture recognizer, animation owner, global damping change, or
  hidden `NavDisplay` replacement is introduced.
- An app-side custom `NavTransition` would alter the global animation contract
  rather than fixing the upstream driver's specific sign error.

The action at `.github/actions/miuix-source/action.yml` applies the patch
after its independent AGP-catalog alignment, checks the exact affected files
and records both overlays in the workflow summary. Corresponding JUnit
regression tests are in `PredictiveBackCommitDirectionTest`. **The built
composite tree is not byte-identical to upstream revision**; never report it
as an unmodified upstream UI. Remove this patch and associated guards once
an equivalent upstream correction is separately source- and device-qualified.

The user's final OS4 system predictive-back acceptance is still required
before this behavior can enter the 1.0.0 stable release.
