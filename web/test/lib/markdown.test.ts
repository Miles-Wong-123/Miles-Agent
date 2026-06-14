import { describe, expect, it } from 'vitest'
import { renderMarkdown, renderMarkdownSync } from '@/lib/markdown'

describe('renderMarkdownSync (fallback)', () => {
  it('escapes HTML and wraps paragraphs', () => {
    const html = renderMarkdownSync('hello\n\nworld')
    expect(html).toContain('<p>hello</p><p>world</p>')
  })

  it('escapes special characters', () => {
    const html = renderMarkdownSync('<script>x</script>')
    expect(html).not.toContain('<script>')
    expect(html).toContain('&lt;script&gt;')
  })
})

describe('renderMarkdown (async, with shiki)', () => {
  it('renders plain paragraphs', async () => {
    const html = await renderMarkdown('hello world')
    expect(html).toContain('<p>hello world</p>')
  })

  it('renders java code blocks via shiki', async () => {
    const html = await renderMarkdown('```java\nint x = 1;\n```')
    expect(html).toContain('code-block-wrapper')
    expect(html).toContain('data-lang="java"')
    expect(html).toContain('shiki')
  })

  it('renders typescript code blocks via shiki', async () => {
    const html = await renderMarkdown('```ts\nconst a: number = 1\n```')
    expect(html).toContain('data-lang="ts"')
    expect(html).toContain('shiki')
  })

  it('falls back to escaped plain pre/code for unknown languages', async () => {
    const html = await renderMarkdown('```cobol\nMOVE 1 TO X.\n```')
    expect(html).toContain('code-block-wrapper')
    expect(html).toContain('data-lang="cobol"')
    expect(html).not.toContain('shiki')
  })

  it('renders tables', async () => {
    const md = '| h1 | h2 |\n| --- | --- |\n| a | b |'
    const html = await renderMarkdown(md)
    expect(html).toContain('<table>')
    expect(html).toContain('<th>h1</th>')
    expect(html).toContain('<td>a</td>')
  })

  it('renders unordered lists', async () => {
    const html = await renderMarkdown('- one\n- two')
    expect(html).toContain('<ul>')
    expect(html).toMatch(/<li>one<\/li>/)
    expect(html).toMatch(/<li>two<\/li>/)
  })
})
