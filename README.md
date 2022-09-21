# Example of blog search using REST API


## Environment

1. JDK - Java SE Development Kit 17.0.4.1

2. IDE - Eclipse 2022-09 (4.25.0)

3. DB - H2 2.1.214 (In Memory DB)


## Run

java -Dfile.encoding=UTF-8 -jar SunSquare98_Release_Version_1.2.jar

※ 주의 인코딩 문제 발생할 수 있으므로 위 스크립트로 실행한다.


[Download jar] https://github.com/SunSquare98/Mohave/releases/download/v1.2/SunSquare98_Release_Version_1.2.jar


## Test

1. http://localhost:8080/ 접속

2. 검색어, 정렬방법, 페이지 입력 후 search 버튼 클릭


## External Libraries and Open Source

1. Lombok - get/set 자동생성 코드를 위해 사용

2. Jackson - JSON <-> Object 변환을 위해 사용

3. 다음 카카오 REST API

  - https://developers.kakao.com/docs/latest/ko/daum-search/dev-guide#search-blog
  
4. 네이버 REST API 예제

  - https://developers.naver.com/docs/serviceapi/search/blog/blog.md#java
  

## License

SunSquare98 © Min
