// Controller: wires user events to API calls and updates the view.
import { api } from './api.js';
import { view } from './view.js';

// --- Lab details and orders ---------------------------------------------------
async function loadInfo() {
  try {
    view.renderInfo(await api.getInfo());
  } catch (error) {
    view.renderInfoUnavailable();
    view.showMessage(error.message, 'error');
  }
}

// Each server keeps its own orders in memory and the load balancer spreads requests, so
// every refresh may come from a different server. The page merges what it has seen from
// all servers (by order ID) and shows the newest first.
const ORDER_REFRESH_MS = 3000;
const MAX_ORDERS_SHOWN = 50;
const knownOrders = new Map();

function mergeOrders(orders) {
  for (const order of orders) knownOrders.set(order.id, order);
}

function renderKnownOrders() {
  const newestFirst = [...knownOrders.values()]
    .sort((a, b) => Date.parse(b.receivedAt) - Date.parse(a.receivedAt))
    .slice(0, MAX_ORDERS_SHOWN);
  view.renderOrders(newestFirst);
}

async function loadOrders() {
  try {
    mergeOrders(await api.listOrders());
    renderKnownOrders();
  } catch (error) {
    if (!knownOrders.size) view.renderOrdersError(error.message);
    // Otherwise keep showing what we have; the next refresh tries again.
  }
}

async function submitOrder(event) {
  event.preventDefault();
  view.setBusy(true);
  view.showMessage('Submitting order...');
  try {
    const order = await api.createOrder(view.readOrderForm());
    // Show the new order immediately, whichever server answers the next refresh.
    mergeOrders([order]);
    renderKnownOrders();
    view.setServer(order.servedBy);
    view.showMessage(`Order ${order.id.slice(0, 8)} received by ${order.servedBy}.`, 'success');
    view.resetOrderForm();
  } catch (error) {
    view.showMessage(error.message, 'error');
  } finally {
    view.setBusy(false);
  }
}

// --- Which server is answering? -----------------------------------------------
const seen = new Map();
let watchTimer = null;

async function pollOnce() {
  try {
    const info = await api.getInfo();
    const entry = seen.get(info.serverId) || { ...info, count: 0 };
    entry.count += 1;
    entry.lastSeen = new Date();
    seen.set(info.serverId, entry);
  } catch {
    // A failed poll is simply not counted; the next one tries again.
  }
  view.renderServers([...seen.values()].sort((a, b) => a.availabilityZone.localeCompare(b.availabilityZone)));
}

function toggleWatching() {
  if (watchTimer) {
    clearInterval(watchTimer);
    watchTimer = null;
  } else {
    view.renderServers([...seen.values()]);
    pollOnce();
    watchTimer = setInterval(pollOnce, 1000);
  }
  view.setWatching(Boolean(watchTimer));
}

// --- Sales-day CPU spike ------------------------------------------------------
async function sendToServers(requests, call) {
  // Separate requests are spread across healthy servers by the load balancer.
  const settled = await Promise.allSettled(Array.from({ length: requests }, () => call()));
  return settled.map((s) => (s.status === 'fulfilled' ? s.value : { error: s.reason.message }));
}

async function startSpike(event) {
  event.preventDefault();
  const { seconds, requests } = view.readSpikeForm();
  view.setSpikeBusy(true);
  view.showSpikeMessage('Starting spike...');
  try {
    const results = await sendToServers(requests, () => api.startSpike(seconds));
    view.renderSpikeResults(results);
    if (results.every((r) => r.error)) {
      view.showSpikeMessage(results[0].error, 'error');
    } else {
      const servers = new Set(results.filter((r) => !r.error).map((r) => r.serverId)).size;
      view.showSpikeMessage(`CPU spike running on ${servers} server(s). Watch the server list: new servers usually appear within 3 to 5 minutes.`, 'success');
    }
  } finally {
    view.setSpikeBusy(false);
  }
}

async function stopSpikes() {
  const { requests } = view.readSpikeForm();
  view.setSpikeBusy(true);
  try {
    view.renderSpikeResults(await sendToServers(requests, api.stopSpike));
    view.showSpikeMessage('Stop sent. Servers that did not receive it finish when their timer ends.', 'success');
  } finally {
    view.setSpikeBusy(false);
  }
}

view.form.addEventListener('submit', submitOrder);
view.watchButton.addEventListener('click', toggleWatching);
view.spikeForm.addEventListener('submit', startSpike);
view.stopButton.addEventListener('click', stopSpikes);
loadInfo();
loadOrders();

// Keep the table current without reloading the page; skip refreshes while the tab is hidden.
setInterval(() => {
  if (!document.hidden) loadOrders();
}, ORDER_REFRESH_MS);
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) loadOrders();
});
