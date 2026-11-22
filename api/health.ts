export default function handler(_req: unknown, res: {
  status: (code: number) => { json: (body: Record<string, unknown>) => void };
}) {
  res.status(200).json({
    ok: true,
    service: 'lands-of-jail-assistant-api',
    route: 'vercel-health-function',
  });
}
