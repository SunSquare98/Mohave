package com.minsproject.controller;


import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
import com.minsproject.entity.SchWord;
import com.minsproject.entity.SchWordRepository;


@Controller
public class MinsController {
	@Autowired
	SchWordRepository schWordRepository;
	
	@Autowired
	DataSource dataSource; 
	
	@GetMapping("search")
	public String search(HttpServletRequest request, Model model) throws Exception {
		
		//검색어 이력 등록
		String searchWord = request.getParameter("query");
		if(searchWord != null && !searchWord.isEmpty()) {
			this.setSearchHistory(searchWord);
		}
		
		//블로그 검색 API 호출
		try {
			ResponseEntity<String> rslt = this.blogSearchKakao(request);
			model.addAttribute("search_result", rslt);
		}catch(Exception e) {
			//TODO 카카오 API 오류일 경우 네이버 검색 API 호출
		}
		
		//인기 검색어 조회 
		List<PopSearchWord> popSearchWordList = this.getPopSearchWordList();
		
		if(popSearchWordList.size() > 0) {
			
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			String jsonString = objectMapper.writeValueAsString(popSearchWordList);
			
			ResponseEntity<String> rslt = new ResponseEntity<String>(jsonString, null, HttpStatus.valueOf(200));
			model.addAttribute("pop_search_word_result", rslt);
		}

		return "search";
	}

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
			Exception ex = new Exception("검색어 필수 입력사항");
			throw ex;
		}
		
		//카카오 REST API 호출 
		RestTemplate rest =  new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String appKey = "KakaoAK 9bc5959aa19e4bd6ba399e04c44c2bed";
		headers.set("Authorization", appKey);

		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
		String encodeKeyword = URLEncoder.encode(query, "UTF-8");
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
				
		schWordRepository.save(SchWord.builder()
                .schDate(curDate)
                .schWord(searchWord)
                .build());
	}
	
	//인기 검색어 조회
	private List<PopSearchWord> getPopSearchWordList() throws Exception {
		PopSearchWord popSearchWord = new PopSearchWord();
		List<PopSearchWord> popSearchWordList = new ArrayList<PopSearchWord>();
		
		Connection connection = dataSource.getConnection();
		
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT SCH_WORD, CNT ");
		sql.append("  FROM (SELECT SCH_WORD, CNT ");
		sql.append("          FROM (SELECT SCH_WORD, COUNT(SCH_WORD) AS CNT ");
		sql.append("                  FROM SCH_WORD ");
		sql.append("                 GROUP BY SCH_WORD) ");
		sql.append("         ORDER BY CNT DESC) ");
		sql.append(" WHERE ROWNUM <= 10 ");
		
		PreparedStatement ps  = connection.prepareStatement(sql.toString());
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
		    connection.close();
		}finally {
			ps.close();
		    rs.close();
		    connection.close();
		}
		
		return popSearchWordList;
		
	}

}
