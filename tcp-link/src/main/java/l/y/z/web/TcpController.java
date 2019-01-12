package l.y.z.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * author: liuyazong <br>
 * datetime: 2019-01-12 10:52 <br>
 * <p></p>
 */
@Slf4j
@RestController
@RequestMapping("tcp")
public class TcpController {

    @RequestMapping
    public ResponseEntity<Integer> test(HttpServletRequest request) {

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String nextElement = headerNames.nextElement();
            Enumeration<String> headers = request.getHeaders(nextElement);
            while (headers.hasMoreElements()) {
                log.info("request header --->> {}: {}", nextElement, headers.nextElement());
            }
        }
        ResponseEntity<Integer> response = ResponseEntity
                .status(200)
//                .header(HttpHeaders.CONNECTION, "close")
                .build();
        HttpHeaders headers = response.getHeaders();
        headers.forEach((key, value) -> log.info("response header --->> {}: {}", key, value));
        return response;
    }

}
