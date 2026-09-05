const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');

const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/home-products.js'), 'utf8');

function element(categoryId) {
    const classes = new Set();
    return {
        dataset: { categoryId },
        attributes: {},
        classList: {
            toggle(name, enabled) { enabled ? classes.add(name) : classes.delete(name); },
            contains(name) { return classes.has(name); }
        },
        setAttribute(name, value) { this.attributes[name] = value; },
        addEventListener(name, handler) { this[name] = handler; }
    };
}

function mount(categoryIds) {
    const products = categoryIds.map(element);
    const filters = ['all', '1', '2', '3', '4', 'empty'].map(element);
    const emptyMessage = element();
    runInNewContext(source, { document: {
        querySelectorAll(selector) {
            return selector === '[data-home-category-filter]' ? filters : products;
        },
        querySelector() { return emptyMessage; }
    }});
    return { products, filters, emptyMessage,
        visible: () => products.filter(product => !product.classList.contains('d-none')) };
}

test('all shows the mixed first eight; category tabs filter locally and returning to all restores the mix', () => {
    const page = mount(Array.from({ length: 32 }, (_, index) => String(index % 4 + 1)));
    const initial = page.visible();
    assert.equal(initial.length, 8);
    assert.equal(new Set(initial.map(product => product.dataset.categoryId)).size, 4);

    page.filters.find(filter => filter.dataset.categoryId === '2').click();
    assert.equal(page.visible().length, 8);
    assert.ok(page.visible().every(product => product.dataset.categoryId === '2'));
    assert.equal(page.filters[2].attributes['aria-pressed'], 'true');

    page.filters[0].click();
    assert.deepEqual(page.visible(), initial);
    assert.equal(page.filters[0].attributes['aria-pressed'], 'true');
    assert.equal(page.filters[2].attributes['aria-pressed'], 'false');
});

test('sparse and empty categories keep the visible count and empty state correct', () => {
    const page = mount(['1', '2', '2']);
    assert.equal(page.visible().length, 3);
    page.filters[1].click();
    assert.equal(page.visible().length, 1);
    assert.ok(page.emptyMessage.classList.contains('d-none'));
    page.filters.at(-1).click();
    assert.equal(page.visible().length, 0);
    assert.ok(!page.emptyMessage.classList.contains('d-none'));
    page.filters[0].click();
    assert.equal(page.visible().length, 3);
    assert.ok(page.emptyMessage.classList.contains('d-none'));
});
