const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/payment-status.js'), 'utf8');
function setup(response, hidden = false) {
    const timers = [], feedback = {textContent: ''}; let reloads = 0, calls = 0, printHandler, printed = false;
    vm.runInNewContext(source, {
        document: {hidden, querySelector: () => ({dataset: {paymentStatusUrl: '/orders/1/payment/status', paymentVersion: 'UNPAID'}}),
            getElementById: () => feedback, querySelectorAll: () => [{addEventListener: (_, fn) => printHandler = fn}]},
        window: {setTimeout: fn => timers.push(fn), location: {reload: () => reloads++}, print: () => printed = true},
        AbortSignal: {timeout: () => ({})},
        fetch: async (url, options) => {calls++; assert.equal(url, '/orders/1/payment/status'); assert.equal(options.cache, 'no-store');
            if (response instanceof Error) throw response; return response;}
    });
    return {timers, feedback, reloads: () => reloads, calls: () => calls, print: () => {printHandler();return printed;}};
}
test('payment status change reloads receipt once; print uses browser print dialog', async () => {
    const s = setup({ok: true, json: async () => ({version: 'PAID'})}); await s.timers.shift()();
    assert.equal(s.reloads(), 1); assert.equal(s.timers.length, 0); assert.equal(s.print(), true);
});
test('unchanged payment does not reload and polling is bounded', async () => {
    const s = setup({ok: true, json: async () => ({version: 'UNPAID'})});
    for (let i = 0; i < 121; i++) await s.timers.shift()();
    assert.equal(s.calls(), 120); assert.equal(s.reloads(), 0); assert.equal(s.timers.length, 0);
    assert.match(s.feedback.textContent, /Đã dừng/);
});
test('network failure never marks paid and retries later', async () => {
    const s = setup(new Error('offline')); await s.timers.shift()();
    assert.equal(s.reloads(), 0); assert.equal(s.timers.length, 1); assert.match(s.feedback.textContent, /thử lại/);
});
test('expired login stops polling instead of parsing login HTML', async () => {
    const s = setup({redirected: true}); await s.timers.shift()();
    assert.equal(s.timers.length, 0); assert.match(s.feedback.textContent, /đăng nhập/);
});
test('hidden tab pauses requests', async () => {
    const s = setup({}, true); await s.timers.shift()();assert.equal(s.calls(), 0);assert.equal(s.timers.length, 1);
});
