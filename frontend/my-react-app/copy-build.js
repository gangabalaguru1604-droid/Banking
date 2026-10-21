import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const sourceDir = path.resolve(__dirname, 'dist');
const targetDir = path.resolve(__dirname, '../../loan_project/loan_app/src/main/resources/static');

if (fs.existsSync(sourceDir)) {
  fs.cpSync(sourceDir, targetDir, { recursive: true });
  console.log(`[Build Sync] Successfully synced React production build to ${targetDir}`);
} else {
  console.warn(`[Build Sync] Source dist directory not found: ${sourceDir}`);
}
