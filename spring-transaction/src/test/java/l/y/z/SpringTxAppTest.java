package l.y.z;

import l.y.z.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class SpringTxAppTest {

    @Autowired
    private UserService userService;


    @Test
    public void contextLoad() {
        userService.test();
    }
}
