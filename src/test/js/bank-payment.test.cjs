const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/bank-payment.js'), 'utf8');

for (const blocked of [false, true]) {
    test(blocked ? 'QR copy handles clipboard denial without changing payment state' : 'QR copy copies exactly the displayed transfer reference', async () => {
        let handler, copied;
        const feedback = {textContent: ''};
        const button = {dataset: {copy: 'VS20260904120000AABBCCDD'}, addEventListener: (_, fn) => handler = fn};
        vm.runInNewContext(source, {
            document: {getElementById: () => feedback, querySelectorAll: () => [button]},
            navigator: {clipboard: {writeText: async value => { if (blocked) throw Error('denied'); copied = value; }}}
        });
        await handler();
        assert.equal(copied, blocked ? undefined : button.dataset.copy);
        assert.match(feedback.textContent, blocked ? /không cho sao chép/ : /Đã sao chép/);
    });
}
