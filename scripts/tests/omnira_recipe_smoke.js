// Development-only fixture, copied into the headless test instance on request.
ServerEvents.recipes(event => {
  event.custom({
    type: 'omnira:reaction',
    items: [{ ingredient: { tag: 'minecraft:planks' }, count: 1 }],
    fluid: { id: 'minecraft:water', amount: 1000 },
    results: [{ id: 'minecraft:stick', count: 1 }],
    duration: 2000
  }).id('omnira_test:kubejs_reaction');

  event.custom({
    type: 'omnira:catalysis',
    substrate: { tag: 'minecraft:dirt' },
    seed: { item: 'minecraft:wheat_seeds' },
    fluid: { id: 'minecraft:water', amount: 250 },
    result: { id: 'minecraft:wheat', count: 2 },
    stages: 4
  }).id('omnira_test:kubejs_catalysis');

  event.custom({
    type: 'omnira:assembly',
    slots: [[], [], [], [], [], [], { item: 'minecraft:stick' }],
    result: { id: 'minecraft:diamond', count: 1 }
  }).id('omnira_test:kubejs_assembly');
});
