import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const sourceDir = path.resolve(__dirname, 'dist');
const targetDir = path.resolve(__dirname, '../../loan_project/loan_app/src/main/resources/static');

if (fs.existsSync(sourceDir)) {
  try {
    if (fs.existsSync(path.dirname(targetDir))) {
      fs.cpSync(sourceDir, targetDir, { recursive: true });
      console.log(`[Build Sync] Successfully synced React production build to ${targetDir}`);
    } else {
      console.log('[Build Sync] Backend directory not found; skipping static asset sync (isolated frontend build).');
    }
  } catch (err) {
    console.warn('[Build Sync] Warning during asset sync:', err.message);
  }
} else {
  console.warn(`[Build Sync] Source dist directory not found: ${sourceDir}`);
}
