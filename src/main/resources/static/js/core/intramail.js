/* Intramail tab of the Mail page: messages between the users of the application (kept in the database). Uses the helpers of mail.html (T, esc, g, when, SFA, Api, bootstrap). */
(function () {
  const IEP = '/api/intramail';
  let IF = null, ICUR = 'inbox', IPAGE = 0, IROWS = [], ITOTAL = 0, ISEL = new Set(), IOPEN = null, ICM = null, IGM = null, IDRAFT = null, IPARENT = null, IATTS = [], IDIRTY = false, ISAVING = false, RC = { to: [], cc: [], bcc: [] }, IT = null, GROUPS = [], GEDIT = null, GMEM = [];
  const ISIZE = 25;
  const FOLD = [['inbox', 'inbox', 'mb.f.inbox', 'Inbox'], ['sent', 'send', 'mb.f.sent', 'Sent'], ['drafts', 'file-earmark', 'mb.f.drafts', 'Drafts'], ['starred', 'star', 'mb.starred', 'Starred'], ['trash', 'trash', 'mb.f.trash', 'Trash']];

  window.introInit = async function () {
    if (typeof MAILMODE !== 'undefined' && MAILMODE === 'EXTERNAL') return;   // the company does not use intramail
    ICM = new bootstrap.Modal('#icomposeModal'); IGM = new bootstrap.Modal('#groupsModal');
    try { IF = await Api.get(IEP + '/folders', { noLoader: true }); } catch (e) { IF = { inbox: 0, drafts: 0, ready: false }; }
    drawBadge(); setInterval(ipoll, 60000);
    // a user without a mail account of his own starts on the Intramail tab
    try { await Api.get('/api/mailbox/admin', { noLoader: true }); window.ME_ADMIN = true; } catch (e) { window.ME_ADMIN = false; }
    if ((typeof MAILMODE !== 'undefined' && MAILMODE === 'INTRA') || (typeof F !== "undefined" && F && !F.account) || new URLSearchParams(location.search).get('tab') === 'intra') window.modeTab('intra');   // ?tab=intra comes from the Intramail menu
  };
  const drawBadge = () => { const b = g('intraBadge'); if (!b) return; b.textContent = IF.inbox; b.classList.toggle('d-none', !IF.inbox); };

  window.modeTab = async function (m) {
    MODE = m; g('tabExt').classList.toggle('active', m === 'ext'); g('tabIntra').classList.toggle('active', m === 'intra');
    g('extPane').classList.toggle('d-none', m !== 'ext'); g('introPane').classList.toggle('d-none', m !== 'intra');
    g('composeBtn').disabled = g('refreshBtn').disabled = m === 'ext' ? !(F && F.account) : false;
    document.title = m === 'intra' ? 'Intramail' : 'Mail'; g('syncInfo').textContent = '';
    if (m === 'intra') await iload(true);
  };

  async function iload(force) {
    try {
      IF = await Api.get(IEP + '/folders', { noLoader: !force }); drawBadge();
      if (!IF.ready) { g('ilist').innerHTML = `<div class="mb-empty text-danger">${esc(T('im.noTables', 'The intramail tables are missing - run migration 136_intramail.sql'))}</div>`; drawIFolders(); return; }
      drawIFolders(); await igo(IPAGE);
    } catch (e) { SFA.toast(e.message, false); }
  }
  window.iload = iload;
  function drawIFolders() {
    g('ifolders').innerHTML = FOLD.map(f => `<div class="mb-fold ${ICUR === f[0] ? 'on' : ''}" onclick="iopenFolder('${f[0]}')"><i class="bi bi-${f[1]}"></i><span>${T(f[2], f[3])}</span>${f[0] === 'inbox' && IF && IF.inbox ? `<span class="badge bg-primary cnt">${IF.inbox}</span>` : ''}${f[0] === 'drafts' && IF && IF.drafts ? `<span class="badge bg-secondary cnt">${IF.drafts}</span>` : ''}</div>`).join('');
  }
  window.iopenFolder = (f) => { ICUR = f; ISEL.clear(); IOPEN = null; g('imb').classList.remove('has-view'); igo(0); drawIFolders(); };
  window.igo = async function (page) {
    IPAGE = page; g('ilist').innerHTML = '<div class="mb-empty"><span class="spinner-border spinner-border-sm"></span></div>';
    try { const r = await Api.get(IEP + '/messages', { query: { folder: ICUR, page, size: ISIZE, q: g('iq').value.trim() || undefined }, noLoader: true }); IROWS = r.rows; ITOTAL = r.total; idrawList(); }
    catch (e) { g('ilist').innerHTML = `<div class="mb-empty text-danger">${esc(e.message)}</div>`; }
  };
  function idrawList() {
    const sentLike = ['sent', 'drafts'].includes(ICUR), b = (a, ic, t) => `<button class="btn btn-sm btn-outline-secondary py-0" title="${t}" onclick="ibulk('${a}')"><i class="bi bi-${ic}"></i></button>`;
    g('ibulk').innerHTML = `<input type="checkbox" onchange="iselAll(this.checked)" ${IROWS.length && IROWS.every(r => ISEL.has(r.id)) ? 'checked' : ''}> <span class="text-muted">${ISEL.size ? ISEL.size + ' ' + T('mb.selected', 'selected') : ''}</span>`
      + (ISEL.size ? (['inbox', 'starred'].includes(ICUR) ? b('READ', 'envelope-open', T('mb.markRead', 'Mark read')) + b('UNREAD', 'envelope', T('mb.markUnread', 'Mark unread')) + b('STAR', 'star', T('mb.star', 'Star')) : '') + (ICUR === 'trash' ? b('RESTORE', 'arrow-counterclockwise', T('mb.restore', 'Restore')) + `<button class="btn btn-sm btn-outline-danger py-0" onclick="ibulk('PURGE')"><i class="bi bi-x-octagon"></i></button>` : b('DELETE', 'trash', T('mb.delete', 'Delete'))) : '');
    g('ilist').innerHTML = IROWS.length ? IROWS.map(r => `<div class="mb-row ${r.isRead ? '' : 'unread'} ${IOPEN === r.id ? 'on' : ''}" onclick="iopen(${r.id})"><input type="checkbox" ${ISEL.has(r.id) ? 'checked' : ''} onclick="event.stopPropagation();ipick(${r.id},this.checked)"><i class="bi bi-star${r.isStarred ? '-fill' : ''} mb-star ${r.isStarred ? 'on' : ''}" onclick="event.stopPropagation();istar(${r.id},${r.isStarred ? 0 : 1})"></i>
      <div style="min-width:0"><div class="mb-from">${esc(sentLike ? (r.toLabel || '(draft)') : r.fromName)}</div><div class="mb-subj">${esc(r.subject || T('mb.noSubject', '(no subject)'))}${r.hasAttach ? ' <i class="bi bi-paperclip"></i>' : ''}</div><div class="mb-snip">${esc(r.snippet || '')}</div></div><div class="mb-date">${esc(when(r.sentAt))}</div></div>`).join('') : `<div class="mb-empty"><i class="bi bi-inbox fs-1 d-block mb-2"></i>${T('mb.empty', 'No mail here')}</div>`;
    const pages = Math.max(1, Math.ceil(ITOTAL / ISIZE));
    g('ipager').innerHTML = `<button class="btn btn-sm btn-outline-secondary py-0" ${IPAGE <= 0 ? 'disabled' : ''} onclick="igo(${IPAGE - 1})"><i class="bi bi-chevron-left"></i></button><span>${ITOTAL ? IPAGE * ISIZE + 1 : 0}-${Math.min(ITOTAL, (IPAGE + 1) * ISIZE)} / ${ITOTAL}</span><button class="btn btn-sm btn-outline-secondary py-0" ${IPAGE + 1 >= pages ? 'disabled' : ''} onclick="igo(${IPAGE + 1})"><i class="bi bi-chevron-right"></i></button>`;
  }
  window.ipick = (id, on) => { on ? ISEL.add(id) : ISEL.delete(id); idrawList(); };
  window.iselAll = (on) => { IROWS.forEach(r => on ? ISEL.add(r.id) : ISEL.delete(r.id)); idrawList(); };
  window.istar = async (id, on) => { try { await Api.post(IEP + '/messages/action', { ids: [id], action: on ? 'STAR' : 'UNSTAR' }, { noLoader: true }); const r = IROWS.find(x => x.id === id); if (r) r.isStarred = on; idrawList(); } catch (e) { SFA.toast(e.message, false); } };
  window.ibulk = async (action) => { if (action === 'PURGE' && !confirm(T('mb.confirmPurge', 'Delete these mails permanently? This cannot be undone.'))) return; try { await Api.post(IEP + '/messages/action', { ids: [...ISEL], action }); ISEL.clear(); IOPEN = null; await iload(); } catch (e) { SFA.toast(e.message, false); } };

  // ---------------------------------------------------------------- view
  window.iopen = async function (id) {
    IOPEN = id; g('imb').classList.add('has-view'); g('iview').innerHTML = '<div class="mb-empty"><span class="spinner-border"></span></div>';
    try {
      const m = await Api.get(IEP + '/messages/' + id, { noLoader: true }); const r = IROWS.find(x => x.id === id); if (r && !r.isRead) { r.isRead = 1; Api.get(IEP + '/folders', { noLoader: true }).then(f => { IF = f; drawBadge(); drawIFolders(); }); } idrawList(); idrawView(m);
    } catch (e) { g('iview').innerHTML = `<div class="mb-empty text-danger">${esc(e.message)}</div>`; }
  };
  function idrawView(m) {
    const act = (mode, ic, t) => `<button class="btn btn-sm btn-outline-secondary" onclick="ireply(${m.id},'${mode}')"><i class="bi bi-${ic}"></i> ${t}</button>`;
    const rec = m.recipients ? m.recipients.map(r => `<span class="badge text-bg-light border">${esc(r.name)}${r.kind !== 'TO' ? ' (' + r.kind + ')' : ''}</span>`).join(' ') : `<span class="text-muted">${m.recipientCount} ${T('im.people', 'people')}</span>`;
    g('iview').innerHTML = `<div class="mb-view-h"><button class="btn btn-sm btn-link mb-back px-0" onclick="document.getElementById('imb').classList.remove('has-view')"><i class="bi bi-arrow-left"></i> ${T('mb.back', 'Back')}</button>
      <h5 class="mb-1">${esc(m.subject || T('mb.noSubject', '(no subject)'))}</h5><div class="small"><b>${esc(m.fromName)}</b> · <span class="text-muted">${esc(String(m.sentAt || '').replace('T', ' ').substring(0, 16))}</span></div>
      ${m.isDraft ? '' : `<div class="small mt-1"><span class="text-muted">To:</span> ${m.toLabel ? esc(m.toLabel) : ''} <span class="ms-1">${rec}</span></div>`}${m.mine && m.readCount != null && !m.isDraft ? `<div class="small text-muted">${T('im.readBy', 'Read by')} ${m.readCount}</div>` : ''}
      <div class="d-flex flex-wrap gap-1 mt-2">${m.isDraft ? `<button class="btn btn-sm btn-primary" onclick="iedit(${m.id})"><i class="bi bi-pencil"></i> ${T('mb.edit', 'Edit')}</button>` : act('REPLY', 'reply', T('mb.reply', 'Reply')) + act('REPLYALL', 'reply-all', T('mb.replyAll', 'Reply all')) + act('FORWARD', 'forward', T('mb.forward', 'Forward'))}
        <button class="btn btn-sm btn-outline-danger" onclick="iselOne(${m.id})"><i class="bi bi-trash"></i> ${T('mb.delete', 'Delete')}</button></div></div>
      <div class="p-2">${m.html ? '<iframe class="mb-frame" id="ifr" sandbox="allow-popups allow-popups-to-escape-sandbox" referrerpolicy="no-referrer"></iframe>' : `<pre style="white-space:pre-wrap;font-family:inherit">${esc(m.text || '')}</pre>`}</div>
      ${m.attachments.length ? `<div class="p-2 border-top"><div class="small fw-semibold mb-1"><i class="bi bi-paperclip"></i> ${T('mb.attachments', 'Attachments')}</div>${m.attachments.map(a => `<button class="btn btn-sm btn-outline-primary me-1 mb-1" onclick="idl(${m.id},${a.id},'${esc(a.name).replace(/'/g, '')}')"><i class="bi bi-download"></i> ${esc(a.name)} <small>${a.size ? Math.ceil(a.size / 1024) + ' KB' : ''}</small></button>`).join('')}</div>` : ''}`;
    if (m.html) { const fr = g('ifr'); fr.srcdoc = '<base target="_blank"><style>body{font-family:system-ui,sans-serif;font-size:14px;margin:8px;word-wrap:break-word}img{max-width:100%;height:auto}</style>' + m.html; fr.onload = () => { try { fr.style.height = (fr.contentDocument.documentElement.scrollHeight + 20) + 'px'; } catch (e) { /* sandboxed */ } }; }
  }
  window.iselOne = async (id) => { try { await Api.post(IEP + '/messages/action', { ids: [id], action: ICUR === 'trash' ? 'PURGE' : 'DELETE' }); IOPEN = null; g('imb').classList.remove('has-view'); g('iview').innerHTML = ''; await iload(); } catch (e) { SFA.toast(e.message, false); } };
  window.idl = async (id, att, name) => { try { await Api.download(IEP + '/messages/' + id + '/attachments/' + att, name); } catch (e) { SFA.toast(e.message, false); } };
  async function ipoll() { if (MODE === 'intra') return; try { const before = IF ? IF.inbox : 0; IF = await Api.get(IEP + '/folders', { noLoader: true }); drawBadge(); if (IF.inbox > before) SFA.toast(T('im.newArrived', 'New intramail arrived'), true); } catch (e) { /* the next poll tries again */ } }

  // ---------------------------------------------------------------- compose
  const chip = (k, i, p) => `<span class="badge text-bg-primary">${esc(p.label)} <a href="#" class="text-white text-decoration-none" onclick="irm('${k}',${i});return false">&times;</a></span>`;
  const drawChips = () => ['to', 'cc', 'bcc'].forEach(k => { g('ichips_' + k).innerHTML = RC[k].map((p, i) => chip(k, i, p)).join('') || '<span class="text-muted small">&nbsp;</span>'; });
  window.irm = (k, i) => { RC[k].splice(i, 1); drawChips(); idirty(); };
  window.idirty = () => { IDIRTY = true; g('isaveInfo').textContent = ''; };
  function ireset() { RC = { to: [], cc: [], bcc: [] }; g('ic_subj').value = ''; g('ic_body').innerHTML = ''; IATTS = []; IDRAFT = null; IPARENT = null; IDIRTY = false; ['iccRow', 'ibccRow'].forEach(i => g(i).classList.add('d-none')); ['iin_to', 'iin_cc', 'iin_bcc'].forEach(i => g(i).value = ''); drawChips(); idrawAtts(); g('isaveInfo').textContent = ''; }
  window.icompose = function (pre) {
    ireset();
    if (pre) { RC.to = pre.to || []; RC.cc = pre.cc || []; RC.bcc = pre.bcc || []; if (RC.cc.length || RC.bcc.length) { g('iccRow').classList.remove('d-none'); g('ibccRow').classList.remove('d-none'); } g('ic_subj').value = pre.subject || ''; g('ic_body').innerHTML = pre.html || ''; IPARENT = pre.parentId || null; IATTS = (pre.attachments || []).map(a => ({ fwdAtt: a.fwdAtt, name: a.name, size: a.size })); drawChips(); idrawAtts(); }
    ICM.show(); setTimeout(() => g('iin_to').focus(), 300);
  };
  window.ireply = async (id, mode) => { try { icompose(await Api.get(IEP + '/messages/' + id + '/prepare', { query: { mode } })); } catch (e) { SFA.toast(e.message, false); } };
  window.iedit = async (id) => { try { const m = await Api.get(IEP + '/messages/' + id, { noLoader: true }); icompose({ subject: m.subject, html: m.html || '', to: ((m.draft || {}).to) || [], cc: ((m.draft || {}).cc) || [], bcc: ((m.draft || {}).bcc) || [] }); IDRAFT = id; } catch (e) { SFA.toast(e.message, false); } };
  window.ifmt = (c) => { document.execCommand(c); g('ic_body').focus(); idirty(); };
  window.ipeople = function (inp, k) {
    clearTimeout(IT); document.querySelectorAll('.mb-sug').forEach(e => e.remove()); const q = inp.value.trim(); if (!q) return;
    IT = setTimeout(async () => { try { const l = (await Api.get(IEP + '/people', { query: { q }, noLoader: true })).filter(p => !RC[k].some(x => x.type === p.type && String(x.id) === String(p.id))); if (!l.length) return; const box = document.createElement('div'); box.className = 'mb-sug';
      box.innerHTML = l.map((p, i) => `<div data-i="${i}"><i class="bi bi-${p.type === 'USER' ? 'person' : p.type === 'GROUP' ? 'people' : 'globe'}"></i> ${esc(p.label)}</div>`).join(''); inp.parentElement.appendChild(box);
      box.onclick = (ev) => { const d = ev.target.closest('div[data-i]'); if (!d) return; RC[k].push(l[+d.dataset.i]); inp.value = ''; box.remove(); drawChips(); idirty(); inp.focus(); }; } catch (e) { /* no suggestions */ } }, 250);
  };
  window.iattach = async function (inp) { for (const f of inp.files) { try { const fd = new FormData(); fd.append('file', f); IATTS.push(await Api.post('/api/mailbox/upload', fd)); } catch (e) { SFA.toast(f.name + ': ' + e.message, false); } } inp.value = ''; idrawAtts(); idirty(); };
  function idrawAtts() { g('ic_atts').innerHTML = IATTS.map((a, i) => `<span class="badge text-bg-light border me-1"><i class="bi bi-paperclip"></i> ${esc(a.name)} <small>${a.size ? Math.ceil(a.size / 1024) + ' KB' : ''}</small> <a href="#" onclick="IATTSplice(${i});return false">&times;</a></span>`).join(''); }
  window.IATTSplice = (i) => { IATTS.splice(i, 1); idrawAtts(); idirty(); };
  const ref = (l) => l.map(p => ({ type: p.type, id: p.id }));
  const ipayload = () => ({ to: ref(RC.to), cc: ref(RC.cc), bcc: ref(RC.bcc), subject: g('ic_subj').value, html: g('ic_body').innerHTML, parentId: IPARENT, draftId: IDRAFT, attachments: IATTS.map(a => a.fwdAtt ? { fwdAtt: a.fwdAtt } : { path: a.path, name: a.name, size: a.size, type: a.type }) });
  window.isaveDraft = async function (manual) {
    if (ISAVING) return; if (!manual && !IDIRTY) return; ISAVING = true;
    try { const r = await Api.post(IEP + '/drafts', ipayload(), { noLoader: true }); IDRAFT = r.draftId; IDIRTY = false; g('isaveInfo').textContent = T('mb.draftSaved', 'Draft saved'); if (manual) SFA.toast(T('mb.draftSaved', 'Draft saved'), true); } catch (e) { if (manual) SFA.toast(e.message, false); } finally { ISAVING = false; }
  };
  setInterval(() => { if (g('icomposeModal') && g('icomposeModal').classList.contains('show')) isaveDraft(false); }, 30000);
  window.icloseCompose = () => { if (IDIRTY && !confirm(T('mb.discardDraft', 'Close without saving? Your changes will be lost.'))) return; IDIRTY = false; ICM.hide(); };
  window.isend = async function () {
    g('isendBtn').disabled = true;
    try { const r = await Api.post(IEP + '/send', ipayload()); IDIRTY = false; ICM.hide(); SFA.toast(T('im.sent', 'Sent to') + ' ' + r.recipients + ' ' + T('im.people', 'people'), true); await iload(); }
    catch (e) { SFA.toast(e.message, false); } finally { g('isendBtn').disabled = false; }
  };

  // ---------------------------------------------------------------- groups
  window.groupsOpen = async function () { IGM.show(); g('g_companyRow').classList.toggle('d-none', !(window.ME_ADMIN)); await gload(); gnew(); };
  async function gload() {
    try { GROUPS = await Api.get(IEP + '/groups', { noLoader: true }); } catch (e) { SFA.toast(e.message, false); return; }
    g('glist').innerHTML = GROUPS.length ? GROUPS.map((x, i) => `<div class="d-flex align-items-center border-bottom py-1"><div class="flex-fill"><b>${esc(x.name)}</b> ${x.company ? `<span class="badge text-bg-info">${T('im.company', 'company')}</span>` : ''}<div class="small text-muted">${x.members.length} ${T('im.people', 'people')}: ${esc(x.members.slice(0, 6).map(m => m.name).join(', '))}${x.members.length > 6 ? '…' : ''}</div></div><button class="btn btn-sm btn-outline-secondary me-1" onclick="gedit(${i})"><i class="bi bi-pencil"></i></button><button class="btn btn-sm btn-outline-danger" onclick="gdel(${x.id})"><i class="bi bi-trash"></i></button></div>`).join('') : `<div class="text-muted small">${T('im.noGroups', 'No groups yet')}</div>`;
  }
  window.gnew = () => { GEDIT = null; GMEM = []; g('g_name').value = ''; g('g_company').checked = false; g('gformTitle').textContent = T('im.newGroup', 'New group'); gchips(); };
  window.gedit = (i) => { const x = GROUPS[i]; GEDIT = x.id; GMEM = x.members.map(m => ({ id: m.empId, label: m.name })); g('g_name').value = x.name; g('g_company').checked = !!x.company; g('gformTitle').textContent = x.name; gchips(); };
  const gchips = () => { g('g_chips').innerHTML = GMEM.map((p, i) => `<span class="badge text-bg-primary">${esc(p.label)} <a href="#" class="text-white text-decoration-none" onclick="GMEMrm(${i});return false">&times;</a></span>`).join(''); };
  window.GMEMrm = (i) => { GMEM.splice(i, 1); gchips(); };
  window.gfind = function (inp) {
    clearTimeout(IT); document.querySelectorAll('.mb-sug').forEach(e => e.remove()); const q = inp.value.trim(); if (!q) return;
    IT = setTimeout(async () => { try { const l = (await Api.get(IEP + '/people', { query: { q }, noLoader: true })).filter(p => p.type === 'USER' && !GMEM.some(x => x.id === p.id)); if (!l.length) return; const box = document.createElement('div'); box.className = 'mb-sug'; box.innerHTML = l.map((p, i) => `<div data-i="${i}">${esc(p.label)}</div>`).join(''); inp.parentElement.appendChild(box);
      box.onclick = (ev) => { const d = ev.target.closest('div[data-i]'); if (!d) return; GMEM.push(l[+d.dataset.i]); inp.value = ''; box.remove(); gchips(); }; } catch (e) { /* none */ } }, 250);
  };
  window.gsave = async () => { try { await Api.post(IEP + '/groups', { id: GEDIT, name: g('g_name').value, company: g('g_company').checked, empIds: GMEM.map(m => m.id) }); SFA.toast(T('inv.saved', 'Saved'), true); await gload(); gnew(); } catch (e) { SFA.toast(e.message, false); } };
  window.gdel = async (id) => { if (!confirm(T('perm.confirmDel', 'Remove?'))) return; try { await Api.post(IEP + '/groups/' + id + '/delete', {}); await gload(); } catch (e) { SFA.toast(e.message, false); } };
  document.addEventListener('click', (e) => { if (!e.target.closest('.mb-sug')) document.querySelectorAll('.mb-sug').forEach(x => x.remove()); });
})();
