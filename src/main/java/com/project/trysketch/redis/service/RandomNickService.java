package com.project.trysketch.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.HashMap;

@Service
public class RandomNickService {
    // 참고 링크 : https://thalals.tistory.com/266

    public String getData(String url) throws JsonProcessingException {
        // Spring restTemplate 방식으로 외부 API 호출
        // 스프링에서 제공하는 http 통신에 유용하게 사용할 수 있는 템플릿
        HashMap<String, Object> result = new HashMap<String, Object>();
        String jsonInString = "";

        // restTemplate, HttpHeaders 객체 생성
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders header = new HttpHeaders();

        // HttpEntity 는 HTTP 요청 또는 응답에 해당하는 HttpHeader 와 HttpBody 를 포함하는 클래스
        HttpEntity<?> entity = new HttpEntity<>(header);

        // URI 를 동적으로 생성해주는 클래스,
        UriComponents uri = UriComponentsBuilder.fromHttpUrl(url).build();

        // exchange 를 이용해서 HTTP 헤더를 새로 만든다.
        ResponseEntity<?> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, Object.class);

        result.put("statusCode", resultMap.getStatusCodeValue());       // http status code 를 확인
        result.put("header", resultMap.getHeaders());                   // 헤더 정보 확인
        result.put("body", resultMap.getBody());                        // 실제 데이터 정보 확인

        // 데이터를 제대로 전달 받았는지 확인 string 형태로 파싱해줌
        // writeValueAsString 는 Java 오브젝트로 부터 JSON 을 만들고 이를 문자열로 반환한다.
        // ObjectMapper 를 사용한 이유는 JSON 컨텐츠를 Java 객체로 직, 역직렬화 하기 위해서 사용
        ObjectMapper mapper = new ObjectMapper();
        jsonInString = mapper.writeValueAsString(resultMap.getBody());

        return jsonInString;
    }

/*    public ResponseEntity<Object> getData(String url) {
        // Spring restTemplate
        HashMap<String, Object> result = new HashMap<String, Object>();

        // 200 을 쓴 이유는 ResponseEntity 는 상태코드를 필수로 받아야 하기 때문에 200 코드를 빈 엔티티에도 추가해줌
        ResponseEntity<Object> resultMap = new ResponseEntity<>(null,null,200);

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders header = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(header);

            UriComponents uri = UriComponentsBuilder.fromHttpUrl(url).build();

            resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, Object.class);

            result.put("statusCode", resultMap.getStatusCodeValue()); //http status code를 확인
            result.put("header", resultMap.getHeaders()); //헤더 정보 확인
            result.put("words", resultMap.getBody()); //실제 데이터 정보 확인

            //에러처리해야댐
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            result.put("statusCode", e.getRawStatusCode());
            result.put("body"  , e.getStatusText());
            System.out.println("error");
            System.out.println(e.toString());

            return resultMap;
        }
        catch (Exception e) {
            result.put("statusCode", "999");
            result.put("body"  , "exception 오류");
            System.out.println(e.toString());

            return resultMap;

        }

        return resultMap;


    }*/
}
