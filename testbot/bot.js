// Sparring partner, talk to it in chat. Usage: node bot.js [name]
const { connect, dash, parry, setShield, lookAt, ensureSword, log } = require("./lib");

const name = process.argv[2] ?? "SparBot";
const bot = connect(name);

const HELP = [
    "!come - walk over to you",
    "!fight - spar: chases, attacks, parries, dashes and shields at random",
    "!attack - attacks you on a steady 1.5s beat, practice your parry timing",
    "!parry - stands still and tries to parry your hits",
    "!counter - swings back the moment you swing, for sword clashes",
    "!shield - toggles the crouch shield",
    "!dash <forward|back|left|right>",
    "!sword - grabs a sword",
    "!stop - stops whatever it's doing",
];

let mode = "idle";
let targetName = null;
let shieldUp = false;
let lastAttack = 0;
let lastParry = 0;
let lastDash = 0;
let shieldUntil = 0;
const swings = [];

const target = () => (targetName ? bot.players[targetName]?.entity : undefined);

function setMode(next, username) {
    mode = next;
    targetName = username;
    bot.clearControlStates();
    shieldUp = false;
    setShield(bot, false);
    log(name, `mode ${next} on ${username}`);
}

// Guesses when the target swings next from the gap between its last two swings, so it can parry just before
function parryDue(now) {
    if (now - lastParry < 1050) {
        return false;
    }
    if (swings.length < 2) {
        return now - lastParry > 1300;
    }
    const gap = Math.min(2000, Math.max(600, swings.at(-1) - swings.at(-2)));
    return now >= swings.at(-1) + gap - 120;
}

function doParry(now) {
    lastParry = now;
    void parry(bot);
}

function approach(distance, stopAt) {
    bot.setControlState("forward", distance > stopAt);
    bot.setControlState("sprint", distance > 6);
    bot.setControlState("jump", distance > stopAt && bot.entity.isCollidedHorizontally);
}

bot.once("spawn", async () => {
    log(name, "joined, say !help in chat");
    if (!await ensureSword(bot)) {
        bot.chat(`I need a sword, op me or run /give ${name} iron_sword`);
    }
    // picks up a sword someone gives it later
    bot.inventory.on("updateSlot", (slot, oldItem, newItem) => {
        if (newItem?.name.endsWith("_sword") && !bot.heldItem?.name.endsWith("_sword")) {
            bot.equip(newItem, "hand").catch(() => {});
        }
    });
});

bot.on("chat", async (username, message) => {
    if (username === bot.username || !message.startsWith("!")) {
        return;
    }
    const [command, arg] = message.slice(1).trim().toLowerCase().split(/\s+/);
    switch (command) {
        case "help":
            HELP.forEach(line => bot.chat(line));
            break;
        case "come":
        case "fight":
        case "attack":
        case "parry":
        case "counter":
            setMode(command, username);
            break;
        case "shield":
            shieldUp = !shieldUp;
            setShield(bot, shieldUp);
            break;
        case "dash":
            await dash(bot, arg ?? "back");
            break;
        case "sword":
            await ensureSword(bot);
            break;
        case "stop":
            setMode("idle", null);
            break;
    }
});

bot.on("entitySwingArm", entity => {
    const player = target();
    if (entity.id !== player?.id) {
        return;
    }
    const now = Date.now();
    swings.push(now);
    if (swings.length > 5) {
        swings.shift();
    }
    const close = bot.entity.position.distanceTo(player.position) < 3.5;
    if (close && (mode === "counter" || (mode === "fight" && Math.random() < 0.3))) {
        bot.attack(player);
        lastAttack = now;
    }
});

bot.on("physicsTick", () => {
    const player = target();
    if (mode === "idle" || !player) {
        return;
    }
    lookAt(bot, player);
    const now = Date.now();
    const distance = bot.entity.position.distanceTo(player.position);

    switch (mode) {
        case "come":
            approach(distance, 2);
            break;
        case "attack":
            approach(distance, 2.5);
            if (distance <= 3 && now - lastAttack >= 1500) {
                bot.attack(player);
                lastAttack = now;
            }
            break;
        case "parry":
            if (distance <= 4 && parryDue(now)) {
                doParry(now);
            }
            break;
        case "counter":
            approach(distance, 2.5);
            break;
        case "fight":
            fight(player, distance, now);
            break;
    }
});

function fight(player, distance, now) {
    if (shieldUp) {
        bot.setControlState("forward", false);
        if (now > shieldUntil) {
            shieldUp = false;
            setShield(bot, false);
        }
        return;
    }
    approach(distance, 2.6);
    if (distance > 4) {
        return;
    }
    if (now - lastAttack > 700 + Math.random() * 400 && distance <= 3) {
        bot.attack(player);
        lastAttack = now;
    } else if (Math.random() < 0.5 && parryDue(now)) {
        doParry(now);
    } else if (now - lastDash > 2500 && Math.random() < 0.02) {
        lastDash = now;
        void dash(bot, ["left", "right", "back"][Math.floor(Math.random() * 3)]);
    } else if (Math.random() < 0.006) {
        shieldUp = true;
        shieldUntil = now + 800 + Math.random() * 700;
        setShield(bot, true);
    }
}
