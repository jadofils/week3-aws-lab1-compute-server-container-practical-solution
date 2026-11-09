// View: reads from and renders to the DOM. No network calls here.

const el = {
  student: document.querySelector('#student'),
  lab: document.querySelector('#lab'),
  server: document.querySelector('#server'),
  orders: document.querySelector('#orders'),
  message: document.querySelector('#message'),
  button: document.querySelector('#submit-button'),
  form: document.querySelector('#order-form'),
  quantity: document.querySelector('#quantity')
};

const COLUMN_COUNT = 4;

function cell(row, value, className) {
  const td = document.createElement('td');
  if (className) td.className = className;
  td.textContent = value;
  row.appendChild(td);
}

function singleRow(text) {
  const row = document.createElement('tr');
  const td = document.createElement('td');
  td.colSpan = COLUMN_COUNT;
  td.className = 'empty';
  td.textContent = text;
  row.appendChild(td);
  el.orders.replaceChildren(row);
}

export const view = {
  form: el.form,

  renderInfo(info) {
    el.student.textContent = info.studentName;
    el.lab.textContent = info.labName;
    el.server.textContent = info.serverId;
  },

  renderInfoUnavailable() {
    el.student.textContent = 'Onward Logistics order demo';
    el.server.textContent = 'unavailable';
  },

  setServer(serverId) {
    el.server.textContent = serverId;
  },

  renderOrders(orders) {
    if (!orders.length) {
      singleRow('No orders on this server yet. Submit the form to create one.');
      return;
    }
    el.orders.replaceChildren();
    for (const order of orders) {
      const row = document.createElement('tr');
      cell(row, order.id.slice(0, 8));
      cell(row, `${order.customerName} - ${order.item}`);
      cell(row, String(order.quantity));
      cell(row, order.servedBy, 'server-cell');
      el.orders.appendChild(row);
    }
  },

  renderOrdersError(text) {
    singleRow(text);
  },

  readOrderForm() {
    const data = new FormData(el.form);
    return {
      customerName: String(data.get('customerName')).trim(),
      item: String(data.get('item')).trim(),
      quantity: Number(data.get('quantity'))
    };
  },

  resetOrderForm() {
    el.form.reset();
    el.quantity.value = '1';
  },

  setBusy(busy) {
    el.button.disabled = busy;
  },

  showMessage(text, kind = '') {
    el.message.textContent = text;
    el.message.className = `notice ${kind}`.trim();
  }
};
