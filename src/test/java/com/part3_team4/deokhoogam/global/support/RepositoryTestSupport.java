package com.part3_team4.deokhoogam.global.support;

import com.part3_team4.deokhoogam.global.config.QuerydslTestConfig;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(QuerydslTestConfig.class)
public abstract class RepositoryTestSupport {
}
