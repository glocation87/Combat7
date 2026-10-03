# Combat7

A Paper 26.2 plugin that adds skill based sword combat.

| Move | How | What happens |
| --- | --- | --- |
| Parry | Right click with a sword | Opens a 0.3s window. A melee hit from the front in that window is cancelled, the attacker is knocked back and can't deal melee damage for a second. A clean parry refunds the cooldown. |
| Dash | Double tap W, A, S or D with a sword out | Launches you that way and leaves glowing afterimage clones along the path. |
| Clash | Two sword fighters hit each other within 4 ticks | Neither takes damage, both bounce back, sparks fly where the blades meet. |
| Shield | Crouch with a sword in hand | A shield appears in front of you and blocks hits from the front. It vanishes when you stand up or switch off the sword. |

All timings and strengths live in `config.yml`, `/combat7 reload` applies changes without a restart.

## Running it

```
./gradlew runServer
```

Starts a dev server on port 25567 with Combat7, ViaVersion and ViaBackwards, in offline mode so test bots can join.

## Test bots

Needs Node 22. Install once with `npm install` inside `testbot/`.

**Sparring partner**: `node bot.js` (or `node bot.js SomeName`). Give it a sword with `/give SparBot iron_sword` (or op it), then type `!help` in chat:

- `!fight` spars with you, mixing attacks, parries, dashes and shielding
- `!attack` hits you on a steady 1.5s beat, good for practicing parry timing
- `!parry` stands still and tries to parry you
- `!counter` swings back the moment you swing, to trigger clashes
- `!shield`, `!dash <dir>`, `!come`, `!stop`

**Self test**: `node selftest.js` runs two bots through every mechanic and prints PASS/FAIL for each check. It builds a platform at y=200 near 0,0 and needs `C7Alpha` to be op (`op C7Alpha` in the server console once).
