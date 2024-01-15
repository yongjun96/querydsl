package jpa.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jpa.querydsl.Entity.Member;
import jpa.querydsl.Entity.QMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;


@Transactional
@SpringBootTest
@Rollback(false)
class QueryDslApplicationTests {

    //@Autowired
    @PersistenceContext
    EntityManager em;

    @Test
    void contextLoads() {

        Member member = new Member("user1", 10);
        em.persist(member);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QMember qMember = QMember.member;

        //이게 queryDsl
        Member result = query
                .selectFrom(qMember)
                .fetchOne();

        assertThat(result).isEqualTo(member);
        assertThat(result.getId()).isEqualTo(member.getId());

    }

}
