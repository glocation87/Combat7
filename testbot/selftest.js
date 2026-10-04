// Two bots run through every mechanic and check the results. C7Alpha has to be op (run "op C7Alpha" in the console once).
const { connect, dash, parry, setShield, lookAt, ensureSword, sleep, log } = require("./lib");

const Y = 200;
const alpha = connect("C7Alpha");
const beta = connect("C7Beta");
const results = [];

// mineflayer reads team flags from a field that no longer exists, so keep the raw ones per team
const teamFlags = {};
alpha._client.on("teams", packet => {
    if (packet.flags) {
        teamFlags[packet.team] = packet.flags;
    }
});

function check(label, ok, detail) {
    results.push(ok);
    log("selftest", `${ok ? "PASS" : "FAIL"} ${label}${detail ? `  (${detail})` : ""}`);
}

const run = command => alpha.chat(command);
const entityOf = (bot, other) => bot.players[other.username]?.entity;
const pos = bot => bot.entity.position.clone();
const fmt = vec => `${vec.x.toFixed(2)}, ${vec.y.toFixed(2)}, ${vec.z.toFixed(2)}`;

function countParticles(bot, names) {
    const seen = { count: 0 };
    const listener = particle => {
        if (names.includes(particle.name)) {
            seen.count++;
        }
    };
    bot.on("particle", listener);
    seen.stop = () => bot.removeListener("particle", listener);
    return seen;
}

function countSpawns(bot, entityName) {
    const seen = { count: 0, positions: [] };
    const listener = entity => {
        if (entity.name === entityName) {
            seen.count++;
            seen.positions.push(entity.position.clone());
        }
    };
    bot.on("entitySpawn", listener);
    seen.stop = () => bot.removeListener("entitySpawn", listener);
    return seen;
}

// Remembers where each entity spawned and when it went away
function watchFade(bot, entityName) {
    const spawned = new Map();
    const gone = [];
    const onSpawn = entity => {
        if (entity.name === entityName) {
            spawned.set(entity.id, entity.position.z);
        }
    };
    const onGone = entity => {
        if (spawned.has(entity.id)) {
            gone.push({ z: spawned.get(entity.id), time: Date.now() });
        }
    };
    bot.on("entitySpawn", onSpawn);
    bot.on("entityGone", onGone);
    return {
        gone: () => gone,
        stop: () => {
            bot.removeListener("entitySpawn", onSpawn);
            bot.removeListener("entityGone", onGone);
        },
    };
}

function countSounds(bot, soundName) {
    const seen = { count: 0 };
    const listener = name => {
        if (String(name).startsWith(soundName)) {
            seen.count++;
        }
    };
    bot.on("soundEffectHeard", listener);
    seen.stop = () => bot.removeListener("soundEffectHeard", listener);
    return seen;
}

// Hotbar slots are inventory slots 36 to 44
function emptyHand(bot) {
    const slot = [...Array(9).keys()].find(i => !bot.inventory.slots[36 + i]);
    bot.setQuickBarSlot(slot);
}

const near = (bot, entityName, center, radius) => Object.values(bot.entities)
    .filter(entity => entity.name === entityName && entity.position.distanceTo(center) < radius);

// Collects the floating combat text this bot gets shown, read straight out of the text display metadata
function watchTexts(bot) {
    const seen = new Map();
    const listener = entity => {
        const match = entity.name === "text_display" && JSON.stringify(entity.metadata).match(/"text":\{"type":"string","value":"([^"]*)"/);
        if (match) {
            seen.set(entity.id, match[1]);
        }
    };
    bot.on("entityUpdate", listener);
    return {
        has: pattern => [...seen.values()].some(text => pattern.test(text)),
        all: () => [...seen.values()].join(", ") || "nothing",
        stop: () => bot.removeListener("entityUpdate", listener),
    };
}

// mineflayer doesn't turn the action bar packet into its actionBar event, so this reads the packet
function watchActionBar(bot) {
    const seen = [];
    const listener = packet => seen.push(JSON.stringify(packet.text));
    bot._client.on("action_bar", listener);
    return {
        has: text => seen.some(line => line.includes(text)),
        all: () => seen.map(line => (line.match(/"text":{"type":"string","value":"([^"]*)"/) ?? [])[1]).join(", ") || "nothing",
        stop: () => bot._client.removeListener("action_bar", listener),
    };
}

// Alpha faces east at x=0.5, Beta faces west at x=3, both full health with a sword out
async function reset(alphaX = 0.5, alphaYaw = -90) {
    setShield(beta, false);
    run(`/tp C7Alpha ${alphaX} ${Y} 0.5 ${alphaYaw} 0`);
    run(`/tp C7Beta 3 ${Y} 0.5 90 0`);
    run("/effect clear @a");
    run("/effect give @a minecraft:instant_health 1 4 true");
    await sleep(1300);
    await lookAt(alpha, entityOf(alpha, beta));
    await lookAt(beta, entityOf(beta, alpha));
    await sleep(200);
}

async function setup() {
    run("/gamerule natural_health_regeneration false");
    run("/gamerule naturalRegeneration false");
    run("/time set day");
    run("/weather clear");
    run(`/fill -8 ${Y - 1} -8 8 ${Y - 1} 8 minecraft:smooth_stone`);
    run(`/fill -8 ${Y} -8 8 ${Y + 4} 8 minecraft:air`);
    run("/gamemode survival C7Alpha");
    run("/gamemode survival C7Beta");
    // otherwise every run adds another sword until the hotbar has no empty slot left
    run("/clear C7Alpha");
    run("/clear C7Beta");
    run("/give C7Alpha minecraft:iron_sword");
    run("/give C7Beta minecraft:iron_sword");
    await sleep(1500);
    check("alpha has a sword", await ensureSword(alpha));
    check("beta has a sword", await ensureSword(beta));
}

async function baseline() {
    await reset();
    const texts = watchTexts(alpha);
    alpha.attack(entityOf(alpha, beta));
    await sleep(400);
    texts.stop();
    check("plain hit does damage", beta.health < 20, `beta health ${beta.health}`);
    check("hit shows a damage number", texts.has(/^\d+(\.\d)?$/), texts.all());
}

async function parryTest() {
    await reset();
    const sparks = countParticles(alpha, ["electric_spark", "flash"]);
    const parryTexts = watchTexts(alpha);
    const before = pos(alpha);
    await parry(beta);
    await sleep(100);
    alpha.attack(entityOf(alpha, beta));
    await sleep(500);
    sparks.stop();
    parryTexts.stop();
    const pushed = before.x - pos(alpha).x;
    check("parried hit does no damage", beta.health === 20, `beta health ${beta.health}`);
    check("attacker is knocked back", pushed > 0.5, `moved ${pushed.toFixed(2)} blocks`);
    check("parry vfx shows", sparks.count > 0, `${sparks.count} particle packets`);
    check("attacker sees PARRY", parryTexts.has(/^PARRY$/), parryTexts.all());

    await reset();
    await parry(beta);
    await sleep(100);
    alpha.attack(entityOf(alpha, beta));
    await sleep(250);
    alpha.attack(entityOf(alpha, beta));
    await sleep(400);
    check("staggered attacker can't follow up", beta.health === 20, `beta health ${beta.health}`);

    await reset();
    await sleep(1200);
    await parry(beta);
    await sleep(600);
    alpha.attack(entityOf(alpha, beta));
    await sleep(400);
    check("late hit after the window lands", beta.health < 20, `beta health ${beta.health}`);
}

async function clashTest() {
    await reset();
    const sparks = countParticles(beta, ["electric_spark", "flash", "sweep_attack"]);
    const clashTexts = watchTexts(beta);
    const alphaBefore = pos(alpha);
    const betaBefore = pos(beta);
    alpha.attack(entityOf(alpha, beta));
    beta.attack(entityOf(beta, alpha));
    await sleep(250);
    const leftover = Object.values(beta.entities)
        .filter(entity => entity.name === "text_display" && /"value":"\d+"/.test(JSON.stringify(entity.metadata)));
    await sleep(350);
    sparks.stop();
    clashTexts.stop();
    check("clash takes the refunded damage number back down", leftover.length === 0, `${leftover.length} numbers still up`);
    check("clash: alpha takes no damage", alpha.health === 20, `alpha health ${alpha.health}`);
    check("clash: beta takes no damage", beta.health === 20, `beta health ${beta.health}`);
    const gap = pos(beta).x - pos(alpha).x;
    const gapBefore = betaBefore.x - alphaBefore.x;
    check("clash pushes both apart", gap > gapBefore + 0.8, `gap ${gapBefore.toFixed(2)} -> ${gap.toFixed(2)}`);
    check("clash vfx shows", sparks.count > 0, `${sparks.count} particle packets`);
    check("both see CLASH!", clashTexts.has(/^CLASH!$/), clashTexts.all());

    await reset();
    alpha.attack(entityOf(alpha, beta));
    await sleep(700);
    beta.attack(entityOf(beta, alpha));
    await sleep(400);
    check("slow trade is two normal hits", alpha.health < 20 && beta.health < 20,
        `alpha ${alpha.health}, beta ${beta.health}`);
}

async function shieldTest() {
    await reset();
    setShield(beta, true);
    await sleep(400);
    const shields = near(alpha, "item_display", pos(beta), 2);
    check("crouching with a sword raises a shield", shields.length === 1, `${shields.length} shield displays`);
    const blockTexts = watchTexts(alpha);
    alpha.attack(entityOf(alpha, beta));
    await sleep(400);
    blockTexts.stop();
    check("attacker sees BLOCK", blockTexts.has(/^BLOCK$/), blockTexts.all());
    check("shield blocks a hit from the front", beta.health === 20, `beta health ${beta.health}`);

    // Alpha goes behind Beta, still facing it
    run(`/tp C7Alpha 5.5 ${Y} 0.5 90 0`);
    await sleep(800);
    await lookAt(alpha, entityOf(alpha, beta));
    alpha.attack(entityOf(alpha, beta));
    await sleep(400);
    check("shield doesn't cover your back", beta.health < 20, `beta health ${beta.health}`);

    setShield(beta, false);
    await sleep(400);
    check("standing up drops the shield", near(alpha, "item_display", pos(beta), 3).length === 0);

    emptyHand(beta);
    await sleep(200);
    setShield(beta, true);
    await sleep(400);
    check("no shield without a sword", near(alpha, "item_display", pos(beta), 3).length === 0);
    setShield(beta, false);
    await ensureSword(beta);
}

async function uiTest() {
    await reset();
    const bar = watchActionBar(alpha);
    for (let hit = 0; hit < 3; hit++) {
        // knockback pushes Beta out of reach, so put it back in front each time
        run(`/tp C7Beta 3 ${Y} 0.5 90 0`);
        await sleep(300);
        await lookAt(alpha, entityOf(alpha, beta));
        alpha.attack(entityOf(alpha, beta));
        await sleep(400);
    }
    await lookAt(beta, entityOf(beta, alpha));
    check("hits in a row count a combo", bar.has("2 HIT COMBO") && bar.has("3 HIT COMBO"), `${bar.all()}, beta health ${beta.health}`);
    beta.attack(entityOf(beta, alpha));
    await sleep(400);
    bar.stop();
    check("getting hit breaks the combo", bar.has("COMBO BROKEN"), bar.all());

    await reset();
    const crit = watchTexts(alpha);
    alpha.setControlState("jump", true);
    await sleep(100);
    alpha.setControlState("jump", false);
    // attack on the way down, that's what makes a vanilla crit
    await sleep(350);
    alpha.attack(entityOf(alpha, beta));
    await sleep(500);
    crit.stop();
    check("falling hit shows CRIT", crit.has(/^CRIT/), crit.all());

    await reset();
    const dodge = watchTexts(alpha);
    const dashing = dash(beta, "left");
    await sleep(140);
    alpha.attack(entityOf(alpha, beta));
    await dashing;
    await sleep(400);
    dodge.stop();
    check("hit during a dash is dodged", beta.health === 20, `beta health ${beta.health}`);
    check("attacker sees DODGE", dodge.has(/^DODGE$/), dodge.all());
}

async function dashTest() {
    await reset();
    const clones = countSpawns(alpha, "mannequin");
    const fade = watchFade(alpha, "mannequin");
    const portal = countParticles(alpha, ["portal"]);
    // the bot runs 26.1 sound ids and hears the neighbouring "entity.enderman.stare", real 26.2 clients get the teleport
    const sounds = countSounds(alpha, "entity.enderman.");
    const start = pos(beta);
    // Beta faces west, so its left is south (+z)
    await dash(beta, "left");
    await sleep(150);
    // The client draws an invisible entity translucent when the viewer shares a team that can see friendly invisibles
    const clone = near(alpha, "mannequin", pos(beta), 8)[0];
    const invisible = clone ? (clone.metadata[0] & 0x20) !== 0 : false;
    const cloneTeam = clone ? alpha.teamMap[clone.uuid] : undefined;
    const sharesTeam = cloneTeam !== undefined && alpha.teamMap[alpha.username] === cloneTeam;
    const seesInvisible = cloneTeam !== undefined && teamFlags[cloneTeam.team]?.see_friendly_invisible === true;
    check("clones are translucent to the viewer", invisible && sharesTeam && seesInvisible,
        `invisible ${invisible}, team ${cloneTeam?.team}, shared ${sharesTeam}, sees friendly invisibles ${seesInvisible}`);
    await sleep(850);
    portal.stop();
    sounds.stop();
    const moved = pos(beta).minus(start);
    check("double tap left dashes left", moved.z > 2 && Math.abs(moved.x) < 0.7, `moved ${fmt(moved)}`);
    // the dash runs along +z here, so sort by z and look at the biggest hole in the trail
    const zs = clones.positions.map(p => p.z).sort((a, b) => a - b);
    const widestGap = Math.max(0, ...zs.slice(1).map((z, i) => z - zs[i]));
    check("dash leaves a dense trail of clones", clones.count >= 8 && widestGap < 0.7,
        `${clones.count} mannequins, widest gap ${widestGap.toFixed(2)} blocks`);
    check("dash plays the ender teleport at both ends", portal.count >= 2 && sounds.count >= 2,
        `${portal.count} portal bursts, ${sounds.count} teleport sounds`);
    await sleep(500);
    check("after images clean up", near(alpha, "mannequin", pos(beta), 20).length === 0);
    check("faded clones leave the ghost team", clone !== undefined && alpha.teamMap[clone.uuid] === undefined);
    clones.stop();
    fade.stop();
    // back of the trail (lowest z) should go first, and spread out instead of vanishing in batches
    const order = fade.gone().sort((a, b) => a.z - b.z);
    const inOrder = order.every((clone, i) => i === 0 || clone.time >= order[i - 1].time);
    const spread = order.length ? order.at(-1).time - order[0].time : 0;
    check("clones fade one at a time from the back", inOrder && spread >= 400, `${order.length} gone over ${spread}ms`);

    // Beta stands flush against a wall on its left, so the dash goes nowhere
    await reset();
    run(`/fill 3 ${Y} 1 3 ${Y + 1} 1 minecraft:stone`);
    run(`/tp C7Beta 3 ${Y} 0.7 90 0`);
    await sleep(600);
    const blocked = countSpawns(alpha, "mannequin");
    await dash(beta, "left");
    await sleep(800);
    blocked.stop();
    check("dash into a wall leaves no clones", blocked.count === 0, `${blocked.count} mannequins`);
    run(`/fill 3 ${Y} 1 3 ${Y + 1} 1 minecraft:air`);

    await reset();
    const back = pos(beta);
    await dash(beta, "back");
    await sleep(1000);
    const movedBack = pos(beta).minus(back);
    check("double tap back dashes back", movedBack.x > 2, `moved ${fmt(movedBack)}`);

    await reset();
    emptyHand(beta);
    await sleep(200);
    const still = pos(beta);
    await dash(beta, "left");
    await sleep(800);
    check("no dash without a sword", pos(beta).distanceTo(still) < 0.3);
    await ensureSword(beta);

    await reset();
    const slow = pos(beta);
    beta.setControlState("forward", false);
    for (const pressed of [true, false, true]) {
        beta.input.left = pressed;
        beta._client.write("player_input", { inputs: { ...beta.input } });
        await sleep(450);
    }
    beta.input.left = false;
    beta._client.write("player_input", { inputs: { ...beta.input } });
    await sleep(500);
    check("slow double tap doesn't dash", pos(beta).distanceTo(slow) < 0.3);
}

(async () => {
    await Promise.all([alpha, beta].map(bot => new Promise(resolve => bot.once("spawn", resolve))));
    await sleep(2000);
    try {
        await setup();
        await baseline();
        await parryTest();
        await clashTest();
        await shieldTest();
        await uiTest();
        await dashTest();
    } catch (err) {
        log("selftest", "crashed " + err.stack);
        results.push(false);
    }
    const passed = results.filter(Boolean).length;
    log("selftest", `${passed}/${results.length} passed`);
    alpha.quit();
    beta.quit();
    await sleep(500);
    process.exit(passed === results.length ? 0 : 1);
})();
