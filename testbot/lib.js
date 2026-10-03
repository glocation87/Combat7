const mineflayer = require("mineflayer");

const HOST = process.env.HOST ?? "localhost";
const PORT = Number(process.env.PORT ?? 25567);

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const log = (who, msg) => console.log(`[${new Date().toISOString().slice(11, 23)}] ${who}: ${msg}`);

// mineflayer is on 26.1, ViaBackwards on the dev server translates to 26.2
function connect(username) {
    const bot = mineflayer.createBot({ host: HOST, port: PORT, username, version: "26.1", auth: "offline" });
    bot.input = { forward: false, backward: false, left: false, right: false, jump: false, shift: false, sprint: false };
    // mineflayer still divides velocity by 8000 like the old packet format, since 1.21.9 it's already blocks per tick.
    // Registered on login so it runs after mineflayer's own handler (its plugins load late) and puts the real value back
    bot.once("login", () => bot._client.on("entity_velocity", packet => {
        const entity = bot.entities[packet.entityId];
        if (entity) {
            entity.velocity.set(packet.velocity.x, packet.velocity.y, packet.velocity.z);
        }
    }));
    bot.on("kicked", reason => log(username, "kicked " + JSON.stringify(reason)));
    bot.on("error", err => log(username, "error " + err.message));
    return bot;
}

// mineflayer only sends the input packet for sneaking, but the dash reads the movement keys from it too
function setInput(bot, changes) {
    Object.assign(bot.input, changes);
    bot._client.write("player_input", { inputs: { ...bot.input } });
}

const KEYS = { forward: "forward", back: "backward", left: "left", right: "right" };

async function dash(bot, direction) {
    const key = KEYS[direction];
    if (!key) {
        throw new Error("unknown dash direction " + direction);
    }
    for (let tap = 0; tap < 2; tap++) {
        setInput(bot, { [key]: true });
        await sleep(60);
        setInput(bot, { [key]: false });
        await sleep(60);
    }
}

// a sword has no use action, the right click itself is what the plugin listens for
async function parry(bot) {
    bot.activateItem();
    await sleep(50);
    bot.deactivateItem();
}

function setShield(bot, up) {
    bot.setControlState("sneak", up);
    setInput(bot, { shift: up });
}

function lookAt(bot, entity) {
    return bot.lookAt(entity.position.offset(0, entity.height * 0.85, 0), true);
}

const swordIn = bot => bot.inventory.items().find(item => item.name.endsWith("_sword"));

async function ensureSword(bot) {
    if (!swordIn(bot)) {
        bot.chat("/give @s minecraft:iron_sword");
        await sleep(1000);
    }
    const sword = swordIn(bot);
    if (!sword) {
        return false;
    }
    await bot.equip(sword, "hand");
    return true;
}

module.exports = { connect, setInput, dash, parry, setShield, lookAt, ensureSword, sleep, log };
