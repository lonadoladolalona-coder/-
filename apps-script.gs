// AJM Family — registration logger + admin-panel data source.
//
// Setup instructions are in ADMIN-SETUP.md at the project root.
// This file is not executed by the website directly — you paste its
// contents into the Apps Script editor bound to a Google Sheet.

// Change this to your own secret before deploying — it's the only thing
// standing between the public internet and your attendee list.
const ADMIN_KEY = 'ajm-2026-change-me';

// The one key the whole media team uses on media.html. Pick your own.
const MEDIA_KEY = 'media-2026-change-me';

const SHEET_NAME = 'Registrations';
const HEADERS = ['Timestamp', 'Source', 'Name', 'Phone', 'Email', 'Details', 'ID', 'Status', 'Archive', 'ArchivedAt'];
const ID_COL = 7;
const STATUS_COL = 8;
const ARCHIVE_COL = 9;      // name of the past event a registration was moved to (empty = still in the main list)
const ARCHIVED_AT_COL = 10;
const STATUSES = ['Pending', 'Contacted', 'Confirmed'];

function getSheet_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
    sheet.appendRow(HEADERS);
    sheet.setFrozenRows(1);
  } else if (sheet.getLastColumn() < HEADERS.length) {
    // migrate an existing sheet from before the ID/Status columns were added
    sheet.getRange(1, sheet.getLastColumn() + 1, 1, HEADERS.length - sheet.getLastColumn())
      .setValues([HEADERS.slice(sheet.getLastColumn())]);
  }
  return sheet;
}

function doPost(e) {
  const sheet = getSheet_();
  const data = JSON.parse(e.postData.contents);
  const row = sheet.getLastRow() + 1;

  sheet.getRange(row, 1).setValue(new Date());

  // Source..Details as plain text — otherwise Sheets reads a phone number
  // like "+91 99999 99999" as a formula and stores #ERROR! instead.
  const textRange = sheet.getRange(row, 2, 1, 5);
  textRange.setNumberFormat('@');
  textRange.setValues([[
    data.source || '',
    data.name || '',
    data.phone || '',
    data.email || '',
    data.details || ''
  ]]);

  const id = Utilities.getUuid();
  sheet.getRange(row, ID_COL).setNumberFormat('@').setValue(id);
  sheet.getRange(row, STATUS_COL).setNumberFormat('@').setValue('Pending');

  return ContentService.createTextOutput(JSON.stringify({ ok: true, id: id }))
    .setMimeType(ContentService.MimeType.JSON);
}

function doGet(e) {
  // Media office monthly work chart — has its own keys, separate from ADMIN_KEY.
  if (String(e.parameter.action || '').indexOf('media') === 0) {
    return handleMedia_(e.parameter);
  }

  // (the sample key is refused, so a forgotten "change me" can never expose the list)
  if (!e.parameter.key || e.parameter.key !== ADMIN_KEY || ADMIN_KEY === 'ajm-2026-change-me') {
    return ContentService.createTextOutput(JSON.stringify({ error: 'unauthorized' }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  if (e.parameter.action === 'delete') {
    return handleDelete_(e.parameter.id);
  }
  if (e.parameter.action === 'setStatus') {
    return handleSetStatus_(e.parameter.id, e.parameter.status);
  }
  if (String(e.parameter.action || '').indexOf('meeting') === 0) {
    return handleMeetings_(e.parameter);
  }
  if (['bulkStatus', 'bulkDelete', 'archive', 'unarchive'].indexOf(e.parameter.action) !== -1) {
    return handleBulk_(e.parameter);
  }

  const sheet = getSheet_();
  const rows = sheet.getDataRange().getValues();
  const headers = rows.shift();
  const data = rows.map(function (row) {
    const obj = {};
    headers.forEach(function (h, i) { obj[h] = row[i]; });
    return obj;
  });
  return ContentService.createTextOutput(JSON.stringify({ ok: true, data: data }))
    .setMimeType(ContentService.MimeType.JSON);
}

function findRowById_(sheet, id) {
  const ids = sheet.getRange(2, ID_COL, Math.max(sheet.getLastRow() - 1, 0), 1).getValues();
  for (let i = 0; i < ids.length; i++) {
    if (ids[i][0] === id) return i + 2; // +2: 1-indexed, plus header row
  }
  return -1;
}

function handleDelete_(id) {
  if (!id) {
    return ContentService.createTextOutput(JSON.stringify({ error: 'missing id' }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  const sheet = getSheet_();
  const row = findRowById_(sheet, id);
  if (row === -1) {
    return ContentService.createTextOutput(JSON.stringify({ error: 'not found' }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  sheet.deleteRow(row);
  return ContentService.createTextOutput(JSON.stringify({ ok: true }))
    .setMimeType(ContentService.MimeType.JSON);
}

function handleSetStatus_(id, status) {
  if (!id || STATUSES.indexOf(status) === -1) {
    return ContentService.createTextOutput(JSON.stringify({ error: 'invalid id or status' }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  const sheet = getSheet_();
  const row = findRowById_(sheet, id);
  if (row === -1) {
    return ContentService.createTextOutput(JSON.stringify({ error: 'not found' }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  sheet.getRange(row, STATUS_COL).setNumberFormat('@').setValue(status);
  return ContentService.createTextOutput(JSON.stringify({ ok: true }))
    .setMimeType(ContentService.MimeType.JSON);
}

// ---------------------------------------------------------------------------
// Bulk actions on registrations (admin.html — select rows, then act on all of them)
//
//   bulkStatus  ids=…&status=Confirmed   set the status of every listed registration
//   archive     ids=…&name=…             move them to History under a past-event name
//                                        (nothing is deleted — they only leave the main list)
//   unarchive   ids=…                    bring them back to the main list
//   bulkDelete  ids=…                    delete them for good
//
// ids is a comma-separated list of registration IDs (at most BULK_MAX_IDS per request;
// the page sends bigger selections in several requests). Unknown IDs are ignored, and the
// reply says how many rows were actually changed.
// ---------------------------------------------------------------------------
const BULK_MAX_IDS = 60;

function handleBulk_(p) {
  const ids = String(p.ids || '').split(',').map(function (x) { return x.trim(); }).filter(Boolean);
  if (!ids.length) return jsonOut_({ error: 'missing ids' });
  if (ids.length > BULK_MAX_IDS) return jsonOut_({ error: 'too many ids' });

  let status = '';
  let name = '';
  if (p.action === 'bulkStatus') {
    status = p.status;
    if (STATUSES.indexOf(status) === -1) return jsonOut_({ error: 'invalid status' });
  }
  if (p.action === 'archive') {
    name = String(p.name || '').replace(/\s+/g, ' ').trim();
    if (!name || name.length > 80) return jsonOut_({ error: 'invalid name' });
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(15000);
  try {
    const sheet = getSheet_();
    const last = sheet.getLastRow();
    const now = new Date().toISOString();
    if (last < 2) return jsonOut_({ ok: true, updated: 0, at: now });

    const want = {};
    ids.forEach(function (id) { want[id] = true; });
    const rows = [];
    sheet.getRange(2, ID_COL, last - 1, 1).getValues().forEach(function (v, i) {
      if (want[String(v[0])]) rows.push(i + 2); // +2: 1-indexed, plus the header row
    });

    if (p.action === 'bulkStatus') {
      rows.forEach(function (r) { sheet.getRange(r, STATUS_COL).setNumberFormat('@').setValue(status); });
    } else if (p.action === 'archive') {
      rows.forEach(function (r) { sheet.getRange(r, ARCHIVE_COL, 1, 2).setNumberFormat('@').setValues([[name, now]]); });
    } else if (p.action === 'unarchive') {
      rows.forEach(function (r) { sheet.getRange(r, ARCHIVE_COL, 1, 2).setNumberFormat('@').setValues([['', '']]); });
    } else if (p.action === 'bulkDelete') {
      // bottom-up, so deleting one row never moves the ones still to be deleted
      rows.sort(function (a, b) { return b - a; }).forEach(function (r) { sheet.deleteRow(r); });
    }
    return jsonOut_({ ok: true, updated: rows.length, at: now });
  } finally {
    lock.releaseLock();
  }
}

// ---------------------------------------------------------------------------
// Meeting history (admin.html → "Send Zoom Link" and "History")
//
// Every time a Zoom link is sent from the admin panel, the send is filed under a
// meeting name. One row per meeting in a "Meetings" tab; the people are stored in
// the Recipients column as JSON: [{"i": id, "n": name, "p": phone, "s": "sent"|"skipped", "t": time}].
// The page records each person as it goes, one small request at a time, so nothing
// is lost if the tab is closed half-way. Using an existing meeting name adds to that
// meeting. A person marked "sent" is never downgraded to "skipped".
// This only ever reads names and phone numbers already in the registrations —
// it never changes or removes a registration.
// ---------------------------------------------------------------------------
const MEETINGS_SHEET = 'Meetings';
const MEETING_HEADERS = ['ID', 'Name', 'CreatedAt', 'UpdatedAt', 'Message', 'Recipients'];
const MEETING_MAX_MEETINGS = 500;
const MEETING_MAX_PEOPLE = 400;

function meetingPhoneKey_(phone) {
  const digits = String(phone || '').replace(/\D/g, '');
  return digits.length >= 8 ? digits.slice(-10) : '';
}

function meetingPeople_(raw) {
  try {
    const list = JSON.parse(String(raw || '[]'));
    return Array.isArray(list) ? list : [];
  } catch (err) {
    return [];
  }
}

function meetingOut_(m) {
  const people = meetingPeople_(m.Recipients).map(function (r) {
    return { id: String(r.i || ''), name: String(r.n || ''), phone: String(r.p || ''), result: r.s === 'sent' ? 'sent' : 'skipped', at: String(r.t || '') };
  });
  return {
    id: String(m.ID),
    name: String(m.Name),
    createdAt: String(m.CreatedAt || ''),
    updatedAt: String(m.UpdatedAt || ''),
    message: String(m.Message || ''),
    recipients: people,
    sent: people.filter(function (r) { return r.result === 'sent'; }).length,
    skipped: people.filter(function (r) { return r.result !== 'sent'; }).length
  };
}

function handleMeetings_(p) {
  if (p.action === 'meetings') {
    const list = readTab_(MEETINGS_SHEET, MEETING_HEADERS).map(meetingOut_);
    list.sort(function (a, b) { return a.updatedAt < b.updatedAt ? 1 : -1; });
    return jsonOut_({ ok: true, meetings: list });
  }

  if (p.action === 'meetingMark') {
    const name = String(p.meeting || '').replace(/\s+/g, ' ').trim();
    if (!name || name.length > 80) return jsonOut_({ error: 'invalid meeting name' });
    const result = p.result === 'sent' ? 'sent' : p.result === 'skipped' ? 'skipped' : '';
    if (!result) return jsonOut_({ error: 'invalid result' });
    const pid = String(p.id || '').slice(0, 60);
    const pname = String(p.name || '').slice(0, 120);
    const pphone = String(p.phone || '').slice(0, 40);
    if (!pid && !meetingPhoneKey_(pphone)) return jsonOut_({ error: 'missing person' });
    const message = p.message === undefined ? undefined : String(p.message).slice(0, 1000);

    const lock = LockService.getScriptLock();
    lock.waitLock(15000);
    try {
      const sheet = getTab_(MEETINGS_SHEET, MEETING_HEADERS);
      const rows = readTab_(MEETINGS_SHEET, MEETING_HEADERS);
      const now = new Date().toISOString();
      let m = null;
      rows.forEach(function (r) { if (!m && String(r.Name).toLowerCase() === name.toLowerCase()) m = r; });
      if (!m) {
        if (rows.length >= MEETING_MAX_MEETINGS) return jsonOut_({ error: 'too many meetings' });
        m = { _row: 0, ID: Utilities.getUuid(), Name: name, CreatedAt: now, UpdatedAt: now, Message: message || '', Recipients: '[]' };
      }

      const people = meetingPeople_(m.Recipients);
      const key = meetingPhoneKey_(pphone);
      let person = null;
      people.forEach(function (r) {
        if (person) return;
        if ((pid && r.i === pid) || (key && meetingPhoneKey_(r.p) === key)) person = r;
      });
      if (person) {
        if (!(person.s === 'sent' && result === 'skipped')) { person.s = result; person.t = now; }
        person.n = pname || person.n;
        person.p = pphone || person.p;
        person.i = pid || person.i;
      } else {
        if (people.length >= MEETING_MAX_PEOPLE) return jsonOut_({ error: 'meeting is full' });
        people.push({ i: pid, n: pname, p: pphone, s: result, t: now });
      }

      m.Recipients = JSON.stringify(people);
      m.UpdatedAt = now;
      if (message !== undefined) m.Message = message;
      writeTabRow_(sheet, m._row, MEETING_HEADERS, MEETING_HEADERS.map(function (h) { return m[h]; }));
      return jsonOut_({ ok: true, meeting: meetingOut_(m) });
    } finally {
      lock.releaseLock();
    }
  }

  return jsonOut_({ error: 'unknown action' });
}

// ---------------------------------------------------------------------------
// Media office — Monthly Work Chart (media.html)
//
// One shared chart per month for the whole media team — no names, no roles.
// It lives in a "MediaWork" tab, one row per month. The cells for each work
// type are stored in the Data column as JSON, e.g. {"Reel":"2..1.c", ...}: one
// entry per day of the month separated by ".", each a whole number or a status
// letter — c = completed, p = in progress, r = under review, o = off / holiday.
// Total = the numbers plus 1 for every "c".
//
// Each month also has an "Uploaded" chart of the same shape: how many of each
// work type were uploaded (posted) on each day. Made minus uploaded = what is
// left to upload.
//
// Several people edit the same chart at once, so a save only sends the cells
// that person changed; they are merged into whatever is stored at that moment
// (one save at a time), so nobody's work overwrites anybody else's.
// ---------------------------------------------------------------------------
const MEDIA_SHEET_NAME = 'MediaWork';
const MEDIA_HEADERS = ['Month', 'Total', 'SubmittedAt', 'UpdatedAt', 'Note', 'Data', 'Uploaded', 'UploadedTotal'];
const MEDIA_VERSION = 4;

// Stock adjustments, one row per work type: what was already in stock before this
// page was used (Opening) and what will never be uploaded (NotNeeded).
//   Left to upload = Opening + Made - Uploaded - NotNeeded
const MEDIA_STOCK_SHEET = 'MediaStock';
const MEDIA_STOCK_HEADERS = ['Type', 'Opening', 'NotNeeded', 'UpdatedAt'];

// The content list: one row per piece of content, so you can see WHICH ones are left.
const MEDIA_ITEMS_SHEET = 'MediaItems';
const MEDIA_ITEM_HEADERS = ['ID', 'Title', 'Type', 'Link', 'Status', 'Platform', 'AddedAt', 'UpdatedAt'];
const ITEM_STATUSES = ['Ready', 'Uploaded', 'Not needed'];
const ITEM_PLATFORMS = ['YouTube', 'Instagram', 'Facebook', 'WhatsApp', 'Other'];

function jsonOut_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

function getMediaSheet_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(MEDIA_SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(MEDIA_SHEET_NAME);
    sheet.appendRow(MEDIA_HEADERS);
    sheet.setFrozenRows(1);
  } else if (sheet.getLastColumn() < MEDIA_HEADERS.length) {
    // an earlier version had fewer columns — add the new ones, keep every existing row
    const have = sheet.getLastColumn();
    sheet.getRange(1, have + 1, 1, MEDIA_HEADERS.length - have).setValues([MEDIA_HEADERS.slice(have)]);
  }
  return sheet;
}

// Refuses the sample key, so a forgotten "change me" can never open the chart.
function mediaAuthorized_(key) {
  return !!key && key === MEDIA_KEY && MEDIA_KEY !== 'media-2026-change-me';
}

function validMonth_(m) {
  return /^\d{4}-(0[1-9]|1[0-2])$/.test(m);
}

function cleanCat_(s) {
  return String(s || '').replace(/\s+/g, ' ').trim().slice(0, 40);
}

// one cell: a whole number (up to 3 digits), c / p / r / o, or empty
function cleanToken_(t) {
  t = String(t || '').toLowerCase();
  if (/^\d{1,3}$/.test(t)) return String(parseInt(t, 10));
  if (/^[cpro]$/.test(t)) return t;
  return '';
}

function splitCells_(s) {
  const a = String(s || '').split('.').slice(0, 31);
  while (a.length < 31) a.push('');
  return a;
}

function joinCells_(a) {
  return a.join('.').replace(/\.+$/, '');
}

function cellsTotal_(cells) {
  let t = 0;
  String(cells || '').split('.').forEach(function (tok) {
    if (/^\d+$/.test(tok)) t += parseInt(tok, 10);
    else if (tok === 'c') t += 1;
  });
  return t;
}

function chartTotal_(data) {
  let total = 0;
  Object.keys(data).forEach(function (cat) { total += cellsTotal_(data[cat]); });
  return total;
}

function parseChartData_(s) {
  try {
    const o = JSON.parse(String(s || '{}'));
    return (o && typeof o === 'object') ? o : {};
  } catch (err) {
    return {};
  }
}

function readMediaMonths_(sheet) {
  const last = sheet.getLastRow();
  if (last < 2) return [];
  return sheet.getRange(2, 1, last - 1, MEDIA_HEADERS.length).getValues().map(function (vals, i) {
    const o = { _row: i + 2 };
    MEDIA_HEADERS.forEach(function (h, c) { o[h] = vals[c]; });
    return o;
  });
}

function findMediaMonth_(rows, month) {
  for (let i = 0; i < rows.length; i++) {
    if (String(rows[i].Month) === month) return rows[i];
  }
  return null;
}

function writeMediaRow_(sheet, rowNum, vals) {
  const r = rowNum || sheet.getLastRow() + 1;
  sheet.getRange(r, 1, 1, MEDIA_HEADERS.length).setNumberFormat('@').setValues([vals]);
}

function chartOut_(r) {
  return {
    month: String(r.Month),
    total: Number(r.Total) || 0,
    submittedAt: String(r.SubmittedAt || ''),
    updatedAt: String(r.UpdatedAt || ''),
    note: String(r.Note || ''),
    data: parseChartData_(r.Data),
    uploaded: parseChartData_(r.Uploaded),
    uploadedTotal: Number(r.UploadedTotal) || 0
  };
}

function handleMedia_(p) {
  if (!mediaAuthorized_(p.key)) return jsonOut_({ error: 'unauthorized' });

  switch (p.action) {
    case 'mediaLogin':
      // version lets the page tell whether this script is up to date; now is
      // the real date, so a wrong computer clock can't open the wrong month
      return jsonOut_({ ok: true, version: MEDIA_VERSION, now: new Date().toISOString() });
    case 'mediaList':
      return jsonOut_({ ok: true, charts: readMediaMonths_(getMediaSheet_()).map(chartOut_), stock: readStock_() });
    case 'mediaStockSet':
      return mediaStockSet_(p);
    case 'mediaItems':
      return jsonOut_({ ok: true, items: readItems_().map(itemOut_) });
    case 'mediaItemSave':
      return mediaItemSave_(p);
    case 'mediaItemDelete':
      return mediaItemDelete_(p);
    case 'mediaGet':
      return mediaGet_(p);
    case 'mediaSave':
      return mediaSave_(p);
    default:
      return jsonOut_({ error: 'unknown action' });
  }
}

function mediaGet_(p) {
  const month = String(p.month || '');
  if (!validMonth_(month)) return jsonOut_({ error: 'invalid month' });
  const found = findMediaMonth_(readMediaMonths_(getMediaSheet_()), month);
  return jsonOut_({ ok: true, chart: found ? chartOut_(found) : null });
}

// Applies {"Reel": {"3": "2", "5": ""}, ...} (work type -> day -> new value, "" clears) to data.
function applyCellChanges_(data, changes) {
  Object.keys(changes).slice(0, 20).forEach(function (cat) {
    const name = cleanCat_(cat);
    if (!name || typeof changes[cat] !== 'object' || !changes[cat]) return;
    const cells = splitCells_(data[name]);
    Object.keys(changes[cat]).forEach(function (d) {
      const n = parseInt(d, 10);
      if (n >= 1 && n <= 31) cells[n - 1] = cleanToken_(changes[cat][d]);
    });
    const joined = joinCells_(cells);
    if (joined) data[name] = joined; else delete data[name];
  });
}

// p.changes   = changes to the "work done" chart, as above
// p.upChanges = changes to the "uploaded" chart, as above
// p.note    = replaces the notes (only sent when it was edited)
// p.submit  = "1" stamps the submission date, "0" clears it
function mediaSave_(p) {
  const month = String(p.month || '');
  if (!validMonth_(month)) return jsonOut_({ error: 'invalid month' });

  let changes = {}, upChanges = {};
  try {
    if (p.changes) changes = JSON.parse(p.changes) || {};
    if (p.upChanges) upChanges = JSON.parse(p.upChanges) || {};
  } catch (err) {
    return jsonOut_({ error: 'invalid data' });
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(15000);
  try {
    const sheet = getMediaSheet_();
    const existing = findMediaMonth_(readMediaMonths_(sheet), month);
    const data = existing ? parseChartData_(existing.Data) : {};
    const uploaded = existing ? parseChartData_(existing.Uploaded) : {};
    applyCellChanges_(data, changes);
    applyCellChanges_(uploaded, upChanges);

    const now = new Date().toISOString();
    let submittedAt = existing ? String(existing.SubmittedAt || '') : '';
    if (p.submit === '1') submittedAt = now;
    else if (p.submit === '0') submittedAt = '';
    const note = p.note !== undefined ? String(p.note).slice(0, 300) : (existing ? String(existing.Note || '') : '');

    const vals = [month, chartTotal_(data), submittedAt, now, note, JSON.stringify(data), JSON.stringify(uploaded), chartTotal_(uploaded)];
    writeMediaRow_(sheet, existing ? existing._row : 0, vals);

    const out = {};
    MEDIA_HEADERS.forEach(function (h, i) { out[h] = vals[i]; });
    return jsonOut_({ ok: true, chart: chartOut_(out) });
  } finally {
    lock.releaseLock();
  }
}

// ---- stock adjustments (opening stock / not needed) ----

function getTab_(name, headers) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.appendRow(headers);
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function readTab_(name, headers) {
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(name);
  if (!sheet || sheet.getLastRow() < 2) return [];
  return sheet.getRange(2, 1, sheet.getLastRow() - 1, headers.length).getValues().map(function (vals, i) {
    const o = { _row: i + 2 };
    headers.forEach(function (h, c) { o[h] = vals[c]; });
    return o;
  });
}

function writeTabRow_(sheet, rowNum, headers, vals) {
  const r = rowNum || sheet.getLastRow() + 1;
  sheet.getRange(r, 1, 1, headers.length).setNumberFormat('@').setValues([vals]);
}

function readStock_() {
  const out = {};
  readTab_(MEDIA_STOCK_SHEET, MEDIA_STOCK_HEADERS).forEach(function (r) {
    out[String(r.Type)] = { opening: Number(r.Opening) || 0, notNeeded: Number(r.NotNeeded) || 0 };
  });
  return out;
}

// p.type, p.field = "opening" | "notNeeded", p.value = whole number 0..99999
function mediaStockSet_(p) {
  const type = cleanCat_(p.type);
  const field = p.field === 'opening' ? 'Opening' : p.field === 'notNeeded' ? 'NotNeeded' : '';
  const value = /^\d{1,5}$/.test(String(p.value)) ? parseInt(p.value, 10) : -1;
  if (!type || !field || value < 0) return jsonOut_({ error: 'invalid stock value' });

  const lock = LockService.getScriptLock();
  lock.waitLock(15000);
  try {
    const sheet = getTab_(MEDIA_STOCK_SHEET, MEDIA_STOCK_HEADERS);
    const rows = readTab_(MEDIA_STOCK_SHEET, MEDIA_STOCK_HEADERS);
    let row = null;
    for (let i = 0; i < rows.length; i++) {
      if (String(rows[i].Type).toLowerCase() === type.toLowerCase()) { row = rows[i]; break; }
    }
    const rec = row || { _row: 0, Type: type, Opening: 0, NotNeeded: 0 };
    rec[field] = value;
    writeTabRow_(sheet, rec._row, MEDIA_STOCK_HEADERS, [rec.Type, rec.Opening, rec.NotNeeded, new Date().toISOString()]);
    return jsonOut_({ ok: true, stock: readStock_() });
  } finally {
    lock.releaseLock();
  }
}

// ---- the content list ----

function readItems_() {
  return readTab_(MEDIA_ITEMS_SHEET, MEDIA_ITEM_HEADERS);
}

function itemOut_(r) {
  return {
    id: String(r.ID),
    title: String(r.Title),
    type: String(r.Type),
    link: String(r.Link || ''),
    status: String(r.Status),
    platform: String(r.Platform || ''),
    addedAt: String(r.AddedAt || ''),
    updatedAt: String(r.UpdatedAt || '')
  };
}

// Create (no p.id) or update (p.id) one piece of content. Only the fields that are sent change.
function mediaItemSave_(p) {
  const lock = LockService.getScriptLock();
  lock.waitLock(15000);
  try {
    const sheet = getTab_(MEDIA_ITEMS_SHEET, MEDIA_ITEM_HEADERS);
    const rows = readItems_();
    let rec = null;
    if (p.id) {
      for (let i = 0; i < rows.length; i++) { if (String(rows[i].ID) === String(p.id)) { rec = rows[i]; break; } }
      if (!rec) return jsonOut_({ error: 'not found' });
    } else {
      if (rows.length >= 5000) return jsonOut_({ error: 'list is full' });
      rec = { _row: 0, ID: Utilities.getUuid(), Title: '', Type: 'Other Work', Link: '', Status: 'Ready', Platform: '', AddedAt: new Date().toISOString(), UpdatedAt: '' };
    }

    if (p.title !== undefined) rec.Title = String(p.title).replace(/\s+/g, ' ').trim().slice(0, 120);
    if (!rec.Title) return jsonOut_({ error: 'title required' });
    if (p.type !== undefined) rec.Type = cleanCat_(p.type) || 'Other Work';
    if (p.link !== undefined) rec.Link = String(p.link).trim().slice(0, 300);
    if (p.status !== undefined) {
      if (ITEM_STATUSES.indexOf(p.status) === -1) return jsonOut_({ error: 'invalid status' });
      rec.Status = p.status;
    }
    if (p.platform !== undefined) rec.Platform = ITEM_PLATFORMS.indexOf(p.platform) === -1 ? '' : p.platform;
    if (rec.Status !== 'Uploaded') rec.Platform = ''; // "uploaded to" only means something once it is uploaded
    rec.UpdatedAt = new Date().toISOString();

    writeTabRow_(sheet, rec._row, MEDIA_ITEM_HEADERS, MEDIA_ITEM_HEADERS.map(function (h) { return rec[h]; }));
    return jsonOut_({ ok: true, item: itemOut_(rec) });
  } finally {
    lock.releaseLock();
  }
}

function mediaItemDelete_(p) {
  const lock = LockService.getScriptLock();
  lock.waitLock(15000);
  try {
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(MEDIA_ITEMS_SHEET);
    if (!sheet || !p.id) return jsonOut_({ error: 'not found' });
    const rows = readItems_();
    for (let i = 0; i < rows.length; i++) {
      if (String(rows[i].ID) === String(p.id)) {
        sheet.deleteRow(rows[i]._row);
        return jsonOut_({ ok: true });
      }
    }
    return jsonOut_({ error: 'not found' });
  } finally {
    lock.releaseLock();
  }
}
