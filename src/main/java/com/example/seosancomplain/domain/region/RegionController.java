package com.example.seosancomplain.domain.region;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    // 서산시 읍/면/동 목록
    @GetMapping("/seosan")
    public ResponseEntity<List<String>> getSeosanRegions() {
        return ResponseEntity.ok(SeosanRegion.names());
    }
}