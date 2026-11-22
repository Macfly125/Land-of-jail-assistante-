export async function readApiJson<T>(response: Response, endpoint: string): Promise<T> {
  const raw = await response.text();
  let parsed: unknown;

  try {
    parsed = JSON.parse(raw);
  } catch {
    const contentType = response.headers.get('content-type') || 'type de contenu inconnu';
    const preview = raw.replace(/\\s+/g, ' ').trim().slice(0, 180);
    const status = `HTTP ${response.status}`;
    throw new Error(
      `Réponse invalide du serveur pour ${endpoint} (${status}, ${contentType}). ` +
      `L'application attend du JSON, mais le serveur a renvoyé une page ou du texte. ` +
      `Vérifie que le serveur API est déployé avec le site. Réponse reçue : ${preview || '(vide)'}`
    );
  }

  return parsed as T;
}
