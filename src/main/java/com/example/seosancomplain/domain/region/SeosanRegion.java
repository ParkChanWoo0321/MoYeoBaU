package com.example.seosancomplain.domain.region;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum SeosanRegion {
    HAEMI_MYEON("해미면"),
    GOBOOK_MYEON("고북면"),
    INJI_MYEON("인지면"),
    PALBONG_MYEON("팔봉면"),
    BUSEOK_MYEON("부석면"),
    JIGOK_MYEON("지곡면"),
    UNSAN_MYEON("운산면"),
    SEONGYEON_MYEON("성연면"),
    EUMAM_MYEON("음암면"),
    BUCHUN_DONG("부춘동"),
    DONGMUN_DONG("동문동"),
    SUSEOK_DONG("수석동"),
    SEOKNAM_DONG("석남동");

    private final String name; // 화면에 보여줄 한글명

    SeosanRegion(String name) { this.name = name; }

    public static List<String> names() {
        return Arrays.stream(values()).map(SeosanRegion::getName).collect(Collectors.toList());
    }

    public static boolean isValid(String input) {
        if (input == null) return false;
        return names().contains(input);
    }
}
