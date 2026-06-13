package com.hojun.interviewnote.interviewnoteapi.service

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * SSE Emitter 관리 서비스
 *
 * Phase 2 리팩토링: MockInterviewService에서 분리
 * - SSE 연결 관리 (등록, 제거)
 * - 메시지 브로드캐스트
 * - 메트릭 수집
 */
@Service
class SseEmitterService(
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    /**
     * SSE Emitter 등록
     *
     * 클라이언트가 /stream 엔드포인트에 연결하면 호출됨
     *
     * @param id 식별자 (예: interviewId)
     * @param emitter SSE Emitter 인스턴스
     */
    fun register(id: Long, emitter: SseEmitter) {
        emitters[id] = emitter
        logger.info("SSE emitter 등록 - id: $id")
        updateConnectionGauge()
    }

    /**
     * SSE Emitter 제거
     *
     * 연결 종료, 타임아웃, 에러 발생 시 호출됨
     *
     * @param id 식별자
     */
    fun remove(id: Long) {
        emitters.remove(id)
        logger.info("SSE emitter 제거 - id: $id")
        updateConnectionGauge()
    }

    /**
     * SSE 메시지 브로드캐스트
     *
     * 특정 ID의 클라이언트에게 SSE 메시지 전송
     *
     * @param id 식별자
     * @param event 이벤트 이름
     * @param data 전송할 데이터
     * @return 전송 성공 여부
     */
    fun broadcast(id: Long, event: String, data: Any): Boolean {
        val emitter = emitters[id]
        if (emitter == null) {
            logger.warn("SSE emitter가 없음 - id: $id")
            return false
        }

        return try {
            emitter.send(
                SseEmitter.event()
                    .name(event)
                    .data(data)
            )
            logger.debug("SSE 메시지 전송 성공 - id: $id, event: $event")
            meterRegistry.counter("sse.messages_sent", "event", event).increment()
            true
        } catch (e: IOException) {
            logger.error("SSE 전송 실패 (IOException) - id: $id", e)
            remove(id)
            meterRegistry.counter("sse.errors", "type", "io_error").increment()
            false
        } catch (e: Exception) {
            logger.error("SSE 전송 중 예외 발생 - id: $id", e)
            meterRegistry.counter("sse.errors", "type", "unknown").increment()
            false
        }
    }

    /**
     * Emitter 존재 여부 확인
     *
     * @param id 식별자
     * @return Emitter가 등록되어 있으면 true
     */
    fun hasEmitter(id: Long): Boolean {
        return emitters.containsKey(id)
    }

    /**
     * Emitter 조회
     *
     * @param id 식별자
     * @return Emitter 인스턴스 또는 null
     */
    fun getEmitter(id: Long): SseEmitter? {
        return emitters[id]
    }

    /**
     * 활성 연결 수
     *
     * @return 현재 등록된 Emitter 개수
     */
    fun getActiveConnectionCount(): Int {
        return emitters.size
    }

    /**
     * 메트릭 게이지 업데이트
     */
    private fun updateConnectionGauge() {
        meterRegistry.gauge("sse.active_connections", emitters.size)
    }
}
