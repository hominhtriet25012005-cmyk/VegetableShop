const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/reviews.js'), 'utf8');
function mount() {
    let change, next = 0;
    const revoked = [], children = [];
    const input = { files: [], message: '', addEventListener: (_, fn) => { change = fn; },
        setCustomValidity(message) { this.message = message; }, reportValidity() {} };
    const preview = { replaceChildren: () => { children.length = 0; }, append: img => children.push(img) };
    runInNewContext(source, { document: {
        querySelector: selector => selector === '[data-review-images]' ? input : preview,
        createElement: () => ({ style: {} })
    }, URL: { createObjectURL: () => 'blob:test-' + next++, revokeObjectURL: url => revoked.push(url) } });
    return { input, children, revoked, change: files => { input.files = files; change(); } };
}
const photo = { type: 'image/png', size: 1000 };
test('review photo preview displays selected images and releases old object URLs', () => {
    const page = mount(); page.change([photo, photo]);
    assert.equal(page.children.length, 2); assert.equal(page.input.message, '');
    assert.ok(page.children.every(img => img.alt && img.width === 80));
    page.change([]); assert.equal(page.revoked.length, 2); assert.equal(page.children.length, 0);
});
test('review photo preview blocks excessive count, size or unsupported type', () => {
    const page = mount();
    for (const files of [Array(6).fill(photo), [{ ...photo, size: 2 * 1024 * 1024 + 1 }], [{ ...photo, type: 'image/svg+xml' }]]) {
        page.change(files); assert.notEqual(page.input.message, ''); assert.equal(page.children.length, 0);
    }
    page.change([photo]); assert.equal(page.input.message, ''); assert.equal(page.children.length, 1);
});
test('pages without a review form do not register listeners or fail', () => {
    assert.doesNotThrow(() => runInNewContext(source, { document: { querySelector: () => null } }));
});
