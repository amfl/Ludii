# 40 Bridges

## Implementation

Implemented as a diamond board instead of tiling T488. Ludii does support this,
but I don't see a way to have diamond terminal points on the outside.

- Pro: Connectivity is semantically a diamond board. This makes the code simpler.
- Con: There are no bridges of the game's namesake.

## Rule clarity

- Are suicide moves legal?
    - If so, a suicide and a capture can occur simultaneously. What happens - Suicide, capture, or both?

- What are the exact end conditions?
    - What if a player only has two octogon pieces? They cannot perform captures, nor reach the victory square.
    - If simultaneous suicide-capture is legal, what if both players achieve this state at once?
