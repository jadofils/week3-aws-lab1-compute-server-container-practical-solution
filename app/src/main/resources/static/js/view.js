// View: reads from and renders to the DOM. No network calls here.

const el = {
  student: document.querySelector('#student'),
  lab: document.querySelector('#lab'),
  server: document.querySelector('#server'),
  orders: document.querySelector('#orders'),
  message: document.querySelector('#message'),
  button: document.querySelector('#submit-button'),
  form: document.querySelector('#order-form'),
  quantity: document.querySelector('#quantity'),
  servers: document.querySelector('#servers'),
  watchButton: document.querySelector('#watch-button'),
  spikeForm: document.querySelector('#spike-form'),
  spikeButton: document.querySelector('#spike-button'),
  stopButton: document.querySelector('#stop-button'),
  spikeMessage: document.querySelector('#spike-message'),
  spikeResults: document.querySelector('#spike-results')
};

function cell(row, value, className) {
  const td = document.createElement('td');
  if (className) td.className = className;
  td.textContent = value;
  row.appendChild(td);
}

function singleRow(tbody, columns, text) {
  const row = document.createElement('tr');
  const td = document.createElement('td');
  td.colSpan = columns;
  td.className = 'empty';
  td.textContent = text;
  row.appendChild(td);
  tbody.replaceChildren(row);
}

function setNotice(target, text, kind = '') {
  target.textContent = text;
  target.className = `notice ${kind}`.trim();
}

export const view = {
  form: el.form,
  watchButton: el.watchButton,
  spikeForm: el.spikeForm,
  stopButton: el.stopButton,

  renderInfo(info) {
    el.student.textContent = info.studentName;
    el.lab.textContent = info.labName;
    el.server.textContent = `${info.serverId} (${info.availabilityZone}, ${info.privateIp})`;
  },

  renderInfoUnavailable() {
    el.student.textContent = 'Onward Logistics order demo';
    el.server.textContent = 'unavailable';
  },

  setServer(label) {
    el.server.textContent = label;
  },

  renderOrders(orders) {
    if (!orders.length) {
      singleRow(el.orders, 4, 'No orders on this server yet. Submit the form to create one.');
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
    singleRow(el.orders, 4, text);
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
    setNotice(el.message, text, kind);
  },

  // --- Server watcher -------------------------------------------------------
  setWatching(watching) {
    el.watchButton.textContent = watching ? 'Stop watching' : 'Start watching';
  },

  renderServers(servers) {
    if (!servers.length) {
      singleRow(el.servers, 5, 'Waiting for the first answer...');
      return;
    }
    el.servers.replaceChildren();
    for (const s of servers) {
      const row = document.createElement('tr');
      cell(row, s.serverId, 'server-cell');
      cell(row, s.availabilityZone);
      cell(row, s.privateIp, 'server-cell');
      cell(row, String(s.count));
      cell(row, s.lastSeen.toLocaleTimeString());
      if (Date.now() - s.lastSeen.getTime() > 15000) row.className = 'stale';
      el.servers.appendChild(row);
    }
  },

  // --- CPU spike ------------------------------------------------------------
  readSpikeForm() {
    const data = new FormData(el.spikeForm);
    return { seconds: Number(data.get('seconds')), requests: Number(data.get('requests')) };
  },

  setSpikeBusy(busy) {
    el.spikeButton.disabled = busy;
    el.stopButton.disabled = busy;
  },

  showSpikeMessage(text, kind = '') {
    setNotice(el.spikeMessage, text, kind);
  },

  renderSpikeResults(results) {
    el.spikeResults.replaceChildren();
    for (const r of results) {
      const li = document.createElement('li');
      li.textContent = r.error
        ? `Request failed: ${r.error}`
        : `${r.serverId} (${r.availabilityZone}): ${r.started ? 'spike started' : r.active ? 'already running' : 'idle'}`
          + (r.active ? `, ${r.secondsRemaining}s left on ${r.workerThreads} CPU thread(s)` : '');
      el.spikeResults.appendChild(li);
    }
  }
};
