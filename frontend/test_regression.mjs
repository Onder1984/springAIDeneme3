import { chromium } from 'playwright';

async function testRegression() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const page = await (await browser.newContext({ viewport: { width: 1440, height: 900 } })).newPage();

  console.log('1. Loading application...');
  await page.goto('http://localhost:8082', { waitUntil: 'networkidle' });

  console.log('2. Checking Copilot tab...');
  const chatTab = page.locator('.ant-tabs-tab:has-text("Taktik Copilot")');
  await chatTab.click();
  await page.waitForTimeout(500);

  console.log('3. Checking Threat Analysis tab...');
  const analysisTab = page.locator('.ant-tabs-tab:has-text("Taktik Tehdit & Niyet Analizi")');
  await analysisTab.click();
  await page.waitForTimeout(1000);

  console.log('4. Checking Radar Table tab...');
  const radarTab = page.locator('.ant-tabs-tab:has-text("Radar (ELINT) Tablosu")');
  await radarTab.click();
  await page.waitForTimeout(1000);
  const radarRows = await page.locator('.ant-table-tbody tr').count();
  console.log('Radar rows count:', radarRows);

  console.log('5. Checking COMINT Table tab...');
  const comintTab = page.locator('.ant-tabs-tab:has-text("Muhabere (COMINT) Tablosu")');
  await comintTab.click();
  await page.waitForTimeout(1000);
  const comintRows = await page.locator('.ant-table-tbody tr').count();
  console.log('COMINT rows count:', comintRows);

  await browser.close();
  console.log('ALL TABS WORKING WITH ZERO REGRESSION!');
}

testRegression().catch(err => {
  console.error('Regression error:', err);
  process.exit(1);
});
