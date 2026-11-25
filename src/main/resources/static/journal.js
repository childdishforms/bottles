const rows = [...document.querySelectorAll('tbody[data-number]')];
const search = document.querySelector('#search');
const statusFilter = document.querySelector('#status');
function filterRows() {
  const query = search.value.trim().toLocaleLowerCase('ru');
  for (const row of rows) {
    row.hidden = !row.dataset.number.toLocaleLowerCase('ru').includes(query)
      || (statusFilter.value !== '' && row.dataset.status !== statusFilter.value);
  }
  document.querySelector('#no-matches').hidden = !rows.length || rows.some(row => !row.hidden);
}
search?.addEventListener('input', filterRows);
statusFilter?.addEventListener('change', filterRows);
for (const form of document.querySelectorAll('.inspection')) {
  form.elements.approved.addEventListener('change', () => {
    form.elements.comment.required = form.elements.approved.value === 'false';
  });
}
