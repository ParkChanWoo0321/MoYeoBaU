package com.example.seosancomplain.domain.Report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportLinkService {

    private static final String MONTH_PAGE_URL = "https://bigdata.epeople.go.kr/bigdata/pot/rptst/forwardBigdataAnalsRptstMonth.npaid";
    private static final String WEEK_PAGE_URL  = "https://bigdata.epeople.go.kr/bigdata/pot/rptst/forwardBigdataAnalsRptstWeek.npaid";
    private static final String LIST_API_URL   = "https://bigdata.epeople.go.kr/bigdata/pot/rptst/selectBigdataAnalsRptstList.npaid";
    private static final String PDF_API_URL    = "https://bigdata.epeople.go.kr/bigdata/pot/rptst/getPDFReport.npaid";

    private static final String KND_MONTH = "B0020002";
    private static final String KND_WEEK  = "B0020001";

    private final ObjectMapper om = new ObjectMapper();

    private static class CsrfCtx {
        Map<String, String> cookies;
        String headerName;
        String token;
    }

    private Connection base(String url) {
        return Jsoup.connect(url)
                .timeout(10000)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127 Safari/537.36")
                .referrer("https://bigdata.epeople.go.kr/")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
    }

    private CsrfCtx fetchCsrf(String pageUrl) throws IOException {
        Connection.Response res = base(pageUrl)
                .method(Connection.Method.GET)
                .execute();
        Document doc = res.parse();
        String headerName = Optional.ofNullable(doc.selectFirst("meta[name=_csrf_header]"))
                .map(m -> m.attr("content")).orElse("X-CSRF-TOKEN");
        String token = Optional.ofNullable(doc.selectFirst("meta[name=_csrf]"))
                .map(m -> m.attr("content")).orElse(null);
        CsrfCtx ctx = new CsrfCtx();
        ctx.cookies = res.cookies();
        ctx.headerName = headerName;
        ctx.token = token;
        return ctx;
    }

    private JsonNode callListApi(CsrfCtx ctx, String kindCode, int pageNo, int pageUnit, int pageSize) throws IOException {
        Connection conn = base(LIST_API_URL)
                .method(Connection.Method.POST)
                .cookies(ctx.cookies)
                .data("pageNo", String.valueOf(pageNo))
                .data("pageUnit", String.valueOf(pageUnit))
                .data("pageSize", String.valueOf(pageSize))
                .data("searchRptstKndCd", kindCode);
        if (ctx.token != null) conn.header(ctx.headerName, ctx.token);
        String body = conn.execute().body();
        return om.readTree(body);
    }

    private String buildPdfUrl(String grpId, String fileId) {
        return PDF_API_URL + "?attachFileGrpId=" + grpId + "&attachFileId=" + fileId;
    }

    private Optional<String> pickPdfUrlFromItem(JsonNode item) {
        String grp = opt(item, "rptstAtchFileGrpId");
        String exts = opt(item, "atchFileExt");
        String ids  = opt(item, "atchFileId");
        if (grp == null || exts == null || ids == null) return Optional.empty();
        String[] extArr = exts.split("\\|");
        String[] idArr  = ids.split("\\|");
        for (int i = 0; i < Math.min(extArr.length, idArr.length); i++) {
            if ("pdf".equalsIgnoreCase(extArr[i])) {
                return Optional.of(buildPdfUrl(grp, idArr[i]));
            }
        }
        return Optional.empty();
    }

    private String opt(JsonNode n, String field) {
        return n.hasNonNull(field) ? n.get(field).asText() : null;
    }

    public String getMonthlyPdfUrl(YearMonth ym) {
        try {
            CsrfCtx ctx = fetchCsrf(MONTH_PAGE_URL);
            JsonNode root = callListApi(ctx, KND_MONTH, 1, 10, 12);
            JsonNode list = root.path("resultList");
            if (!list.isArray() || list.size() == 0)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "월간 목록이 비어있습니다.");
            for (JsonNode item : list) {
                String name = opt(item, "rptstNm");
                String reg  = opt(item, "frstRegDt");
                String ymDash = ym.toString();
                String ymKor  = ym.getYear() + "년 " + String.format("%02d", ym.getMonthValue()) + "월";
                if ((name != null && (name.contains(ymDash) || name.contains(ymKor)))
                        || (reg != null && reg.startsWith(ym.getYear() + "-" + String.format("%02d", ym.getMonthValue())))) {
                    return pickPdfUrlFromItem(item)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "월간 PDF 파일ID를 찾지 못했습니다."));
                }
            }
            return pickPdfUrlFromItem(list.get(0))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "월간 PDF 링크를 찾지 못했습니다."));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "월간 목록 조회 실패", e);
        }
    }

    public String getWeeklyPdfUrl(LocalDate day) {
        try {
            CsrfCtx ctx = fetchCsrf(WEEK_PAGE_URL);
            JsonNode root = callListApi(ctx, KND_WEEK, 1, 10, 20);
            JsonNode list = root.path("resultList");
            if (!list.isArray() || list.size() == 0)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주간 목록이 비어있습니다.");
            JsonNode best = null;
            long bestDiff = Long.MAX_VALUE;
            for (JsonNode item : list) {
                String reg = opt(item, "frstRegDt");
                if (reg == null || reg.length() < 10) continue;
                LocalDate pub = LocalDate.parse(reg.substring(0, 10));
                long diff = Math.abs(pub.toEpochDay() - day.toEpochDay());
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = item;
                }
            }
            if (best != null) {
                return pickPdfUrlFromItem(best)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주간 PDF 파일ID를 찾지 못했습니다."));
            }
            return pickPdfUrlFromItem(list.get(0))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주간 PDF 링크를 찾지 못했습니다."));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주간 목록 조회 실패", e);
        }
    }
}