// The file editor inside Origin Studio: CodeMirror 6 in a WebView.
//
// Kotlin drives it through window.tf (open, save request, settings) and hears back through the
// `Android` JavaScript interface (ready, dirty state, save with the text, cursor position).
// Build: npm install && npm run build (writes app/src/main/assets/editor/editor.js).
import { EditorState, Compartment } from "@codemirror/state";
import {
  EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter,
  drawSelection, highlightSpecialChars, rectangularSelection, crosshairCursor, dropCursor,
} from "@codemirror/view";
import { defaultKeymap, history, historyKeymap, indentWithTab, undo, redo } from "@codemirror/commands";
import {
  indentOnInput, bracketMatching, foldGutter, foldKeymap, syntaxHighlighting,
  HighlightStyle, StreamLanguage, indentUnit,
} from "@codemirror/language";
import { searchKeymap, highlightSelectionMatches, openSearchPanel } from "@codemirror/search";
import { closeBrackets, closeBracketsKeymap, autocompletion, completionKeymap } from "@codemirror/autocomplete";
import { tags as t } from "@lezer/highlight";

import { javascript } from "@codemirror/lang-javascript";
import { python } from "@codemirror/lang-python";
import { json } from "@codemirror/lang-json";
import { markdown } from "@codemirror/lang-markdown";
import { html } from "@codemirror/lang-html";
import { css } from "@codemirror/lang-css";
import { rust } from "@codemirror/lang-rust";
import { cpp } from "@codemirror/lang-cpp";
import { java } from "@codemirror/lang-java";
import { go } from "@codemirror/lang-go";
import { php } from "@codemirror/lang-php";
import { sql } from "@codemirror/lang-sql";
import { xml } from "@codemirror/lang-xml";
import { yaml } from "@codemirror/lang-yaml";
import { shell } from "@codemirror/legacy-modes/mode/shell";
import { toml } from "@codemirror/legacy-modes/mode/toml";
import { kotlin, csharp, dart, scala } from "@codemirror/legacy-modes/mode/clike";
import { swift } from "@codemirror/legacy-modes/mode/swift";
import { ruby } from "@codemirror/legacy-modes/mode/ruby";
import { lua } from "@codemirror/legacy-modes/mode/lua";
import { dockerFile } from "@codemirror/legacy-modes/mode/dockerfile";
import { properties } from "@codemirror/legacy-modes/mode/properties";
import { diff } from "@codemirror/legacy-modes/mode/diff";
import { nginx } from "@codemirror/legacy-modes/mode/nginx";
import { cmake } from "@codemirror/legacy-modes/mode/cmake";

const host = window.Android || { ready() {}, dirty() {}, save() {}, cursor() {} };

// ---- Language by file name -------------------------------------------------------------------
const legacy = (mode) => StreamLanguage.define(mode);
const byExtension = {
  js: () => javascript(), mjs: () => javascript(), cjs: () => javascript(),
  jsx: () => javascript({ jsx: true }),
  ts: () => javascript({ typescript: true }), mts: () => javascript({ typescript: true }),
  cts: () => javascript({ typescript: true }),
  tsx: () => javascript({ jsx: true, typescript: true }),
  py: () => python(), pyi: () => python(),
  json: () => json(), jsonc: () => json(), json5: () => json(),
  md: () => markdown(), markdown: () => markdown(), mdx: () => markdown(),
  html: () => html(), htm: () => html(), vue: () => html(), svelte: () => html(),
  css: () => css(), scss: () => css(), less: () => css(),
  rs: () => rust(),
  c: () => cpp(), h: () => cpp(), cc: () => cpp(), cpp: () => cpp(), cxx: () => cpp(),
  hpp: () => cpp(), hh: () => cpp(), m: () => cpp(), mm: () => cpp(),
  java: () => java(),
  go: () => go(),
  php: () => php(),
  sql: () => sql(),
  xml: () => xml(), svg: () => xml(), plist: () => xml(), xaml: () => xml(), csproj: () => xml(),
  yml: () => yaml(), yaml: () => yaml(),
  sh: () => legacy(shell), bash: () => legacy(shell), zsh: () => legacy(shell), fish: () => legacy(shell),
  toml: () => legacy(toml),
  kt: () => legacy(kotlin), kts: () => legacy(kotlin), gradle: () => legacy(kotlin),
  cs: () => legacy(csharp), dart: () => legacy(dart), scala: () => legacy(scala),
  swift: () => legacy(swift), rb: () => legacy(ruby), lua: () => legacy(lua),
  properties: () => legacy(properties), ini: () => legacy(properties), cfg: () => legacy(properties),
  conf: () => legacy(properties), env: () => legacy(properties),
  diff: () => legacy(diff), patch: () => legacy(diff),
  cmake: () => legacy(cmake),
};
const byName = {
  dockerfile: () => legacy(dockerFile), containerfile: () => legacy(dockerFile),
  makefile: () => legacy(shell), ".bashrc": () => legacy(shell), ".profile": () => legacy(shell),
  ".zshrc": () => legacy(shell), ".gitignore": () => legacy(properties),
  ".gitconfig": () => legacy(properties), "cmakelists.txt": () => legacy(cmake),
  "nginx.conf": () => legacy(nginx),
};
function languageFor(name) {
  const lower = name.toLowerCase();
  if (byName[lower]) return byName[lower]();
  if (lower.startsWith(".env")) return legacy(properties);
  const dot = lower.lastIndexOf(".");
  const ext = dot >= 0 ? lower.slice(dot + 1) : "";
  return byExtension[ext] ? byExtension[ext]() : [];
}

// ---- Theme: the app's ink and steel, matched to the Compose palette --------------------------
const theme = EditorView.theme({
  "&": { color: "#EEF1FF", backgroundColor: "#05070E", height: "100%" },
  ".cm-scroller": { fontFamily: "'JetBrains Mono', ui-monospace, monospace", lineHeight: "1.55" },
  ".cm-content": { caretColor: "#9EC1E6", padding: "10px 0 40vh" },
  ".cm-cursor, .cm-dropCursor": { borderLeftColor: "#9EC1E6", borderLeftWidth: "2px" },
  "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection": {
    backgroundColor: "#9EC1E638",
  },
  ".cm-activeLine": { backgroundColor: "#FFFFFF08" },
  ".cm-gutters": { backgroundColor: "#05070E", color: "#5A6480", border: "none", paddingLeft: "4px" },
  ".cm-activeLineGutter": { backgroundColor: "transparent", color: "#C8CEDA" },
  ".cm-foldGutter .cm-gutterElement": { color: "#5A6480", padding: "0 4px" },
  ".cm-matchingBracket, &.cm-focused .cm-matchingBracket": {
    backgroundColor: "#9EC1E626", outline: "1px solid #9EC1E666",
  },
  ".cm-selectionMatch": { backgroundColor: "#FFFFFF14" },
  ".cm-searchMatch": { backgroundColor: "#E6BE8A40", outline: "1px solid #E6BE8A80" },
  ".cm-searchMatch.cm-searchMatch-selected": { backgroundColor: "#9EC1E666" },
  ".cm-panels": { backgroundColor: "#0E1322", color: "#EEF1FF", borderColor: "#232B45" },
  ".cm-panels.cm-panels-top": { borderBottom: "1px solid #232B45" },
  ".cm-panel.cm-search": { padding: "8px 10px", fontFamily: "system-ui, sans-serif" },
  ".cm-panel.cm-search input, .cm-panel.cm-search button": { fontSize: "14px" },
  ".cm-textfield": {
    backgroundColor: "#05070E", border: "1px solid #2A3450", borderRadius: "8px",
    color: "#EEF1FF", padding: "5px 8px",
  },
  ".cm-button": {
    backgroundImage: "none", backgroundColor: "#161D30", border: "1px solid #2A3450",
    borderRadius: "8px", color: "#EEF1FF", padding: "4px 10px",
  },
  ".cm-panel.cm-search label": { color: "#7B83A3" },
  ".cm-tooltip": { backgroundColor: "#0E1322", border: "1px solid #232B45", borderRadius: "10px" },
  ".cm-tooltip-autocomplete > ul > li[aria-selected]": { backgroundColor: "#9EC1E633", color: "#FFFFFF" },
  ".cm-foldPlaceholder": { backgroundColor: "#161D30", border: "none", color: "#7B83A3" },
}, { dark: true });

const highlight = HighlightStyle.define([
  { tag: [t.keyword, t.controlKeyword, t.moduleKeyword, t.operatorKeyword], color: "#9EC1E6" },
  { tag: [t.definitionKeyword, t.modifier], color: "#9EC1E6" },
  { tag: [t.string, t.special(t.string), t.regexp], color: "#9FD4A8" },
  { tag: [t.number, t.bool, t.null, t.atom], color: "#C2B2FF" },
  { tag: [t.comment, t.lineComment, t.blockComment, t.docComment], color: "#5A6480", fontStyle: "italic" },
  { tag: [t.function(t.variableName), t.function(t.propertyName)], color: "#A8D5FF" },
  { tag: [t.typeName, t.className, t.namespace], color: "#E6BE8A" },
  { tag: [t.propertyName], color: "#8FE0D8" },
  { tag: [t.tagName], color: "#9EC1E6" },
  { tag: [t.attributeName], color: "#E6BE8A" },
  { tag: [t.operator, t.punctuation, t.bracket], color: "#A7AECB" },
  { tag: [t.meta, t.annotation], color: "#7B83A3" },
  { tag: t.heading, color: "#9EC1E6", fontWeight: "bold" },
  { tag: t.emphasis, fontStyle: "italic" },
  { tag: t.strong, fontWeight: "bold" },
  { tag: t.link, color: "#A8D5FF", textDecoration: "underline" },
  { tag: t.inserted, color: "#9FD4A8" },
  { tag: t.deleted, color: "#FF8FAE" },
  { tag: t.invalid, color: "#FF8FAE" },
]);

// ---- The editor -------------------------------------------------------------------------------
const wrapping = new Compartment();
const fontSize = new Compartment();
let wrapOn = false;
let fontPx = 14;

let savedDoc = null;
let lastDirty = false;

function reportDirty(state) {
  const dirty = savedDoc !== null && !state.doc.eq(savedDoc);
  if (dirty !== lastDirty) {
    lastDirty = dirty;
    host.dirty(dirty);
  }
}

// The document as it was sent to be written; it becomes the clean state once Kotlin confirms.
let pendingDoc = null;

function save() {
  pendingDoc = view.state.doc;
  host.save(pendingDoc.toString());
  return true;
}

const sizeTheme = (px) => EditorView.theme({ ".cm-scroller": { fontSize: px + "px" } });

function extensionsFor(name, isReadOnly) {
  return [
    lineNumbers(), highlightActiveLineGutter(), highlightSpecialChars(), history(), foldGutter(),
    drawSelection(), dropCursor(), EditorState.allowMultipleSelections.of(true), indentOnInput(),
    syntaxHighlighting(highlight), bracketMatching(), closeBrackets(), autocompletion(),
    rectangularSelection(), crosshairCursor(), highlightActiveLine(), highlightSelectionMatches(),
    indentUnit.of("    "),
    keymap.of([
      { key: "Mod-s", run: save, preventDefault: true },
      ...closeBracketsKeymap, ...defaultKeymap, ...searchKeymap, ...historyKeymap,
      ...foldKeymap, ...completionKeymap, indentWithTab,
    ]),
    theme, languageFor(name),
    EditorState.readOnly.of(!!isReadOnly),
    wrapping.of(wrapOn ? EditorView.lineWrapping : []),
    fontSize.of(sizeTheme(fontPx)),
    EditorView.updateListener.of((u) => {
      if (u.docChanged) reportDirty(u.state);
      if (u.selectionSet || u.docChanged) {
        const head = u.state.selection.main.head;
        const line = u.state.doc.lineAt(head);
        host.cursor(line.number, head - line.from + 1);
      }
    }),
  ];
}

const view = new EditorView({
  parent: document.getElementById("editor"),
  state: EditorState.create({ doc: "", extensions: extensionsFor("", true) }),
});

function decode(b64) {
  const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
  return new TextDecoder("utf-8").decode(bytes);
}

window.tf = {
  /** Shows a file. The text arrives base64-encoded UTF-8 so any character survives the bridge. */
  open(b64, name, isReadOnly) {
    const state = EditorState.create({ doc: decode(b64), extensions: extensionsFor(name, isReadOnly) });
    view.setState(state);
    savedDoc = state.doc;
    lastDirty = false;
    host.cursor(1, 1);
  },
  /** The text was written to disk; it is the new clean state. */
  markSaved() {
    savedDoc = pendingDoc || view.state.doc;
    pendingDoc = null;
    reportDirty(view.state);
  },
  /** The file changed on disk (an agent edited it): take the new text, keeping the cursor. */
  replace(b64) {
    const text = decode(b64);
    const head = Math.min(view.state.selection.main.head, text.length);
    const top = view.scrollDOM.scrollTop;
    view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: text }, selection: { anchor: head } });
    view.scrollDOM.scrollTop = top;
    savedDoc = view.state.doc;
    reportDirty(view.state);
  },
  requestSave() { save(); },
  setWrap(on) {
    wrapOn = !!on;
    view.dispatch({ effects: wrapping.reconfigure(wrapOn ? EditorView.lineWrapping : []) });
  },
  setFontSize(px) {
    fontPx = px;
    view.dispatch({ effects: fontSize.reconfigure(sizeTheme(px)) });
  },
  search() { view.focus(); openSearchPanel(view); },
  undo() { undo(view); },
  redo() { redo(view); },
  focus() { view.focus(); },
};

host.ready();
