package com.minsproject.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.minsproject.dto.PopSearchWord;
import com.minsproject.entity.TbSchWord;
import com.minsproject.entity.TbSchWordRepository;

@Controller
public class MinsController {
	@Autowired
	TbSchWordRepository schWordRepository;
	
	@Autowired
	DataSource dataSource; 
	
	@GetMapping("search")
	public String search(HttpServletRequest request, Model model) throws Exception {
		
		//검색어 이력 등록
		String searchWord = request.getParameter("query");
		if(searchWord != null && !searchWord.isEmpty()) {
			this.setSearchHistory(searchWord);
		}
		
		String exception_yn = request.getParameter("exception_yn");
		
		//블로그 검색 API 호출
		try {
			// 블로그 검색 API 호출 (다음카카오)
			ResponseEntity<String> rslt = this.blogSearchKakao(request);
			model.addAttribute("search_result", rslt);
			model.addAttribute("search_API", "Daum Kakao API");
			
			//Test Code - 예외를 일으켜 네이버 검색 API를 호추하도록 한다.
			if("Y".equals(exception_yn)) {
				throw new RuntimeException("다음카카오 API 예외발생!");
			}
			
		}catch(Exception e) {
			//다음카카오 블로그 검색 API 오류일 경우 네이버 검색 API 호출
			ResponseEntity<String> rslt = this.blogSearchNaver(request);
			model.addAttribute("search_result", rslt);
			model.addAttribute("search_API", "Naver API");
		}
		
		//인기 검색어 조회 
		ResponseEntity<String> rslt = this.getPopSearchWordList();
		model.addAttribute("pop_search_word_result", rslt);

		return "search";
	}
	

	/**
	 * 블로그 검색 API (다음카카오)
	 * @param request
	 * @return
	 * @throws Exception
	 */
	private ResponseEntity<String> blogSearchKakao(HttpServletRequest request) throws Exception {

		//파라미터 세팅
		String query = request.getParameter("query");	//검색어
		String sort = request.getParameter("sort");		//정렬방법
		
		//page null일 경우 Default 값 1 세팅
		int page = 0;
		if(request.getParameter("page") == null || request.getParameter("page").isEmpty()) {
			page = 1;													
		}else {
			page = Integer.parseInt(request.getParameter("page"));	
		}

		//page size null일 경우 Default 값 10 세팅
		int size = 0;
		if(request.getParameter("size") == null || request.getParameter("size").isEmpty()) {
			size = 10;													
		}else {
			size = Integer.parseInt(request.getParameter("size"));
		}
		
		//검색어 필수 입력 체크
		if(query.isEmpty()) {
			throw new RuntimeException("검색어 필수 입력사항");
		}
		
		//카카오 REST API 호출 
		RestTemplate rest =  new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String appKey = "KakaoAK 9bc5959aa19e4bd6ba399e04c44c2bed";
		headers.set("Authorization", appKey);

		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
		
		//검색어 인코딩
		String encodeKeyword = "";
		try {
			encodeKeyword = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("검색어 인코딩 실패", e);
        }
		
		//URI 조립
		String etc = "&sort=" + sort + "&page=" + page + "&size=" + size;
		String rawURI = "https://dapi.kakao.com/v2/search/blog?query=" + encodeKeyword + etc;

		URI uri = new URI(rawURI);

		ResponseEntity<String> res = rest.exchange(uri, HttpMethod.GET, entity, String.class);

		return res;
	}
	
	
	//검색어 이력 등록
	private void setSearchHistory(String searchWord) throws Exception {
		//현재날짜 조회
		LocalDate now = LocalDate.now();
		String curDate = now.toString().replace("-", "");
				
		schWordRepository.save(TbSchWord.builder()
                .schDate(curDate)
                .schWord(searchWord)
                .build());
	}
	
	//인기 검색어 조회
	private ResponseEntity<String> getPopSearchWordList() throws Exception {
		PopSearchWord popSearchWord = new PopSearchWord();
		List<PopSearchWord> popSearchWordList = new ArrayList<PopSearchWord>();
		
		Connection con = dataSource.getConnection();
		
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT SCH_WORD, CNT ");
		sql.append("  FROM (SELECT SCH_WORD, CNT ");
		sql.append("          FROM (SELECT SCH_WORD, COUNT(SCH_WORD) AS CNT ");
		sql.append("                  FROM TB_SCH_WORD ");
		sql.append("                 GROUP BY SCH_WORD) ");
		sql.append("         ORDER BY CNT DESC) ");
		sql.append(" WHERE ROWNUM <= 10 ");
		
		PreparedStatement ps  = con.prepareStatement(sql.toString());
		ResultSet rs = null;
		
		try {	
			rs = ps.executeQuery();
			
			while(rs.next()){
				popSearchWord = new PopSearchWord();
				
				popSearchWord.setSchWord(rs.getString("SCH_WORD"));
				popSearchWord.setCnt(rs.getInt("CNT"));
				
				popSearchWordList.add(popSearchWord);
			}
			
			ps.close();
		    rs.close();
		    con.close();
		} catch (Exception e) {
            throw new RuntimeException("인기검색어 조회 오류", e);
        }finally {
			ps.close();
		    rs.close();
		    con.close();
		}
		
		ResponseEntity<String> rslt = null;
		
		if(popSearchWordList.size() > 0) {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			String jsonString = objectMapper.writeValueAsString(popSearchWordList);
			
			rslt = new ResponseEntity<String>(jsonString, null, HttpStatus.valueOf(200));
		}
		
		return rslt;
	}
	
	/**
	 * 블로그 검색 API (네이버)
	 * @param request
	 * @return
	 * @throws Exception
	 */
	private ResponseEntity<String> blogSearchNaver(HttpServletRequest request) throws Exception {
		//애플리케이션 클라이언트 정보 세팅
		String clientId = "pwEVNO3LbWASFeo_KoA7"; //애플리케이션 클라이언트 아이디
        String clientSecret = "0E66lgde4b"; //애플리케이션 클라이언트 시크릿
        
        //파라미터 세팅
        String query = request.getParameter("query");	//검색어
      
        //page null일 경우 Default 값 1 세팅
		int page = 0;
		if(request.getParameter("page") == null || request.getParameter("page").isEmpty()) {
			page = 1;													
		}else {
			page = Integer.parseInt(request.getParameter("page"));	
		}
	
		//page size null일 경우 Default 값 10 세팅
		int size = 0;
		if(request.getParameter("size") == null || request.getParameter("size").isEmpty()) {
			size = 10;													
		}else {
			size = Integer.parseInt(request.getParameter("size"));
		}
		
		//검색어 필수 입력 체크
		if(query.isEmpty()) {
			throw new RuntimeException("검색어 필수 입력사항");
		}

		//검색어 인코딩
        String text = null;
        try {
            text = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("검색어 인코딩 실패", e);
        }
        
        //URL 조립
        String etc = "&start=" + page + "&display=" + size;
        String apiURL = "https://openapi.naver.com/v1/search/blog?query=" + text + etc;    // JSON 결과
		
        //헤더 조립
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);
        
        //네이버 API 호출
        String responseBody = getNaverApi(apiURL,requestHeaders);
		
        //ResponseEntity 객체에 결과 세팅
		ResponseEntity<String> rslt = new ResponseEntity<String>(responseBody, null, HttpStatus.valueOf(200));
		
		return rslt;
	}
	
	private static String getNaverApi(String apiUrl, Map<String, String> requestHeaders){
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 오류 발생
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }
	
	private static HttpURLConnection connect(String apiUrl){
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection)url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }
	
	private static String readBody(InputStream body){
        InputStreamReader streamReader = new InputStreamReader(body);

        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();

            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }

            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는 데 실패했습니다.", e);
        }
    }
}
