package l.y.z.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * author: liuyazong <br>
 * datetime: 2019-01-12 10:52 <br>
 * <p></p>
 */
@RestController
@RequestMapping("tcp")
public class TcpController {

    @RequestMapping
    public ResponseEntity<Integer> keepAlive() {
        return ResponseEntity.ok(1);
    }

}
