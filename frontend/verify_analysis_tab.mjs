import { chromium } from 'playwright';
import path from 'path';

async function check() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  console.log('Navigating to http://localhost:8082...');
  await page.goto('http://localhost:8082', { waitUntil: 'networkidle' });

  console.log('Finding and clicking on analysis tab...');
  const analysisTab = page.locator('.ant-tabs-tab:has-text("Taktik Tehdit & Niyet Analizi")');
  await analysisTab.click();

  console.log('Waiting for analysis data...');
  await page.waitForTimeout(3000);

  const screenshotPath = path.resolve('./test-results/screenshots/tc08_threat_analysis_dashboard.png');
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log('Screenshot saved at:', screenshotPath);

  await browser.close();
  console.log('SUCCESS: Tab verified and screenshot captured!');
}

check().catch(err => {
  console.error('Error verifying tab:', err);
  process.exit(1);
});
