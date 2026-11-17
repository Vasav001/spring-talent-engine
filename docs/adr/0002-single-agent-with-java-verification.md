# ADR 0002: One AI agent, verified in Java

**Status:** accepted

## Context
Recruiters need scores they can trust and explain. LLMs can invent experience that is not in the resume, free-tier APIs are rate-limited, and the developer is a Spring developer rather than an AI specialist.

## Decision
Screening is a pipeline in which only one step uses the LLM:
1. **Keyword gate (Java):** rubric skills plus synonyms, matched on word boundaries. A missing MUST skill ends screening with `LOW_MATCH`, no LLM call and no credit used.
2. **One agent call (Spring AI `ChatClient`):** structured output into a score card with a score and a word-for-word quote per rubric skill. A `getGithubSummary` tool is offered only when the resume links a GitHub profile; its output may shape strengths and gaps but is never accepted as evidence.
3. **Verification (Java):** every quote with a score above 0 must appear in the resume (normalized for case, quotes, bullets and whitespace; `...` splits a quote). If any quote is missing, the agent gets one retry with feedback naming those skills; anything still unsupported scores 0 and is marked `verified: false`.
4. **Scoring (Java):** `final = 0.3 × keyword score + 0.7 × AI score`.

## Alternatives considered
- **Multi-agent setups** (extractor, scorer, critic): more calls, more cost and more failure modes, and harder to test.
- **Trusting the model's scores:** no protection against invented evidence.
- **Embeddings or vector search:** not needed to compare one resume with one rubric.

## Consequences
- The deterministic parts (gate, verifier, scoring) are unit-tested without a model; the agent is tested against a scripted `ChatModel`; an eval measures ranking agreement with a human ranking (1.00 on the first set).
- At most two LLM calls per resume, usually one.
- Offering the tool only when it is usable also avoided a real failure on AWS: Groq's `gpt-oss-120b` sometimes called a non-existent `json` tool when tools and JSON output were combined.
