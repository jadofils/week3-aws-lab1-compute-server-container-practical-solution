// Data access: all AJAX calls to the Spring Boot REST API live here.

async function request(url, options = {}) {
  const response = await fetch(url, {
    cache: 'no-store',
    headers: { 'Accept': 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}) },
    ...options
  });
  let data = null;
  try {
    data = await response.json();
  } catch {
    // Non-JSON body; handled below.
  }
  if (!response.ok) {
    throw new Error((data && (data.detail || data.message)) || 'The request could not be completed.');
  }
  return data;
}

export const api = {
  getInfo: () => request('/api/info'),
  listOrders: () => request('/api/orders'),
  createOrder: (order) => request('/api/orders', { method: 'POST', body: JSON.stringify(order) }),
  startSpike: (seconds) => request('/api/load/spike', { method: 'POST', body: JSON.stringify({ seconds }) }),
  stopSpike: () => request('/api/load/stop', { method: 'POST' })
};
