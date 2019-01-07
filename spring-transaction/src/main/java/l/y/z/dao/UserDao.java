package l.y.z.dao;

import l.y.z.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDao {
    @Select("select * from user where id = #{id};")
    User select(Integer id);

    @Select("select * from user where mobile = #{mobile};")
    User selectByMobile(String mobile);

    @Insert("insert ignore into user(mobile,password,salt) values(#{mobile},#{password},#{salt});")
    Integer insert(User user);

}
