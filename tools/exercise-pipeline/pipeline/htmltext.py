"""Turns wger's HTML exercise descriptions into plain instruction steps."""

import html
import re
from html.parser import HTMLParser

_BLOCK_TAGS = {"p", "li", "br", "div", "ol", "ul", "h1", "h2", "h3", "h4", "tr"}


class _StepExtractor(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.blocks: list[str] = []
        self._current: list[str] = []

    def _flush(self):
        text = " ".join("".join(self._current).split())
        if text:
            self.blocks.append(text)
        self._current = []

    def handle_starttag(self, tag, attrs):
        if tag in _BLOCK_TAGS:
            self._flush()

    def handle_endtag(self, tag):
        if tag in _BLOCK_TAGS:
            self._flush()

    def handle_data(self, data):
        self._current.append(data)

    def close(self):
        super().close()
        self._flush()


def html_to_steps(markup: str | None) -> list[str]:
    if not markup or not markup.strip():
        return []
    parser = _StepExtractor()
    parser.feed(markup)
    parser.close()
    steps = []
    for block in parser.blocks:
        block = html.unescape(block)
        block = re.sub(r"^\s*(\d+[.)]|[-*•])\s*", "", block).strip()
        if block:
            steps.append(block)
    return steps
