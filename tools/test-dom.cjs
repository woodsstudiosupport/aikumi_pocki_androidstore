const {JSDOM}=require('jsdom');
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const ui=path.join(__dirname,'../remote/ui');
const wait=()=>new Promise(resolve=>setTimeout(resolve,40));
function boot(saved, native=false) {
 const dom=new JSDOM(fs.readFileSync(path.join(ui,'index.html'),'utf8'),{url:'https://appassets.androidplatform.net/release/ui/index.html',runScripts:'outside-only',pretendToBeVisual:true});
 const w=dom.window;
 if(saved) w.sessionStorage.setItem('aikumi-preview',saved);
 if(native) {
  let profile={};
  w.AikumiNative={postMessage(message){
   const request=JSON.parse(message);
   if(request.type==='save') profile=request.payload;
   queueMicrotask(()=>w.AikumiNative.onmessage({data:JSON.stringify({ok:true,state:profile})}));
  }};
 }
 for(const file of ['state.js','app.js']) w.eval(fs.readFileSync(path.join(ui,file),'utf8'));
 return dom;
}
function click(w,text) {
 const b=[...w.document.querySelectorAll('button')].find(n=>n.textContent===text);
 assert.ok(b, 'Missing button '+text); b.click();
}
(async()=>{
 let dom=boot(); let w=dom.window; await wait();
 assert.match(w.document.querySelector('h1').textContent,/A little company/);
 click(w,'Continue'); await wait();
 w.document.querySelector('input').value='<img src=x onerror=alert(1)>';
 w.document.querySelector('textarea').value='drawing and games';
 click(w,'Continue'); await wait();
 assert.equal(w.document.querySelector('[type=checkbox]').checked,false);
 const saved=w.sessionStorage.getItem('aikumi-preview'); dom.window.close();
 dom=boot(saved); w=dom.window; await wait();
 assert.equal(w.document.querySelector('h1').textContent,'Make yourself comfortable.');
 click(w,'Meet Aikumi'); await wait();
 assert.equal(w.document.querySelector('h1').textContent,'Aikumi');
 assert.equal(w.document.querySelector('.send').disabled,true);
 assert.match(w.document.querySelector('.bubble').textContent,/drawing and games/);
 w.document.querySelector('.menu').click(); await wait();
 click(w,'Edit name and interests'); await wait();
 click(w,'Continue'); await wait(); click(w,'Meet Aikumi'); await wait();
 assert.equal(w.document.querySelector('h1').textContent,'Aikumi');
 const complete=w.sessionStorage.getItem('aikumi-preview'); dom.window.close();
 dom=boot(complete); w=dom.window; await wait();
 assert.equal(w.document.querySelector('h1').textContent,'Aikumi');
 w.document.querySelector('.menu').click(); await wait();
 assert.match(w.document.querySelector('.settings').textContent,/<img src=x onerror=alert\(1\)>/);
 assert.equal(w.document.querySelectorAll('img').length,0);
 w.confirm=()=>true; click(w,'Reset local personalisation'); await wait();
 assert.equal(JSON.parse(w.sessionStorage.getItem('aikumi-preview')).name,'');
 assert.match(w.document.querySelector('h1').textContent,/A little company/);
 dom.window.close();
 dom=boot(null,true); await wait(); w=dom.window;
 click(w,'Continue'); await wait(); click(w,'Continue'); await wait(); click(w,'Meet Aikumi'); await wait();
 assert.equal(w.document.querySelector('h1').textContent,'Aikumi'); dom.window.close();
 console.log('DOM flow passed: first launch, partial resume, consent default, repeat launch, safe rendering, reset, native messaging.');
})().catch(e=>{console.error(e);process.exit(1)});
