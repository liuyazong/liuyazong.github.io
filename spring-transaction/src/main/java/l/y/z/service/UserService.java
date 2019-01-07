package l.y.z.service;

import l.y.z.dao.UserDao;
import l.y.z.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserDao userDao;

    @Transactional
    public void test() {
        User user = new User();
        user.setMobile("1111111111");
        user.setPassword("1");
        user.setSalt("1");
        Integer insert = userDao.insert(user);
        if (1 != insert) {
            user = userDao.selectByMobile(user.getMobile());
        } else {
            user = userDao.select(1);
        }
        log.info("user: {}", user);
    }

}