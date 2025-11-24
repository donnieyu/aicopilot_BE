package com.example.aicopilot.event;

import com.example.aicopilot.dto.process.ProcessResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 프로세스 생성 완료 시 발행되는 이벤트.
 * 이 이벤트가 발행되면 비동기 리스너들이 데이터 모델링 및 폼 디자인 작업을 시작합니다.
 */
@Getter
public class ProcessGeneratedEvent extends ApplicationEvent {
    private final String jobId;
    private final String userRequest;
    private final ProcessResponse processResponse;

    public ProcessGeneratedEvent(Object source, String jobId, String userRequest, ProcessResponse processResponse) {
        super(source);
        this.jobId = jobId;
        this.userRequest = userRequest;
        this.processResponse = processResponse;
    }
}