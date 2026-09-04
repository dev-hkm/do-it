interface Env {
  BACKUPS: D1Database;
}

const TOKEN_PATTERN = /^hkm-[A-Za-z0-9]{36}$/;
const MAX_PAYLOAD_BYTES = 4 * 1024 * 1024;

const json = (body: unknown, status = 200): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "Content-Type",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Cache-Control": "no-store",
    },
  });

async function tokenHash(token: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return [...new Uint8Array(digest)].map((value) => value.toString(16).padStart(2, "0")).join("");
}

function validToken(token: unknown): token is string {
  return typeof token === "string" && TOKEN_PATTERN.test(token);
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") return json({}, 204);
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ status: "ok", service: "do-it-cloud-backup" });
    }

    if (url.pathname !== "/v1/backup") return json({ error: "Not found" }, 404);

    if (request.method === "POST") {
      const contentLength = Number(request.headers.get("content-length") ?? 0);
      if (contentLength > MAX_PAYLOAD_BYTES) return json({ error: "Backup is too large" }, 413);
      let body: { token?: unknown; payload?: unknown };
      try {
        body = await request.json();
      } catch {
        return json({ error: "Invalid JSON" }, 400);
      }
      if (!validToken(body.token) || typeof body.payload !== "string") {
        return json({ error: "Invalid token or payload" }, 400);
      }
      if (new TextEncoder().encode(body.payload).byteLength > MAX_PAYLOAD_BYTES) {
        return json({ error: "Backup is too large" }, 413);
      }
      await env.BACKUPS.prepare(
        "INSERT INTO backups (token_hash, payload, updated_at) VALUES (?, ?, ?) ON CONFLICT(token_hash) DO UPDATE SET payload = excluded.payload, updated_at = excluded.updated_at"
      ).bind(await tokenHash(body.token), body.payload, Date.now()).run();
      return json({ success: true, updatedAt: Date.now() });
    }

    if (request.method === "GET") {
      const token = url.searchParams.get("token");
      if (!validToken(token)) return json({ error: "Invalid token" }, 400);
      const row = await env.BACKUPS.prepare(
        "SELECT payload, updated_at FROM backups WHERE token_hash = ?"
      ).bind(await tokenHash(token)).first<{ payload: string; updated_at: number }>();
      if (!row) return json({ error: "Backup not found" }, 404);
      return json({ success: true, payload: row.payload, updatedAt: row.updated_at });
    }

    return json({ error: "Method not allowed" }, 405);
  },
};
