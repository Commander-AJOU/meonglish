package com.ajikmeoreosseo.app.ai

import com.ajikmeoreosseo.app.BuildConfig
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.RateLimitException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.messages.BetaJsonOutputFormat
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams
import java.time.Duration

/**
 * 앱이 쓰는 생성형 AI는 모두 Claude입니다(공식 Anthropic Java SDK): 매일의 이야기 쓰기와 레벨 테스트의 출제.
 * 키와 모델은 secrets.properties의 ANTHROPIC_API_KEY, ANTHROPIC_MODEL에서 옵니다.
 */
object ClaudeApi {

    /**
     * 기본 모델. 이야기 한 편과 레벨 테스트 한 번이 모두 1원도 들지 않으면서(입력 100만 토큰에 0.1달러, 출력 0.5달러)
     * 우리말 동화의 문장과 조사가 깨지지 않아 기본으로 둡니다. ANTHROPIC_MODEL로 바꿀 수 있습니다
     * (예: claude-sonnet-5-5, claude-opus-5-5).
     */
    const val DEFAULT_MODEL = "claude-haiku-5-5"

    val key: String = BuildConfig.ANTHROPIC_API_KEY.trim()
    val model: String = BuildConfig.ANTHROPIC_MODEL.trim().ifEmpty { DEFAULT_MODEL }
    val hasKey: Boolean get() = key.isNotEmpty()

    /** 얼마나 깊이 생각하고 쓸지. 아이가 기다리는 일은 [LOW]. */
    enum class Effort { LOW, MEDIUM }

    /** 안전 분류기가 요청을 거절했습니다. */
    class Refused : RuntimeException("refusal")

    /** 응답이 정해 둔 길이를 넘겨 잘렸습니다. */
    class TooLong : RuntimeException("too long")

    fun client(timeoutSeconds: Long): AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(key)
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .maxRetries(1)
        .build()

    /**
     * [system]과 [user]를 보내고, [schema](JSON 스키마)에 맞는 JSON 글을 받습니다. 호출한 스레드에서 기다립니다.
     * 거절되면 [Refused], 잘리면 [TooLong], 그 밖의 실패는 SDK의 예외([AnthropicServiceException] 등)를 그대로 던집니다.
     */
    fun json(client: AnthropicClient, system: String, user: String, schema: Map<String, Any>, effort: Effort): String {
        val format = BetaJsonOutputFormat.Schema.builder().apply {
            schema.forEach { (name, value) -> putAdditionalProperty(name, JsonValue.from(value)) }
        }.build()
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(MAX_TOKENS)
            .system(system)
            .addUserMessage(user)
            .outputConfig(
                BetaOutputConfig.builder()
                    .effort(if (effort == Effort.LOW) BetaOutputConfig.Effort.LOW else BetaOutputConfig.Effort.MEDIUM)
                    .format(BetaJsonOutputFormat.builder().schema(format).build())
                    .build()
            )
            .apply {
                // Haiku를 뺀 모델은 안전 분류기가 요청을 거절하면 서버가 다른 모델로 한 번 더 시도하게 합니다(Haiku는 이 기능이 없습니다).
                if (!model.contains("haiku")) {
                    addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
                    fallbacksDefault()
                }
            }
            .build()
        val message = client.beta().messages().create(params)
        when (message.stopReason().orElse(null)) {
            BetaStopReason.REFUSAL -> throw Refused()
            BetaStopReason.MAX_TOKENS -> throw TooLong()
            else -> Unit
        }
        // 응답은 생각(thinking) 블록으로 시작할 수 있으므로, 글 블록을 종류로 찾습니다.
        return message.content().firstNotNullOfOrNull { block -> block.text().orElse(null)?.text() } ?: throw TooLong()
    }

    /** 실패 이유(짧은 우리말). 화면과 기록에 씁니다. */
    fun reason(error: Throwable): String = when (error) {
        is Refused -> "만들지 못했어요(거절)"
        is TooLong -> "응답이 너무 길어요"
        is RateLimitException -> "요금 한도 또는 크레딧 부족"
        is AnthropicServiceException -> when (error.statusCode()) {
            401, 403 -> "API 키 오류"
            402 -> "요금 한도 또는 크레딧 부족"
            404 -> "모델 이름 오류"
            else -> "서버 오류(${error.statusCode()})"
        }
        else -> "통신 오류"
    }

    private const val MAX_TOKENS = 16_000L
}
