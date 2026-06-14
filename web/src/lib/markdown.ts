import MarkdownIt from 'markdown-it'
import { createHighlighterCore, type HighlighterCore } from 'shiki/core'
import { createOnigurumaEngine } from 'shiki/engine/oniguruma'

let mdInstance: MarkdownIt | null = null
let initPromise: Promise<MarkdownIt> | null = null
let highlighterRef: HighlighterCore | null = null

const REGISTERED_LANGS = new Set([
  'bash',
  'css',
  'diff',
  'go',
  'html',
  'http',
  'java',
  'javascript',
  'json',
  'jsx',
  'kotlin',
  'markdown',
  'python',
  'rust',
  'scss',
  'shellscript',
  'sql',
  'toml',
  'tsx',
  'typescript',
  'vue',
  'xml',
  'yaml',
])

const LANG_ALIASES: Record<string, string> = {
  js: 'javascript',
  ts: 'typescript',
  sh: 'bash',
  shell: 'bash',
  py: 'python',
  md: 'markdown',
  yml: 'yaml',
}

function escapeHtml(s: string) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function resolveLang(input: string): string | null {
  const lang = input.trim().toLowerCase()
  if (!lang) return null
  if (REGISTERED_LANGS.has(lang)) return lang
  if (lang in LANG_ALIASES) return LANG_ALIASES[lang]
  return null
}

async function buildHighlighter(): Promise<HighlighterCore> {
  return await createHighlighterCore({
    themes: [
      import('shiki/themes/github-light.mjs'),
      import('shiki/themes/github-dark.mjs'),
    ],
    langs: [
      import('shiki/langs/bash.mjs'),
      import('shiki/langs/css.mjs'),
      import('shiki/langs/diff.mjs'),
      import('shiki/langs/go.mjs'),
      import('shiki/langs/html.mjs'),
      import('shiki/langs/http.mjs'),
      import('shiki/langs/java.mjs'),
      import('shiki/langs/javascript.mjs'),
      import('shiki/langs/json.mjs'),
      import('shiki/langs/jsx.mjs'),
      import('shiki/langs/kotlin.mjs'),
      import('shiki/langs/markdown.mjs'),
      import('shiki/langs/python.mjs'),
      import('shiki/langs/rust.mjs'),
      import('shiki/langs/scss.mjs'),
      import('shiki/langs/shellscript.mjs'),
      import('shiki/langs/sql.mjs'),
      import('shiki/langs/toml.mjs'),
      import('shiki/langs/tsx.mjs'),
      import('shiki/langs/typescript.mjs'),
      import('shiki/langs/vue.mjs'),
      import('shiki/langs/xml.mjs'),
      import('shiki/langs/yaml.mjs'),
    ],
    engine: createOnigurumaEngine(import('shiki/wasm')),
  })
}

async function build(): Promise<MarkdownIt> {
  highlighterRef = await buildHighlighter()

  const md = new MarkdownIt({
    html: false,
    linkify: true,
    breaks: false,
  })

  // Custom fence renderer: shiki when the lang is registered, plain otherwise.
  md.renderer.rules.fence = (tokens, idx) => {
    const token = tokens[idx]
    const requested = (token.info || '').trim().split(/\s+/)[0] || ''
    const lang = resolveLang(requested)
    const code = token.content
    const safeLangAttr = (requested || 'text').replace(/[^a-zA-Z0-9_-]/g, '') || 'text'
    const raw = encodeURIComponent(code)

    let inner: string
    if (lang && highlighterRef) {
      inner = highlighterRef.codeToHtml(code, {
        lang,
        themes: { light: 'github-light', dark: 'github-dark' },
        defaultColor: false,
      })
    } else {
      inner = `<pre><code>${escapeHtml(code)}</code></pre>`
    }

    return `<div class="code-block-wrapper" data-lang="${safeLangAttr}" data-raw="${raw}">${inner}</div>`
  }

  return md
}

export function getMarkdown(): Promise<MarkdownIt> {
  if (mdInstance) return Promise.resolve(mdInstance)
  if (!initPromise) {
    initPromise = build().then((m) => {
      mdInstance = m
      return m
    })
  }
  return initPromise
}

export async function renderMarkdown(text: string): Promise<string> {
  const md = await getMarkdown()
  return md.render(text)
}

export function renderMarkdownSync(text: string): string {
  if (!mdInstance) {
    const escaped = escapeHtml(text)
    return `<p>${escaped.replace(/\n\n+/g, '</p><p>').replace(/\n/g, '<br>')}</p>`
  }
  return mdInstance.render(text)
}
