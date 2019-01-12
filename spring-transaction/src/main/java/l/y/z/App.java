package l.y.z;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@MapperScan(basePackages = {"l.y.z"})
@EnableTransactionManagement
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(App.class, args);

        /*String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for(String beanName: beanDefinitionNames){
            log.info("{}--->>{}",beanName,applicationContext.getBean(beanName).getClass());
        }*/
    }
}
