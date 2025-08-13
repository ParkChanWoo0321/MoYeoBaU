package com.example.seosancomplain.domain.complaint;

public enum RejectionReason {
    LAW_OR_POLICY, // 관련 법령 및 규정 불일치
    DUPLICATE, // 기존 민원과 동일/유사
    LACK_OF_INFO, // 정보 부족 또는 내용 불분명
    OUT_OF_JURISDICTION, // 관할 행정기관 외의 사안
    PRIVATE_DISPUTE, // 사적 분쟁 또는 개인적 이해관계
    INAPPROPRIATE_CONTENT, // 욕설,비방,허위사실 유포 등 부적절한 내용
    ALREADY_RESOLVED, // 이미 처리 완료된 사안
    OTHER // 기타
}