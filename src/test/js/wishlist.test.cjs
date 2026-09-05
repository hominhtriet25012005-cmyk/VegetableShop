const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');

const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/wishlist.js'), 'utf8');

function toggle(withLabel = false) {
    const classes = new Set(['far']);
    const label = withLabel ? { textContent: 'Thêm vào yêu thích' } : null;
    return {
        dataset: { productId: '42', wishlisted: 'false' }, disabled: false, attributes: {}, classes, label,
        setAttribute(name, value) { this.attributes[name] = value; },
        querySelector(selector) {
            return selector === 'i' ? { classList: { toggle(name, on) { on ? classes.add(name) : classes.delete(name); } } } : label;
        }
    };
}

function mount(fetchResponse) {
    const buttons = [toggle(), toggle(true)];
    const count = { textContent: 0 };
    const calls = [], notifications = [];
    let click;
    const Swal = { fire: payload => notifications.push(payload) };
    runInNewContext(source, {
        window: { Swal }, Swal,
        fetch: async (url, options) => { calls.push({ url, options }); return fetchResponse(calls.length); },
        document: {
            querySelector(selector) { return { content: selector.includes('_csrf_header') ? 'X-CSRF-TOKEN' : 'test-csrf' }; },
            querySelectorAll(selector) { return selector.includes('data-wishlist-toggle') ? buttons : [count]; },
            addEventListener(event, handler) { click = handler; }
        }
    });
    return { buttons, calls, count, notifications,
        click(button = buttons[0]) {
            return click({ preventDefault() {}, target: { closest: selector => selector === '[data-wishlist-toggle]' ? button : null } });
        }
    };
}

test('icon-only heart toggles all copies, accessibility labels and count; add/remove sends CSRF', async () => {
    const page = mount(n => ({ ok: true, json: async () => ({ wishlisted: n === 1, wishlistCount: n === 1 ? 1 : 0 }) }));
    await page.click();
    assert.equal(page.calls[0].url, '/api/wishlist/items?productId=42');
    assert.equal(page.calls[0].options.method, 'POST');
    assert.equal(page.calls[0].options.headers['X-CSRF-TOKEN'], 'test-csrf');
    for (const button of page.buttons) {
        assert.equal(button.attributes['aria-pressed'], 'true');
        assert.equal(button.attributes['aria-label'], 'Bỏ yêu thích');
        assert.equal(button.attributes.title, 'Bỏ yêu thích');
        assert.ok(button.classes.has('fas') && button.classes.has('text-danger'));
        assert.ok(!button.classes.has('far'));
    }
    assert.equal(page.count.textContent, 1);
    assert.equal(page.buttons[1].label.textContent, 'Bỏ yêu thích');
    await page.click();
    assert.equal(page.calls[1].url, '/api/wishlist/items/42');
    assert.equal(page.calls[1].options.method, 'DELETE');
    assert.equal(page.calls[1].options.headers['X-CSRF-TOKEN'], 'test-csrf');
    assert.equal(page.count.textContent, 0);
    for (const button of page.buttons) {
        assert.equal(button.attributes['aria-pressed'], 'false');
        assert.equal(button.attributes['aria-label'], 'Thêm vào yêu thích');
        assert.ok(button.classes.has('far') && !button.classes.has('fas'));
        assert.equal(button.disabled, false);
    }
});

test('failed request preserves heart state and enables retry', async () => {
    const page = mount(() => ({ ok: false, json: async () => ({ message: 'Vui lòng đăng nhập lại' }) }));
    await page.click();
    assert.equal(page.buttons[0].dataset.wishlisted, 'false');
    assert.equal(page.buttons[0].disabled, false);
    assert.equal(page.count.textContent, 0);
    assert.equal(page.notifications[0].icon, 'error');
    assert.equal(page.notifications[0].title, 'Vui lòng đăng nhập lại');
});

test('repeated click while request is pending does not issue a duplicate request', async () => {
    let finish;
    const page = mount(() => new Promise(resolve => { finish = resolve; }));
    const pending = page.click();
    assert.equal(page.buttons[0].disabled, true);
    await page.click();
    assert.equal(page.calls.length, 1);
    finish({ ok: true, json: async () => ({ wishlisted: true, wishlistCount: 1 }) });
    await pending;
    assert.equal(page.buttons[0].disabled, false);
});
