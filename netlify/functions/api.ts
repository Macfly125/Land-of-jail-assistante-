import serverless from 'serverless-http';
import { createApp } from '../../server';

type NetlifyEvent = Parameters<ReturnType<typeof serverless>>[0];
type NetlifyContext = Parameters<ReturnType<typeof serverless>>[1];

let wrappedHandler: ReturnType<typeof serverless> | undefined;

export const handler = async (event: NetlifyEvent, context: NetlifyContext) => {
  if (!wrappedHandler) {
    // Reuse the Express application during warm invocations.
    const app = await createApp(false);
    wrappedHandler = serverless(app, {
      // The rewrite keeps the original Express route prefix (/api/...).
      basePath: '/.netlify/functions/api',
    });
  }

  return wrappedHandler(event, context);
};
