import json
import logging
import threading
from kafka import KafkaConsumer
from app.config.settings import settings

logger = logging.getLogger(__name__)


class ResourceProvidedConsumer:
    """
    Kafka Consumer: resource.provided 이벤트 수신 (담당: 백엔드 C가 발행, 명세서 9.1)
    - 연관 리소스 캐시 갱신 트리거 용도 (Sprint 1은 로그 처리로 대체, 선택 사항)
    """

    def __init__(self):
        self.topic = settings.kafka_topic_resource_provided
        self.consumer = None
        self._running = False

    def start(self):
        """별도 스레드로 Kafka Consumer 시작"""
        self._running = True
        thread = threading.Thread(target=self._consume, daemon=True)
        thread.start()
        logger.info(f"[KafkaConsumer] 시작 - topic: {self.topic}")

    def stop(self):
        self._running = False
        if self.consumer:
            self.consumer.close()

    def _consume(self):
        try:
            self.consumer = KafkaConsumer(
                self.topic,
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.kafka_consumer_group_id,
                auto_offset_reset="earliest",
                enable_auto_commit=True,
                value_deserializer=lambda m: json.loads(m.decode("utf-8")),
                consumer_timeout_ms=1000,
            )

            while self._running:
                for message in self.consumer:
                    if not self._running:
                        break
                    self._handle_message(message.value)

        except Exception as e:
            logger.error(f"[KafkaConsumer] 오류 발생: {e}")
        finally:
            if self.consumer:
                self.consumer.close()

    def _handle_message(self, event: dict):
        """
        resource.provided 이벤트 처리
        - eventId, enrollmentId, userId, resourceId 추출
        - 연관 리소스 캐시 갱신 트리거 (Sprint 1: 로그로 대체)
        """
        try:
            event_id = event.get("eventId")
            enrollment_id = event.get("enrollmentId")
            user_id = event.get("userId")
            resource_id = event.get("resourceId")

            logger.info(
                f"[KafkaConsumer] resource.provided 수신 - eventId: {event_id}, "
                f"enrollmentId: {enrollment_id}, userId: {user_id}, resourceId: {resource_id}"
            )

            # 실습 포인트: 여기서 사용자별 연관 리소스 캐시 무효화 등을 추가할 수 있다.

        except Exception as e:
            logger.error(f"[KafkaConsumer] 메시지 처리 실패: {e}, event: {event}")


resource_provided_consumer = ResourceProvidedConsumer()
