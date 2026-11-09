// Controller: wires user events to API calls and updates the view.
import { api } from './api.js';
import { view } from './view.js';

async function loadInfo() {
  try {
    view.renderInfo(await api.getInfo());
  } catch (error) {
    view.renderInfoUnavailable();
    view.showMessage(error.message, 'error');
  }
}

async function loadOrders() {
  try {
    view.renderOrders(await api.listOrders());
  } catch (error) {
    view.renderOrdersError(error.message);
  }
}

async function submitOrder(event) {
  event.preventDefault();
  view.setBusy(true);
  view.showMessage('Submitting order...');
  try {
    const order = await api.createOrder(view.readOrderForm());
    view.setServer(order.servedBy);
    view.showMessage(`Order ${order.id.slice(0, 8)} received by ${order.servedBy}.`, 'success');
    view.resetOrderForm();
    await loadOrders();
  } catch (error) {
    view.showMessage(error.message, 'error');
  } finally {
    view.setBusy(false);
  }
}

view.form.addEventListener('submit', submitOrder);
loadInfo();
loadOrders();
