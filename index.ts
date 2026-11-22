import express from 'express';
import { createApp } from './server';

// Vercel détecte ce point d'entrée Express et expose toutes les routes /api/*.
// Le frontend Vite est publié séparément depuis dist/.
void express;
const app = await createApp(false);

export default app;
