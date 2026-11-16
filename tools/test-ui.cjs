/* Run against python -m http.server 8765 --directory remote. Requires Playwright. */
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
(async () => {
 const browser = await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH || undefined,args:['--no-sandbox']});
 try {
  const page = await browser.newPage({viewport:{width:390,height:844},deviceScaleFactor:1});
  const errors=[]; page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://127.0.0.1:8765/ui/');
  await page.getByRole('heading',{name:'A little company, wherever you are.'}).waitFor();
  await page.getByRole('button',{name:'Continue',exact:true}).click();
  await page.getByLabel('Your name (optional)').fill('<img src=x onerror=alert(1)>');
  await page.getByLabel('A few things you like (optional)').fill('drawing and games');
  await page.getByRole('button',{name:'Continue',exact:true}).click();
  await page.reload();
  await page.getByRole('heading',{name:'Make yourself comfortable.'}).waitFor();
  assert.equal(await page.getByRole('checkbox').isChecked(),false);
  await page.getByRole('button',{name:'Meet Aikumi'}).click();
  await page.getByRole('heading',{name:'Aikumi',exact:true}).waitFor();
  await page.reload();
  await page.getByRole('heading',{name:'Aikumi',exact:true}).waitFor();
  assert.equal(await page.getByRole('button',{name:'Send unavailable: chat is not connected'}).isDisabled(),true);
  assert.equal(await page.locator('.bubble').innerText().then(s=>s.includes('drawing and games')),true);
  await page.getByRole('button',{name:'Open settings'}).click();
  assert.equal(await page.locator('.settings img').count(),0);
  assert.ok((await page.locator('.settings').innerText()).includes('<img src=x onerror=alert(1)>'));
  page.once('dialog',d=>d.accept());
  await page.getByRole('button',{name:'Reset local personalisation'}).click();
  await page.getByRole('heading',{name:'A little company, wherever you are.'}).waitFor();
  await page.screenshot({path:'/tmp/aikumi-onboarding.png',fullPage:true,animations:'disabled'});
  await page.getByRole('button',{name:'Continue',exact:true}).click();
  await page.getByLabel('Your name (optional)').fill('Flynn');
  await page.getByRole('button',{name:'Continue',exact:true}).click();
  await page.getByRole('button',{name:'Meet Aikumi'}).click();
  await page.screenshot({path:'/tmp/aikumi-chat.png',fullPage:true,animations:'disabled'});
  for (const viewport of [{width:320,height:568},{width:768,height:1024},{width:844,height:390}]) {
   await page.setViewportSize(viewport);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
   await page.getByRole('button',{name:'Open settings'}).click();
   await page.getByRole('button',{name:'Edit name and interests'}).click();
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
   await page.getByRole('button',{name:'Continue',exact:true}).click();
   await page.getByRole('button',{name:'Meet Aikumi'}).click();
  }
  assert.deepEqual(errors,[]);
  // A separate browser context must start with no profile or onboarding completion.
  const fresh=await browser.newPage(); await fresh.goto('http://127.0.0.1:8765/ui/');
  await fresh.getByRole('heading',{name:'A little company, wherever you are.'}).waitFor();
  console.log('UI passed: first run, resume, consent default, repeat launch, safe text, reset, responsive widths, isolated profile.');
 } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exit(1)});
