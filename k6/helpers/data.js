// k6/helpers/data.js

export function firstId(res) {
  try {
    const content = res.json('content');
    return (content && content.length > 0) ? content[0].id : null;
  } catch (e) {
    return null;
  }
}