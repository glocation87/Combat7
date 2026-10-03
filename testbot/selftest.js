// Two bots run through every mechanic and check the results. C7Alpha has to be op (run "op C7Alpha" in the console once).
const { connect, dash, parry, setShield, lookAt, ensureSword, sleep, log } = require("./lib");

const Y = 200;
const alpha = connect("C7Alpha");
const beta = connect("C7Beta");
const results = [];

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
    const seen = { count: 0 };
    const listener = entity => {
        if (entity.name === entityName) {
            seen.count++;
        }
    };
    bot.on("entitySpawn", listener);
    seen.stop = () => bot.removeListener("entitySpawn", listener);
    return seen;
}

// Hotbar slots are inventory slots 36 to 44
function emptyHand(bot) {
    const slot = [...Array(9).keys()].find(i => !bot.inventory.slots[36 + i]);
    bot.setQuickBarSlot(slot);
}

const near = (bot, entityName, center, radius) => Object.values(bot.entities)
    .filter(entity => entity.name === entityName && entity.position.distanceTo(center) < radius);

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
    run("/give C7Alpha minecraft:iron_sword");
    run("/give C7Beta minecraft:iron_sword");
    await sleep(1500);
    check("alpha has a sword", await ensureSword(alpha));
    check("beta has a sword", await ensureSword(beta));
}

async function baseline() {
    await reset();
    alpha.attack(entityOf(alpha, beta));
    await sleep(400);
    check("plain hit does damage", beta.health < 20, `beta health ${beta.health}`);
}

async function parryTest() {
    await reset();
    const sparks = countParticles(alpha, ["electric_spark", "flash"]);
    const before = pos(alpha);
    await parry(beta);
    await sleep(100);
    alpha.attack(entityOf(alpha, beta));
    await sleep(500);
    sparks.stop();
    const pushed = before.x - pos(alpha).x;
    check("parried hit does no damage", beta.health === 20, `beta health ${beta.health}`);
    check("attacker is knocked back", pushed > 0.5, `moved ${pushed.toFixed(2)} blocks`);
    check("parry vfx shows", sparks.count > 0, `${sparks.count} particle packets`);

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
    const alphaBefore = pos(alpha);
    const betaBefore = pos(beta);
    alpha.attack(entityOf(alpha, beta));
    beta.attack(entityOf(beta, alpha));
    await sleep(600);
    sparks.stop();
    check("clash: alpha takes no damage", alpha.health === 20, `alpha health ${alpha.health}`);
    check("clash: beta takes no damage", beta.health === 20, `beta health ${beta.health}`);
    const gap = pos(beta).x - pos(alpha).x;
    const gapBefore = betaBefore.x - alphaBefore.x;
    check("clash pushes both apart", gap > gapBefore + 0.8, `gap ${gapBefore.toFixed(2)} -> ${gap.toFixed(2)}`);
    check("clash vfx shows", sparks.count > 0, `${sparks.count} particle packets`);

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
    alpha.attack(entityOf(alpha, beta));
    await sleep(400);
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

async function dashTest() {
    await reset();
    const clones = countSpawns(alpha, "mannequin");
    const start = pos(beta);
    // Beta faces west, so its left is south (+z)
    await dash(beta, "left");
    await sleep(1000);
    const moved = pos(beta).minus(start);
    check("double tap left dashes left", moved.z > 2 && Math.abs(moved.x) < 0.7, `moved ${fmt(moved)}`);
    check("dash leaves after images", clones.count >= 2, `${clones.count} mannequins`);
    await sleep(500);
    check("after images clean up", near(alpha, "mannequin", pos(beta), 20).length === 0);
    clones.stop();

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
