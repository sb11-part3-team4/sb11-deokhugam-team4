// k6/helpers/auth.js

export function getAuthHeaders(userId, isMultipart = false) {
  const headers = {
    'Deokhugam-Request-User-Id': userId,
  };

  if (!isMultipart) {
    headers['Content-Type'] = 'application/json';
  }
  return headers;
}