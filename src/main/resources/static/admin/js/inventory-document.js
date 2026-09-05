document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('[data-inventory-document-form]');
  if (!form) return;

  const rows = form.querySelector('[data-inventory-rows]');
  const template = form.querySelector('[data-inventory-row-template]');
  const typeSelect = form.querySelector('[data-document-type]');
  const supplierField = form.querySelector('[data-supplier-field]');
  const costHeading = form.querySelector('[data-cost-heading]');
  const quantityHeading = form.querySelector('[data-quantity-heading]');

  function renumber() {
    rows.querySelectorAll('[data-inventory-row]').forEach((row, index) => {
      row.querySelectorAll('[name]').forEach(input => {
        input.name = input.name.replace(/items\[\d+\]/, `items[${index}]`);
      });
    });
  }

  function bindRemove(button) {
    button.addEventListener('click', () => {
      const allRows = rows.querySelectorAll('[data-inventory-row]');
      if (allRows.length === 1) {
        allRows[0].querySelectorAll('input,select').forEach(input => input.value = '');
        return;
      }
      button.closest('[data-inventory-row]').remove();
      renumber();
    });
  }

  function updateTypeFields() {
    const inbound = typeSelect.value === 'INBOUND';
    const adjustment = typeSelect.value === 'ADJUSTMENT';
    supplierField.hidden = !inbound;
    costHeading.hidden = !inbound;
    quantityHeading.textContent = adjustment ? 'Tồn thực tế' : 'Số lượng';
    rows.querySelectorAll('[data-unit-cost]').forEach(input => {
      input.closest('td').hidden = !inbound;
      input.required = inbound;
      if (!inbound) input.value = '';
    });
  }

  form.querySelectorAll('[data-remove-inventory-row]').forEach(bindRemove);
  form.querySelector('[data-add-inventory-row]').addEventListener('click', () => {
    const index = rows.querySelectorAll('[data-inventory-row]').length;
    const holder = document.createElement('tbody');
    holder.innerHTML = template.innerHTML.replaceAll('__INDEX__', String(index)).trim();
    const row = holder.firstElementChild;
    rows.appendChild(row);
    bindRemove(row.querySelector('[data-remove-inventory-row]'));
    updateTypeFields();
  });
  typeSelect.addEventListener('change', updateTypeFields);
  updateTypeFields();
});
