import { writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const configuredOrigin = process.env.API_ORIGIN?.trim();
if (!configuredOrigin) {
  throw new Error('API_ORIGIN must be set to the deployed backend host before building for Render.');
}

const origin = configuredOrigin.startsWith('http')
  ? configuredOrigin
  : `https://${configuredOrigin}`;
const apiUrl = new URL(origin);

if (apiUrl.protocol !== 'https:' || apiUrl.pathname !== '/' || apiUrl.search || apiUrl.hash) {
  throw new Error('API_ORIGIN must be an HTTPS origin without a path, query, or fragment.');
}

const config = `export const API_BASE_URL = ${JSON.stringify(`${apiUrl.origin}/api/v1`)};\n`;
const outputPath = resolve('src/app/runtime-config.ts');

await writeFile(outputPath, config, 'utf8');
