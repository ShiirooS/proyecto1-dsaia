export const UNAUTHORIZED_EVENT = 'auth:unauthorized';

export function notifyUnauthorized() {
  window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
}
