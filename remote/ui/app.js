'use strict';
const root = document.getElementById('root');
let state;
let nativeReply;
let panel = 'main';
function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}
function button(text, className, action) {
  const node = element('button', className, text); node.type = 'button';
  node.addEventListener('click', action); return node;
}
function nativeCall(type, payload) {
  return new Promise((resolve, reject) => {
    if (!window.AikumiNative) { reject(new Error('Native bridge unavailable')); return; }
    const timeout = setTimeout(() => { nativeReply = null; reject(new Error('Request timed out')); }, 6000);
    nativeReply = message => { clearTimeout(timeout); nativeReply = null;
      try { const result = JSON.parse(message.data); result.ok ? resolve(result.state) : reject(new Error(result.error)); }
      catch (error) { reject(error); }
    };
    window.AikumiNative.postMessage(JSON.stringify({ type, payload }));
  });
}
const preview = !window.AikumiNative;
async function load() {
  if (preview) {
    try { return AikumiState.normalise(JSON.parse(sessionStorage.getItem('aikumi-preview'))); }
    catch (_) { return AikumiState.normalise({}); }
  }
  window.AikumiNative.onmessage = event => nativeReply && nativeReply(event);
  return AikumiState.normalise(await nativeCall('read', null));
}
async function persist() {
  state = AikumiState.normalise(state);
  if (preview) sessionStorage.setItem('aikumi-preview', JSON.stringify(state));
  else await nativeCall('save', state);
}
function avatar(size = '') { return element('div', 'avatar ' + size, 'a'); }
function privacy() {
  return element('p', 'privacy', 'Your name and interests stay in this app. When chat is connected, messages and relevant personalisation are sent to OpenAI to generate replies.');
}
function setView(view, title) {
  root.replaceChildren(view); document.title = title + ' · Aikumi';
  requestAnimationFrame(() => { const heading = view.querySelector('h1'); if (heading) { heading.tabIndex = -1; heading.focus(); } });
}
async function transition(next, control) {
  if (control) control.disabled = true;
  const previous = { ...state };
  Object.assign(state, next);
  try { await persist(); render(); }
  catch (_) { state = previous; if (control) control.disabled = false;
    const error = element('p', 'error', 'Couldn’t save that. Please try again.'); error.role = 'alert'; root.append(error); }
}
function onboarding() {
  const page = element('section', 'onboarding');
  const top = element('header', 'onboarding-top'); top.append(element('span', 'wordmark', 'aikumi'), element('span', 'eyebrow', 'WOODS STUDIO')); page.append(top);
  const content = element('div', 'welcome-content');
  const step = state.onboardingStep;
  const hero = element('div', 'hero'); hero.append(avatar('large')); content.append(hero);
  const titles = ['A little company, wherever you are.', 'What should I call you?', 'Make yourself comfortable.'];
  content.append(element('p', 'eyebrow', ['HELLO, I’M AIKUMI', 'LET’S GET ACQUAINTED', 'YOUR OWN LITTLE SPACE'][step]), element('h1', '', titles[step]));
  const descriptions = ['A quiet chat. A curious question. A little room to be yourself. I’m an AI companion, and I’m glad you’re here.', 'A name and a few things you enjoy. Only if you feel like sharing.', 'Choose what stays on this device. You can change your mind in settings.'];
  content.append(element('p', 'intro', descriptions[step]));
  let nameInput, interestsInput, history;
  if (step === 1) {
    const nameLabel = element('label', '', 'Your name (optional)');
    nameInput = element('input', 'field'); nameInput.type = 'text'; nameInput.maxLength = 60; nameInput.autocomplete = 'off'; nameInput.value = state.name; nameLabel.append(nameInput);
    const interestsLabel = element('label', '', 'A few things you like (optional)');
    interestsInput = element('textarea', 'field'); interestsInput.maxLength = 300; interestsInput.rows = 2; interestsInput.placeholder = 'Drawing, games, quiet evenings…'; interestsInput.value = state.interests; interestsLabel.append(interestsInput);
    content.append(nameLabel, interestsLabel);
  }
  if (step === 2) {
    content.append(privacy());
    const card = element('label', 'choice'); history = element('input'); history.type = 'checkbox'; history.checked = state.saveHistory;
    const copy = element('span'); copy.append(element('strong', '', 'Save chat history on this device'), element('small', '', 'Off by default. Personalisation is stored separately.'));
    card.append(history, copy); content.append(card);
    content.append(element('p', 'quiet', 'No microphone or notification permission is needed to get started.'));
  }
  page.append(content);
  const bottom = element('footer', 'onboarding-bottom');
  const dots = element('div', 'steps'); dots.setAttribute('aria-label', 'Step ' + (step + 1) + ' of 3');
  for (let i = 0; i < 3; i++) dots.append(element('span', i === step ? 'dot selected' : 'dot'));
  bottom.append(dots);
  const actions = element('div', 'actions');
  const values = () => step === 1 ? { name: nameInput.value, interests: interestsInput.value } : step === 2 ? { saveHistory: history.checked } : {};
  if (step > 0) actions.append(button('Back', 'secondary', event => transition({ ...values(), onboardingStep: step - 1 }, event.currentTarget)));
  actions.append(button(step === 2 ? 'Meet Aikumi' : 'Continue', 'primary', event => transition({ ...values(), onboardingStep: Math.min(2, step + 1), onboardingComplete: step === 2 }, event.currentTarget)));
  bottom.append(actions); page.append(bottom); setView(page, titles[step]);
}
function settings() {
  const view = element('section', 'settings');
  view.append(button('‹ Back to chat', 'text-button', () => { panel = 'main'; render(); }), element('h1', '', 'Your space'), privacy());
  view.append(element('h2', '', 'Personalisation'), element('p', 'intro', state.name ? 'You go by ' + state.name + '.' : 'You haven’t added a name.'), element('p', 'quiet', state.interests || 'No interests added yet.'));
  view.append(button('Edit name and interests', 'secondary', event => transition({ onboardingComplete: false, onboardingStep: 1 }, event.currentTarget)));
  const label = element('label', 'choice'); const toggle = element('input'); toggle.type = 'checkbox'; toggle.checked = state.saveHistory;
  toggle.addEventListener('change', async () => { toggle.disabled = true;
    const before = state.saveHistory; state.saveHistory = toggle.checked;
    try { await persist(); } catch (_) { state.saveHistory = before; toggle.checked = before; } finally { toggle.disabled = false; }
  });
  label.append(toggle, element('span', '', 'Save future chat history on this device')); view.append(label);
  view.append(element('p', 'quiet', 'Chat and voice are not connected in this first build. No conversation is sent or saved.'));
  view.append(button('Reset local personalisation', 'danger', () => {
    if (!confirm('Clear your name, interests and onboarding choices on this device?')) return;
    transition({ name: '', interests: '', onboardingComplete: false, onboardingStep: 0, saveHistory: false });
  }));
  setView(view, 'Settings');
}
function chat() {
  const view = element('section', 'chat');
  const header = element('header', 'chat-header'); const identity = element('div', 'identity');
  const text = element('div'); text.append(element('h1', '', 'Aikumi'), element('span', 'subtitle', 'A little space for you'));
  identity.append(avatar(), text);
  const menu = button('···', 'menu', () => { panel = 'settings'; render(); }); menu.setAttribute('aria-label', 'Open settings'); header.append(identity, menu); view.append(header);
  const conversation = element('div', 'conversation');
  const intro = element('div', 'chat-intro'); intro.append(element('span', 'eyebrow', 'MAKE YOURSELF AT HOME'), element('p', '', 'No rush. We can start small.')); conversation.append(intro);
  const row = element('div', 'message-row'); row.append(avatar('tiny'));
  const bubble = element('div', 'bubble');
  const greeting = state.interests ? 'Hey… you mentioned ' + state.interests + '. What have you been enjoying lately?' : 'Hey… what should we talk about first?';
  bubble.append(element('p', '', greeting));
  const time = element('small', 'timestamp', 'Welcome message'); time.hidden = true;
  const timestampButton = button('Show message details', 'message-details', () => { time.hidden = !time.hidden; timestampButton.textContent = time.hidden ? 'Show message details' : 'Hide message details'; });
  bubble.append(time, timestampButton); row.append(bubble); conversation.append(row); view.append(conversation);
  const bottom = element('footer', 'chat-bottom');
  const status = element('p', 'connection-note', 'Your space is ready. Chat connection is coming next.'); status.role = 'status'; bottom.append(status);
  const composer = element('div', 'composer'); const input = element('textarea'); input.rows = 1; input.maxLength = 4000; input.placeholder = 'What’s on your mind?'; input.setAttribute('aria-label', 'Message draft');
  input.addEventListener('input', () => { input.style.height = 'auto'; input.style.height = Math.min(input.scrollHeight, 140) + 'px'; });
  const send = button('↑', 'send', () => {}); send.disabled = true; send.setAttribute('aria-label', 'Send unavailable: chat is not connected');
  composer.append(input, send); bottom.append(composer, element('p', 'composer-hint', 'AI companion · Drafts aren’t saved in this build')); view.append(bottom);
  setView(view, 'Chat');
}
function render() { if (!state.onboardingComplete) onboarding(); else if (panel === 'settings') settings(); else chat(); }
load().then(async value => { state = value; render(); if (!preview) await nativeCall('ready', null); }).catch(() => {
  const view = element('section', 'settings'); view.append(element('h1', '', 'A little trouble settling in'), element('p', 'intro', 'Your local settings couldn’t be loaded.'), button('Try again', 'primary', () => location.reload())); root.replaceChildren(view);
});
