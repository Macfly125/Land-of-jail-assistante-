import { createApp } from '../server';

// Fonction Vercel catch-all : toutes les requêtes /api/* sont transmises
// à Express, qui conserve ses routes /api/... existantes.
const app = await createApp(false);

export default app;
