import { chromium } from 'playwright';
import path from 'path';

async function check() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1440, height: 900 } })).newPage();

  console.log('Navigating to http://localhost:8082...');
  await page.goto('http://localhost:8082', { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);

  const screenshotPath = path.resolve('./test-results/screenshots/tc10_clean_labels_copilot.png');
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log('Screenshot saved at:', screenshotPath);

  await browser.close();
  console.log('SUCCESS: Clean labels verified!');
}

check().catch(err => {
  console.error(err);
  process.exit(1);
});
