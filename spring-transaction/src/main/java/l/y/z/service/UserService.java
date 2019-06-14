package l.y.z.service;

import l.y.z.dao.UserDao;
import l.y.z.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class UserService {

    @Autowired
    private ExecutorService executorService;
    @Autowired
    private UserDao userDao;

    @Transactional
    public void test() {

        User user = new User();
        user.setMobile("11111111111");
        Integer insert = userDao.insert(user);
        if (insert > 0) {
        } else {
            user = userDao.selectByMobile(user.getMobile());
        }
        log.info("user: {}", user);
    }

}
