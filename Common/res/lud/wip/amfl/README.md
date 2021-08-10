# 40 Bridges

## Implementation

Implemented as a diamond board instead of tiling T488. Ludii does support this,
but I don't see a way to have diamond terminal points on the outside.

- Pro: Connectivity is semantically a diamond board. This makes the code simpler.
- Con: There are no bridges of the game's namesake.

TODO:

Suicide is legal. Simultaneous suicide-capture is handled with Go rules - The
aggressor captures. This is contrary to the published rules, which imply a
choice by stating that capture is "at the discretion of the aggressor".

## Rule clarity

- What are the exact end conditions?
    - What if a player only has two octogon pieces? They cannot perform captures, nor reach the victory square.
