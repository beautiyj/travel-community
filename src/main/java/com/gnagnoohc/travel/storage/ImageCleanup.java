package com.gnagnoohc.travel.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * 고아 파일 정리. 저장소 파일 삭제는 트랜잭션 롤백으로 되돌릴 수 없으므로 트랜잭션이 끝난 뒤에 실행한다.
 *
 * 사진을 바꾸는 화면은 두 방향 모두 정리가 필요하다.
 * 커밋되면 화면에서 빠진 예전 파일을, 롤백되면 방금 올린 파일을 지워야 어느 쪽이든 파일만 남지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanup {

    private final ImageStorage imageStorage;

    /** 롤백되면 지운다. 방금 올린 파일용 — 커밋되면 그 파일들이 정상 저장분이므로 그대로 둔다. */
    public void deleteOnRollback(Collection<String> urls) {
        // 트랜잭션이 없으면 롤백도 없다 -> 지울 일 자체가 없다
        registerIfTransactional(urls, false);
    }

    /** 커밋되면 지운다. 화면에서 빠진 예전 파일용. */
    public void deleteOnCommit(Collection<String> urls) {
        if (!registerIfTransactional(urls, true)) {
            // 트랜잭션 밖에서 불렸다면 이미 확정된 변경이므로 바로 지운다
            urls.forEach(this::deleteQuietly);
        }
    }

    /** 동기화를 등록했으면 true. 트랜잭션이 없어 등록하지 못했으면 false. */
    private boolean registerIfTransactional(Collection<String> urls, boolean onCommit) {
        if (urls.isEmpty()) {
            return true;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        List<String> targets = List.copyOf(urls);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == onCommit) {
                    targets.forEach(ImageCleanup.this::deleteQuietly);
                }
            }
        });
        return true;
    }

    // 정리에 실패해도 이미 끝난 등록/수정을 실패로 되돌릴 수는 없으므로 로그만 남긴다
    private void deleteQuietly(String url) {
        try {
            imageStorage.delete(url);
        } catch (RuntimeException e) {
            log.warn("이미지 정리 실패: {}", url, e);
        }
    }
}
