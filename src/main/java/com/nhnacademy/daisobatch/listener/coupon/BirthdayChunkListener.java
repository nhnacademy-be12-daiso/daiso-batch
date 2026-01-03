package com.nhnacademy.daisobatch.listener.coupon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BirthdayChunkListener implements ChunkListener {

    @Override
    public void beforeChunk(ChunkContext context) {
        log.info("[BirthdayChunkListener] 청크 시작 - {}", context.getStepContext().getStepName());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        // 청크 단위 커밋까지 정상 완료된 상태
        var stepExecution = context.getStepContext().getStepExecution();

        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long skipCount = stepExecution.getSkipCount();
        long commitCount = stepExecution.getCommitCount();

        log.info("[BirthdayChunkListener] 청크 커밋 완료 - step={}, read={}, write={}, skip={}, commit={}",
                context.getStepContext().getStepName(),
                readCount, writeCount, skipCount, commitCount);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        log.error("[BirthdayChunkListener] 청크 롤백 발생 - step={} (해당 범위 데이터 재시도 혹은 확인 필요)",
                context.getStepContext().getStepName());
    }
}
